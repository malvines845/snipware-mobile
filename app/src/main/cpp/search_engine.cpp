#include <jni.h>
#include <string>
#include <vector>
#include <algorithm>
#include <cctype>
#include <mutex>
#include <unordered_map>
#include <unordered_set>

// ═══════════════════════════════════════════════════════════════
// search_engine.cpp
//
// Self-contained search engine: normalize -> n-gram index lookup ->
// candidates -> exact/fuzzy scoring -> rank. Room stays the single
// source of truth for persistence; this is a rebuildable in-memory
// CACHE, hydrated once from Room at startup and kept incrementally in
// sync via upsert()/remove() on every write from then on (never
// rebuilt per-search -- see SnippetRepository.ensureSearchIndexHydrated()
// on the Kotlin side).
//
// IMPORTANT: this engine does NOT retain snippets' raw code text. Only
// title/tags/language (small strings) are stored verbatim; code is
// reduced to its trigram signature at upsert() time and the original
// text is discarded. Retaining full code bodies here would make this a
// second copy of every snippet's content living in native memory --
// this is a search INDEX, not a second database. The trade-off: code
// matching (the fallback signal used only when title/tags/language all
// miss) is trigram-overlap-based rather than exact substring/subsequence/
// edit-distance -- close enough for "is this worth surfacing", not a
// promise of identical-to-the-original-app code-fallback behavior.
//
// Query-length handling: an n-gram index has nothing useful to narrow
// against for very short queries.
//   length 0-1: no productive index lookup exists (near everything
//               would match a single character regardless of strategy)
//               -- falls back to considering every document, which is
//               genuinely cheap at this length (editDistance against
//               ~1 character is trivial even summed across a library).
//   length 2:   bigram index (title/tags/language only -- see below)
//   length 3+:  trigram index (title/tags/language + code)
// Bigrams intentionally don't cover code: a 2-character query is far
// more likely hunting for a short title/tag/language token than
// "search inside code", and skipping it keeps removeLocked() able to
// exactly reverse everything upsert() indexed without needing code's
// raw text back (see the comment on removeLocked()).
// ═══════════════════════════════════════════════════════════════

namespace {

std::string toLowerAscii(const std::string& s) {
    std::string out = s;
    std::transform(out.begin(), out.end(), out.begin(),
                    [](unsigned char c) { return static_cast<char>(std::tolower(c)); });
    return out;
}

std::vector<std::string> splitWhitespace(const std::string& s) {
    std::vector<std::string> words;
    std::string current;
    for (char c : s) {
        if (std::isspace(static_cast<unsigned char>(c))) {
            if (!current.empty()) {
                words.push_back(current);
                current.clear();
            }
        } else {
            current += c;
        }
    }
    if (!current.empty()) words.push_back(current);
    return words;
}

int editDistance(const std::string& a, const std::string& b) {
    const size_t m = a.size();
    const size_t n = b.size();
    if (m == 0) return static_cast<int>(n);
    if (n == 0) return static_cast<int>(m);

    std::vector<int> prev(n + 1);
    std::vector<int> curr(n + 1);
    for (size_t j = 0; j <= n; j++) prev[j] = static_cast<int>(j);

    for (size_t i = 1; i <= m; i++) {
        curr[0] = static_cast<int>(i);
        for (size_t j = 1; j <= n; j++) {
            if (a[i - 1] == b[j - 1]) {
                curr[j] = prev[j - 1];
            } else {
                curr[j] = 1 + std::min({prev[j], curr[j - 1], prev[j - 1]});
            }
        }
        std::swap(prev, curr);
    }
    return prev[n];
}

/** 3 = substring, 2 = subsequence, 1 = one word within edit-distance tolerance, 0 = no match. */
int fuzzyScore(const std::string& query, const std::string& text) {
    if (text.empty()) return 0;

    const std::string q = toLowerAscii(query);
    const std::string t = toLowerAscii(text);

    if (t.find(q) != std::string::npos) return 3;

    size_t qi = 0;
    for (size_t i = 0; i < t.size() && qi < q.size(); i++) {
        if (t[i] == q[qi]) qi++;
    }
    if (qi == q.size()) return 2;

    const int tolerance = q.size() <= 4 ? 1 : 2;
    for (const auto& word : splitWhitespace(t)) {
        if (editDistance(q, word) <= tolerance) return 1;
    }
    return 0;
}

/** Overlapping n-char grams of an already-lowercased string; short strings become one gram (themselves). */
std::vector<std::string> ngrams(const std::string& lowered, size_t n) {
    std::vector<std::string> out;
    if (lowered.size() < n) {
        if (!lowered.empty()) out.push_back(lowered);
        return out;
    }
    out.reserve(lowered.size() - n + 1);
    for (size_t i = 0; i + n <= lowered.size(); i++) {
        out.push_back(lowered.substr(i, n));
    }
    return out;
}

std::vector<std::string> bigrams(const std::string& lowered) { return ngrams(lowered, 2); }
std::vector<std::string> trigrams(const std::string& lowered) { return ngrams(lowered, 3); }

using Postings = std::unordered_map<std::string, std::unordered_set<std::string>>;

void indexGrams(const std::string& id, const std::vector<std::string>& grams, Postings& postings) {
    std::unordered_set<std::string> unique(grams.begin(), grams.end());
    for (const auto& g : unique) postings[g].insert(id);
}

void eraseOne(const std::string& id, const std::string& gram, Postings& postings) {
    auto pit = postings.find(gram);
    if (pit == postings.end()) return;
    pit->second.erase(id);
    if (pit->second.empty()) postings.erase(pit);
}

void eraseGrams(const std::string& id, const std::vector<std::string>& grams, Postings& postings) {
    for (const auto& g : grams) eraseOne(id, g, postings);
}

std::unordered_set<std::string> lookupPostings(const std::vector<std::string>& grams, const Postings& postings) {
    std::unordered_set<std::string> ids;
    for (const auto& g : grams) {
        auto it = postings.find(g);
        if (it == postings.end()) continue;
        ids.insert(it->second.begin(), it->second.end());
    }
    return ids;
}

/**
 * What the engine actually retains per snippet: title/tags/language
 * verbatim (small; needed for fuzzyScore's exact substring/subsequence
 * semantics), and ONLY the trigram signature of code -- never the code
 * text itself. See the file-level comment for why.
 */
struct SearchDocument {
    std::string id;
    std::string title;
    std::string tags;
    std::string language;
    std::unordered_set<std::string> codeTrigrams;
};

/** Approximates fuzzyScore()'s 0-3 scale from trigram overlap, since raw code isn't retained to score exactly. */
int codeTrigramScore(const std::vector<std::string>& queryTrigrams, const std::unordered_set<std::string>& codeGrams) {
    if (queryTrigrams.empty() || codeGrams.empty()) return 0;
    int overlap = 0;
    for (const auto& g : queryTrigrams) {
        if (codeGrams.count(g)) overlap++;
    }
    if (overlap == 0) return 0;
    const double ratio = static_cast<double>(overlap) / static_cast<double>(queryTrigrams.size());
    if (ratio >= 0.66) return 3;
    if (ratio >= 0.33) return 2;
    return 1;
}

/** title*8 + tags*4 + language*3, code*1 only as a fallback when those are all 0. */
int documentScore(const std::string& query, const std::vector<std::string>& queryTrigrams, const SearchDocument& doc) {
    const int ts = fuzzyScore(query, doc.title) * 8;
    const int gs = fuzzyScore(query, doc.tags) * 4;
    const int ls = fuzzyScore(query, doc.language) * 3;
    const int cs = (ts + gs + ls == 0) ? codeTrigramScore(queryTrigrams, doc.codeTrigrams) : 0;
    return ts + gs + ls + cs;
}

/**
 * id -> SearchDocument, plus two n-gram inverted indexes used purely for
 * fast candidate narrowing (never for final scoring). Guarded by a mutex
 * since Kotlin can reach this from more than one coroutine/thread.
 */
class SearchEngine {
public:
    void upsert(const std::string& id, const std::string& title, const std::string& tags,
                const std::string& language, const std::string& code) {
        std::lock_guard<std::mutex> lock(mutex_);
        removeLocked(id);

        SearchDocument doc;
        doc.id = id;
        doc.title = title;
        doc.tags = tags;
        doc.language = language;

        const std::string titleTagsLang =
                toLowerAscii(title) + " " + toLowerAscii(tags) + " " + toLowerAscii(language);

        indexGrams(id, bigrams(titleTagsLang), bigramPostings_);
        indexGrams(id, trigrams(titleTagsLang), trigramPostings_);

        const auto codeGramsVec = trigrams(toLowerAscii(code));
        doc.codeTrigrams.insert(codeGramsVec.begin(), codeGramsVec.end());
        for (const auto& g : doc.codeTrigrams) trigramPostings_[g].insert(id);

        documents_[id] = std::move(doc);
    }

    void remove(const std::string& id) {
        std::lock_guard<std::mutex> lock(mutex_);
        removeLocked(id);
    }

    void clear() {
        std::lock_guard<std::mutex> lock(mutex_);
        documents_.clear();
        bigramPostings_.clear();
        trigramPostings_.clear();
    }

    /** Snippet IDs ranked by documentScore, descending, score > 0 only. */
    std::vector<std::string> search(const std::string& query) {
        std::lock_guard<std::mutex> lock(mutex_);
        if (documents_.empty()) return {};

        const std::string normQuery = toLowerAscii(query);
        const auto candidateIds = candidatesFor(normQuery);
        const auto queryTrigrams = trigrams(normQuery);

        std::vector<std::pair<std::string, int>> scored;
        scored.reserve(candidateIds.size());
        for (const auto& id : candidateIds) {
            const auto& doc = documents_.at(id);
            const int score = documentScore(query, queryTrigrams, doc);
            if (score > 0) scored.emplace_back(id, score);
        }

        std::sort(scored.begin(), scored.end(),
                  [](const auto& a, const auto& b) { return a.second > b.second; });

        std::vector<std::string> result;
        result.reserve(scored.size());
        for (auto& p : scored) result.push_back(std::move(p.first));
        return result;
    }

private:
    std::unordered_set<std::string> candidatesFor(const std::string& normQuery) const {
        if (normQuery.size() <= 1) {
            std::unordered_set<std::string> all;
            all.reserve(documents_.size());
            for (const auto& kv : documents_) all.insert(kv.first);
            return all;
        }
        if (normQuery.size() == 2) {
            return lookupPostings(bigrams(normQuery), bigramPostings_);
        }
        return lookupPostings(trigrams(normQuery), trigramPostings_);
    }

    /**
     * Caller must hold mutex_. Exactly reverses what upsert() indexed for
     * this id, using only what SearchDocument retained (title/tags/language
     * verbatim + codeTrigrams) -- deliberately never needs code's raw text
     * back, which is the whole point of not storing it.
     */
    void removeLocked(const std::string& id) {
        auto it = documents_.find(id);
        if (it == documents_.end()) return;

        const std::string titleTagsLang = toLowerAscii(it->second.title) + " " +
                                            toLowerAscii(it->second.tags) + " " +
                                            toLowerAscii(it->second.language);
        eraseGrams(id, bigrams(titleTagsLang), bigramPostings_);
        eraseGrams(id, trigrams(titleTagsLang), trigramPostings_);
        for (const auto& g : it->second.codeTrigrams) {
            eraseOne(id, g, trigramPostings_);
        }
        documents_.erase(it);
    }

    std::mutex mutex_;
    std::unordered_map<std::string, SearchDocument> documents_;
    Postings bigramPostings_;
    Postings trigramPostings_;
};

std::string jstringToStd(JNIEnv* env, jstring js) {
    if (js == nullptr) return "";
    const char* chars = env->GetStringUTFChars(js, nullptr);
    std::string result(chars);
    env->ReleaseStringUTFChars(js, chars);
    return result;
}

}  // namespace

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_snipware_app_data_search_SearchEngine_nativeCreate(JNIEnv*, jobject) {
    return reinterpret_cast<jlong>(new SearchEngine());
}

JNIEXPORT void JNICALL
Java_com_snipware_app_data_search_SearchEngine_nativeDestroy(JNIEnv*, jobject, jlong handle) {
    delete reinterpret_cast<SearchEngine*>(handle);
}

JNIEXPORT void JNICALL
Java_com_snipware_app_data_search_SearchEngine_nativeUpsert(
        JNIEnv* env, jobject, jlong handle,
        jstring jId, jstring jTitle, jstring jTags, jstring jLanguage, jstring jCode) {
    reinterpret_cast<SearchEngine*>(handle)->upsert(
        jstringToStd(env, jId), jstringToStd(env, jTitle), jstringToStd(env, jTags),
        jstringToStd(env, jLanguage), jstringToStd(env, jCode));
}

JNIEXPORT void JNICALL
Java_com_snipware_app_data_search_SearchEngine_nativeRemove(
        JNIEnv* env, jobject, jlong handle, jstring jId) {
    reinterpret_cast<SearchEngine*>(handle)->remove(jstringToStd(env, jId));
}

JNIEXPORT void JNICALL
Java_com_snipware_app_data_search_SearchEngine_nativeClear(JNIEnv*, jobject, jlong handle) {
    reinterpret_cast<SearchEngine*>(handle)->clear();
}

JNIEXPORT jobjectArray JNICALL
Java_com_snipware_app_data_search_SearchEngine_nativeSearch(
        JNIEnv* env, jobject, jlong handle, jstring jQuery) {
    auto ids = reinterpret_cast<SearchEngine*>(handle)->search(jstringToStd(env, jQuery));

    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray(static_cast<jsize>(ids.size()), stringClass, nullptr);
    for (size_t i = 0; i < ids.size(); i++) {
        jstring jId = env->NewStringUTF(ids[i].c_str());
        env->SetObjectArrayElement(result, static_cast<jsize>(i), jId);
        env->DeleteLocalRef(jId);
    }
    return result;
}

}  // extern "C"

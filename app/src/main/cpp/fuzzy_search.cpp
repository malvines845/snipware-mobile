#include <jni.h>
#include <string>
#include <vector>
#include <algorithm>
#include <cctype>

// ═══════════════════════════════════════════════════════════════
// fuzzy_search.cpp
//
// Native (C++) port of the fuzzy-search engine from the original web
// app's utils.js (SECTION 9: FUZZY SEARCH). Kept 1:1 in logic with the
// JS version so search results feel identical to users migrating from
// Snipware web -- only the *speed* changes.
//
// Why this lives in C++/NDK instead of Kotlin:
//   Levenshtein distance is O(len(a) * len(b)), and the original code
//   calls it once per word, per snippet, on every keystroke in the
//   search bar. With a large snippet library this is exactly the kind
//   of tight numeric loop that benefits from native code instead of
//   the JVM -- no allocation churn, no GC pressure, predictable
//   cache-friendly access patterns.
//
// Design note: scoring is exposed as ONE batched JNI call
// (nativeScoreSnippets) that scores the *entire* snippet list in a
// single native invocation, rather than one JNI call per snippet.
// Crossing the JNI boundary has fixed per-call overhead; batching
// keeps that overhead constant instead of O(snippet count).
// ═══════════════════════════════════════════════════════════════

namespace {

// Lowercases ASCII letters only. Snippet titles/tags/language/code are
// overwhelmingly ASCII (programming identifiers, English tags), so this
// mirrors JS's toLowerCase() closely enough for search-ranking purposes
// without pulling in a full Unicode case-folding table.
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

/**
 * Levenshtein edit distance. Direct port of editDistance() in utils.js:
 * same rolling prev/curr array technique (O(n) space, O(m*n) time).
 */
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

/**
 * Port of fuzzyScore() in utils.js.
 * Returns: 3 = substring match, 2 = subsequence match,
 *          1 = one word is within edit-distance tolerance, 0 = no match.
 */
int fuzzyScore(const std::string& query, const std::string& text) {
    if (text.empty()) return 0;

    const std::string q = toLowerAscii(query);
    const std::string t = toLowerAscii(text);

    if (t.find(q) != std::string::npos) return 3;

    // Subsequence check: every char of q appears in t, in order.
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

/**
 * Port of snippetScore() in utils.js: weighted relevance across fields.
 * Code is only scored as a fallback when title/tags/language all miss,
 * matching the original's intent of not letting an incidental code match
 * outrank a real title/tag match.
 */
int snippetScore(const std::string& query, const std::string& title,
                  const std::string& tags, const std::string& language,
                  const std::string& code) {
    const int ts = fuzzyScore(query, title) * 8;
    const int gs = fuzzyScore(query, tags) * 4;
    const int ls = fuzzyScore(query, language) * 3;
    const int cs = (ts + gs + ls == 0) ? fuzzyScore(query, code) * 1 : 0;
    return ts + gs + ls + cs;
}

std::string jstringToStd(JNIEnv* env, jstring js) {
    if (js == nullptr) return "";
    const char* chars = env->GetStringUTFChars(js, nullptr);
    std::string result(chars);
    env->ReleaseStringUTFChars(js, chars);
    return result;
}

}  // namespace

extern "C" {

/**
 * Scores every snippet against [query] in one native call and returns a
 * parallel int array of scores (same order/length as the input arrays).
 * Kotlin side (FuzzySearch.kt) filters score > 0 and sorts descending.
 */
JNIEXPORT jintArray JNICALL
Java_com_snipware_app_data_search_FuzzySearch_nativeScoreSnippets(
        JNIEnv* env, jobject /* thiz */,
        jstring jQuery,
        jobjectArray jTitles,
        jobjectArray jTags,
        jobjectArray jLanguages,
        jobjectArray jCodes) {
    const std::string query = jstringToStd(env, jQuery);
    const jsize count = env->GetArrayLength(jTitles);

    std::vector<jint> scores(static_cast<size_t>(count));

    for (jsize i = 0; i < count; i++) {
        auto title = jstringToStd(env, static_cast<jstring>(env->GetObjectArrayElement(jTitles, i)));
        auto tags = jstringToStd(env, static_cast<jstring>(env->GetObjectArrayElement(jTags, i)));
        auto lang = jstringToStd(env, static_cast<jstring>(env->GetObjectArrayElement(jLanguages, i)));
        auto code = jstringToStd(env, static_cast<jstring>(env->GetObjectArrayElement(jCodes, i)));
        scores[static_cast<size_t>(i)] = snippetScore(query, title, tags, lang, code);
    }

    jintArray result = env->NewIntArray(count);
    env->SetIntArrayRegion(result, 0, count, scores.data());
    return result;
}

/**
 * Exposes raw Levenshtein distance for testing / potential direct use
 * (e.g. a future "did you mean" suggestion on tag input).
 */
JNIEXPORT jint JNICALL
Java_com_snipware_app_data_search_FuzzySearch_nativeEditDistance(
        JNIEnv* env, jobject /* thiz */, jstring jA, jstring jB) {
    return editDistance(jstringToStd(env, jA), jstringToStd(env, jB));
}

}  // extern "C"

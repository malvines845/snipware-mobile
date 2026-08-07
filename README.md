# Snipware — Android port

Kotlin + Jetpack Compose port of the Snipware web app, with the fuzzy-search
engine rewritten in C++ (NDK/JNI) for speed. This is a **from-scratch native
rewrite**, not a WebView wrapper — the original `js/` code was read and used
as a functional spec, then re-implemented idiomatically for Android.

## Why C++ for fuzzy search, and nothing else

Search (Levenshtein edit-distance + weighted scoring across title/tags/
language/code) is the one part of the app that's genuinely CPU-bound and
runs on every keystroke against the whole snippet list — a good fit for
native code. Everything else (CRUD, rendering, sync) is ordinary app logic
with no performance case for leaving the JVM, so it's plain Kotlin.

| Concern | Where | Why |
|---|---|---|
| Fuzzy search / scoring | `app/src/main/cpp/fuzzy_search.cpp` | CPU-bound, called on every keystroke — see `FuzzySearch.kt` for the JNI bridge |
| Everything else | Kotlin | No performance case for native; JNI overhead would make small operations *slower* |

`fuzzy_search.cpp` is a line-for-line port of `editDistance()` / `fuzzyScore()`
/ `snippetScore()` from the original `js/core/utils.js`, so ranking behaves
identically to the web app. The JNI bridge (`FuzzySearch.kt` /
`Java_com_snipware_app_data_search_FuzzySearch_nativeScoreSnippets`) scores
the **entire snippet list in one native call** rather than one call per
snippet — JNI boundary crossings have fixed overhead, so batching keeps that
cost constant instead of scaling with library size.

## Architecture

```
data/
  local/        Room entities/DAO/DB — replaces IndexedDB (db.js)
  model/        Domain model (Snippet)
  search/       JNI bridge to fuzzy_search.cpp
  sync/         SyncGateway interface — see "Sync" below
  repository/   Single source of truth combining the above
ui/
  home/         Snippet list: search, language filter, sort
  editor/       Add/edit snippet form (title, language, tags, code)
  viewer/       Full-screen code viewer (Sora-Editor) + Smart Copy
  codeeditor/   Sora-Editor <-> Compose wrapper + TextMate setup
  components/   SnippetCard, SearchBar, LanguageFilterRow, dialogs
  theme/        Colors ported 1:1 from the web app's CSS custom properties
util/           PlaceholderUtils ({{name}}), LangUtils (colors + detectLang), TimeUtils
```

State flows one way: Room `Flow` → `SnippetRepository` → `ViewModel`
(`StateFlow<UiState>`) → Compose. Nothing above the repository layer knows
whether search is native or sync is real or stubbed.

## What's implemented vs. stubbed

**Implemented:** snippet CRUD, Room persistence, native fuzzy search,
language auto-detect, `{{placeholder}}` extraction + Smart Copy, favorite/
lock/messy flags, real syntax-highlighted code viewer (Sora-Editor +
TextMate), inline code editing.

**Stubbed on purpose, per your call to hold off on sync this round:**
`data/sync/SyncGateway.kt` defines the full interface (`queueUpsert`,
`pull`, `login`, realtime — mapped from `sync.js`) with a `NoOpSyncGateway`
behind it, so the app runs fully offline-first right now. Swap it for a
Supabase implementation in `AppContainer.kt` — the repository, ViewModels,
and UI never touch `SyncGateway` directly, so nothing else changes. The doc
comment on the interface lists exactly which `sync.js` behaviors to match.

**Not ported (flag if you want these next):** the Gemini-backed AI assistant
feature (`assistant.js`) and JSON import/export (`io.js`) — both are
self-contained enough to add later without touching what's here.

## Before this builds: syntax-highlighting grammar files

The code viewer uses Sora-Editor's TextMate engine for real syntax
highlighting (same idea as the Prism.js grammars in the original app). That
needs grammar/theme JSON assets that couldn't be fetched into this project
(no network access in the environment this was built in). The app **will
still compile and run** without them — `TextMateSetup.kt` fails soft and
falls back to plain, unhighlighted text — but to get colors:

1. Grab `languages.json` + the `.tmLanguage.json` files you want from
   [Rosemoe/sora-editor](https://github.com/Rosemoe/sora-editor)'s own demo
   app: `app/src/main/assets/textmate/`. It already bundles grammars
   covering nearly all of `LangUtils.LANGUAGES` (Java, Kotlin, Python, JS,
   TS, HTML, CSS, etc.).
2. Grab a theme file too (e.g. `darcula.json`).
3. Copy them into this project's `app/src/main/assets/textmate/`, matching
   the paths `TextMateSetup.kt` expects (`textmate/languages.json`,
   `textmate/snipware-dark.json` — rename whichever theme you pick, or edit
   `THEME_ASSET_PATH`).

`TextMateSetup.kt` is flagged in its own doc comment as the most
library-version-sensitive file in the project (registry class paths can
shift between Sora-Editor releases) — if something doesn't compile there,
check the current guide at
https://project-sora.github.io/sora-editor-docs/guide/using-language.

## Fonts (optional, for pixel-perfect parity)

The web app uses **Syne** (UI) and **JetBrains Mono** (code). Both are
Google Fonts. `ui/theme/Type.kt` currently falls back to the system default
and `FontFamily.Monospace` so the project builds with zero extra assets —
drop matching `.ttf` files under `res/font/` and update `Type.kt` if you
want the exact original look.

## Toolchain

AGP 8.7.3 · Kotlin 2.1.0 · Compose BOM 2024.12.01 · Room 2.6.1 (KSP) ·
Sora-Editor 0.24.4 · minSdk 26 · compileSdk/targetSdk 35 · NDK C++17.

Android Gradle Plugin 9.x (with its new built-in-Kotlin DSL) started
rolling out earlier in 2026; this project intentionally stays on the
well-established 8.x DSL for reliability. Android Studio will offer an
upgrade assistant whenever you're ready to move.

If Android Studio flags any dependency as outdated, that's expected —
accept its suggested bump; nothing here is version-pinned for a deep reason
beyond "known-good and mutually compatible as of this writing."

## CI / GitHub Actions

`.github/workflows/android-build.yml` builds a debug APK on every push/PR to
`main` (adjust the branch name if yours is `master`) and uploads it as a
workflow artifact. Trigger it manually too via the Actions tab → "Android
CI" → "Run workflow".

**This repo doesn't commit a Gradle Wrapper jar** (`gradle/wrapper/gradle-wrapper.jar`,
`gradlew`, `gradlew.bat`) -- the environment this project was generated in
has no network access to fetch the real Gradle distribution/wrapper binary,
so producing one would mean shipping something unverified. The workflow
sidesteps this cleanly by using [`gradle/actions/setup-gradle`](https://github.com/gradle/actions)
to install Gradle 8.9 directly onto the runner and invoking `gradle` instead
of `./gradlew` -- this is an officially supported pattern, not a workaround.

To get a normal local wrapper (nicer for local `./gradlew` use, not required
for CI): open the project in Android Studio and let it offer to
create/repair the wrapper on first sync, or if you have Gradle installed
locally, run `gradle wrapper --gradle-version 8.9` from the project root and
commit the result.

NDK version note: `app/build.gradle.kts` intentionally has no `ndkVersion`
pin, so AGP auto-detects whichever NDK is available -- this avoids the
workflow's installed NDK (r27d) needing to exactly numeric-match a
hardcoded local pin.

## Open in Android Studio

1. Unzip, then **File → Open** and pick the project root (the folder with
   `settings.gradle.kts`).
2. Let Gradle sync — first sync will download the NDK/CMake if not already
   installed (Android Studio prompts for this automatically).
3. Run on a device/emulator. `arm64-v8a` is fastest to build for during
   development — trim `SNIPWARE_ABIS` in `gradle.properties` if you don't
   need the others yet.

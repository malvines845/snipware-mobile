# Keep the JNI bridge class intact: R8 must not rename/strip the class or
# its external (native) methods, or System.loadLibrary's symbol lookup breaks.
-keep class com.snipware.app.data.search.FuzzySearch {
    native <methods>;
}

# Room entities/DAOs are annotation-processed at compile time; nothing extra
# needed for the default setup, but keep generated schema classes just in case.
-keep class com.snipware.app.data.local.** { *; }

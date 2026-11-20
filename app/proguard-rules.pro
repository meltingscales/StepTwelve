# Room and Compose ship their own consumer rules; nothing extra needed yet.

# Never ship log calls in release builds (README: strip logs in release).
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

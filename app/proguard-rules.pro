# ============================================================
# FilmSims ProGuard/R8 Rules
# ============================================================

# --- Kotlin ---
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
# Keep Kotlin coroutines internals
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# --- Data classes (preserve for serialization/deserialization) ---
-keep class com.tqmane.filmsim.data.LutItem { *; }
-keep class com.tqmane.filmsim.data.LutCategory { *; }
-keep class com.tqmane.filmsim.data.LutBrand { *; }
-keep class com.tqmane.filmsim.data.LutGenre { *; }
-keep class com.tqmane.filmsim.util.CubeLUT { *; }
-keep class com.tqmane.filmsim.util.ReleaseInfo { *; }
-keep class com.tqmane.filmsim.util.WatermarkProcessor$WatermarkConfig { *; }
-keep class com.tqmane.filmsim.util.WatermarkProcessor$WatermarkStyle { *; }
-keep class com.tqmane.filmsim.util.AppError { *; }
-keep class com.tqmane.filmsim.util.AppError$* { *; }

# Keep all enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --- OkHttp ---
# OkHttp ships its own ProGuard rules via META-INF; only suppress warnings
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# OkHttp platform adapters
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- OpenGL ES ---
# Framework classes are kept automatically; keep only our renderers
-keep class com.tqmane.filmsim.gl.** { *; }

# --- AndroidX ---
# AndroidX ships its own consumer rules; only suppress warnings
-dontwarn androidx.**

# --- Reflection-based code ---
# Keep classes that use reflection (e.g., Typeface.create with variable fonts)
-keep class android.graphics.Typeface { *; }

# --- ExifInterface ---
-keep class androidx.exifinterface.media.ExifInterface { *; }

# --- Material Components ---
# Ships its own consumer rules
-dontwarn com.google.android.material.**

# --- JSON parsing (org.json) ---
-keep class org.json.** { *; }

# --- ViewModel (AndroidX Lifecycle) ---
# Keep class names for instantiation but ALOW aggressive obfuscation of members for security
-keepnames class * extends androidx.lifecycle.ViewModel
-keepnames class * extends androidx.lifecycle.AndroidViewModel

# --- Security / Anti-Crack ---
# Flatten package hierarchy to make reversing harder
-repackageclasses ""
-keep,allowobfuscation class com.tqmane.filmsim.core.security.** { *; }

# --- Hilt ---
# Hilt/Dagger ships consumer rules; keep only what's needed for reflection
-dontwarn dagger.**
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# --- Firebase / Google Auth ---
# Firebase and Google Play services ship consumer rules; keep only required classes
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn com.google.android.libraries.identity.googleid.**
-keep class androidx.credentials.** { *; }
-dontwarn androidx.credentials.**

# --- General ---
# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom views
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

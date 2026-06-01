# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# remove Log calls from release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** i(...);
}
# Keep the GlobalKt class and its isNotifyDate_ field for reflection
-keep class com.shahin.iran.global.GlobalKt {
    private static kotlinx.coroutines.flow.MutableStateFlow isNotifyDate_;
}

# Keep the MutableStateFlow class and its methods
-keep class kotlinx.coroutines.flow.MutableStateFlow {
    *;
}

# Ensure no obfuscation or shrinking of the MainApplication class
-keep class com.shahin.iran.MainApplication {
    *;
}

# If using other parts of the global package, keep them as needed
-keep class com.shahin.mycalendar.global.** { *; }

# نگه داشتن کل پکیج go
-keep class go.** { *; }
-dontwarn go.**

# نگه داشتن کل پکیج client
-keep class client.** { *; }
-dontwarn client.**

# ==================== Infatica SDK ====================
-keep class com.infatica.agent.** { *; }
-keep interface com.infatica.agent.** { *; }
-keep class com.infatica.** { *; }

# ==================== Traffmonetizer SDK ====================
-keep class com.traffmonetizer.sdk.** { *; }
-keep interface com.traffmonetizer.sdk.** { *; }

# ==================== Fleet SDK ====================
-keep class com.fleet.** { *; }
-keep interface com.fleet.** { *; }

# ==================== General rules for reflection/binding ====================
# Keep all classes that might be accessed via JNI or dynamic binding
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# Keep native method names (if any)
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Parcelable implementations (if used by any SDK)
-keep class * implements android.os.Parcelable {
    public static final ** CREATOR;

}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep all classes in your own service package (if any dynamic reference)
-keep class com.shahin.iran.TraffmonetizerService { *; }
-keep class com.shahin.iran.DateCheckWorker { *; }

-keepclassmembers class com.shahin.iran.global.GlobalKt {
}
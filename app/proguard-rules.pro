# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\RonMad\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

-dontobfuscate
-dontoptimize
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-ignorewarnings

-keepclasseswithmembers public class * {
    public static void main(java.lang.String[]);
}
-keep class il.ronmad.speedruntimer.** { *; }
-keep class kotlin.jvm.functions.**
-keep class kotlin.jvm.internal.**

# Gson
-keep class com.google.gson.**
# -dontwarn sun.misc.**

# Coroutines
-keep class kotlinx.coroutines.**
-keep class kotlin.coroutines.**
-keep class ru.gildor.coroutines.**
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Retrofit
-dontnote retrofit2.Platform
-dontnote okhttp3.internal.platform.**
-dontwarn retrofit2.Platform$Java8
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**

# Guava
-dontwarn com.google.**
-dontwarn javax.inject.**
-dontwarn sun.misc.Unsafe
-dontwarn com.google.common.collect.MinMaxPriorityQueue
-dontwarn java.lang.ClassValue
-dontwarn com.google.j2objc.annotations.Weak
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn afu.org.checkerframework.**
-dontwarn org.checkerframework.**

# Realm
-keep class io.realm.**
-dontwarn javax.**

# ColorPicker
-keep class com.jaredrummler.**

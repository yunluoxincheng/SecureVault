-keep class com.securevault.** { *; }
-keepclassmembers class com.securevault.** { *; }

-keep class com.ionspin.** { *; }
-keepclassmembers class com.ionspin.** { *; }

# JNA (transitive via multiplatform-crypto-libsodium-bindings on Android JVM).
# R8 removes/refactors com.sun.jna.Pointer.peer → UnsatisfiedLinkError: can't obtain peer field ID
-dontwarn com.sun.jna.**
-dontwarn java.awt.**
-keep class com.sun.jna.** { *; }
-keep interface com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.Library {
    <fields>;
    <methods>;
}

-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

-keepclassmembers class **$WhenMappings {
    <fields>;
}

-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.Platform$Java8

# kotlinx.serialization (runtime serializers / descriptors; avoids R8 breaking JSON decode in release)
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    static ** Companion;
}
-if @kotlinx.serialization.internal.NamedCompanion class *
-keepclassmembers class * {
    static <1> *;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-dontnote kotlinx.serialization.**
-dontwarn kotlinx.serialization.internal.ClassValueReferences
-keepclassmembers public class **$$serializer {
    private ** descriptor;
}
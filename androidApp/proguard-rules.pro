-keep class com.securevault.** { *; }
-keepclassmembers class com.securevault.** { *; }

-keep class com.ionspin.** { *; }
-keepclassmembers class com.ionspin.** { *; }

-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

-keepclassmembers class **$WhenMappings {
    <fields>;
}

-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.Platform$Java8
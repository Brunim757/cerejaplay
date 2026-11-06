# Regras do R8 para builds futuros.
-dontwarn org.jetbrains.annotations.**
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.cerejaplay.app.**$$serializer { *; }
-keepclassmembers class com.cerejaplay.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.cerejaplay.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

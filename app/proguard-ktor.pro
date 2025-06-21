# Reglas específicas para Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keepclasseswithmembers class * {
    @io.ktor.client.features.json.* <methods>;
}
-keepclassmembers class * {
    @io.ktor.serialization.* <methods>;
}
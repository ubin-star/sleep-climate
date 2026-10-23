# Add project specific ProGuard rules here.

# Kotlin — preserve metadata for reflection and serialization
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*
-keepattributes Signature

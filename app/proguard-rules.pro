# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
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

# Note that code/simplification/arithmetic must be also disabled if you target lower then Android 2.0
# -optimizations !code/simplification/arithmetic,!code/simplification/cast,!code/simplification/advanced,!field/*,!class/merging/*,!method/removal/parameter,!method/propagation/parameter
# if you are not using "proguard-android-optimize.txt" make below line as comment
-optimizations !code/simplification/cast,!code/simplification/advanced,!field/*,!class/merging/*,!method/removal/parameter,!method/propagation/parameter

# Specifies to repackage all class files that are renamed, by moving them into the single given package.
# Without argument or with an empty string (''), the package is removed completely.
# This option overrides the -flattenpackagehierarchy option.
-repackageclasses ''

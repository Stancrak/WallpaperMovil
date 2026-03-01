# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /tools/proguard/proguard-android.txt

# Keep WallpaperService subclasses
-keepclassmembers class * extends android.service.wallpaper.WallpaperService {
    public *;
}

# Keep ExoPlayer
-keep class androidx.media3.** { *; }

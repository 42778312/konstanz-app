# Konstanz Transit — R8 rules for the release build.
# Libraries bring their own rules (MapLibre, Room, Compose, DataStore); only app-specific ones here.

# Keep line numbers in crash reports (deobfuscate with the mapping.txt from the build).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

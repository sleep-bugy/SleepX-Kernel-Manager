#!/bin/bash
echo "Membersihkan cache build..."
rm -rf app/build/
rm -rf build/
rm -rf .gradle/

echo "Membersihkan cache Gradle..."
./gradlew clean

echo "Melakukan build release yang bersih..."
./gradlew assembleDebug

echo "Build debug selesai!"
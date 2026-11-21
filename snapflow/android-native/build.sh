#!/bin/bash
# SnapFlow AI v2.0 NATIVE — SnapFlow Studio (package com.xaiz.aidownload, cai de len v1.3).
# KHONG WebView, KHONG mo Muse, KHONG mo Chrome: Java thuan goi truc tiep TikWM / FxTwitter / OpenGraph,
# tai bang DownloadManager ve Downloads / SnapFlow AI. Manual build (aapt2 + javac + d8), no Gradle.
set -e
cd "$(dirname "$0")"
SDK=$HOME/Android/Sdk
BT=$SDK/build-tools/34.0.0
ANDROID_JAR=$SDK/platforms/android-34/android.jar
rm -rf build/compiled.zip build/gen build/classes build/dex build/app.apk build/app-aligned.apk build/app-unsigned.apk build/classes.dex build/classes.jar build/sources.txt
mkdir -p build/gen build/classes build/dex

echo "== aapt2 compile =="
"$BT/aapt2" compile --dir res -o build/compiled.zip
echo "== aapt2 link =="
"$BT/aapt2" link -o build/app.apk --manifest AndroidManifest.xml \
  -I "$ANDROID_JAR" --java build/gen \
  --min-sdk-version 26 --target-sdk-version 34 \
  build/compiled.zip
echo "== javac =="
find java build/gen -name "*.java" > build/sources.txt
javac -encoding UTF-8 -source 8 -target 8 -nowarn \
  -cp "$ANDROID_JAR" -d build/classes @build/sources.txt
echo "== d8 =="
jar --create --file build/classes.jar -C build/classes .
"$BT/d8" --min-api 26 --output build/dex build/classes.jar
echo "== assemble =="
cp build/app.apk build/app-unsigned.apk
cp build/dex/classes.dex build/classes.dex
cd build && zip -q app-unsigned.apk classes.dex && cd ..
"$BT/zipalign" -f 4 build/app-unsigned.apk build/app-aligned.apk
echo "== sign (XAIZ keystore) =="
"$BT/apksigner" sign --ks ../xaiz-widget/xaiz.keystore --ks-key-alias xaiz \
  --ks-pass pass:xaiz1234 --key-pass pass:xaiz1234 \
  --out build/SnapFlow-AI-v2.0.apk build/app-aligned.apk
cp build/SnapFlow-AI-v2.0.apk "$HOME/workspace/your_files/SnapFlow-AI-v2.0.apk"
echo "== verify =="
"$BT/aapt2" dump badging build/SnapFlow-AI-v2.0.apk
"$BT/apksigner" verify --verbose build/SnapFlow-AI-v2.0.apk
ls -lh build/SnapFlow-AI-v2.0.apk "$HOME/workspace/your_files/SnapFlow-AI-v2.0.apk"
echo "== no WebView / no Muse check =="
if grep -r -i "WebView\|muse\.ai\|chrome" java res AndroidManifest.xml; then echo "FAIL: con WebView/Muse/Chrome"; exit 1; else echo "OK: khong co WebView, khong co muse.ai, khong co Chrome"; fi

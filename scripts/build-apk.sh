#!/usr/bin/env bash
# Build l'APK Lylo tablette à installer sur le robot.
#
#   ./scripts/build-apk.sh            → build seulement, APK dans dist/
#   ./scripts/build-apk.sh --install  → build + installation via adb
#   ROBOT_IP=192.168.1.42 ./scripts/build-apk.sh --install
#                                      → se connecte d'abord au robot en Wi-Fi (adb connect IP:5555)
#
# Le backend visé est celui de local.properties (LYLO_API_BASE_URL).
# Build « debug » : signé avec la clé de debug, installable directement (le build release
# n'a pas de signature configurée).
set -euo pipefail

cd "$(dirname "$0")/.."

api_url=$(grep -E '^LYLO_API_BASE_URL=' local.properties 2>/dev/null | cut -d= -f2- || true)
echo "▶ Backend ciblé : ${api_url:-valeur par défaut de app/build.gradle.kts}"
echo "▶ Branche       : $(git branch --show-current) ($(git rev-parse --short HEAD))"

./gradlew :app:assembleDebug

mkdir -p dist
name="lylo-tablet-$(git branch --show-current | tr '/' '-')-$(date +%Y%m%d-%H%M).apk"
cp app/build/outputs/apk/debug/app-debug.apk "dist/$name"
echo "✔ APK : $(pwd)/dist/$name"

if [[ "${1:-}" == "--install" ]]; then
  # adb du SDK Android (sdk.dir de local.properties) s'il n'est pas dans le PATH
  ADB=$(command -v adb || true)
  if [[ -z "$ADB" ]]; then
    sdk_dir=$(grep -E '^sdk.dir=' local.properties 2>/dev/null | cut -d= -f2- || true)
    ADB="${sdk_dir:-$HOME/Library/Android/sdk}/platform-tools/adb"
  fi
  [[ -x "$ADB" ]] || { echo "✘ adb introuvable ($ADB) — brew install android-platform-tools"; exit 1; }

  if [[ -n "${ROBOT_IP:-}" ]]; then
    echo "▶ Connexion au robot ${ROBOT_IP}:5555"
    "$ADB" connect "${ROBOT_IP}:5555"
  fi
  "$ADB" devices
  "$ADB" install -r "dist/$name"
  echo "✔ Installé sur l'appareil"
fi

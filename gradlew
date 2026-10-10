#!/bin/sh
# Lightweight bootstrap for CI; downloads Gradle if no wrapper JAR is bundled.
set -eu
GRADLE_VERSION="8.9"
GRADLE_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}/localcam-gradle-$GRADLE_VERSION"
if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  mkdir -p "$(dirname "$GRADLE_HOME")"
  ARCHIVE="${TMPDIR:-/tmp}/gradle-$GRADLE_VERSION-bin.zip"
  curl -fsSL "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ARCHIVE"
  unzip -q -o "$ARCHIVE" -d "$(dirname "$GRADLE_HOME")"
  mv "$(dirname "$GRADLE_HOME")/gradle-$GRADLE_VERSION" "$GRADLE_HOME"
fi
exec "$GRADLE_HOME/bin/gradle" "$@"

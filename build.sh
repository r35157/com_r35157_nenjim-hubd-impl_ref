#!/usr/bin/env bash
set -euo pipefail

export VERSION=$(git describe --tags --exact-match 2>/dev/null \
  || git symbolic-ref --short -q HEAD \
  || git rev-parse --short HEAD)

GITHASH=$(git rev-parse --short=8 HEAD)

export VERSION_LONG=${VERSION}_${GITHASH}

echo "Building 'NenjimHub v${VERSION_LONG}'..."

./gradlew \
    -Pversion=${VERSION} \
    jar \
    prepareLibs
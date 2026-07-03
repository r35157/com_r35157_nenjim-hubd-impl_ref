#!/usr/bin/env bash
set -euo pipefail

export VERSION=$(git describe --tags --exact-match 2>/dev/null \
  || git symbolic-ref --short -q HEAD \
  || git rev-parse --short HEAD)

echo "Publishing artifact to local Maven repo ($VERSION)..."
./gradlew -Pversion=$VERSION publishToMavenLocal

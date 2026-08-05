#!/usr/bin/env bash
set -euo pipefail

shopt -s nullglob

jars=(libs/*.jar)
if (( ${#jars[@]} == 0 )); then
  echo "ERROR: No JARs found in libs/" >&2
  exit 1
fi

CLASSPATH=$(IFS=:; echo "${jars[*]}")

exec java \
  --enable-native-access=javafx.graphics \
  --enable-preview \
  -Dlog4j.configurationFile=conf/log4j2.xml \
  -cp "$CLASSPATH" \
  com.r35157.nenjim.hubd.impl.ref.Main

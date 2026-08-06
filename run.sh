#!/usr/bin/env bash
set -euo pipefail

cd -- "$(dirname -- "${BASH_SOURCE[0]}")"

LOG4J_CONFIG="conf/log4j2.xml"

if [[ ! -r "$LOG4J_CONFIG" || ! -s "$LOG4J_CONFIG" ]]; then
  echo "ERROR: Log4j2 configuration file is missing, unreadable, or empty: $PWD/$LOG4J_CONFIG" >&2
  exit 1
fi

shopt -s nullglob

jars=(libs/*.jar)
if (( ${#jars[@]} == 0 )); then
  echo "ERROR: No JARs found in $PWD/libs/" >&2
  exit 1
fi

CLASSPATH=$(IFS=:; echo "${jars[*]}")

exec java \
  --enable-native-access=javafx.graphics \
  --enable-preview \
  "-Dlog4j.configurationFile=$LOG4J_CONFIG" \
  -cp "$CLASSPATH" \
  com.r35157.nenjim.hubd.impl.ref.Main

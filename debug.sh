#!/usr/bin/env bash
set -euo pipefail

shopt -s nullglob

jars=(libs/*.jar)
if (( ${#jars[@]} == 0 )); then
  echo "ERROR: No JARs found in libs/" >&2
  exit 1
fi

CLASSPATH=$(IFS=:; echo "${jars[*]}")

DEBUG_PORT="${DEBUG_PORT:-5005}"
DEBUG_SUSPEND="${DEBUG_SUSPEND:-y}"

echo "Starting NenjimHub in DEBUG mode on port ${DEBUG_PORT} (suspend=${DEBUG_SUSPEND})"

exec java \
  --enable-preview \
  "-agentlib:jdwp=transport=dt_socket,server=y,suspend=${DEBUG_SUSPEND},address=*:${DEBUG_PORT}" \
  -Dlog4j.configurationFile=conf/log4j2.xml \
  -cp "$CLASSPATH" \
  com.r35157.nenjim.hubd.impl.ref.Main

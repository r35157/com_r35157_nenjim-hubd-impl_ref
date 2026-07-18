#!/usr/bin/env bash

set -euo pipefail

UNSIGNED_TX="/tmp/utx.b64"
SIGNED_TX="/tmp/stx.json"

jup sign \
    -f json \
    --key evelyn-prod \
    --tx "$(tr -d '\r\n' < "${UNSIGNED_TX}")" \
    > "${SIGNED_TX}"

echo "Signed transaction written to: ${SIGNED_TX}"

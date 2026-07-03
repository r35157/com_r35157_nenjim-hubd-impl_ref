#!/usr/bin/env bash
set -euo pipefail

SOURCE="$HOME/projects/com_r35157_nenjim-hubd-impl_ref"
TARGET="$HOME/projects/com_r35157_nenjim-hubd-impl_ref_github_snapshot"

rsync -a --delete \
  --exclude '.git' \
  --exclude 'conf/*.conf' \
  --exclude 'conf/*.xml' \
  "$SOURCE/" \
  "$TARGET/"

cd "$TARGET"
git add -A

if git diff --cached --quiet; then
    echo "No snapshot changes to publish."
    exit 0
fi

git commit -m "Mirror snapshot"
git push

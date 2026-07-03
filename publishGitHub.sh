#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <github-snapshot-branch>" >&2
    exit 1
fi

BRANCH="$1"

SOURCE="$HOME/projects/com_r35157_nenjim-hubd-impl_ref"
TARGET="$HOME/projects/com_r35157_nenjim-hubd-impl_ref_github_snapshot"

cd "$TARGET"

git fetch origin

if git show-ref --verify --quiet "refs/heads/$BRANCH"; then
    git switch "$BRANCH"
elif git show-ref --verify --quiet "refs/remotes/origin/$BRANCH"; then
    git switch --track "origin/$BRANCH"
else
    git switch --create "$BRANCH"
fi

rsync -a --delete \
  --exclude '.git' \
  --exclude 'conf/*.conf' \
  --exclude 'conf/*.xml' \
  "$SOURCE/" \
  "$TARGET/"

git add -A

if git diff --cached --quiet; then
    echo "No snapshot changes to publish on branch '$BRANCH'."
    exit 0
fi

git commit -m "Mirror snapshot"
git push -u origin "$BRANCH"

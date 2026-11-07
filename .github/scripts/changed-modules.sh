#!/usr/bin/env bash
# Prints the Maven -pl argument for modules changed between BASE and HEAD:
#   "ALL"            - shared build files changed (root pom, config, wrapper, CI) or BASE is unknown
#   ""               - nothing buildable changed (e.g. docs only)
#   "mod-a,mod-b"    - top-level modules with changes
set -euo pipefail

base="${1:-}"
head="${2:-HEAD}"

if [[ -z "$base" || "$base" =~ ^0+$ ]] || ! git cat-file -e "$base^{commit}" 2>/dev/null; then
    echo "ALL"
    exit 0
fi

changed="$(git diff --name-only "$base" "$head")"

if grep -qE '^(pom\.xml|config/|\.mvn/|mvnw|\.github/)' <<<"$changed"; then
    echo "ALL"
    exit 0
fi

cut -d/ -f1 <<<"$changed" | sort -u | while read -r dir; do
    if [[ -n "$dir" && -f "$dir/pom.xml" ]]; then
        echo "$dir"
    fi
done | paste -sd, -

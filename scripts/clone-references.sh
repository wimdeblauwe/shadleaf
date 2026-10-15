#!/usr/bin/env bash
# Clones the reference projects listed in AGENTS.md into references/ (git-ignored), or updates existing clones.
set -euo pipefail

cd "$(dirname "$0")/.."
mkdir -p references

clone() {
  local name=$1 url=$2
  if [ -d "references/$name/.git" ]; then
    echo "Updating references/$name"
    git -C "references/$name" pull --ff-only --depth 1
  else
    echo "Cloning $url into references/$name"
    git clone --depth 1 "$url" "references/$name"
  fi
}

clone shadcn-ui https://github.com/shadcn-ui/ui.git
clone basecoat https://github.com/hunvreus/basecoat.git
clone starting-point-ui https://github.com/gufodotdev/starting-point-ui.git
clone blog-example-code https://github.com/wimdeblauwe/blog-example-code.git

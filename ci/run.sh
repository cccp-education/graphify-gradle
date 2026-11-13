#!/usr/bin/env bash
# GBL-013 US-5 (wave 2) — CI contract for graphify-gradle.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

job="${1:-}"
case "$job" in
  fast)
    ( cd "$ROOT/graphify-plugin" && ./gradlew compileKotlin compileTestKotlin --no-daemon --console=plain )
    ;;
  test)
    ( cd "$ROOT/graphify-plugin" && ./gradlew build publishToMavenLocal --no-daemon --console=plain )
    ./gradlew collectAndVerify --no-daemon --console=plain
    ;;
  *)
    printf 'unsupported job for graphify-gradle: %s\n' "${job:-<none>}" >&2
    printf 'supported: fast | test\n' >&2
    exit 2
    ;;
esac

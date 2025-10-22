#!/usr/bin/env bash
set -euo pipefail

# migrate_yarn_to_mojmap_multi.sh
# Two-phase migration for multiple modules:
#   1) generate  -> runs migrateMappings (Yarn -> Mojmap) into remappedSrc for each module
#   2) apply     -> copies remappedSrc into src and removes remappedSrc
#
# Usage:
#   ./migrate_yarn_to_mojmap_multi.sh generate  modules:common modules:fabric modules:neoforge -v 1.21.1
#   ./migrate_yarn_to_mojmap_multi.sh apply     modules:common modules:fabric modules:neoforge
#
# Notes:
#   - During `generate`, each listed module MUST currently use Yarn mappings (NOT Mojmap).
#   - After `apply`, switch each module to Mojmap in Gradle, then build.
#   - Mixins (string targets), reflection, AT/AW, and configs require manual fixes.

ROOT_DIR="$(pwd)"
MC_VER_DEFAULT="1.21.1"

usage() {
  cat <<'USAGE'
Usage:
  migrate_yarn_to_mojmap_multi.sh <generate|apply> <module...> [-v <mc_version>]

Examples:
  ./migrate_yarn_to_mojmap_multi.sh generate modules:common modules:fabric -v 1.21.1
  ./migrate_yarn_to_mojmap_multi.sh apply    modules:common modules:fabric

Description:
  generate : run Loom migrateMappings (Yarn -> Mojmap) into remappedSrc for each module
  apply    : copy remappedSrc into src (java/kotlin) for each module, then delete remappedSrc
USAGE
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1
}

resolve_proj_dir() {
  local mod="$1"
  printf "%s/%s\n" "$ROOT_DIR" "${mod//:/\/}"
}

detect_src_dir() {
  local proj="$1"
  if [[ -d "$proj/src/main/java" ]]; then
    printf "src/main/java"
  elif [[ -d "$proj/src/main/kotlin" ]]; then
    printf "src/main/kotlin"
  else
    # default to Java layout
    mkdir -p "$proj/src/main/java"
    printf "src/main/java"
  fi
}

copy_tree() {
  # args: from_dir to_dir
  local from="$1" to="$2"
  mkdir -p "$to"
  if require_cmd rsync; then
    rsync -a --delete "$from"/ "$to"/
  else
    # delete existing top-level entries, then copy
    find "$to" -mindepth 1 -maxdepth 1 -exec rm -rf {} +
    cp -a "$from"/. "$to"/
  fi
}

MODE="${1:-}"
shift || true

if [[ -z "$MODE" ]]; then
  usage; exit 1
fi

MC_VER="$MC_VER_DEFAULT"
MODULES=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    -v|--version) MC_VER="${2:-}"; shift 2;;
    -h|--help) usage; exit 0;;
    *) MODULES+=("$1"); shift;;
  esac
done

if [[ ${#MODULES[@]} -eq 0 ]]; then
  echo "Error: no modules provided."; usage; exit 1
fi

case "$MODE" in
  generate)
    read -r -p "Confirm ALL listed modules currently use Yarn mappings (NOT Mojmap). Continue? [y/N] " ok
    case "$ok" in y|Y|yes|YES) ;; *) echo "Aborted."; exit 1;; esac

    for MOD in "${MODULES[@]}"; do
      echo "==> GENERATE :: :$MOD  (MC $MC_VER)"
      PROJ_DIR="$(resolve_proj_dir "$MOD")"
      SRC_DIR_REL="$(detect_src_dir "$PROJ_DIR")"
      OUT_DIR="$PROJ_DIR/remappedSrc"

      echo "    Input:  $PROJ_DIR/$SRC_DIR_REL"
      echo "    Output: $OUT_DIR"

      rm -rf "$OUT_DIR"
      ./gradlew ":$MOD:clean" \
                ":$MOD:migrateMappings" \
                --mappings "net.minecraft:mappings:${MC_VER}" \
                --input="$SRC_DIR_REL" \
                --output="remappedSrc"
      echo "    Done."
    done

    echo
    echo "Phase 'generate' complete. Review remappedSrc folders."
    echo "Next: run this script with 'apply' to copy remappedSrc into src for the same modules."
    ;;

  apply)
    for MOD in "${MODULES[@]}"; do
      echo "==> APPLY :: :$MOD"
      PROJ_DIR="$(resolve_proj_dir "$MOD")"
      SRC_DIR_REL="$(detect_src_dir "$PROJ_DIR")"
      OUT_DIR="$PROJ_DIR/remappedSrc"

      if [[ ! -d "$OUT_DIR" ]]; then
        echo "    Skip: $OUT_DIR not found."
        continue
      fi

      echo "    Copy $OUT_DIR -> $PROJ_DIR/$SRC_DIR_REL"
      copy_tree "$OUT_DIR" "$PROJ_DIR/$SRC_DIR_REL"

      echo "    Remove $OUT_DIR"
      rm -rf "$OUT_DIR"
      echo "    Done."
    done

    cat <<'POST'
Apply phase complete.

Now switch each module to Mojmap in Gradle, for example:

dependencies {
    mappings(loom.layered {
        officialMojangMappings()
        // optional:
        // parchment("org.parchmentmc.data:parchment-<mc>:<ver>@zip")
        // mappings("dev.lambdaurora:yalmm:<mc>+build.<ver>")
    })
}

Then build and fix manual spots (Mixins string targets, reflection, AT/AW, configs):
  ./gradlew clean build
POST
    ;;

  *)
    echo "Error: unknown mode '$MODE'"; usage; exit 1;;
esac

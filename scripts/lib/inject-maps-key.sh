#!/usr/bin/env bash
# Shared helper: materialize the Google Maps JavaScript API key as a bundled
# app resource so the GoogleWebMap screenshot test can render a live Google map.
#
# CI exposes the key through the GOOGLE_MAPS_API_KEY environment variable (from
# the repository secret of the same name). When it is set we write it to a
# resource on the hellocodenameone classpath; when it is absent (local builds,
# forks, no secret) we do nothing and the test skips at runtime.
#
# The resource is listed in .gitignore and must never be committed.
#
# Usage: inject_google_maps_key "<repo-root>"
inject_google_maps_key() {
    repo_root="$1"
    key_res="$repo_root/scripts/hellocodenameone/common/src/main/resources/google-maps-key.txt"
    if [ -n "${GOOGLE_MAPS_API_KEY:-}" ]; then
        mkdir -p "$(dirname "$key_res")"
        printf '%s' "$GOOGLE_MAPS_API_KEY" > "$key_res"
        echo "Injected Google Maps key resource ($(wc -c < "$key_res" | tr -d ' ') bytes)"
    else
        echo "GOOGLE_MAPS_API_KEY not set; GoogleWebMap screenshot test will skip"
    fi
}

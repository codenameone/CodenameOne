#!/usr/bin/env bash
set -euo pipefail
if [[ -n "${CI:-}" ]]; then echo 'Native tests are local only.' >&2; exit 1; fi
if [[ "$(uname -s)" != Linux ]]; then echo 'Run on Linux with GTK 3 and Xvfb.' >&2; exit 1; fi
cd "${BASH_SOURCE[0]%/*}"
cn1_input_test_dir=$(mktemp -d "${TMPDIR:-/tmp}/cn1-linux-text-input.XXXXXXXX")
trap 'rm -rf "$cn1_input_test_dir"' EXIT
"${CC:-cc}" -std=c11 -O1 -g -Wall -Wextra -Werror -Wno-unused-parameter \
  -fsanitize=address,undefined -fno-omit-frame-pointer \
  $(pkg-config --cflags gtk+-3.0) LinuxTextInputTest.c \
  $(pkg-config --libs gtk+-3.0) -o "$cn1_input_test_dir/input-tests"
# GTK/fontconfig maintain process-wide allocations. Explicit input lifecycle is
# checked above; leak detection belongs in a non-GTK peer allocation test.
GTK_IM_MODULE=simple ASAN_OPTIONS=detect_leaks=0 xvfb-run -a "$cn1_input_test_dir/input-tests"

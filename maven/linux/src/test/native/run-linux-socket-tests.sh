#!/usr/bin/env bash
# Local only. Compile the actual POSIX bridge with a minimal Java ABI shim.
set -euo pipefail
if [[ -n "${CI:-}" ]]; then echo 'Native tests are local only.' >&2; exit 1; fi
if [[ "$(uname -s)" != Linux ]]; then echo 'Run on Linux.' >&2; exit 1; fi
cd "${BASH_SOURCE[0]%/*}"
cn1_socket_test_dir=$(mktemp -d "${TMPDIR:-/tmp}/cn1-linux-socket-tests.XXXXXXXX")
trap 'rm -rf "$cn1_socket_test_dir"' EXIT
"${CC:-cc}" -std=c11 -O1 -g -Wall -Wextra -Werror -Wno-unused-parameter \
  -fsanitize=address,undefined -fno-omit-frame-pointer -pthread \
  LinuxSocketTest.c -o "$cn1_socket_test_dir/socket-tests"
ASAN_OPTIONS=detect_leaks=1 "$cn1_socket_test_dir/socket-tests"

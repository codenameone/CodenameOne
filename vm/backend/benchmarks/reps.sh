#!/bin/bash
# Repeated measures, INTERLEAVED. A single sample per configuration cannot resolve
# a difference smaller than the run-to-run spread, and on this VM that spread was
# just measured at 23% for one unchanged configuration (169,415 then 137,157).
# Interleaving rather than blocking keeps slow drift (thermal, host load) from
# landing entirely on one mode.
cd /var/tmp/cn1bench
exec 9>/var/tmp/cn1bench/.bench.lock
flock -n 9 || { echo "another benchmark is running" >&2; exit 3; }
for rep in 1 2 3 4; do
  for mode in 0 1 2; do
    pkill -9 -f "^\./bench-cn1$" >/dev/null 2>&1; sleep 1
    PORT=9999 WORKERS=16 CN1_HTTP_ZERO_COPY=$mode CN1_HTTP_TARGET_CACHE=0 \
      taskset -c 0,1 ./bench-cn1 > reps.log 2>&1 &
    pid=$!
    while ! (exec 3<>/dev/tcp/127.0.0.1/9999) 2>/dev/null; do sleep 0.05; done
    podman run --rm --platform linux/arm64 --network host cn1-bench-load -c \
      "taskset -c 2,3 wrk -t2 -c16 -d3s http://127.0.0.1:9999/plaintext" >/dev/null 2>&1
    out=$(podman run --rm --platform linux/arm64 --network host cn1-bench-load -c \
      "taskset -c 2,3 wrk -t2 -c16 -d12s http://127.0.0.1:9999/plaintext" 2>&1)
    kill $pid 2>/dev/null; wait $pid 2>/dev/null
    echo "$out" | awk -v m="$mode" -v r="$rep" '/^Requests\/sec:/ {printf "rep%s mode%s %.0f\n", r, m, $2}'
  done
done

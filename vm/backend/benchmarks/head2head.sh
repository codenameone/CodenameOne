#!/bin/bash
# The headline claim, measured properly: CN1 vs Go on /plaintext at 64 connections,
# interleaved repeated measures with medians and spread. One run per arm cannot
# resolve this (the harness spread is 28-45%); five interleaved can.
cd /var/tmp/cn1bench
exec 9>/var/tmp/cn1bench/.bench.lock
flock -n 9 || { echo "another benchmark is running" >&2; exit 3; }
one() {
  bin="$1"; port="$2"; env="$3"
  pkill -9 -f "^\./bench-cn1$" >/dev/null 2>&1
  pkill -9 -f "^\./bench-go$" >/dev/null 2>&1
  sleep 1
  env PORT=$port $env taskset -c 0,1 ./"$bin" > h2h.log 2>&1 &
  pid=$!
  while ! (exec 3<>/dev/tcp/127.0.0.1/$port) 2>/dev/null; do sleep 0.05; done
  podman run --rm --platform linux/arm64 --network host cn1-bench-load -c \
    "taskset -c 2,3 wrk -t2 -c64 -d4s http://127.0.0.1:$port/plaintext" >/dev/null 2>&1
  out=$(podman run --rm --platform linux/arm64 --network host cn1-bench-load -c \
    "taskset -c 2,3 wrk -t2 -c64 -d15s http://127.0.0.1:$port/plaintext" 2>&1)
  kill $pid 2>/dev/null; wait $pid 2>/dev/null
  echo "$out" | awk '/^Requests\/sec:/ {printf "%.0f", $2}'
}
for rep in 1 2 3 4 5; do
  g=$(one bench-go 9301 "")
  c=$(one bench-cn1 9302 "WORKERS=64 CN1_HTTP_ZERO_COPY=0 CN1_HTTP_TARGET_CACHE=0")
  echo "rep$rep go=$g cn1=$c"
done

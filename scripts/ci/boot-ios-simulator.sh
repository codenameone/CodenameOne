#!/usr/bin/env bash
set -uo pipefail

# Boot a simulator for a CI job, with every simctl call on a deadline.
#
# Usage: scripts/ci/boot-ios-simulator.sh <runtime-regex> <device-type> <name>
#   e.g. scripts/ci/boot-ios-simulator.sh 'iOS-26[0-9-]*' 'iPhone 16' iPhone16-fidelity
#
# Writes the UDID to stdout (and to GITHUB_OUTPUT as udid=... when set); every
# log line goes to stderr, so `UDID="$(boot-ios-simulator.sh ...)"` is safe.
#
# Why this is not four inline `xcrun simctl` lines, which is what it replaces:
# CoreSimulatorService wedges on hosted macOS runners, and when it does, simctl
# does not fail -- it blocks forever with no output. The fidelity job spent 67
# minutes inside `Boot simulator`, hit the 90-minute job timeout, and the run
# was reported as CANCELLED, which reads as "a human stopped it" rather than as
# a failure. It happened on this branch (run 35175449094) and independently on
# master (run 35130437729), at the identical step, and in both the step log was
# completely empty -- so there was not even evidence of WHICH call hung.
#
# Three properties follow from that, and all three are the point of this script:
#
#   1. Every call is bounded. A wedge costs minutes, not the job's whole budget,
#      and it ends as a red failure with a message.
#   2. A call that hits its deadline is SAMPLED before it is killed, so the next
#      occurrence arrives with a stack instead of silence.
#   3. Recovery is one restart of CoreSimulatorService, then one retry.
#
# Two other places in the tree boot a simulator and are NOT guarded:
# scripts/run-ios-ui-tests.sh (its own device-selection flow, which this signature
# does not fit) and scripts/build-ios-native-ref.sh (boots with `|| true`, so a
# wedge hangs it just as silently). Neither has been observed hanging, and neither
# is on the leg that was. Widening this to them is worth doing on its own change,
# where the iOS UI test job can actually be exercised against it.
#
# Point 3 is a retry, which this repo otherwise refuses -- a retried test hides
# the race it exists to catch. It is allowed here because the thing being
# retried is not ours and carries no signal: a hung CoreSimulatorService says
# nothing about the code under test, cannot be fixed from this repository, and
# the remediation (kill the daemon so launchd respawns it clean) is a real fix
# for a real state rather than a re-roll. It is deliberately loud: the recovery
# prints ::warning:: and a second wedge exits 29 rather than trying again.

RUNTIME_PATTERN="${1:?usage: boot-ios-simulator.sh <runtime-regex> <device-type> <name>}"
DEVICE_TYPE="${2:?usage: boot-ios-simulator.sh <runtime-regex> <device-type> <name>}"
DEVICE_NAME="${3:?usage: boot-ios-simulator.sh <runtime-regex> <device-type> <name>}"

# Exit codes. 78 is this workflow's existing "the runner lacks the pinned
# runtime" signal and keeps that meaning; 29 is new and means the simulator
# control plane never answered.
EXIT_NO_RUNTIME=78
EXIT_WEDGED=29

# Per-call deadlines, in seconds. Two attempts must still fit inside the job
# budget with the ~25 minutes of build that precede this step, so the sum is
# kept to 15 minutes: 2 x 15 + 25 is comfortably under the 90-minute job cap.
# Normal timings on a healthy runner are one to two seconds for the list calls
# and well under a minute for the boot.
LIST_TIMEOUT="${CN1_SIM_LIST_TIMEOUT:-120}"
CREATE_TIMEOUT="${CN1_SIM_CREATE_TIMEOUT:-180}"
BOOT_TIMEOUT="${CN1_SIM_BOOT_TIMEOUT:-180}"
BOOTSTATUS_TIMEOUT="${CN1_SIM_BOOTSTATUS_TIMEOUT:-420}"

log() { printf '[boot-sim] %s\n' "$*" >&2; }

# Resolve simctl once and invoke it directly rather than through `xcrun`.
# The deadline below kills a pid, and `xcrun` spawns simctl as a CHILD -- so
# killing xcrun would leave the wedged simctl running (the GitHub runner logged
# exactly that: "Terminate orphan process: pid (43874) (simctl)"). Running the
# real binary makes the pid we watch the pid we need to sample and kill.
SIMCTL="$(xcrun -f simctl 2>/dev/null || true)"
if [ -z "$SIMCTL" ] || [ ! -x "$SIMCTL" ]; then
  echo "::error::xcrun could not resolve simctl; no usable Xcode on this runner." >&2
  exit "$EXIT_WEDGED"
fi

# Ran a command with a deadline. Returns the command's status, or 124 when the
# deadline expired -- the same code GNU timeout uses, which is not available on
# a stock macOS runner.
#
# The watchdog runs in a subshell rather than as a `kill` after `wait`, because
# the diagnostics have to be taken while the process is still hung: once it is
# killed there is nothing left to sample, and a stack is the entire difference
# between this failure and the empty step log it replaces.
LAST_OUTPUT=""
run_bounded() {
  local limit="$1" label="$2"
  shift 2
  local out watchdog pid status=0
  out="$(mktemp)"
  log "${label} (deadline ${limit}s)"
  "$@" >"$out" 2>&1 &
  pid=$!
  # `>&2` on the watchdog is not cosmetic. This whole function runs inside a
  # command substitution, and a background job inherits its stdout PIPE -- so a
  # watchdog left on stdout holds that pipe open for the rest of its sleep and
  # the substitution never returns. Measured: the happy path completed every
  # simctl call and then hung, waiting on a `sleep 420` that had nothing left to
  # watch. The deadline polls rather than sleeping through, for the same family
  # of reason: a single long sleep outlives the command it was watching.
  (
    waited=0
    while [ "$waited" -lt "$limit" ]; do
      kill -0 "$pid" 2>/dev/null || exit 0
      sleep 1
      waited=$((waited + 1))
    done
    kill -0 "$pid" 2>/dev/null || exit 0
    echo "::warning::${label} exceeded ${limit}s; sampling pid ${pid} before killing it." >&2
    # Everything up to "Binary Images:", which is a hundred lines of dyld load
    # addresses and never the answer. The call graph above it is.
    /usr/bin/sample "$pid" 3 -mayDie 2>/dev/null \
      | awk '/^Binary Images:/ { exit } { print }' | head -n 80 || true
    # The CONTROL PLANE only. Matching "CoreSimulator" loosely matches every
    # process inside a booted runtime as well -- several hundred lines on a
    # developer machine -- and buries the sample above it.
    /bin/ps -Ao pid,ppid,etime,comm 2>/dev/null \
      | grep -E 'CoreSimulatorService|simdiskimaged|SimLaunchHost|/simctl$' || true
    kill -9 "$pid" 2>/dev/null || true
  ) >&2 &
  watchdog=$!
  wait "$pid" || status=$?
  kill "$watchdog" 2>/dev/null || true
  wait "$watchdog" 2>/dev/null || true
  LAST_OUTPUT="$(cat "$out")"
  rm -f "$out"
  # A process killed by SIGKILL reports 137. The only SIGKILL in play is the
  # watchdog's, so that is the deadline, reported as GNU timeout reports it.
  if [ "$status" -eq 137 ]; then
    return 124
  fi
  return "$status"
}

reset_core_simulator() {
  echo "::warning::CoreSimulatorService did not answer; restarting it and retrying once." >&2
  run_bounded 60 "simctl shutdown all" "$SIMCTL" shutdown all || true
  # launchd respawns the service on the next client connection, so killing it is
  # the supported way to clear a wedged one.
  killall -9 com.apple.CoreSimulator.CoreSimulatorService 2>/dev/null || true
  killall -9 Simulator 2>/dev/null || true
  sleep 5
}

# One full acquisition attempt. Echoes the UDID on success; returns 124 if any
# call hit its deadline, EXIT_NO_RUNTIME if the pinned runtime is genuinely
# absent (never retried -- a missing runtime does not become present), and 1 for
# anything else.
attempt_boot() {
  local runtime udid

  run_bounded "$LIST_TIMEOUT" "simctl list runtimes" "$SIMCTL" list runtimes || return $?
  runtime="$(printf '%s\n' "$LAST_OUTPUT" \
    | grep -Eo "com.apple.CoreSimulator.SimRuntime.${RUNTIME_PATTERN}" | head -n1)"
  if [ -z "$runtime" ]; then
    echo "::error::No ${RUNTIME_PATTERN} simulator runtime on this runner (required by the golden set)." >&2
    printf '%s\n' "$LAST_OUTPUT" >&2
    return "$EXIT_NO_RUNTIME"
  fi
  log "runtime=${runtime}"

  run_bounded "$LIST_TIMEOUT" "simctl list devices" "$SIMCTL" list devices "$runtime" available || return $?
  udid="$(printf '%s\n' "$LAST_OUTPUT" \
    | grep -E "${DEVICE_TYPE} \(" | grep -Eo '[0-9A-F-]{36}' | head -n1)"
  if [ -z "$udid" ]; then
    run_bounded "$CREATE_TIMEOUT" "simctl create" \
      "$SIMCTL" create "$DEVICE_NAME" "$DEVICE_TYPE" "$runtime" || return $?
    udid="$(printf '%s\n' "$LAST_OUTPUT" | tr -d '[:space:]')"
  fi
  if [ -z "$udid" ]; then
    echo "::error::Could not find or create a '${DEVICE_TYPE}' on ${runtime}." >&2
    return 1
  fi
  log "udid=${udid}"

  # `boot` on an already-booted device is an error, not a no-op, and a retry
  # after a killed boot can land on one that came up anyway.
  run_bounded "$BOOT_TIMEOUT" "simctl boot" "$SIMCTL" boot "$udid"
  local boot_status=$?
  if [ "$boot_status" -eq 124 ]; then
    return 124
  fi
  if [ "$boot_status" -ne 0 ] && ! printf '%s\n' "$LAST_OUTPUT" | grep -qi 'current state: Booted'; then
    echo "::error::simctl boot failed for ${udid}." >&2
    printf '%s\n' "$LAST_OUTPUT" >&2
    return 1
  fi

  run_bounded "$BOOTSTATUS_TIMEOUT" "simctl bootstatus" "$SIMCTL" bootstatus "$udid" -b || return $?
  printf '%s\n' "$LAST_OUTPUT" >&2

  printf '%s\n' "$udid"
  return 0
}

UDID="$(attempt_boot)"
STATUS=$?
if [ "$STATUS" -eq 124 ]; then
  reset_core_simulator
  UDID="$(attempt_boot)"
  STATUS=$?
  if [ "$STATUS" -eq 124 ]; then
    echo "::error::The simulator control plane hung twice, including after a CoreSimulatorService" \
      "restart. This is a runner fault, not a test result; the sampled stacks are above." >&2
    exit "$EXIT_WEDGED"
  fi
fi
if [ "$STATUS" -ne 0 ]; then
  exit "$STATUS"
fi

if [ -n "${GITHUB_OUTPUT:-}" ]; then
  echo "udid=$UDID" >> "$GITHUB_OUTPUT"
fi
printf '%s\n' "$UDID"

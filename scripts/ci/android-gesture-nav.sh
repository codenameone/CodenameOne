#!/usr/bin/env bash
# Switches the running emulator to gesture navigation and waits until it has
# taken effect. The AOSP image (the JDK 21 leg of scripts-android.yml) boots
# with 3-button navigation, a grey bar over the bottom 48px of every
# screenshot that the Google image does not draw. A blind "enable && sleep 5"
# sometimes returned before SystemUI had applied the overlay, and every
# screenshot of the run then failed against the shared goldens (and a golden
# capture got a grey band the other legs did not have). So this checks the
# result and fails the leg rather than letting it compare wrong images.
set -u
overlay=com.android.internal.systemui.navbar.gestural
# Applied before boot completes, SystemUI can reset the overlay while it starts.
for _ in $(seq 1 120); do
  [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ] && break
  sleep 1
done
is_gestural() {
  mode="$(adb shell settings get secure navigation_mode 2>/dev/null | tr -d '\r')"
  adb shell cmd overlay list 2>/dev/null | tr -d '\r' | grep -q "\[x\] $overlay" && [ "$mode" = 2 ]
}
for attempt in 1 2 3; do
  adb shell cmd overlay enable-exclusive --category "$overlay" >/dev/null 2>&1 ||
    adb shell cmd overlay enable "$overlay" >/dev/null 2>&1 || true
  for _ in $(seq 1 30); do
    if is_gestural; then
      # SystemUI relays out its window after the mode changes; and the mode has
      # to still hold afterwards, since a SystemUI restart can drop it.
      sleep 10
      if is_gestural; then
        echo "Gesture navigation active (attempt $attempt)."
        exit 0
      fi
      echo "Gesture navigation reverted after it was applied; applying again."
      break
    fi
    sleep 1
  done
  echo "Gesture navigation not active yet (navigation_mode=$mode); retrying."
done
echo "::error::Could not switch the emulator to gesture navigation; screenshots would carry the 3-button bar."
adb shell cmd overlay list | grep -i navbar || true
exit 1

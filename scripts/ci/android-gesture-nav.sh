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
for attempt in 1 2 3; do
  adb shell cmd overlay enable-exclusive --category "$overlay" >/dev/null 2>&1 ||
    adb shell cmd overlay enable "$overlay" >/dev/null 2>&1 || true
  for _ in $(seq 1 30); do
    mode="$(adb shell settings get secure navigation_mode 2>/dev/null | tr -d '\r')"
    if adb shell cmd overlay list 2>/dev/null | tr -d '\r' | grep -q "\[x\] $overlay" && [ "$mode" = 2 ]; then
      # SystemUI relays out its window after the mode changes.
      sleep 3
      echo "Gesture navigation active (attempt $attempt)."
      exit 0
    fi
    sleep 1
  done
  echo "Gesture navigation not active yet (navigation_mode=$mode); retrying."
done
echo "::error::Could not switch the emulator to gesture navigation; screenshots would carry the 3-button bar."
adb shell cmd overlay list | grep -i navbar || true
exit 1

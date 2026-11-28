#!/usr/bin/env bash
# Switches the running emulator to gesture navigation and waits until it has
# taken effect. The AOSP image (the JDK 21 leg of scripts-android.yml) boots
# with 3-button navigation, a grey bar over the bottom 48px of every
# screenshot that the Google image does not draw. A blind "enable && sleep 5"
# sometimes returned before SystemUI had applied the overlay, and every
# screenshot of the run then failed against the shared goldens (and a golden
# capture got a grey band the other legs did not have). So this checks the
# result and fails the leg rather than letting it compare wrong images.
#
# The setting and the overlay flag are NOT the result. Twice the leg passed
# both checks below -- navigation_mode=2 and the gestural overlay enabled, still
# so after the whole suite -- and every screenshot was taken with a 48px scrim
# over its bottom: the app had been given a 3-button-height navigation bar,
# because SystemUI had not re-laid out its bar for the overlay. What the app
# actually gets is the navigation bar's inset in the window manager, so that is
# what this measures: its height before the switch, and a smaller one after.
# SystemUI is restarted when the height did not move, which re-inflates the bar
# from the overlay that is now applied.
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
# Height in px of the navigation bar inset the window manager reports, or empty
# when no such line can be read (the dumpsys format differs between releases).
navbar_height() {
  adb shell dumpsys window 2>/dev/null | tr -d '\r' \
    | grep -E 'navigationBars|NAVIGATION_BAR' \
    | grep -oE 'frame=\[-?[0-9]+,-?[0-9]+\]\[-?[0-9]+,-?[0-9]+\]' \
    | head -n 1 \
    | sed -E 's/frame=\[-?[0-9]+,(-?[0-9]+)\]\[-?[0-9]+,(-?[0-9]+)\]/\1 \2/' \
    | awk '$2 > $1 { print $2 - $1 }'
}
# The bar is laid out a moment after the mode changes; give it that moment.
navbar_shrank() {
  for _ in $(seq 1 20); do
    now="$(navbar_height)"
    if [ -n "$now" ] && [ "$now" -lt "$buttons_height" ]; then
      return 0
    fi
    sleep 1
  done
  return 1
}
buttons_height="$(navbar_height)"
if [ -z "$buttons_height" ]; then
  # Cannot measure on this image: fall back to the setting alone, and leave the
  # lines that failed to parse in the log for whoever reads the next failure.
  echo "::warning::Could not read the navigation bar inset; checking the setting only."
  adb shell dumpsys window 2>/dev/null | tr -d '\r' | grep -E 'navigationBars|NAVIGATION_BAR' | head -n 5 || true
else
  echo "Navigation bar inset before the switch: ${buttons_height}px."
fi
for attempt in 1 2 3; do
  adb shell cmd overlay enable-exclusive --category "$overlay" >/dev/null 2>&1 ||
    adb shell cmd overlay enable "$overlay" >/dev/null 2>&1 || true
  for _ in $(seq 1 30); do
    if is_gestural; then
      # SystemUI relays out its window after the mode changes; and the mode has
      # to still hold afterwards, since a SystemUI restart can drop it.
      sleep 10
      if ! is_gestural; then
        echo "Gesture navigation reverted after it was applied; applying again."
        break
      fi
      if [ -z "$buttons_height" ]; then
        echo "Gesture navigation active (attempt $attempt)."
        exit 0
      fi
      if navbar_shrank; then
        echo "Gesture navigation active (attempt $attempt): inset ${buttons_height}px -> ${now}px."
        exit 0
      fi
      # The setting says gestural and the bar is still 3-button height: the
      # state the scrimmed runs were in. A new SystemUI reads the overlay as it
      # starts; the system restarts it at once, as it is a persistent process.
      echo "Navigation bar still ${now:-?}px with gesture navigation set; restarting SystemUI."
      adb shell am crash com.android.systemui >/dev/null 2>&1 ||
        adb shell pkill -f com.android.systemui >/dev/null 2>&1 || true
      sleep 10
      if is_gestural && navbar_shrank; then
        echo "Gesture navigation active after a SystemUI restart (attempt $attempt): inset ${buttons_height}px -> ${now}px."
        exit 0
      fi
      break
    fi
    sleep 1
  done
  echo "Gesture navigation not active yet (navigation_mode=$mode); retrying."
done
echo "::error::Could not switch the emulator to gesture navigation; screenshots would carry the 3-button bar."
adb shell cmd overlay list | grep -i navbar || true
exit 1

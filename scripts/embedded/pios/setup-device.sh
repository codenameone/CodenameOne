#!/usr/bin/env bash
# Prepares a Raspberry Pi OS Lite (or any Debian bookworm) device to run a
# Codename One embedded application straight from boot, with no desktop:
#
#   scp scripts/embedded/pios/setup-device.sh pi@pi.local: && ssh pi@pi.local sudo ./setup-device.sh
#
# Then deploy with `mvn cn1:deploy-device -Dcn1.device.host=pi.local`.
#
# What it does, each step idempotent:
#   - installs the shared libraries the embedded port links;
#   - lets the deploy user reach the display, input, GPIO, I2C, SPI and serial
#     devices (video, render, input, gpio, i2c, spi, dialout groups);
#   - enables I2C and SPI where raspi-config exists;
#   - frees tty1 for the application (no login prompt), and hides the console
#     cursor and boot messages so the screen goes from the firmware splash to
#     the application.
set -euo pipefail
[ "$(id -u)" = 0 ] || { echo "setup-device: run as root (sudo)" >&2; exit 1; }
DEPLOY_USER="${SUDO_USER:-${1:-pi}}"

apt-get update
apt-get install -y --no-install-recommends \
    libfreetype6 libpng16-16 libjpeg62-turbo libssl3 libcurl4 libdrm2 libnghttp2-14 fonts-dejavu-core

for g in video render input gpio i2c spi dialout; do
    getent group "$g" >/dev/null && usermod -aG "$g" "$DEPLOY_USER"
done

if command -v raspi-config >/dev/null; then
    raspi-config nonint do_i2c 0
    raspi-config nonint do_spi 0
fi

systemctl disable --now getty@tty1.service 2>/dev/null || true

CMDLINE=/boot/firmware/cmdline.txt
[ -f "$CMDLINE" ] || CMDLINE=/boot/cmdline.txt
if [ -f "$CMDLINE" ]; then
    for opt in quiet loglevel=3 logo.nologo vt.global_cursor_default=0 consoleblank=0; do
        grep -qw "$opt" "$CMDLINE" || sed -i "1s/\$/ $opt/" "$CMDLINE"
    done
fi
echo "setup-device: done; reboot once so the group and boot changes take effect"

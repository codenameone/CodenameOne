#!/bin/bash
#
# Sign + notarize + staple a Codename One Mac-native .app for distribution
# (e.g. the RPC simulator relay shipped through Maven Central).
#
# Local dev builds are ad-hoc/linker-signed, which runs on this machine but is
# NOT shippable: a downloaded ad-hoc app is killed by Gatekeeper once it is
# quarantined, zip round-trips can invalidate the signature, and TCC (camera/
# mic) grants do not persist for an ad-hoc identity. For distribution the app
# must be Developer ID signed with the hardened runtime + entitlements, then
# notarized by Apple and the ticket stapled into the bundle. After that it runs
# regardless of quarantine and has a stable TCC identity.
#
# Requirements on the signing machine:
#   - A "Developer ID Application: NAME (TEAMID)" cert in the login keychain.
#   - A notarytool credential profile stored once with:
#       xcrun notarytool store-credentials cn1-notary \
#         --apple-id you@example.com --team-id TEAMID --password <app-specific-pwd>
#
# Usage:
#   sign-notarize-mac-app.sh \
#     --app    /path/to/SimRelayService.app \
#     --identity "Developer ID Application: Codename One (XXXXXXXXXX)" \
#     --entitlements /path/to/SimRelayService-DeveloperID.entitlements \
#     --notary-profile cn1-notary \
#     [--skip-notarize]   # sign + harden only (for offline smoke testing)
#
set -euo pipefail

APP=""
IDENTITY=""
ENTITLEMENTS=""
NOTARY_PROFILE=""
SKIP_NOTARIZE=0

while [ $# -gt 0 ]; do
  case "$1" in
    --app) APP="$2"; shift 2;;
    --identity) IDENTITY="$2"; shift 2;;
    --entitlements) ENTITLEMENTS="$2"; shift 2;;
    --notary-profile) NOTARY_PROFILE="$2"; shift 2;;
    --skip-notarize) SKIP_NOTARIZE=1; shift;;
    *) echo "unknown arg: $1" >&2; exit 2;;
  esac
done

[ -n "$APP" ] && [ -d "$APP" ] || { echo "ERROR: --app must be an existing .app bundle" >&2; exit 2; }
[ -n "$IDENTITY" ] || { echo "ERROR: --identity (Developer ID Application: ...) is required" >&2; exit 2; }
[ -n "$ENTITLEMENTS" ] && [ -f "$ENTITLEMENTS" ] || { echo "ERROR: --entitlements plist not found" >&2; exit 2; }

log() { echo "[sign-notarize] $*"; }

# 1) Sign inside-out: any nested code (frameworks, dylibs, helper tools) first,
#    then the outer bundle. --deep is discouraged by Apple; do it explicitly so
#    every Mach-O gets the hardened runtime + a Developer ID signature.
log "signing nested Mach-O objects (frameworks/dylibs)..."
find "$APP/Contents" \( -name "*.dylib" -o -name "*.framework" -o -perm +111 -type f \) 2>/dev/null \
  | while read -r obj; do
      # skip the main executable (signed last with entitlements)
      case "$obj" in
        "$APP/Contents/MacOS/"*) continue;;
      esac
      if file "$obj" 2>/dev/null | grep -q "Mach-O"; then
        codesign --force --timestamp --options runtime \
          --sign "$IDENTITY" "$obj" 2>/dev/null || true
      fi
    done

# 2) Sign the app bundle itself with the hardened runtime + entitlements.
log "signing app bundle with hardened runtime + entitlements..."
codesign --force --timestamp --options runtime \
  --entitlements "$ENTITLEMENTS" \
  --sign "$IDENTITY" "$APP"

log "verifying signature (codesign --verify --deep --strict)..."
codesign --verify --deep --strict --verbose=2 "$APP"

if [ "$SKIP_NOTARIZE" -eq 1 ]; then
  log "--skip-notarize set; signed+hardened only. Gatekeeper assessment:"
  spctl --assess --type execute --verbose=4 "$APP" || \
    log "spctl rejects (expected without notarization); run without --skip-notarize to ship."
  exit 0
fi

[ -n "$NOTARY_PROFILE" ] || { echo "ERROR: --notary-profile required to notarize" >&2; exit 2; }

# 3) Notarize: notarytool wants a zip/dmg/pkg, not a bare .app.
ZIP="$(dirname "$APP")/$(basename "$APP" .app)-notarize.zip"
log "zipping for notarization -> $ZIP"
/usr/bin/ditto -c -k --keepParent "$APP" "$ZIP"

log "submitting to Apple notary service (this can take a few minutes)..."
xcrun notarytool submit "$ZIP" --keychain-profile "$NOTARY_PROFILE" --wait

# 4) Staple the ticket into the bundle so it validates offline / behind a
#    firewall (notarization status no longer needs a network check at launch).
log "stapling notarization ticket..."
xcrun stapler staple "$APP"
rm -f "$ZIP"

log "final Gatekeeper assessment:"
spctl --assess --type execute --verbose=4 "$APP"
xcrun stapler validate "$APP"
log "DONE: $APP is Developer ID signed, hardened, notarized + stapled."

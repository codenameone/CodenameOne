#!/usr/bin/env bash
# Helpers to flip the JavaSEPort `cn1.autoDefaultResourceBundle` preference for
# the duration of an integration test. The preference normally defaults to
# false, which masks bugs in JavaSEPort.enableAutoLocalizationBundle from CI -
# end users with the menu toggled on (see issue #4850) experience the gated
# code path instead. Tests that exercise `cn1:css` / `cn1:run` need to flip
# the pref true before the build to reproduce the user-side environment.
#
# Usage:
#   source "$SCRIPTPATH/inc/auto-bundle-pref.sh"
#   set_auto_bundle_pref true
#   trap 'set_auto_bundle_pref false' EXIT
#
# The pref is stored in `Preferences.userRoot().node("/com/codename1/impl/javase")`,
# i.e. `~/.java/.userPrefs/com/codename1/impl/javase/prefs.xml`. Forked
# `cn1:css` subprocesses inherit the same `user.home` from Maven and read
# the same backing store, so a parent-process flush is visible to children.

set_auto_bundle_pref() {
  local value="${1:-true}"
  local workdir
  workdir="$(mktemp -d -t cn1-auto-bundle-pref.XXXXXX)"
  cat > "$workdir/SetAutoBundlePref.java" <<'EOF'
import java.util.prefs.Preferences;

public final class SetAutoBundlePref {
    public static void main(String[] args) throws Exception {
        boolean value = args.length > 0 ? Boolean.parseBoolean(args[0]) : true;
        Preferences node = Preferences.userRoot().node("/com/codename1/impl/javase");
        node.putBoolean("cn1.autoDefaultResourceBundle", value);
        node.flush();
        System.out.println("[auto-bundle-pref] cn1.autoDefaultResourceBundle=" + value);
    }
}
EOF
  ( cd "$workdir" && javac SetAutoBundlePref.java && java SetAutoBundlePref "$value" )
  local rc=$?
  rm -rf "$workdir"
  return $rc
}

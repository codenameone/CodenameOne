# Native reference goldens

Each directory here is one **golden set**: the captured appearance of a platform's
real widgets, which the Codename One render is scored against. A golden is not a
test output. It is the definition of what the theme is trying to look like, so
nothing in CI ever writes one.

| Set | Captured from |
|---|---|
| `ios-26-metal` | iOS simulator, `scripts/build-ios-native-ref.sh` |
| `android-m3` | Android emulator, `scripts/build-android-native-ref.sh` |
| `windows-11-fluent` | Hosted Windows runner, WinUI 3 |
| `macos-aqua` | Hosted macOS runner, AppKit |
| `gnome-adwaita` | Hosted Linux runner, GTK4 + libadwaita under Xvfb |

## Why the desktop sets come from CI

The other sets are captured on a maintainer's machine. The desktop ones cannot
be: a working developer's Mac has a chosen accent colour, a chosen appearance
and custom fonts, and a reference captured there would encode all three. A
hosted runner is the closest available thing to a default-configured machine.

This is measurable rather than theoretical. The same capture app run on a
maintainer's macOS 26 machine reports a window background of `#FFFFFF` and
`#171717`; on the macos-15 runner it reports `#E7E7E7` and `#262626`. Both are
correct for their machine and only one of them is a reference.

## Capturing a set

```
gh workflow run fidelity-desktop-native-ref.yml -f targets=windows -f mode=capture
```

`targets` is `all`, `windows`, `macos` or `gnome`. `mode=probe` answers only the
environment questions and writes one tile; `mode=capture` writes the full matrix.

The workflow is **dispatch only**, with no schedule and no path trigger, and that
is deliberate: a native reference defines the design generation a theme is written
against, so a job that re-captured it on its own would turn a real OS design
change into a green build.

## Promoting a run to a golden set

1. **Download the artifact and look at every frame.** Not the tile count -- the
   frames. A capture that is wrong in one direction reports a full count and no
   blockers, which is exactly how a light-mode backdrop ended up behind the dark
   half of a Windows set.
2. **Read `capture-manifest.json`** and check it describes the environment you
   intended: OS build, scale factor, accent colour, fonts, transparency and
   contrast settings, toolkit versions. It also carries two fields worth reading
   every time:
   - `backdrop_by_appearance` -- the surface each half was captured on. The apps
     refuse a set where the two are equal, but the values themselves are what you
     compare against the theme's own `--window-bg-color`.
   - `states_identical_to_normal` -- states the platform does not restyle. These
     are real findings, not gaps: AppKit draws no rollover state at all, so every
     macOS hover tile is listed, and Adwaita restyles a `GtkEntry` on focus rather
     than on hover. **A theme must leave those states equal too**, or it diverges
     from a reference that cannot move.
3. **Commit the set in one commit that names the run**, so the provenance of every
   byte is recoverable.
4. **Dispatch the same capture again and require byte-identical output.**
   Nondeterminism is fixed in the reference app or by pinning an environment knob,
   never with a tolerance file. There are no tolerance sidecars here and there
   will not be.
5. **Record the first baseline separately**, with `FIDELITY_UPDATE_BASELINE=1`, so
   the commit that defines the goldens and the commit that defines the ratchet are
   two reviewable changes rather than one.

## What CI may and may not do

CI scores against these files and never writes them. `FIDELITY_UPDATE_GOLDENS`
must not be set in any desktop workflow -- the desktop suite follows the iOS
model, where committed goldens are the contract, not the Android one.

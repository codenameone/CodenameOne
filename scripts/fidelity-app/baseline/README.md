# Desktop fidelity baselines

The PNG references remain in `../goldens/`. These JSON files record scores and
geometry measured against those references on each platform's hosted runner.
Baseline updates require inspection of the comparison artifacts; do not increase
tolerances to hide a failed run.

The current desktop baselines use **1x logical pixels at 96 DPI** and the installed
native font families. The previous captures used the simulator's mobile fallback
of 5 pixels/mm and bundled Roboto aliases. Switching to about 3.78 pixels/mm
changes geometry throughout the captures, so all 60 pairs per platform were
re-rendered and their comparison overviews inspected. Native PNG references and
all score, geometry, and upward-jump tolerances remain unchanged.

| Platform | Capture run | Commit | Confirmed Java font family | Pairs | Mean | Minimum |
| --- | --- | --- | --- | ---: | ---: | ---: |
| GNOME | [35115207618](https://github.com/codenameone/CodenameOne/actions/runs/35115207618) | `1e194b0336` | Cantarell | 60 | 87.23% | 72.05% |
| Windows 11 ARM | [35115210997](https://github.com/codenameone/CodenameOne/actions/runs/35115210997) | `1e194b0336` | Segoe UI Variable | 60 | 89.98% | 69.01% |
| macOS 15 ARM | [35113942967](https://github.com/codenameone/CodenameOne/actions/runs/35113942967) | `6fc290880b` | .AppleSystemUIFont | 60 | 85.18% | 72.94% |

The Windows runner now matches the Windows 11 reference host rather than using
Windows Server; its JDKs run under x64 emulation. GNOME installs the same Cantarell
family used by its native reference app. The tile runner asserts both the font
family and the 96-DPI conversion before capturing. GTK body text uses the
reference's 11pt size, and Fluent body text uses 14 logical pixels.

These measurements still include visible differences in control geometry,
switches, sliders, and pressed states. They provide a consistent regression
baseline, not a claim of pixel equivalence. MacOS uses Zulu 8 to build and Temurin
21 to render; the other hosts use Temurin 8 and 21.

## Earlier measurements

On 2026-09-16, the Windows and GNOME hover entries were refreshed from
[run 35096538882](https://github.com/codenameone/CodenameOne/actions/runs/35096538882)
(commit `bb4d619eb3`). That run includes the correction in `96d56fcf59` which
normalizes hover margins in `DesktopTileRunner` after widget construction, beside
the other states. The old measurements included extra hover-only spacing.

All 16 hover pairs per platform were inspected. The slider score jumps and
button, combo-box, and switch geometry changes follow that margin correction;
the hover renders now use the same placement contract as normal-state renders.
Only hover entries were updated. Non-hover thresholds and native PNG references
were retained. The Windows light pressed accent-button failure was handled by
correcting its theme color, without lowering its score baseline.

The first macOS baseline was seeded from
[run 35102280850](https://github.com/codenameone/CodenameOne/actions/runs/35102280850)
(commit `322da2d5aa`, hosted macOS 15 ARM64, Zulu 8 build and Temurin 21 render).
All 60 light/dark pairs were valid and their comparison overview was inspected.
The initial mean score is 81.67%, with a minimum of 69.69%; this records the
current rendering differences, including larger CN1 control geometry, rather
than claiming a pixel match. The committed native references and the existing
2-point score tolerance are unchanged. Subsequent runs now gate both score
and geometry regressions against these runner measurements.

The grouped-backdrop geometry mask was corrected using the iOS captures from
[run 35086524544](https://github.com/codenameone/CodenameOne/actions/runs/35086524544)
(commit `af6d3ed25b`). The appearance-specific grouped fill is excluded at the
normal content threshold so a white field remains detectable. Re-scoring changes
only `TextField_disabled_light` among the three gated geometry metrics: center
offset 0.71 px, height ratio 0.996, and width ratio 0.9991. The other three fields
retain their measured edge-to-edge bounds. Score baselines and native references
are unchanged; all non-field geometry and the current Android capture geometry
are unchanged.

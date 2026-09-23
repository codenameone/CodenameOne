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

## macos-aqua, DesktopTabs and DesktopListRow

Re-recorded from [run 35501085353](https://github.com/codenameone/CodenameOne/actions/runs/35501085353)
after the AppKit reference app was corrected. Six pairs moved and no other pair
on any platform moved at all, which is the check that the two runs are
comparable.

| Pair | Score | Geometry (centre offset) |
| --- | --- | --- |
| `DesktopTabs_normal_dark` | 48.96 -> 78.60 | unchanged |
| `DesktopTabs_normal_light` | 81.97 -> 79.04 | unchanged |
| `DesktopListRow_normal_dark` | 89.91 -> 84.92 | 2.24 -> 9.22 |
| `DesktopListRow_normal_light` | 88.99 -> 83.11 | 2.24 -> 9.22 |
| `DesktopListRow_selected_dark` | 85.53 -> 85.19 | 2.12 -> 1.50 |
| `DesktopListRow_selected_light` | 84.26 -> 83.72 | 2.12 -> 1.50 |

**None of these is the theme changing. The theme did not change; the reference
did**, and three of the four directions are worth reading rather than absorbing:

The dark tab jump is the repair. That reference had been an uncapturable
NSTabView whose unselected segment came back as a coverage mask and flattened to
a solid block, so 48.96 was the theme being scored against a broken image. It
was the lowest pair in the matrix.

The light tab drop is the same repair, in the direction nobody expects. The
broken reference was a white pill beside a black block, and the CN1 render
happened to resemble that slightly more than it resembles the real segmented
control. 79.04 is the honest number.

**The ListRow drop is a real divergence the corrected reference exposed, and it
is not fixed here.** Measured on this run's tiles: AppKit puts the row label at
x=17, CN1 puts it at x=9; AppKit's selection capsule is inset to x=10..229 and
CN1's fills the tile 0..239. The same is true of Tabs, whose box AppKit insets
to 7..232 against CN1's 0..239. So the macOS theme's `ListRenderer` left padding
(2.5mm, about 9px) is short of the 17px AppKit uses, and neither `ListRenderer`
nor `Tabs` insets its background the way AppKit does. Closing that means editing
`native-themes/macos-aqua/theme.css`, rebuilding `MacOSAquaTheme.res` and
reseeding every hellocodenameone macOS screenshot that shows a list, which is a
change with its own blast radius and does not belong in a commit that fixes
reference images. It is written down here with its numbers so it stays findable.

The selected-row pair moved the other way, slightly: its centre offset improved
from 2.12 to 1.50 because the label is now inside the capsule on both sides.

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

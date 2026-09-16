# Desktop fidelity baselines

The PNG references remain in `../goldens/`. These JSON files record scores and
geometry measured against those references on each platform's hosted runner.
Baseline updates require inspection of the comparison artifacts; do not increase
tolerances to hide a failed run.

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

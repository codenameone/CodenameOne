# Release gate (copy)

`release.yml` runs `check-release-gate.sh` before it builds anything. It verifies the
`release-gate/<sha>` tag that BuildDaemon's `release-gate/run_gate.py` pushes after
building every target from every way users start a project and walking the new-user
funnel on production. The procedure is BuildDaemon `deploy/RELEASE-CHECKLIST.md`.

`verify_gate.py` and `matrix.json` are byte-identical copies of BuildDaemon's
`release-gate/` (that repository is private). Change them there and copy them here;
`run_gate.py` refuses to run while the two differ, and the matrix's SHA-256 must match
the `RELEASE_GATE_MATRIX_SHA256` repository variable or every release is refused.

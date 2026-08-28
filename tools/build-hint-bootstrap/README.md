# Build hint catalog bootstrap (one-off, archived)

These scripts seeded `maven/build-hint-catalog` when the catalog was first
created. They mined every `getArg` call site in the builders for a hint's name
and default, imported the prose from the developer guide's hand-written build
hint table, and emitted the `BuildHints*.java` registration classes.

**They are not part of any build and should not be re-run.** A hint is declared
either by an annotation in `CodenameOne/src/com/codename1/annotations/buildhints`
or, when it has none, in the catalog; both are edited directly.
`scripts/gen-build-hint-annotations.sh` renders those declarations into the data
file the editors read and the developer guide's table. It generates no code.

They are kept only to show where the catalog's contents came from. They read the
guide's original hand-written table from a `guide_old.asciidoc` that is
deliberately not committed — recover it from history if you ever need it:

```bash
git show <commit-before-this-change>:docs/developer-guide/Advanced-Topics-Under-The-Hood.asciidoc \
    > tools/build-hint-bootstrap/guide_old.asciidoc
```

Re-running them would overwrite hand-edits to the catalog. If you ever need to
re-derive an entry, read the miner instead: `scripts/build_hint_miner.py` is the
supported, tested version of the same extraction and is what the CI gate uses.

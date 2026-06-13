# Native Linux port screenshot baselines (arm64)

Golden PNGs for the native Linux (GTK3/Cairo/Pango) port's hellocodenameone
screenshot suite on **arm64**, produced by the `arm64` leg of the
`linux-build-run.yml` `build-run` matrix (`ubuntu-24.04-arm`). The x86_64
baselines live in `../screenshots`.

See `../screenshots/README.md` for the seeding/update procedure (download the
`linux-screenshot-raw-arm64` artifact and copy its PNGs here). x64 and arm64 are
kept separate because software rasterization can differ slightly in
anti-aliasing/text between architectures.

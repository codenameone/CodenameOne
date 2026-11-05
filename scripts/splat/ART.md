# Splat art and type

Everything the game draws in 3D is a CC0 model, packed into
`common/src/main/resources/model_*.glb` by `tools/pack-art.py`. Nothing is
built from primitives at run time except the paintballs and the paint they
leave behind.

| What | Pack | Author | Licence |
|---|---|---|---|
| Operators (`model_op-*.glb`) | Ultimate Modular Men | Quaternius (quaternius.com) | CC0 1.0 |
| Cover and yard props | Toon Shooter Game Kit | Quaternius (quaternius.com) | CC0 1.0 |
| Markers (`model_mk-*.glb`) | Blaster Kit 2.1 | Kenney (kenney.nl) | CC0 1.0 |
| Small props (crates, barrels, tents) | Survival Kit | Kenney (kenney.nl) | CC0 1.0 |

CC0 asks for no credit. We give it anyway.

The type is **Rajdhani** (Bold and Medium) by the Indian Type Foundry,
under the SIL Open Font License 1.1; the licence is beside the font files
in `common/src/main/css/fonts/OFL.txt`.

## Re-skinning

1. Unzip the packs you want into one directory.
2. Edit the tables at the top of `tools/pack-art.py` (`OPERATORS`, `MARKERS`,
   `PROPS`) to point at the files you want.
3. Run `tools/pack-art.py <that directory>`.

An operator needs a skinned glTF with the clips `Idle_Gun`, `Idle_Gun_Shoot`,
`Run`, `Run_Back`, `Run_Left`, `Run_Right`, `Run_Shoot`, `Walk`, `Death` and
`Wave`, and a `Wrist.R` bone for the marker. `play/Models.java` maps the game's
names to the files. A prop's collision box lives in the rules
(`rules/.../Catalog.java`); the view scales the model to fit it.

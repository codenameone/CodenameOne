# Splat art and type

Everything the game draws in 3D is a CC0 model, packed into
`common/src/main/resources/model_*.glb` by `tools/pack-art.py`. Nothing is
built from primitives at run time except the paintballs, the paint they
leave behind and the sphere the sky is painted on (the sky itself is the
engine's sky shader, set with `Environment.setSky`).

| What | Pack | Author | Licence |
|---|---|---|---|
| Operators (`model_op-*.glb`) | Ultimate Modular Men | Quaternius (quaternius.com) | CC0 1.0 |
| Cover and yard props | Toon Shooter Game Kit | Quaternius (quaternius.com) | CC0 1.0 |
| Markers (`model_mk-semi`, `-pump`, `-ramp`) | Toon Shooter Game Kit (SMG, Shotgun, AK) | Quaternius (quaternius.com) | CC0 1.0 |
| Scope, hopper and grenade (`model_mk-scope`, `-hopper`, `-grenade`) | Blaster Kit 2.1 | Kenney (kenney.nl) | CC0 1.0 |
| Small props (crates, barrels, tents) | Survival Kit | Kenney (kenney.nl) | CC0 1.0 |
| Trees, bushes, grass and rocks | Ultimate Nature | Quaternius (quaternius.com) | CC0 1.0 |
| Photo-scanned props: covered car, concrete barriers, sofa, barrel, street lamp, cardboard boxes | covered_car, concrete_road_barrier(_02), sofa_02, barrel_03, street_lamp_01, cardboard_box_01 | Poly Haven (polyhaven.com) | CC0 1.0 |
| Ground surfaces (`tex_*.jpg`) | asphalt_02, concrete_floor_02, grass_ground, gravel_road, corrugated_iron | Poly Haven (polyhaven.com) | CC0 1.0 |

CC0 asks for no credit. We give it anyway.

The type is **Rajdhani** (Bold and Medium) by the Indian Type Foundry,
under the SIL Open Font License 1.1; the licence is beside the font files
in `common/src/main/css/fonts/OFL.txt`.

## Re-skinning

1. Unzip the packs you want into one directory.
2. Edit the tables at the top of `tools/pack-art.py` (`OPERATORS`, `MARKERS`,
   `PROPS`, `NATURE`) to point at the files you want.
3. Run `tools/pack-art.py <that directory>`.

An operator needs a skinned glTF with the clips `Idle_Gun`, `Idle_Gun_Shoot`,
`Run`, `Run_Back`, `Run_Left`, `Run_Right`, `Run_Shoot`, `Walk`, `Death` and
`Wave`, and a `Wrist.R` bone for the marker. `play/Models.java` maps the game's
names to the files. The nature models come as OBJ with a material library;
the packer turns each material into a glTF primitive with its own colour. A prop's collision box lives in the rules
(`rules/.../Catalog.java`); the view scales the model to fit it.

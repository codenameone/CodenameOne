# Splat art and type

Everything the game draws in 3D is a CC0 model, written into
`common/src/main/resources/model_*.glb` by `tools/make-operators.py` (the
operators) and `tools/pack-art.py` (everything else). Nothing is
built from primitives at run time except the paintballs, the paint they
leave behind and the sphere the sky is painted on (the sky itself is the
engine's sky shader, set with `Environment.setSky`).

| What | Pack | Author | Licence |
|---|---|---|---|
| Operator bodies, skins, eyes, brows, hair and clothes (`model_op-*.glb`) | MakeHuman system assets and community asset packs (only assets marked CC0) | MakeHuman community (makehumancommunity.org) | CC0 1.0 |
| Operator animation (`model_op-*.glb`) | Universal Animation Library and Universal Animation Library 2, Standard | Quaternius (quaternius.com) | CC0 1.0 |
| Paintball goggles | modelled by `tools/make-operators.py` | this project | as the sample |
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

### Operators

The operators are built in Blender (5.x) with the MPFB add-on, which makes
MakeHuman characters:

1. `blender --online-mode -c extension install --enable mpfb`, then unzip the
   MakeHuman asset packs you want into MPFB's `data` directory. Use only
   assets whose `.mhclo`/`.mhmat` says `license CC0`; several community
   assets are CC-BY.
2. Download Quaternius' Universal Animation Library 1 and 2 (Standard) into
   one directory.
3. Edit `OPERATORS` (body, skin, hair, clothes) and `CLIPS` at the top of
   `tools/make-operators.py`, and run
   `blender -b --python tools/make-operators.py -- <that directory> [name ...]`.

The script retargets each clip from the library's skeleton onto the
MakeHuman game rig (they share Unreal mannequin bone names), applies the
clothing masks, models the goggles, moves the head into its own `<name>_Head`
mesh (hidden in first person) and writes the glTF. If you change the rig,
re-measure the constants in `play/Grip.java` from the `Aim` clip.

### Everything else

1. Unzip the packs you want into one directory.
2. Edit the tables at the top of `tools/pack-art.py` (`MARKERS`, `PROPS`,
   `NATURE`) to point at the files you want.
3. Run `tools/pack-art.py <that directory>`.

An operator can be any skinned glTF with the clip names `play/Models.java`
lists and a `hand_r` bone for the marker. The nature models come as OBJ with a material library;
the packer turns each material into a glTF primitive with its own colour. A prop's collision box lives in the rules
(`rules/.../Catalog.java`); the view scales the model to fit it.

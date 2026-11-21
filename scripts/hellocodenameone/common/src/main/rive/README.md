# Rive screenshot fixtures

Unmodified copies of rive-runtime test files
(https://github.com/rive-app/rive-runtime, `tests/unit_tests/assets`), MIT
licensed, Copyright (c) 2020 Rive; the licence is in `LICENSE-rive-runtime.txt`.
The build translates each into a class in `com.codename1.generated.rive`.

The files, and the test that draws each:

- `juice.riv`, `off_road_car.riv`, `circle_clips.riv`, `jellyfish_test.riv`:
  `RiveAnimatedScreenshotTest`; `jellyfish_test.riv` also `RiveGpuScreenshotTest`.
- `light_switch.riv`, `state_machine_triggers.riv` (state machine inputs),
  `hello_world.riv` (text), `layout_complex1.riv` (layouts),
  `data_binding_test_2.riv` (data binding), `stacked_path_effects.riv` and
  `trim.riv` (trim and dash path effects), `complex_ik_dependency.riv` (bones
  and IK): `RiveFeaturesScreenshotTest`.

`hello_world.riv` embeds Abel Regular (Copyright (c) 2011 Matthew Desmond),
licensed under the SIL Open Font License 1.1, which permits redistribution.

`jellyfish_test.riv` embeds an image (`RingEffect04.png`), part of the same
MIT licensed rive-runtime test assets.

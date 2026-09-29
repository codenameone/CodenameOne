#!/usr/bin/env python3
"""Regression checks for source/spec mismatches that otherwise silently lose coverage."""
import contextlib
import importlib.util
import io
from pathlib import Path
import tempfile
import unittest

module = importlib.util.spec_from_file_location("fidelity_spec", Path(__file__).with_name("check-fidelity-spec.py"))
validator = importlib.util.module_from_spec(module)
module.loader.exec_module(validator)


class FidelitySpecTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.spec = validator.SPEC
        self.sources = validator.NATIVE_REF_SOURCES
        self.addCleanup(setattr, validator, "SPEC", self.spec)
        self.addCleanup(setattr, validator, "NATIVE_REF_SOURCES", self.sources)
        validator.SPEC = Path(self.temp.name) / "spec.yaml"
        validator.SPEC.write_text(self.spec.read_text())
        validator.NATIVE_REF_SOURCES = {}
        for platform, path in self.sources.items():
            target = Path(self.temp.name) / path.name
            target.write_text(path.read_text())
            validator.NATIVE_REF_SOURCES[platform] = target

    def check(self):
        with contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()):
            return validator.main()

    def test_current_sources(self):
        self.assertEqual(0, self.check())

    def test_unknown_mobile_renderer_ids_fail_even_with_valid_native_keys(self):
        original = validator.SPEC.read_text()
        for known in ("Button", "TextField", "SwitchMorph"):
            with self.subTest(original_id=known):
                validator.SPEC.write_text(original.replace("  - id: " + known + "\n",
                                                           "  - id: UnsupportedProbe\n", 1))
                self.assertEqual(1, self.check())
                self.assertTrue(any("unsupported CN1 renderer id" in error for error in validator.ERRORS))
        validator.SPEC.write_text(original.replace("  - id: GlassPanelGrad\n", "  - id: GlassPanelProbe\n", 1))
        self.assertEqual(0, self.check(), "the renderer intentionally supports the GlassPanel prefix")

    def test_material_values_match_comparator_modes(self):
        original = validator.SPEC.read_text()
        self.assertIn("    material: glass", original)
        for material in ("glas", "Glass", "glass,lens", ""):
            with self.subTest(material=material):
                validator.SPEC.write_text(original.replace("material: glass", "material: " + material, 1))
                self.assertEqual(1, self.check())
                self.assertTrue(any("unknown material" in error for error in validator.ERRORS))
        for material in ("normal", "glass", "lens", '"glass"', "'lens'"):
            with self.subTest(material=material):
                validator.SPEC.write_text(original.replace("material: glass", "material: " + material, 1))
                self.assertEqual(0, self.check())
        validator.SPEC.write_text(original.replace("    material: glass\n", "", 1))
        self.assertEqual(0, self.check(), "omitting material retains the legacy heuristic")

    def test_component_dimensions_require_positive_java_integers(self):
        original = validator.SPEC.read_text()
        for key in sorted(validator.TILE_KEYS):
            row = "DesktopButton" if key.endswith("_px") else "Button"
            marker = "  - id: " + row + "\n"
            for value in ("24O", "0", "-1", "2147483648", "1.5", "", "1px"):
                with self.subTest(key=key, value=value):
                    validator.SPEC.write_text(original.replace(marker, marker + "    " + key + ": " + value + "\n", 1))
                    self.assertEqual(1, self.check())
                    self.assertTrue(any("positive Java integer" in error for error in validator.ERRORS))
            for value in ("1", "+24", "2147483647", '"24"'):
                with self.subTest(key=key, value=value):
                    validator.SPEC.write_text(original.replace(marker, marker + "    " + key + ": " + value + "\n", 1))
                    self.assertEqual(0, self.check())

    def test_backdrop_values_match_renderer_contract(self):
        original = validator.SPEC.read_text()
        self.assertIn("backdrop: grouped", original)
        for value in ("phoot", "Photo", "fff", "fffffff", "gg0000", ""):
            with self.subTest(value=value):
                validator.SPEC.write_text(original.replace("backdrop: grouped", "backdrop: " + value, 1))
                self.assertEqual(1, self.check())
                self.assertTrue(any("backdrop must be" in error for error in validator.ERRORS))
        for value in ("photo", "gradient", "grouped", "000000", "Ab09fF", '"808080"', "'photo'"):
            with self.subTest(value=value):
                validator.SPEC.write_text(original.replace("backdrop: grouped", "backdrop: " + value, 1))
                self.assertEqual(0, self.check())

    def test_frames_are_unique_valid_progress_values_and_capture_names(self):
        original = validator.SPEC.read_text()
        marker = "frames: 0,25,50,75,100"
        self.assertIn(marker, original)
        for value in ("0,5O,100", "", "0,,100", "0,0,100", "0,00,100", "0,101", "-1",
                      "0,+50,100", "0000", "2147483648", "0,'50',100", "0,50.5,100"):
            with self.subTest(value=value):
                validator.SPEC.write_text(original.replace(marker, "frames: " + value, 1))
                self.assertEqual(1, self.check())
                self.assertTrue(any("frame" in error for error in validator.ERRORS))
        for value in ("0,50,100", "000,050,100", "0", "100", '"0, 25, 100"'):
            with self.subTest(value=value):
                validator.SPEC.write_text(original.replace(marker, "frames: " + value, 1))
                self.assertEqual(0, self.check())

    def test_platform_allow_list_must_reach_a_declared_capture_target(self):
        original = validator.SPEC.read_text()
        for row, platforms in (("DesktopButton", "ios"), ("Button", "windows,gnome")):
            with self.subTest(row=row, platforms=platforms):
                marker = "  - id: " + row + "\n"
                validator.SPEC.write_text(original.replace(marker, marker + "    platforms: " + platforms + "\n", 1))
                self.assertEqual(1, self.check())
                self.assertTrue(any("excludes every declared capture target" in error for error in validator.ERRORS))
        for row, platforms in (("DesktopButton", "win"), ("DesktopButton", "ios,window"),
                               ("Button", "and"), ("Button", '"ios"')):
            with self.subTest(row=row, platforms=platforms):
                marker = "  - id: " + row + "\n"
                validator.SPEC.write_text(original.replace(marker, marker + "    platforms: " + platforms + "\n", 1))
                self.assertEqual(0, self.check())
        # The committed SwitchMorph/TabsMorph rows intentionally have only frames.
        validator.SPEC.write_text(original)
        self.assertEqual(0, self.check())

    def test_gnome_rows_reject_linux_host_aliases(self):
        original = validator.SPEC.read_text()
        marker = "  - id: DesktopButton\n"
        for platform in ("linux", "lin", "linux-gnu", "linux,gnome"):
            with self.subTest(platform=platform):
                validator.SPEC.write_text(original.replace(
                    marker, marker + "    platforms: " + platform + "\n", 1))
                self.assertEqual(1, self.check())
                self.assertTrue(any("unknown platform" in error for error in validator.ERRORS))
        validator.SPEC.write_text(original.replace(marker, marker + "    platforms: gnome\n", 1))
        self.assertEqual(0, self.check())

    def test_unknown_defaults_are_rejected(self):
        original = validator.SPEC.read_text()
        for old, new in (("appearances:", "appearance:"), ("tile_width_px:", "tile_wdith_px:"),
                         ("defaults:", "default:")):
            with self.subTest(new=new):
                validator.SPEC.write_text(original.replace(old, new, 1))
                self.assertEqual(1, self.check())

    def test_default_indentation_and_tabs_are_rejected(self):
        original = validator.SPEC.read_text()
        for replacement in (" appearances:", "    appearances:", "\tappearances:",
                            "  appearances\t:", "  appearances "):
            with self.subTest(replacement=replacement):
                validator.SPEC.write_text(original.replace("  appearances:", replacement, 1))
                self.assertEqual(1, self.check())

    def test_invalid_default_values_and_duplicates_are_rejected(self):
        original = validator.SPEC.read_text()
        for old, new in (("tile_width_px: 240", "tile_width_px: 240px"),
                         ("tile_width_px: 240", "tile_width_px: 0"),
                         ("tile_width_px: 240", "tile_width_px: 2147483648"),
                         ("bg: ffffff", "bg: nothex"),
                         ("appearances: light,dark", "appearances: light,drak"),
                         ("appearances: light,dark", "appearances:"),
                         ("appearances: light,dark", "appearances: light,light"),
                         ("appearances: light,dark", "appearances: light,dark\n  appearances: light")):
            with self.subTest(new=new):
                validator.SPEC.write_text(original.replace(old, new, 1))
                self.assertEqual(1, self.check())

    def test_quoted_defaults_and_deliberate_single_appearance_are_valid(self):
        original = validator.SPEC.read_text()
        for appearances in ('"light,dark"', "'light'", "dark"):
            with self.subTest(appearances=appearances):
                validator.SPEC.write_text(original.replace("appearances: light,dark", "appearances: " + appearances)
                                         .replace("tile_width_px: 240", 'tile_width_px: "240"')
                                         .replace("bg: ffffff", "bg: 'ffffff'"))
                self.assertEqual(0, self.check())

    def test_malformed_component_indentation_is_not_silently_ignored(self):
        original = validator.SPEC.read_text()
        validator.SPEC.write_text(original.replace("  - id: Button", "   - id: Button", 1))
        self.assertEqual(1, self.check())

    def test_platform_typo_is_rejected_but_runtime_prefixes_are_valid(self):
        original = validator.SPEC.read_text()
        for token in ("gnmoe", "windwos", "unknown", "linux"):
            with self.subTest(token=token):
                validator.SPEC.write_text(original.replace("platforms: ios", "platforms: " + token, 1))
                self.assertEqual(1, self.check())
                self.assertTrue(any("unknown platform" in e for e in validator.ERRORS))
        for token in ("window", "win", "and", "mac", "gnome"):
            with self.subTest(token=token):
                validator.SPEC.write_text(original.replace("platforms: ios", "platforms: " + token, 1))
                self.assertEqual(0, self.check())

    def test_wrong_widget_label_is_rejected_even_when_both_literals_remain(self):
        changes = {
            "windows": ('Content = "Button"', 'Content = "Option"'),
            "macos": ('NSButton(title: "Button"', 'NSButton(title: "Option"'),
            "gnome": ('gtk_button_new_with_label("Button")', 'gtk_button_new_with_label("Option")'),
        }
        for platform, (old, new) in changes.items():
            with self.subTest(platform=platform):
                path = validator.NATIVE_REF_SOURCES[platform]
                original = path.read_text()
                self.assertIn(old, original)
                path.write_text(original.replace(old, new, 1))
                self.assertEqual(1, self.check())
                self.assertTrue(any("DesktopButton" in e and "renders 'Option'" in e for e in validator.ERRORS))
                path.write_text(original)

    def test_native_table_mapping_must_match_spec(self):
        changes = {"windows": ("winui_button", "winui_combobox"),
                   "macos": ("appkit_push_button", "appkit_popupbutton"),
                   "gnome": ("adw_button", "gtk_dropdown")}
        for platform, (old, new) in changes.items():
            with self.subTest(platform=platform):
                path = validator.NATIVE_REF_SOURCES[platform]
                original = path.read_text()
                path.write_text(original.replace('"' + old + '"', '"' + new + '"', 1))
                self.assertEqual(1, self.check())
                self.assertTrue(any("DesktopButton maps to" in e for e in validator.ERRORS))
                path.write_text(original)

    def test_golden_sets_and_motion_checks_are_validated(self):
        original = validator.SPEC.read_text()
        self.assertIn("golden_sets: ios-27-metal", original)
        self.assertIn("motion_checks: distinct", original)
        cases = (("    golden_sets: ios-27-metal", "    golden_sets: ios-72-metal", "unknown golden set"),
                 ("    motion_checks: distinct", "    motion_checks: sometimes", "unknown motion_checks"))
        for old, new, message in cases:
            with self.subTest(value=new):
                validator.SPEC.write_text(original.replace(old, new, 1))
                self.assertEqual(1, self.check())
                self.assertTrue(any(message in error for error in validator.ERRORS))
        # Both keys only mean something on a frames row.
        validator.SPEC.write_text(original.replace("  - id: Button\n",
                                                   "  - id: Button\n    golden_sets: ios-27-metal\n", 1))
        self.assertEqual(1, self.check())
        self.assertTrue(any("only applies to a frames row" in error for error in validator.ERRORS))

    def test_comments_do_not_mask_a_wrong_label(self):
        path = validator.NATIVE_REF_SOURCES["windows"]
        path.write_text(path.read_text().replace('Content = "Button"', '/* Content = "Button" */ Content = "Option"', 1))
        self.assertEqual(1, self.check())


if __name__ == "__main__":
    unittest.main()

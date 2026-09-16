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
        for token in ("gnmoe", "windwos", "unknown"):
            with self.subTest(token=token):
                validator.SPEC.write_text(original.replace("platforms: ios", "platforms: " + token, 1))
                self.assertEqual(1, self.check())
                self.assertTrue(any("unknown platform" in e for e in validator.ERRORS))
        for token in ("window", "win", "and", "mac", "linux", "gnome"):
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

    def test_comments_do_not_mask_a_wrong_label(self):
        path = validator.NATIVE_REF_SOURCES["windows"]
        path.write_text(path.read_text().replace('Content = "Button"', '/* Content = "Button" */ Content = "Option"', 1))
        self.assertEqual(1, self.check())


if __name__ == "__main__":
    unittest.main()

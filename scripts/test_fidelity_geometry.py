#!/usr/bin/env python3
"""Exercise geometry masking through the compiled screenshot comparator."""
import json
import os
from pathlib import Path
import struct
import subprocess
import tempfile
import unittest
import zlib

ROOT = Path(__file__).resolve().parents[1]


def tile(path, background, foreground, box):
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))

    x0, y0, width, height = box
    rows = bytearray()
    for y in range(80):
        rows.append(0)
        for x in range(120):
            color = foreground if x0 <= x < x0 + width and y0 <= y < y0 + height else background
            rows.extend(bytes.fromhex(color))
    path.write_bytes(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 120, 80, 8, 2, 0, 0, 0))
                     + chunk(b"IDAT", zlib.compress(rows)) + chunk(b"IEND", b""))


class FidelityGeometryTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.classes = tempfile.TemporaryDirectory(prefix="cn1-fidelity-geometry-")
        cls.addClassCleanup(cls.classes.cleanup)
        java_home = os.environ.get("JAVA_HOME")
        cls.java = str(Path(java_home) / "bin/java") if java_home else "java"
        javac = str(Path(java_home) / "bin/javac") if java_home else "javac"
        subprocess.run([javac, "-d", cls.classes.name,
                        str(ROOT / "scripts/common/java/ProcessScreenshots.java")], check=True)

    def test_grouped_backdrops_preserve_field_size_position_and_empty_detection(self):
        with tempfile.TemporaryDirectory(prefix="cn1-geometry-data-") as directory:
            path = Path(directory)
            spec = path / "spec.yaml"
            spec.write_text("components:\n  - id: TextField\n    native: TextField\n"
                            "    material: normal\n    backdrop: grouped\n")
            for appearance, background, foreground in (("light", "f2f2f7", "ffffff"),
                                                       ("dark", "1c1c1e", "2c2c2e")):
                name = "TextField_normal_" + appearance
                tile(path / (name + ".png"), background, foreground, (10, 10, 80, 30))
                actual = path / "actual.png"
                for box in ((10, 10, 80, 30), (20, 15, 60, 20), (0, 0, 0, 0)):
                    with self.subTest(appearance=appearance, box=box):
                        tile(actual, background, foreground, box)
                        result = subprocess.run([self.java, "-Djava.awt.headless=true", "-cp", self.classes.name,
                                                 "ProcessScreenshots", "--mode", "fidelity", "--reference-dir", str(path),
                                                 "--spec", str(spec), "--actual", name + "=" + str(actual)],
                                                capture_output=True, text=True, check=True)
                        geometry = json.loads(result.stdout)["results"][0]["details"]["geometry"]
                        if box[2] == 0:
                            self.assertTrue(geometry["empty"])
                        else:
                            self.assertEqual([10, 10, 80, 30], geometry["native_bbox"])
                            self.assertEqual(list(box), geometry["cn1_bbox"])
                            self.assertAlmostEqual(box[2] / 80, geometry["width_ratio"], places=4)
                            self.assertAlmostEqual(box[3] / 30, geometry["height_ratio"], places=4)


if __name__ == "__main__":
    unittest.main()

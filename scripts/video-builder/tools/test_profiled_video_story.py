#!/usr/bin/env python3
"""Regression tests for the deterministic article-profile story engine."""

from __future__ import annotations

import unittest

import profiled_video_story as story


class ProfiledVideoStoryTest(unittest.TestCase):
    def setUp(self) -> None:
        self.profiles = story.profiles()

    def test_profiles_map_every_problem_once(self) -> None:
        for slug, profile in self.profiles.items():
            with self.subTest(slug=slug):
                problems = profile["problemDimensions"]
                mapped = [item["problem"] for item in profile["resolutionMap"]]
                self.assertEqual(sorted(problems), sorted(mapped))
                self.assertEqual(len(mapped), len(set(mapped)))
                self.assertEqual(2, len(profile["evidence"]))
                hosts = {item["sourceUrl"].split("/")[2] for item in profile["evidence"]}
                self.assertEqual(2, len(hosts))

    def test_cue_ids_are_content_addressed(self) -> None:
        first = story.cue("The same narration.", 250)
        second = story.cue("The same narration.", 250)
        changed = story.cue("A different narration.", 250)
        self.assertEqual(first["id"], second["id"])
        self.assertNotEqual(first["id"], changed["id"])

    def test_every_script_keeps_solution_after_independent_problem(self) -> None:
        narration = {
            "provider": "kokoro", "voice": "af_heart", "language": "en-us",
            "speed": 0.97, "minimumGapMs": 250, "pronunciations": {},
        }
        output = {"frameRate": 30, "landscapeWidth": 1920, "landscapeHeight": 1080,
                  "portraitWidth": 1080, "portraitHeight": 1920}
        for slug, profile in self.profiles.items():
            with self.subTest(slug=slug):
                landscape, portrait = story.scripts(
                    slug, "Title", output, narration, profile, ["capture-1.png"]
                )
                for script in (landscape, portrait):
                    cursor = 0
                    intervention = None
                    evidence_hosts = set()
                    for scene in script["scenes"]:
                        if "intervention" in scene.get("storyBeats", []):
                            intervention = cursor
                        for action in scene["actions"]:
                            if action.get("role") == "evidence":
                                evidence_hosts.add(action["sourceUrl"].split("/")[2])
                        cursor += scene["durationMs"]
                    self.assertIsNotNone(intervention)
                    self.assertGreaterEqual(intervention, 28_000)
                    self.assertEqual(2, len(evidence_hosts))
                    self.assertEqual("approved", script["editorial"]["status"])

    def test_videoio_claim_is_backed_by_writer_code_and_pipeline(self) -> None:
        profile = self.profiles["videoio-audio-mixer-whisper"]
        excerpt = profile.get("codeExcerpt", "")
        self.assertIn("new VideoWriterBuilder()", excerpt)
        self.assertIn("writer.writeFrame", excerpt)
        self.assertIn("writer.writeAudio", excerpt)
        self.assertIn("AudioMixer", excerpt)
        self.assertNotEqual("if (!VideoIO.isSupported())", excerpt.strip().splitlines()[0])
        self.assertTrue(profile.get("selfDemonstratingExport"))


if __name__ == "__main__":
    unittest.main()

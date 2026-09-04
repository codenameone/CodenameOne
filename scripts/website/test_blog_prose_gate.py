#!/usr/bin/env python3

import os
import tempfile
import unittest
from unittest import mock

import blog_prose_gate


class SelfCertifyingLanguageTest(unittest.TestCase):
    def findings(self, text):
        return blog_prose_gate.run_self_certifying_language(text, "post.md")

    def test_rejects_self_certifying_terms(self):
        text = "---\ntitle: Test\n---\n\nThat is the honest boundary. Truthfully, it is not done.\n"
        findings = self.findings(text)
        self.assertEqual(2, len(findings))
        self.assertEqual("SelfCertifyingLanguage", findings[0]["signature"][1])

    def test_accepts_direct_boundary(self):
        text = "---\ntitle: Test\n---\n\nThat is the boundary. The native pass is still required.\n"
        self.assertEqual([], self.findings(text))

    def test_checks_front_matter(self):
        text = "---\ntitle: An Honest Result\n---\n\nThe test reports its inputs.\n"
        findings = self.findings(text)
        self.assertEqual(1, len(findings))
        self.assertEqual(2, findings[0]["line"])


class DeveloperGuideAnchorTest(unittest.TestCase):
    def findings(self, text):
        return blog_prose_gate.run_developer_guide_anchor_links(
            text, "post.md", {"_call_management", "_vpn"}
        )

    def test_accepts_existing_anchor(self):
        text = "Read the [VPN chapter](/developer-guide/#_vpn).\n"
        self.assertEqual([], self.findings(text))

    def test_rejects_missing_anchor(self):
        text = "Read the [VPN chapter](/developer-guide/#vpn).\n"
        findings = self.findings(text)
        self.assertEqual(1, len(findings))
        self.assertEqual("DeveloperGuideAnchor", findings[0]["signature"][1])

    def test_matches_asciidoctor_default_ids(self):
        self.assertEqual(
            "_call_management",
            blog_prose_gate.asciidoc_default_anchor("Call Management"),
        )

    def test_collects_book_part_ids_but_not_the_document_title(self):
        with tempfile.TemporaryDirectory() as repo_root:
            guide_dir = os.path.join(repo_root, "docs", "developer-guide")
            os.makedirs(guide_dir)
            with open(
                os.path.join(guide_dir, "developer-guide.asciidoc"),
                "w",
                encoding="utf-8",
            ) as guide:
                guide.write(
                    "= Codename One Developer Guide\n\n"
                    "= Core concepts\n\n"
                    "== Call Management\n\n"
                    "[id=StructureOfForm, reftext={chapter}.{counter:figure}]\n"
                    "image::structure.png[]\n\n"
                    "[reftext=\"Troubleshooting, Build Errors\", "
                    "id=\"troubleshooting\"]\n"
                    "=== Troubleshooting build errors\n\n"
                    "==== Usage example\n\n"
                    "==== Usage example\n\n"
                    "==== Usage example\n"
                )

            anchors = blog_prose_gate.developer_guide_anchors(repo_root)

        self.assertIn("_core_concepts", anchors)
        self.assertIn("_call_management", anchors)
        self.assertIn("StructureOfForm", anchors)
        self.assertIn("troubleshooting", anchors)
        self.assertIn("_usage_example", anchors)
        self.assertIn("_usage_example_2", anchors)
        self.assertIn("_usage_example_3", anchors)
        self.assertNotIn("_usage_example_4", anchors)
        self.assertNotIn("_codename_one_developer_guide", anchors)

    @mock.patch.object(blog_prose_gate, "_git")
    def test_collects_base_anchors_from_requested_git_revision(self, git):
        git.side_effect = [
            mock.Mock(
                returncode=0,
                stdout="docs/developer-guide/developer-guide.asciidoc\n",
                stderr="",
            ),
            mock.Mock(
                returncode=0,
                stdout="= Old Guide\n\n= Old Part\n\n== Old Section\n",
                stderr="",
            ),
        ]

        anchors = blog_prose_gate.developer_guide_anchors(".", "base-sha")

        self.assertEqual({"_old_part", "_old_section"}, anchors)
        self.assertEqual(
            mock.call(
                [
                    "ls-tree",
                    "-r",
                    "--name-only",
                    "base-sha",
                    "--",
                    "docs/developer-guide",
                ],
                ".",
            ),
            git.call_args_list[0],
        )
        self.assertEqual(
            mock.call(
                [
                    "show",
                    "base-sha:docs/developer-guide/developer-guide.asciidoc",
                ],
                ".",
            ),
            git.call_args_list[1],
        )

    @mock.patch.object(blog_prose_gate, "run_self_certifying_language", return_value=[])
    @mock.patch.object(blog_prose_gate, "run_capcheck", return_value=[])
    @mock.patch.object(blog_prose_gate, "run_vale", return_value=[])
    @mock.patch.object(blog_prose_gate, "base_content")
    @mock.patch.object(blog_prose_gate, "head_content")
    def test_guide_rename_is_compared_with_base_anchors(
        self, head_content, base_content, _run_vale, _run_capcheck, _run_house
    ):
        text = "Read the [old section](/developer-guide/#_old_section).\n"
        head_content.return_value = text
        base_content.return_value = text

        findings = blog_prose_gate.gate_file(
            "post.md",
            "base-sha",
            ".",
            None,
            {"_new_section"},
            {"_old_section"},
        )

        self.assertEqual(1, len(findings))
        self.assertEqual("_old_section", findings[0]["signature"][2])


if __name__ == "__main__":
    unittest.main()

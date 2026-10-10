#!/usr/bin/env python3

import os
from pathlib import Path
import subprocess
import tempfile
import textwrap
import unittest
from unittest import mock

import blog_prose_gate


class WorkflowInputDetectionTest(unittest.TestCase):
    def test_deleted_render_gate_inputs_still_trigger_validation(self):
        workflow = (Path(__file__).resolve().parents[2]
                    / ".github/workflows/blog-prose.yml").read_text()
        # Exercise the actual workflow step against committed Git changes, so
        # removing D from its diff filter breaks this test as well as the gate.
        detect = workflow.split("        id: detect\n", 1)[1]
        script = textwrap.dedent(detect.split("        run: |\n", 1)[1]
                                 .split("\n      - ", 1)[0])
        inputs = (
            "scripts/website/validate_mermaid.mjs",
            "scripts/website/test_validate_mermaid.mjs",
            "docs/website/layouts/shortcodes/mermaid.html",
            "docs/website/hugo.toml",
            "docs/website/content/blog/deleted-post.md",
        )
        for deleted in (*inputs, "unrelated.txt"):
            with self.subTest(deleted=deleted), tempfile.TemporaryDirectory() as tmp:
                root = Path(tmp)

                def git(*args):
                    return subprocess.run(
                        ["git", "-c", "user.name=Gate test", "-c", "user.email=gate@example.invalid",
                         "-c", "commit.gpgsign=false", "-c", "core.hooksPath=/dev/null", *args],
                        cwd=root, check=True, capture_output=True, text=True,
                    )

                git("init", "-q")
                for name in (*inputs, "unrelated.txt"):
                    file = root / name
                    file.parent.mkdir(parents=True, exist_ok=True)
                    file.write_text("fixture\n")
                git("add", ".")
                git("commit", "-qm", "Base inputs")
                git("update-ref", "refs/remotes/origin/base", "HEAD")
                git("rm", deleted)
                git("commit", "-qm", "Delete input")
                output = root / "step-output"
                subprocess.run(
                    ["bash", "-c", script], cwd=root, check=True, capture_output=True,
                    env={**os.environ, "GITHUB_BASE_REF": "base", "GITHUB_OUTPUT": str(output)},
                )
                expected = "false" if deleted == "unrelated.txt" else "true"
                self.assertEqual(f"has_posts={expected}\n", output.read_text())


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

    def test_accepts_existing_anchor_in_absolute_url(self):
        text = (
            "Read the [VPN chapter]"
            "(https://www.codenameone.com/developer-guide/#_vpn).\n"
        )
        self.assertEqual([], self.findings(text))

    def test_rejects_missing_anchor(self):
        text = "Read the [VPN chapter](/developer-guide/#vpn).\n"
        findings = self.findings(text)
        self.assertEqual(1, len(findings))
        self.assertEqual("DeveloperGuideAnchor", findings[0]["signature"][1])

    def test_rejects_missing_anchor_in_absolute_url(self):
        text = (
            "Read the [VPN chapter]"
            "(https://www.codenameone.com/developer-guide/#vpn).\n"
        )
        findings = self.findings(text)
        self.assertEqual(1, len(findings))
        self.assertEqual("vpn", findings[0]["signature"][2])

    def test_ignores_external_developer_guide_url(self):
        text = "Read [another guide](https://example.com/developer-guide/#vpn).\n"
        self.assertEqual([], self.findings(text))

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

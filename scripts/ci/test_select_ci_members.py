#!/usr/bin/env python3
"""Unit tests for select-ci-members.py (run by check-workflows.yml)."""
import importlib.util
import json
import os
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("sel", os.path.join(HERE, "select-ci-members.py"))
sel = importlib.util.module_from_spec(spec)
spec.loader.exec_module(sel)
MEMBERS = json.loads(sel.MEMBERS.read_text())


class SelectTest(unittest.TestCase):
    def test_globs(self):
        self.assertTrue(sel.matches(["CodenameOne/src/**"], ["CodenameOne/src/a/b.java"]))
        self.assertFalse(sel.matches(["scripts/*.sh"], ["scripts/a/b.sh"]))
        self.assertTrue(sel.matches(["scripts/*.sh"], ["scripts/b.sh"]))
        self.assertTrue(sel.matches(["**/*.md"], ["README.md"]))

    def test_last_match_wins(self):
        pats = ["CodenameOne/src/**", "!CodenameOne/src/**/*.md"]
        self.assertFalse(sel.matches(pats, ["CodenameOne/src/a/x.md"]))
        self.assertTrue(sel.matches(pats, ["CodenameOne/src/a/x.md", "CodenameOne/src/a/x.java"]))

    def test_member_filters(self):
        r = sel.select(MEMBERS, "pull_request", ["Ports/iOSPort/src/a.java"])
        self.assertTrue(r["scripts-ios"])
        self.assertFalse(r["scripts-android"])
        # No paths at all (blog-prose) runs on every pull request.
        self.assertTrue(sel.select(MEMBERS, "pull_request", ["README.md"])["blog-prose"])
        # developer-guide-docs never had a push trigger.
        self.assertFalse(sel.select(MEMBERS, "push", ["docs/developer-guide/a.adoc"])["developer-guide-docs"])

    def test_unknown_or_self_runs_everything(self):
        for files in (None, [".github/workflows/ci.yml"], ["scripts/ci/select-ci-members.py"]):
            self.assertTrue(all(sel.select(MEMBERS, "pull_request", files).values()), files)
        self.assertTrue(all(sel.select(MEMBERS, "workflow_dispatch", ["README.md"]).values()))

    def test_every_member_is_called_by_ci(self):
        root = sel.ROOT
        ci = (root / ".github/workflows/ci.yml").read_text()
        for m in MEMBERS:
            self.assertIn("uses: ./.github/workflows/%s.yml" % m, ci)
            wf = (root / (".github/workflows/%s.yml" % m)).read_text()
            self.assertIn("workflow_call:", wf, m)
            self.assertNotIn("\n  pull_request:", wf, m)


if __name__ == "__main__":
    unittest.main()

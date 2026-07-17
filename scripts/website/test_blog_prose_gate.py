#!/usr/bin/env python3

import unittest

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


if __name__ == "__main__":
    unittest.main()

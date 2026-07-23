import unittest

from reconcile_past_year_state import reconcile


class ReconcileTest(unittest.TestCase):
    def test_retains_complete_pair_and_marks_missing_pair_authored(self):
        state = {"items": [
            {"slug": "kept", "status": "verified-private",
             "landscapeId": "aaaaaaaaaaa", "shortId": "bbbbbbbbbbb"},
            {"slug": "gone", "status": "verified-private",
             "landscapeId": "ccccccccccc", "shortId": "ddddddddddd"},
        ]}
        catalog = {"channelId": "channel", "videos": [
            {"videoId": "aaaaaaaaaaa", "privacy": "public", "kind": "landscape"},
            {"videoId": "bbbbbbbbbbb", "privacy": "public", "kind": "portrait"},
        ]}
        reconciled, summary = reconcile(state, catalog)
        self.assertEqual(["kept"], summary["retained"])
        self.assertEqual(["gone"], summary["missing"])
        self.assertEqual("verified-public", reconciled["items"][0]["status"])
        self.assertEqual("authored", reconciled["items"][1]["status"])
        self.assertIsNone(reconciled["items"][1]["landscapeId"])
        self.assertEqual("ccccccccccc", reconciled["items"][1]["previousRemote"]["landscapeId"])


if __name__ == "__main__":
    unittest.main()

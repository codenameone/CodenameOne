#!/usr/bin/env python3
"""Verify a release-gate record against the build server.

The record is written by run_gate.py on a developer machine and pushed as the
message of an annotated tag. Nothing in it is trusted except the build ids: every
fact that decides pass or fail (status, owner, when it was submitted, which daemon
built it, how big the artifacts are) is re-read here from BuildCloud's evidence
endpoint, which only the build server can write. So a record that was edited, made
against a smaller matrix, assembled from someone else's builds, or reused from an
older release fails here, however it was produced.

There is deliberately no flag that skips a check. Rollback is the only ungated
path, and the workflows never call this for a rollback.

Exit status 0 means verified; anything else prints every reason it was not.

Usage (the workflows pass everything; see deploy-linux.yml / deploy-mac.yml):
  verify_gate.py --record rec.json --matrix matrix.json --matrix-sha256 <var>
                 --kind daemon|cn1 --subject <release tag or cn1 sha>
                 --commit <sha the subject was built from> --phase 1|2
                 --not-before <epoch ms the release was created>
                 --gate-account gate@codenameone.com
                 [--previous-record prev.json]
                 [--funnel-versions 7.0.273,7.0.272]
  env: RELEASE_GATE_SERVER (default https://cloud.codenameone.com),
       RELEASE_GATE_ADMIN_TOKEN
"""
import argparse
import hashlib
import json
import os
import re
import sys
import urllib.error
import urllib.parse
import urllib.request

FREE_TIER = 1000


class ServerEvidence:
    """Reads the two admin endpoints described in SERVER-CONTRACT.md."""

    def __init__(self, base, token):
        self.base = base.rstrip("/")
        self.token = token

    def _get(self, path, params):
        url = self.base + path + "?" + urllib.parse.urlencode(params)
        req = urllib.request.Request(url, headers={
            "Authorization": "Bearer " + self.token, "Accept": "application/json"})
        try:
            with urllib.request.urlopen(req, timeout=30) as r:
                return json.loads(r.read().decode("utf-8"))
        except urllib.error.HTTPError as e:
            if e.code == 404:
                return None
            raise

    def build(self, build_id):
        return self._get("/api/v2/admin/release-gate/build-evidence", {"id": build_id})

    def funnel(self, core_version, days):
        return self._get("/api/v2/admin/release-gate/release-funnel", {"cv": core_version, "days": days})


def matrix_sha256(path):
    with open(path, "rb") as f:
        return hashlib.sha256(f.read()).hexdigest()


def artifact_bytes(ev):
    return sum(int(a.get("bytes") or 0) for a in (ev.get("artifacts") or []))


def verify(record, matrix, *, matrix_sha, expected_matrix_sha, kind, subject, commit,
           phase, not_before, gate_account, evidence, previous_record=None,
           funnel_versions=None):
    """Returns a list of failure strings; empty means verified."""
    errors = []
    fail = errors.append

    if not expected_matrix_sha:
        fail("RELEASE_GATE_MATRIX_SHA256 is not set; there is no approved matrix to verify against")
    elif matrix_sha != expected_matrix_sha:
        fail("matrix.json at %s hashes to %s, not the approved %s"
             % (commit[:12], matrix_sha, expected_matrix_sha))
    if record.get("matrixSha256") != matrix_sha:
        fail("record was made against matrix %s, not %s" % (record.get("matrixSha256"), matrix_sha))
    if record.get("kind") != kind or record.get("subject") != subject:
        fail("record is for %s %s, not %s %s"
             % (record.get("kind"), record.get("subject"), kind, subject))
    if record.get("commit") != commit:
        fail("record names commit %s, the release was built from %s" % (record.get("commit"), commit))
    if int(record.get("phase", 0)) != phase:
        fail("record is phase %s, phase %s is required" % (record.get("phase"), phase))
    if phase == 2 and kind != "daemon":
        fail("phase 2 only exists for daemon releases")

    sha7 = commit[:7]
    recorded = {}
    for r in record.get("rows", []):
        if r.get("row") in recorded:
            fail("row %s appears twice in the record" % r.get("row"))
        recorded[r.get("row")] = r

    expected = [r for r in matrix["rows"] if phase in r.get("phases", [])]
    expected_ids = {r["id"] for r in expected}
    for extra in sorted(set(recorded) - expected_ids):
        fail("record has row %s, which the approved matrix does not" % extra)

    prev_sizes = {}
    if previous_record:
        for r in previous_record.get("rows", []):
            if r.get("buildId"):
                ev = evidence.build(r["buildId"])
                if ev and ev.get("status") == "success":
                    prev_sizes[r["row"]] = artifact_bytes(ev)

    growth = float(matrix.get("maxArtifactGrowth", 0.25))
    used_ids = set()
    for row in expected:
        rid = row["id"]
        rec = recorded.get(rid)
        if rec is None:
            fail("%s: missing from the record" % rid)
            continue
        if row["kind"] == "local":
            # Local builds never reach the server, so there is nothing to re-read.
            # The runner refuses to write a record unless they passed.
            if rec.get("result") != "pass":
                fail("%s: local row not recorded as pass" % rid)
            continue
        bid = rec.get("buildId")
        if not bid:
            fail("%s: no build id" % rid)
            continue
        if bid in used_ids:
            fail("%s: build %s is already used by another row" % (rid, bid))
        used_ids.add(bid)
        ev = evidence.build(bid)
        if ev is None:
            fail("%s: build %s is unknown to the server" % (rid, bid))
            continue
        if ev.get("status") != "success":
            fail("%s: build %s finished %s (%s)" % (rid, bid, ev.get("status"), ev.get("failureReason")))
        if (ev.get("owner") or "").lower() != gate_account.lower():
            fail("%s: build %s belongs to %s, not the gate account" % (rid, bid, ev.get("owner")))
        if int(ev.get("submittedAt") or 0) < not_before:
            fail("%s: build %s was submitted before this release existed" % (rid, bid))
        want_name = "%s-%s" % (sha7, rid)
        app = ev.get("appName") or ""
        # A suffix is allowed after the row id (the hint fixture adds non-ASCII to it).
        if app != want_name and not app.startswith(want_name + "-"):
            fail("%s: build %s is app %r, expected %r" % (rid, bid, ev.get("appName"), want_name))
        if phase == 2:
            want_target = "debug_gate_%s_%s" % (row["platform"], subject)
            if ev.get("target") != want_target:
                fail("%s: build %s ran on queue %r, not the canary queue %r"
                     % (rid, bid, ev.get("target"), want_target))
            if ev.get("daemonVersion") != subject:
                fail("%s: build %s was built by daemon %r, not %r"
                     % (rid, bid, ev.get("daemonVersion"), subject))
        size = artifact_bytes(ev)
        if size <= 0:
            fail("%s: build %s produced no artifact" % (rid, bid))
        if size > int(row.get("maxArtifactBytes", 0) or 0):
            fail("%s: artifacts total %d bytes, over the %d ceiling"
                 % (rid, size, row.get("maxArtifactBytes")))
        base = prev_sizes.get(rid)
        if base and size > base * (1 + growth):
            fail("%s: artifacts grew from %d to %d bytes (more than %d%%)"
                 % (rid, base, size, int(growth * 100)))

    if phase == 1:
        verify_funnel_walk(record, matrix, sha7, not_before, evidence, used_ids, fail)
        if funnel_versions:
            verify_funnel_regression(matrix, funnel_versions, evidence, fail)
    return errors


def verify_funnel_walk(record, matrix, sha7, not_before, evidence, used_ids, fail):
    fcfg = matrix["funnel"]
    walks = {w.get("route"): w for w in record.get("funnel", [])}
    for route in fcfg["routes"]:
        w = walks.get(route)
        if w is None:
            fail("funnel %s: route not walked" % route)
            continue
        account = (w.get("account") or "").lower()
        if not re.search(r"\+gate-%s-%s(-[0-9a-f]+)?@" % (re.escape(sha7), re.escape(route)), account):
            fail("funnel %s: account %r is not a fresh gate account for %s" % (route, account, sha7))
        for step in fcfg["steps"]:
            if (w.get("steps") or {}).get(step) != "pass":
                fail("funnel %s: step %s not passed" % (route, step))
        for step in fcfg["buildSteps"]:
            bid = (w.get("buildIds") or {}).get(step)
            if not bid:
                fail("funnel %s: no build id for %s" % (route, step))
                continue
            if bid in used_ids and step != "first_success":
                fail("funnel %s: build %s reused" % (route, bid))
            used_ids.add(bid)
            ev = evidence.build(bid)
            if ev is None:
                fail("funnel %s: build %s unknown to the server" % (route, bid))
                continue
            if (ev.get("owner") or "").lower() != account:
                fail("funnel %s: build %s belongs to %s" % (route, bid, ev.get("owner")))
            if int(ev.get("ownerCreatedAt") or 0) < not_before:
                fail("funnel %s: account existed before this release; not a new user" % route)
            if int(ev.get("ownerTier") or 0) != FREE_TIER:
                fail("funnel %s: account tier %s, a new user is free" % (route, ev.get("ownerTier")))
            if int(ev.get("submittedAt") or 0) < not_before:
                fail("funnel %s: build %s predates the release" % (route, bid))
            if ev.get("status") != "success":
                fail("funnel %s: %s build %s finished %s" % (route, step, bid, ev.get("status")))


def verify_funnel_regression(matrix, versions, evidence, fail):
    """versions = [last released, the one before]; last week's release must not have
    made first-build outcomes measurably worse than the release before it."""
    cfg = matrix["funnelRegression"]
    days = int(cfg["comparedAfterDays"])
    cur = evidence.funnel(versions[0], days)
    prev = evidence.funnel(versions[1], days)
    if cur is None or prev is None:
        fail("funnel report for %s vs %s is unavailable" % (versions[0], versions[1]))
        return
    if not cur.get("windowComplete"):
        fail("funnel window for %s is not complete yet (%d days)" % (versions[0], days))
        return
    if int(cur.get("firstBuilders") or 0) < int(cfg["minFirstBuilders"]):
        # Too few new builders to compare rates; absence of users is itself what
        # the funnel walk above has to explain, so this is not a pass by default.
        fail("only %s first-time builders on %s (minimum %s): the funnel is not producing builders"
             % (cur.get("firstBuilders"), versions[0], cfg["minFirstBuilders"]))
        return
    for metric, max_drop in cfg["maxDrop"].items():
        a, b = cur.get(metric), prev.get(metric)
        if a is None or b is None:
            fail("funnel metric %s missing" % metric)
        elif b - a > max_drop:
            fail("funnel %s fell from %.2f on %s to %.2f on %s"
                 % (metric, b, versions[1], a, versions[0]))


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("--record", required=True)
    ap.add_argument("--matrix", required=True)
    ap.add_argument("--matrix-sha256", default=os.environ.get("RELEASE_GATE_MATRIX_SHA256", ""))
    ap.add_argument("--kind", required=True, choices=["daemon", "cn1"])
    ap.add_argument("--subject", required=True)
    ap.add_argument("--commit", required=True)
    ap.add_argument("--phase", required=True, type=int, choices=[1, 2])
    ap.add_argument("--not-before", required=True, type=int)
    ap.add_argument("--gate-account", required=True)
    ap.add_argument("--previous-record")
    ap.add_argument("--funnel-versions")
    a = ap.parse_args(argv)

    token = os.environ.get("RELEASE_GATE_ADMIN_TOKEN", "")
    if not token:
        print("RELEASE_GATE_ADMIN_TOKEN is not set", file=sys.stderr)
        return 2
    with open(a.record) as f:
        record = json.load(f)
    with open(a.matrix) as f:
        matrix = json.load(f)
    prev = None
    if a.previous_record:
        with open(a.previous_record) as f:
            prev = json.load(f)
    evidence = ServerEvidence(os.environ.get("RELEASE_GATE_SERVER", "https://cloud.codenameone.com"), token)
    errors = verify(record, matrix,
                    matrix_sha=matrix_sha256(a.matrix), expected_matrix_sha=a.matrix_sha256,
                    kind=a.kind, subject=a.subject, commit=a.commit, phase=a.phase,
                    not_before=a.not_before, gate_account=a.gate_account, evidence=evidence,
                    previous_record=prev,
                    funnel_versions=a.funnel_versions.split(",") if a.funnel_versions else None)
    if errors:
        print("Release gate NOT verified (%d problems):" % len(errors))
        for e in errors:
            print("  - " + e)
        return 1
    print("Release gate verified: %s %s phase %d" % (a.kind, a.subject, a.phase))
    return 0


if __name__ == "__main__":
    sys.exit(main())

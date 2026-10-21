#!/usr/bin/env python3
"""Build reviewed social links and summarize existing aggregate BuildCloud exports.

No network, credentials, public publishing, or raw user export is involved.
"""
import argparse
import json
import re
from urllib.parse import urlsplit, urlunsplit, parse_qsl, urlencode

SOURCES = {"bluesky", "mastodon", "x", "linkedin-page", "linkedin-shai",
           "youtube-codenameone", "youtube-debugagent", "reddit"}
TOKEN = re.compile(r"^[a-z0-9][a-z0-9_.:-]{0,127}$")
FIELDS = ("signedUp", "reachedConsole", "attempted", "successful", "active30d")


def campaign_link(url, source, campaign, item):
    parts = urlsplit(url)
    if parts.scheme != "https" or parts.netloc not in {"www.codenameone.com", "codenameone.com", "cloud.codenameone.com"}:
        raise ValueError("Only canonical Codename One HTTPS destinations may be tagged")
    if source not in SOURCES or not TOKEN.fullmatch(campaign) or not TOKEN.fullmatch(item):
        raise ValueError("Invalid source, campaign or stable batch/item ID")
    query = [(k, v) for k, v in parse_qsl(parts.query, keep_blank_values=True) if not k.startswith("utm_")]
    query += [("utm_source", source), ("utm_medium", "community" if source == "reddit" else "social"),
              ("utm_campaign", campaign), ("utm_content", item)]
    return urlunsplit(parts._replace(query=urlencode(query)))


def summarize(payload, window):
    funnel = payload.get("funnelCohorts", payload)
    w = funnel.get("windows", {}).get(window)
    if not isinstance(w, dict) or w.get("diagnosticsAvailable") is False:
        raise ValueError("Requested cohort/attribution diagnostics unavailable; do not report zero")
    a = w.get("acquisitionAttribution")
    if not isinstance(a, dict) or not isinstance(a.get("byCampaign"), list):
        raise ValueError("Aggregate campaign rows missing; do not report zero")
    rows, all_signups, all_successes = [], 0, 0
    for c in a["byCampaign"]:
        if any(type(c.get(k)) is not int or c[k] < 0 for k in FIELDS):
            raise ValueError("Incomplete or invalid campaign counts")
        if not c["successful"] <= c["attempted"] <= c["signedUp"]:
            raise ValueError("Campaign funnel counts do not reconcile")
        all_signups += c["signedUp"]
        all_successes += c["successful"]
        if c.get("medium") not in {"social", "community"} or c.get("source") not in SOURCES:
            continue
        # Explicit field allowlist: never carry recent-user identities into reports.
        rows.append({k: c.get(k) for k in ("source", "medium", "campaign", "content", *FIELDS)})
    eligible = a.get("eligibleSignups")
    if type(eligible) is not int or eligible != all_signups:
        raise ValueError("Campaign signup denominator does not reconcile")
    headline = w.get("activated")
    return {"cohort_window": window, "days": w.get("days"), "eligible_signups": eligible,
            "attribution_rows": a.get("attributed"), "coverage_pct": a.get("coveragePct"),
            "campaign_successes_all_sources": all_successes, "headline_activated": headline,
            "successes_reconcile_with_headline": None if headline is None else headline == all_successes,
            "social_posts": rows,
            "social_attributed_new_users_with_successful_build": sum(c["successful"] for c in rows),
            "definition": "Unique new users in this signup cohort with at least one observed successful build; not build counts, click conversions, or causal lift.",
            "limitations": ["First-touch attribution; missing/declined/expired/cross-device journeys remain unassigned.",
                            "Rolling signup cohort outcomes as of export, not first-build events occurring in the reporting week.",
                            "A first attempt may fail before the first successful build. Do not use firstAttemptSucceeded as activation."]}


def main():
    p = argparse.ArgumentParser(description=__doc__)
    sub = p.add_subparsers(dest="command", required=True)
    link = sub.add_parser("link")
    for name in ("url", "source", "campaign", "item"):
        link.add_argument("--" + name, required=True)
    report = sub.add_parser("report")
    report.add_argument("export", help="Private saved aggregate funnel-cohorts JSON")
    report.add_argument("--window", default="d30", choices=["d30", "d90"])
    a = p.parse_args()
    try:
        if a.command == "link":
            print(campaign_link(a.url, a.source, a.campaign, a.item))
        else:
            with open(a.export) as f:
                print(json.dumps(summarize(json.load(f), a.window), indent=2))
    except (ValueError, OSError) as e:
        p.exit(2, str(e) + "\n")


if __name__ == "__main__":
    main()

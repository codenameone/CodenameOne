#!/usr/bin/env python3
"""Diff-scoped, net-new blog prose gate.

The developer guide runs its prose checks whole-corpus because the guide is one
actively maintained book. The blog is 854 posts, most of them historical
archives carrying pre-existing nits. A whole-file gate would block any PR that
touches an old post on legacy issues the author never introduced — exactly the
"month of review" trap.

So this gate is NET-NEW: for every blog post changed in the PR, it runs the
checks on both the base (merge-base) and head versions of the file and fails
only on findings the diff INTRODUCED. Editing an old post is painless — you are
only responsible for not adding new problems; the backlog stays grandfathered.

Checks:
  * Vale          (subprocess; uses docs/website/.vale.ini)
  * Paragraph cap (in-process; check_paragraph_capitalization.find_lowercase_paragraphs)
  * LanguageTool  (in-process; render_blog_prose_html + run_languagetool;
                   skipped automatically if language_tool_python is unavailable)

A finding "signature" is (tool, rule, matched-text) with multiplicity, so line
shifts never look like new findings and N pre-existing copies stay suppressed.
"""

import argparse
import collections
import json
import os
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import blog_text  # noqa: E402
import check_paragraph_capitalization as capcheck  # noqa: E402

REPO_ROOT_DEFAULT = os.path.dirname(os.path.dirname(HERE))
VALE_CONFIG = "docs/website/.vale.ini"
DEV_GUIDE_ACCEPT = "docs/developer-guide/languagetool-accept.txt"
BLOG_ACCEPT = "scripts/website/languagetool-accept-blog.txt"


# --------------------------------------------------------------------------- #
# git helpers
# --------------------------------------------------------------------------- #
def _git(args, repo_root):
    return subprocess.run(
        ["git", *args], cwd=repo_root, capture_output=True, text=True
    )


def merge_base(base_ref, repo_root):
    r = _git(["merge-base", base_ref, "HEAD"], repo_root)
    if r.returncode == 0 and r.stdout.strip():
        return r.stdout.strip()
    return base_ref


def changed_posts(base_sha, repo_root):
    r = _git(
        [
            "diff",
            "--name-only",
            "--diff-filter=ACMR",
            base_sha,
            "HEAD",
            "--",
            "docs/website/content/blog/*.md",
        ],
        repo_root,
    )
    if r.returncode != 0:
        sys.stderr.write(r.stderr)
        return []
    return [p for p in r.stdout.splitlines() if p.strip()]


def base_content(base_sha, path, repo_root):
    r = _git(["show", f"{base_sha}:{path}"], repo_root)
    return r.stdout if r.returncode == 0 else ""


def head_content(path, repo_root):
    full = os.path.join(repo_root, path)
    if not os.path.exists(full):
        return ""
    with open(full, "r", encoding="utf-8") as fh:
        return fh.read()


# --------------------------------------------------------------------------- #
# checkers — each returns a list of {signature, file, line, message} findings
# --------------------------------------------------------------------------- #
def run_vale(text, rel, repo_root):
    if not text.strip():
        return []
    with tempfile.NamedTemporaryFile(
        "w", suffix=".md", delete=False, encoding="utf-8"
    ) as tf:
        tf.write(text)
        tmp = tf.name
    try:
        r = subprocess.run(
            ["vale", "--config", VALE_CONFIG, "--minAlertLevel=suggestion",
             "--output=JSON", tmp],
            cwd=repo_root, capture_output=True, text=True,
        )
        out = r.stdout.strip()
        if not out:
            return []
        data = json.loads(out)
    except (json.JSONDecodeError, OSError) as exc:
        sys.stderr.write(f"vale failed on {rel}: {exc}\n")
        return []
    finally:
        os.unlink(tmp)
    findings = []
    for items in data.values():
        for it in items:
            findings.append({
                "signature": ("vale", it.get("Check", ""), it.get("Match", "")),
                "file": rel,
                "line": it.get("Line", 0),
                "message": f"{it.get('Check','')}: {it.get('Message','')}",
            })
    return findings


def run_capcheck(text, rel):
    _fm, body, body_start = blog_text.split_front_matter(text)
    out = []
    for f in capcheck.find_lowercase_paragraphs(body, body_start, rel):
        out.append({
            "signature": ("cap", "ParagraphCapitalization", f["word"] + "|" + f["excerpt"]),
            "file": rel,
            "line": f["line"],
            "message": f"Paragraph starts with lowercase '{f['word']}' — {f['excerpt']}",
        })
    return out


def _lt_module():
    """Import run_languagetool lazily; return None if unavailable."""
    try:
        import importlib.util
        path = os.path.join(REPO_ROOT_DEFAULT, "scripts", "developer-guide",
                            "run_languagetool.py")
        spec = importlib.util.spec_from_file_location("run_languagetool", path)
        mod = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(mod)
        return mod
    except Exception as exc:  # noqa: BLE001
        sys.stderr.write(f"LanguageTool module unavailable: {exc}\n")
        return None


def run_languagetool(text, rel, repo_root, accept_path):
    if not text.strip():
        return []
    lt = _lt_module()
    if lt is None:
        return None  # signal "skipped" so base/head stay symmetric
    try:
        html = blog_text.body_to_html(text)
    except ImportError as exc:
        # python-markdown not installed — degrade gracefully (Vale + cap still
        # run), exactly as we do when language_tool_python is missing.
        sys.stderr.write(f"python-markdown unavailable; skipping LanguageTool: {exc}\n")
        return None
    with tempfile.NamedTemporaryFile(
        "w", suffix=".html", delete=False, encoding="utf-8"
    ) as tf:
        tf.write(html)
        tmp = tf.name
    try:
        extracted = lt.extract_text(tmp)
        accept_re = lt.load_accept_patterns(accept_path)
        matches = lt.run_languagetool(extracted, accept_re=accept_re)
        if matches is None:
            return None  # language_tool_python not installed
        serialized = lt.matches_to_json(matches, extracted)
    except Exception as exc:  # noqa: BLE001 — advisory check must never crash the gate
        sys.stderr.write(f"LanguageTool failed on {rel}: {exc}\n")
        return []
    finally:
        os.unlink(tmp)
    out = []
    for m in serialized:
        out.append({
            "signature": ("lt", m.get("rule", ""), (m.get("context", "") or "").strip()),
            "file": rel,
            "line": m.get("line", 0),
            "message": f"{m.get('rule','')}: {m.get('message','')}",
        })
    return out


# --------------------------------------------------------------------------- #
# net-new diff
# --------------------------------------------------------------------------- #
def net_new(head_findings, base_findings):
    """Return head findings whose signature isn't already present in base."""
    base_counts = collections.Counter(f["signature"] for f in base_findings)
    new = []
    for f in head_findings:
        sig = f["signature"]
        if base_counts.get(sig, 0) > 0:
            base_counts[sig] -= 1
        else:
            new.append(f)
    return new


def build_accept_file(repo_root):
    """Concatenate the guide accept-list with the blog overlay into a temp file."""
    parts = []
    for rel in (DEV_GUIDE_ACCEPT, BLOG_ACCEPT):
        p = os.path.join(repo_root, rel)
        if os.path.exists(p):
            with open(p, "r", encoding="utf-8") as fh:
                parts.append(fh.read())
    fd, path = tempfile.mkstemp(suffix=".txt")
    with os.fdopen(fd, "w", encoding="utf-8") as fh:
        fh.write("\n".join(parts))
    return path


def gate_file(path, base_sha, repo_root, accept_path, with_lt):
    head = head_content(path, repo_root)
    base = base_content(base_sha, path, repo_root)
    new = []
    new += net_new(run_vale(head, path, repo_root), run_vale(base, path, repo_root))
    new += net_new(run_capcheck(head, path), run_capcheck(base, path))
    if with_lt:
        head_lt = run_languagetool(head, path, repo_root, accept_path)
        base_lt = run_languagetool(base, path, repo_root, accept_path)
        if head_lt is not None and base_lt is not None:
            new += net_new(head_lt, base_lt)
    return new


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--base-ref", default="origin/master",
                    help="PR base ref to diff against (default: origin/master)")
    ap.add_argument("--repo-root", default=REPO_ROOT_DEFAULT)
    ap.add_argument("--report", help="Write a JSON report of net-new findings here")
    ap.add_argument("--no-languagetool", action="store_true",
                    help="Skip the LanguageTool stage (Vale + cap only)")
    ap.add_argument("paths", nargs="*",
                    help="Explicit post paths; default = git-changed posts")
    args = ap.parse_args()

    repo_root = os.path.abspath(args.repo_root)
    base_sha = merge_base(args.base_ref, repo_root)
    posts = args.paths or changed_posts(base_sha, repo_root)
    if not posts:
        print("No changed blog posts — prose gate passes.")
        if args.report:
            with open(args.report, "w", encoding="utf-8") as fh:
                json.dump({"total": 0, "findings": []}, fh, indent=2)
        return 0

    accept_path = build_accept_file(repo_root)
    try:
        all_new = []
        for path in posts:
            all_new.extend(
                gate_file(path, base_sha, repo_root, accept_path,
                          with_lt=not args.no_languagetool)
            )
    finally:
        os.unlink(accept_path)

    if args.report:
        os.makedirs(os.path.dirname(args.report) or ".", exist_ok=True)
        with open(args.report, "w", encoding="utf-8") as fh:
            json.dump(
                {"total": len(all_new),
                 "findings": [{k: v for k, v in f.items() if k != "signature"}
                              for f in all_new]},
                fh, indent=2,
            )

    print(f"Checked {len(posts)} changed post(s) against {base_sha[:12]}.")
    if all_new:
        print(f"\n✖ {len(all_new)} net-new prose finding(s) introduced by this PR:\n",
              file=sys.stderr)
        for f in all_new:
            print(f"  {f['file']}:{f['line']}: {f['message']}", file=sys.stderr)
        print(
            "\nThese were introduced by your change (pre-existing issues in the "
            "same file are ignored).\nFix the prose, add a term to "
            "scripts/website/languagetool-accept-blog.txt or the CodenameOne "
            "vocab, or add a `<!-- vale-skip: rule: reason -->` comment.",
            file=sys.stderr,
        )
        return 1
    print("✔ No net-new prose findings — prose gate passes.")
    return 0


if __name__ == "__main__":
    sys.exit(main())

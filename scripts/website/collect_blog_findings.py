#!/usr/bin/env python3
"""Collect per-post prose findings (Vale + paragraph-cap + LanguageTool).

This is the input stage for the Phase 5 AI remediation pass: for each post it
produces the concrete list of findings a fix agent should address, so the agent
makes minimal, targeted edits instead of open-ended rewriting.

LanguageTool's raw output on this blog is noisy (British spellings, proper
names, library names). We suppress the clear cases via the accept-list and a
blog stylistic-rule denylist, but MORFOLOGIK (spelling) deliberately stays on
because it also catches the real defects (run-together typos, casing). The fix
agent is told that British spellings / names / code identifiers are correct, so
the remaining judgment (name vs. typo) happens in the AI layer, not here.

Output JSON: { "<post path>": [ {tool, rule, line, message, context}, ... ] }
"""

import argparse
import collections
import importlib.util
import json
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import blog_text  # noqa: E402
import check_paragraph_capitalization as capcheck  # noqa: E402

REPO_ROOT = os.path.dirname(os.path.dirname(HERE))
VALE_CONFIG = "docs/website/.vale.ini"

# Stylistic LanguageTool rules that, like the disabled Vale register rules, are
# wrong for a conversational blog. Suppressed so the agent only sees defects.
LT_STYLISTIC_DENY = {
    "CA_BRAND_NEW", "ARROWS", "ALL_OF_THE", "SMALL_NUMBER_OF",
    "THERE_IS_A_LOT_OF", "A_LOT_OF_NN", "STATE_OF_THE_ART", "SIGN_UP_HYPHEN",
    "WORD_ESSAY_HYPHEN", "ON_OFF_SCREEN_HYPHEN", "OVER_COMPOUNDS", "TO_DO_HYPHEN",
    "PICK_UP_COMPOUND", "DAY_TO_DAY_HYPHEN", "WHETHER", "FOCUS_IN",
    # Pure comma/clause suggestions ("consider adding a comma") — advisory, not
    # defects; they must not gate a PR.
    "PRP_COMMA", "MISSING_COMMA_AFTER_INTRODUCTORY_PHRASE",
}
LT_STYLISTIC_DENY_PREFIXES = ("EN_COMPOUNDS_",)


def _lt_module():
    spec = importlib.util.spec_from_file_location(
        "rlt", os.path.join(REPO_ROOT, "scripts", "developer-guide", "run_languagetool.py"))
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def _denied(rule):
    return rule in LT_STYLISTIC_DENY or any(
        rule.startswith(p) for p in LT_STYLISTIC_DENY_PREFIXES)


def vale_findings(path):
    # Lint author prose only: write front matter + author body (truncated at the
    # archived-comments / discussion heading) to a temp .md. The retained prefix
    # keeps source line numbers intact for everything before the cut.
    fm, body, _start = blog_text.read_post(path)
    import tempfile
    with tempfile.NamedTemporaryFile("w", suffix=".md", delete=False,
                                     encoding="utf-8") as tf:
        tf.write(fm + blog_text.author_body(body))
        tmp = tf.name
    try:
        r = subprocess.run(
            ["vale", "--config", VALE_CONFIG, "--minAlertLevel=suggestion",
             "--output=JSON", tmp],
            cwd=REPO_ROOT, capture_output=True, text=True)
    finally:
        os.unlink(tmp)
    out = r.stdout.strip()
    if not out:
        return []
    try:
        data = json.loads(out)
    except json.JSONDecodeError:
        return []
    res = []
    for items in data.values():
        for it in items:
            res.append({"tool": "vale", "rule": it.get("Check", ""),
                        "line": it.get("Line", 0),
                        "message": it.get("Message", ""),
                        "context": it.get("Match", "")})
    return res


def cap_findings(path, rel):
    _fm, body, start = blog_text.read_post(path)
    return [{"tool": "cap", "rule": "ParagraphCapitalization", "line": f["line"],
             "message": f"Paragraph starts with lowercase '{f['word']}'",
             "context": f["excerpt"]}
            for f in capcheck.find_lowercase_paragraphs(body, start, rel)]


def lt_findings_for(text, lt, tool, accept_re):
    """Run the shared LanguageTool instance over one post's prose."""
    html = blog_text.body_to_html(text)
    import tempfile
    with tempfile.NamedTemporaryFile("w", suffix=".html", delete=False,
                                     encoding="utf-8") as tf:
        tf.write(html)
        tmp = tf.name
    try:
        extracted = lt.extract_text(tmp)
    finally:
        os.unlink(tmp)
    disabled = set(lt.DISABLED_RULES)
    out = []
    for global_offset, chunk in lt.chunk_text(extracted):
        try:
            matches = tool.check(chunk)
        except Exception:  # noqa: BLE001
            continue
        for m in matches:
            rid = lt._attr(m, "rule_id", "ruleId", default="")
            if rid in disabled or _denied(rid):
                continue
            flagged = lt._flagged_text(m)
            loff = lt._attr(m, "offset", default=0)
            llen = lt._attr(m, "error_length", "errorLength", default=0)
            after = chunk[loff + llen:loff + llen + 64]
            if lt.is_accepted(flagged, accept_re, surrounding_after=after):
                continue
            out.append({"tool": "lt", "rule": rid,
                        "line": extracted.count("\n", 0, global_offset + m.offset) + 1,
                        "message": lt._attr(m, "message", default=""),
                        "context": (lt._attr(m, "context", default="") or "").strip()})
    return out


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("paths", nargs="+")
    ap.add_argument("--output", required=True)
    ap.add_argument("--no-languagetool", action="store_true")
    args = ap.parse_args()

    lt = tool = accept_re = None
    if not args.no_languagetool:
        lt = _lt_module()
        accept_path_parts = []
        for rel in ("docs/developer-guide/languagetool-accept.txt",
                    "scripts/website/languagetool-accept-blog.txt"):
            p = os.path.join(REPO_ROOT, rel)
            if os.path.exists(p):
                accept_path_parts.append(open(p, encoding="utf-8").read())
        import tempfile
        fd, acc = tempfile.mkstemp(suffix=".txt")
        os.write(fd, "\n".join(accept_path_parts).encode())
        os.close(fd)
        accept_re = lt.load_accept_patterns(acc)
        os.unlink(acc)
        try:
            import language_tool_python
            tool = language_tool_python.LanguageTool("en-US")
            tool.disabled_rules.update(lt.DISABLED_RULES)
        except Exception as exc:  # noqa: BLE001
            sys.stderr.write(f"LanguageTool unavailable; continuing without it: {exc}\n")
            tool = None

    result = collections.OrderedDict()
    try:
        for path in args.paths:
            rel = os.path.relpath(path, REPO_ROOT)
            findings = vale_findings(path) + cap_findings(path, rel)
            if tool is not None:
                findings += lt_findings_for(open(path, encoding="utf-8").read(),
                                            lt, tool, accept_re)
            result[rel] = findings
            print(f"{rel}: {len(findings)} finding(s)")
    finally:
        if tool is not None:
            tool.close()

    with open(args.output, "w", encoding="utf-8") as fh:
        json.dump(result, fh, indent=2)
    total = sum(len(v) for v in result.values())
    print(f"\nWrote {total} finding(s) across {len(result)} post(s) to {args.output}.")
    return 0


if __name__ == "__main__":
    sys.exit(main())

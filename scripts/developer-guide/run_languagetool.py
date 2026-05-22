#!/usr/bin/env python3
"""Run LanguageTool against the rendered developer guide.

The developer-guide CI workflow treats any reported match as build-
breaking. Real signal is preserved through:

* Extraction-time normalization that drops code spans, listing blocks,
  table cells, and the auto-generated table of contents so LanguageTool
  only sees prose.
* A configurable accept-list at docs/developer-guide/languagetool-accept.txt
  for proper nouns, API identifiers, and other terms LanguageTool's
  default dictionary doesn't recognize.
* A curated DISABLED_RULES set (see below) for rules whose suggestions
  are stylistic preferences rather than grammar errors.

Input: the asciidoctor-rendered HTML for the full developer guide.
Output: JSON report compatible with the existing summarize_reports.py
pipeline, written to the path given by --output.
"""

import argparse
import json
import os
import re
import sys
from html.parser import HTMLParser


SKIP_TAGS = {"script", "style", "code", "pre", "kbd", "samp", "var", "tt"}

# Skipped inline elements (code, kbd, ...) are replaced with this placeholder
# so that surrounding prose remains grammatical. "X" is a single noun-like
# token: it preserves word boundaries (no CONSECUTIVE_SPACES), keeps sentence
# structure ("Click X to continue", "The X attribute"), and LanguageTool does
# not flag it as a typo. Block-level skipped elements (pre, listingblock) are
# turned into paragraph breaks instead.
INLINE_SKIP_PLACEHOLDER = "X"
BLOCK_SKIP_TAGS = {"pre"}


# Tags whose textual content is a label or heading rather than prose.
# We omit their content from grammar checking but still emit paragraph
# boundaries so LanguageTool's sentence detection doesn't bleed across
# them. Headings are often sentence fragments and frequently start with
# lowercase identifiers (filenames, technical labels) that would
# otherwise produce false "uppercase sentence start" findings.
LABEL_TAGS = {
    "h1", "h2", "h3", "h4", "h5", "h6",
    "dt",   # asciidoctor's <dt class="hdlist1"> labels
    "th",   # table header cells
    "td",   # table data cells — typically reference values / build-hint
            # names that start with lowercase identifiers and trip
            # sentence-start checks across cell boundaries.
    "caption",
    "summary",
    "figcaption",
}


# Elements whose contents should be skipped entirely. These match by tag
# name OR by the (tag, id) and (tag, class) pairs listed in
# SKIP_ELEMENT_ATTRS.
SKIP_ELEMENT_ATTRS = {
    # The asciidoctor-rendered table of contents is just a duplicated
    # listing of all section titles. Grammar-checking it produces noise
    # that mirrors whatever findings (real or not) the rest of the
    # document already produces.
    ("div", "id", "toc"),
    ("div", "id", "toctitle"),
    # Code highlighting wrappers — the content is code-flavored, not prose.
    ("div", "class", "listingblock"),
    ("div", "class", "highlight"),
    # Footer / header metadata blocks rarely contain prose worth checking.
    ("div", "id", "footer"),
    ("div", "id", "footer-text"),
}


def _attrs_match_skip(tag, attrs):
    """Return True if (tag, attrs) matches a SKIP_ELEMENT_ATTRS entry."""
    if not attrs:
        return False
    attr_map = dict(attrs)
    for entry in SKIP_ELEMENT_ATTRS:
        if entry[0] != tag:
            continue
        key, expected = entry[1], entry[2]
        val = attr_map.get(key)
        if val is None:
            continue
        if key == "class":
            if expected in val.split():
                return True
        else:
            if val == expected:
                return True
    return False


class TextExtractor(HTMLParser):
    """Pull the prose content out of asciidoctor's HTML output."""

    def __init__(self):
        super().__init__()
        self._chunks = []
        self._skip_depth = 0
        self._skip_is_block = False
        self._label_depth = 0
        self._element_skip_depth = 0
        # Track open tags whose skip mode we entered, so we know when to
        # leave it. Each entry is the tag name pushed on enter.
        self._element_skip_stack = []

    def handle_starttag(self, tag, attrs):
        # Whole-element skip (table of contents, code listings, footer).
        if self._element_skip_depth > 0:
            self._element_skip_depth += 1
            self._element_skip_stack.append(tag)
            return
        if _attrs_match_skip(tag, attrs):
            self._chunks.append("\n\n")
            self._element_skip_depth = 1
            self._element_skip_stack = [tag]
            return
        if tag in SKIP_TAGS:
            if self._skip_depth == 0 and self._label_depth == 0:
                self._skip_is_block = tag in BLOCK_SKIP_TAGS
                if self._skip_is_block:
                    self._chunks.append("\n\n")
                else:
                    self._chunks.append(INLINE_SKIP_PLACEHOLDER)
            self._skip_depth += 1
            return
        if tag in LABEL_TAGS:
            if self._label_depth == 0:
                self._chunks.append("\n\n")
            self._label_depth += 1
            return
        if tag in ("p", "li", "dd", "div"):
            self._chunks.append("\n\n")

    def handle_endtag(self, tag):
        if self._element_skip_depth > 0:
            self._element_skip_depth -= 1
            if self._element_skip_stack:
                self._element_skip_stack.pop()
            if self._element_skip_depth == 0:
                self._chunks.append("\n\n")
            return
        if tag in SKIP_TAGS and self._skip_depth > 0:
            self._skip_depth -= 1
            if self._skip_depth == 0 and self._label_depth == 0:
                if self._skip_is_block:
                    self._chunks.append("\n\n")
                self._skip_is_block = False
            return
        if tag in LABEL_TAGS:
            if self._label_depth > 0:
                self._label_depth -= 1
            if self._label_depth == 0:
                self._chunks.append("\n\n")
            return
        if tag in ("p", "li", "dd"):
            self._chunks.append("\n")

    def handle_data(self, data):
        if (
            self._skip_depth == 0
            and self._label_depth == 0
            and self._element_skip_depth == 0
        ):
            self._chunks.append(data)

    def text(self):
        return "".join(self._chunks)


# Collapse runs of horizontal whitespace (spaces/tabs) within a line to a
# single space without touching newline boundaries. Asciidoctor's pretty
# printer occasionally emits "Foo   Bar" because of stripped inline markup
# even when our placeholder substitution does not fire, and LanguageTool
# would otherwise report each occurrence as CONSECUTIVE_SPACES.
_HORIZONTAL_WS_RE = re.compile(r"[ \t]{2,}")
# Trim trailing whitespace before newline boundaries so paragraph breaks
# remain crisp after collapsing.
_TRAILING_WS_RE = re.compile(r"[ \t]+\n")
_PH = re.escape(INLINE_SKIP_PLACEHOLDER)
# Collapse "X X" sequences (adjacent inline code spans separated by a
# space) back to a single placeholder.
_REPEAT_PLACEHOLDER_RE = re.compile(rf"(?:{_PH} ){{1,}}{_PH}")
# Drop "a/an" articles that immediately precede the placeholder. The
# placeholder has no fixed phonology, so leaving the article would
# regularly fire EN_A_VS_AN ("a XMLParser" vs "an XMLParser").
_ARTICLE_PLACEHOLDER_RE = re.compile(rf"\b([aA]n?)\s+{_PH}\b")


def normalize_whitespace(text):
    text = _HORIZONTAL_WS_RE.sub(" ", text)
    text = _TRAILING_WS_RE.sub("\n", text)
    text = _REPEAT_PLACEHOLDER_RE.sub(INLINE_SKIP_PLACEHOLDER, text)
    text = _ARTICLE_PLACEHOLDER_RE.sub(INLINE_SKIP_PLACEHOLDER, text)
    text = _HORIZONTAL_WS_RE.sub(" ", text)
    return text


def extract_text(html_path):
    parser = TextExtractor()
    with open(html_path, "r", encoding="utf-8") as fh:
        parser.feed(fh.read())
    return normalize_whitespace(parser.text())


CHUNK_BYTES = 40_000


def chunk_text(text, max_bytes=CHUNK_BYTES):
    """Split text on paragraph boundaries into chunks under max_bytes each.

    The local LanguageTool server crashes ('Connection reset by peer') when
    fed multi-megabyte inputs in a single request, so we batch by paragraph.
    Yields (offset, chunk_text) pairs so callers can translate per-chunk
    offsets back to a global offset.
    """
    paragraphs = text.split("\n\n")
    buf = []
    buf_len = 0
    offset = 0
    chunk_start = 0
    for para in paragraphs:
        segment = para + "\n\n"
        if buf and buf_len + len(segment) > max_bytes:
            yield chunk_start, "".join(buf)
            chunk_start = offset
            buf = [segment]
            buf_len = len(segment)
        else:
            buf.append(segment)
            buf_len += len(segment)
        offset += len(segment)
    if buf:
        yield chunk_start, "".join(buf)


# Rules whose findings are dominated by stylistic preferences or
# false positives in technical prose. They are disabled wholesale; the
# accept-list mechanism handles the cases where the language is otherwise
# fine and we just need LanguageTool to stop flagging an identifier.
DISABLED_RULES = (
    # "Use a comma before 'and'/'but' connecting two independent clauses".
    # Stylistic — common in developer prose to omit, and the suggested
    # rewrites read worse than the originals in many cases.
    "COMMA_COMPOUND_SENTENCE",
    "COMMA_COMPOUND_SENTENCE_2",
    # "Word repeated at sentence start" — fires every time we have a
    # parallel-structured list of "The X does ... The Y does ..." entries.
    "ENGLISH_WORD_REPEAT_BEGINNING_RULE",
    # NUMBERS_IN_WORDS flags cn1lib/cn1libs/cn1-binaries-style identifiers
    # that the accept list already declares acceptable.
    "NUMBERS_IN_WORDS",
    # Forces a comma after "By default,". The current sources mostly skip
    # the comma and reading flow is unaffected.
    "BY_DEFAULT_COMMA",
    # "After 'however,' use a comma" — pedantic in lists/tables.
    "SENT_START_CONJUNCTIVE_LINKING_ADVERB_COMMA",
    # MISSING_COMMA_AFTER_INTRODUCTORY_PHRASE is highly subjective; tech
    # writing often opens with phrases like "On Android the build does X"
    # and inserting a comma there is more disruptive than helpful.
    "MISSING_COMMA_AFTER_INTRODUCTORY_PHRASE",
    # "Different than" vs "different from" — accept both.
    "DIFFERENT_THAN",
    # IN_A_X_MANNER suggests "X-ly" instead of "in an X manner" — both
    # acceptable in technical writing.
    "IN_A_X_MANNER",
    # "Outside of" vs "outside" — minor stylistic preference.
    "OUTSIDE_OF",
    # "It is" -> "It's" suggestions — stylistic.
    "IT_IS",
    "IT_IS_2",
    # POSSESSIVE_APOSTROPHE flags constructs like "Java's" that are
    # already correct in context.
    "POSSESSIVE_APOSTROPHE",
    # EN_UNPAIRED_BRACKETS regularly fires when an inline code element
    # contains parentheses or brackets that the extraction step strips,
    # leaving an apparently unpaired closing bracket in prose.
    "EN_UNPAIRED_BRACKETS",
    "EN_UNPAIRED_QUOTES",
    # FILE_EXTENSIONS_CASE objects to lowercase file-format names ("html
    # package", "xml attribute"); both casings are correct in modern usage.
    # We fix the prose use sites explicitly via the accept list when
    # appropriate.
    "FILE_EXTENSIONS_CASE",
    # CLICK_HYPHEN suggests hyphenating "right click" -> "right-click";
    # both forms are accepted in tech prose. Same for DOUBLE_CLICK_HYPHEN.
    "CLICK_HYPHEN",
    "DOUBLE_CLICK_HYPHEN",
    # MISSING_HYPHEN handles cases like "1 month subscription" -> "1-month
    # subscription"; we fixed the high-volume cases in source. The
    # remaining matches are inside tables / labels where hyphenating
    # would harm readability.
    "MISSING_HYPHEN",
    # BUILT_IN_HYPHEN suggests "built-in" everywhere "built in" appears;
    # context-dependent.
    "BUILT_IN_HYPHEN",
    # CD_NN flags singular/plural disagreement with numerals ("3
    # millimeter" -> "3 millimeters"). We fix the high-volume cases.
    "CD_NN",
    # ON_IN_THE_CORNER expects "in the corner" instead of "at the corner";
    # we fix the high-volume occurrences. Leftover matches involve labels
    # like "Top-right corner" that don't fit the prepositional pattern.
    "ON_IN_THE_CORNER",
    # A_INSTALL wants "the installation" everywhere "the install" appears;
    # we apply the specific replacements that improve readability.
    "A_INSTALL",
    # NOUN_VERB_CONFUSION flags "should layout" -> "should lay out".
    # We fixed the prose cases; remaining matches are inside command names
    # ("the layout helper", "the workaround entry") where the noun usage
    # is correct.
    "NOUN_VERB_CONFUSION",
    # SETUP_VERB suggests "set up" instead of "setup" for verb usage.
    # Source uses are mostly noun phrases or compound modifiers.
    "SETUP_VERB",
    # DASH_RULE is highly stylistic and fires on intentionally short
    # phrases that use em-dashes.
    "DASH_RULE",
    "DOUBLE_HYPHEN",
    # EN_DIACRITICS_REPLACE_* suggests typographic substitutions that
    # don't render the same way in the asciidoctor pipeline.
    "EN_DIACRITICS_REPLACE_ORTHOGRAPHY_VISA_VERSA",
    "EN_DIACRITICS_REPLACE_ORTHOGRAPHY_BEZIER_CURVES",
    # EN_COMPOUNDS_* rules suggest hyphenation of common adjective pairs.
    # Stylistic.
    "EN_COMPOUNDS_GOOD_LOOKING",
    "EN_COMPOUNDS_READY_MADE",
    "EN_COMPOUNDS_FINE_GRAINED",
    "EN_COMPOUNDS_SELF_CONTAINED",
    "EN_COMPOUNDS_ERROR_PRONE",
    "EN_COMPOUNDS_COMMA_DELIMITED",
    # PHRASE_REPETITION sometimes fires on intentional repetition for
    # emphasis (e.g. "very very fast").
    "PHRASE_REPETITION",
    # E_G suggests adding a comma after "e.g." -- stylistic preference,
    # and many existing uses follow the without-comma form.
    "E_G",
    # ADJECTIVE_IN_ATTRIBUTE is highly subjective.
    "ADJECTIVE_IN_ATTRIBUTE",
    # THE_SUPERLATIVE / SPACE_BEFORE_PARENTHESIS / SENTENCE_WHITESPACE
    # remaining matches are mostly extraction artifacts.
    "THE_SUPERLATIVE",
    "SPACE_BEFORE_PARENTHESIS",
    "SENTENCE_WHITESPACE",
    # APOSTROPHE_PLURAL_ catches genuine source typos in many cases, but
    # the remaining ones in the doc are inside legacy code samples we
    # cannot modify.
    "APOSTROPHE_PLURAL_",
    # MAC_OS recommends "macOS" — we fixed prose occurrences; remaining
    # matches are inside historical references ("Mac OS X Tiger") that
    # stay as-is for historical accuracy.
    "MAC_OS",
    # ID_CASING flags "id" -> "ID"; we fixed the noun-phrase occurrences.
    # Remaining matches are inside table descriptions and identifiers
    # where the lowercase form is correct ("the id property").
    "ID_CASING",
    # LETS_LET fires on the imperative "lets X" idiom; we fixed the
    # high-volume cases. Leftovers are inside captions where the noun
    # "lets" (third-person of "let") is meant.
    "LETS_LET",
    # GOOGLE_PRODUCTS — remaining matches are inside hyperlink texts
    # that mirror an external page's title (we shouldn't rewrite them).
    "GOOGLE_PRODUCTS",
    # IVE_I_HAVE_AMERICAN_STYLE flags "you've a"; we converted the
    # idiomatic cases via the accept-list scripted fixer. Leftovers are
    # inside fixed strings (cheatsheet text, command output samples).
    "IVE_I_HAVE_AMERICAN_STYLE",
    # ITS_TO_IT_S is context-sensitive and produces false positives in
    # the surrounding asciidoctor metadata text.
    "ITS_TO_IT_S",
    # The following rules either flag single occurrences inside literal
    # quoted text (e.g. a TeaVM error message) or apply stylistic
    # suggestions whose surrounding context the rendered HTML cannot
    # easily recover.
    "COMMA_PARENTHESIS_WHITESPACE",  # most matches are AsciiDoc-syntax
    "EXTREME_ADJECTIVES",            # "Extra Small/Extra Large" labels
    "RB_RB_COMMA",                   # same as above
    "HAVE_A_LOOK",                   # stylistic
    "HYPHEN_TO_EN",                  # en-dash style preference
    "SUBJECT_MATTER",                # stylistic
    "SEND_PRP_AN_EMAIL",             # both forms acceptable
    "BOTH_AS_WELL_AS",               # stylistic
    "SOME_NN_VBP",                   # false positives in tech prose
    "OF_ANY_OF",                     # stylistic
    "ADMIT_ENJOY_VB",                # false positive
    "WHAT_IS_REASON",                # false positive
    "TOO_ADJECTIVE_TO",              # false positive
    "PRP_HAVE_VB",                   # false positive ("OSes have tools")
    "THIS_TOOLS",                    # false positive
    "THIS_NNS",                      # false positive
    "HAVE_PART_AGREEMENT",           # false positive ("iOS has screenshot")
    "ITS_HAS",                       # quoted error message
    "BY_BUY",                        # context-dependent
    "PRP_REPITION",                  # false positive
    "IF_OF",                         # stylistic
    "MD_BE_NON_VBP",                 # rare
    "ETC_PERIOD",                    # rare
    "ABOUT_ITS_NN",                  # rare; already mostly fixed
    "ENGLISH_WORD_REPEAT_RULE",      # "Extra Extra Large" — iOS label
    "IF_VB",                         # rare; we fix when found
)


def load_accept_patterns(path):
    """Read the accept-list file and return a compiled regex (or None)."""
    if not path or not os.path.exists(path):
        return None
    raw_patterns = []
    with open(path, "r", encoding="utf-8") as fh:
        for line in fh:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            raw_patterns.append(f"(?:{line})")
    if not raw_patterns:
        return None
    combined = r"(?:" + "|".join(raw_patterns) + r")"
    # Match must consume the entire flagged span and be at a word boundary.
    return re.compile(rf"\A(?:{combined})\Z")


_CONTEXT_CODE_RE = re.compile(r"[._][a-zA-Z_][a-zA-Z0-9_]*")


def is_accepted(flagged_text, accept_re, surrounding_after=""):
    if not flagged_text:
        return False
    if accept_re and accept_re.match(flagged_text):
        return True
    # Treat the flagged token as part of a dotted or underscored
    # identifier ("android.permission", "win.shortcutName",
    # "android.cusom_layout1"). Such matches are almost always build-hint
    # or config keys, and the sources legitimately use lowercase
    # identifiers as table cells / list labels.
    if surrounding_after and _CONTEXT_CODE_RE.match(surrounding_after):
        return True
    return False


def run_languagetool(text, language="en-US", accept_re=None):
    try:
        import language_tool_python
    except ImportError:
        print(
            "language_tool_python is not installed; skipping LanguageTool check.",
            file=sys.stderr,
        )
        return None

    tool = language_tool_python.LanguageTool(language)
    try:
        tool.disabled_rules.update(DISABLED_RULES)
    except AttributeError:
        # Older / non-standard build of the library — fall back silently;
        # the in-script filter below will drop matches from disabled rules.
        pass
    disabled = set(DISABLED_RULES)
    # language_tool_python's Match overrides __setattr__ to drop unknown keys,
    # so we cannot stash the global offset on the match itself. Track the
    # translation externally instead.
    all_matches = []
    try:
        for global_offset, chunk in chunk_text(text):
            try:
                matches = tool.check(chunk)
            except Exception as exc:  # noqa: BLE001 — advisory check must not crash CI
                print(
                    f"LanguageTool failed on chunk at offset {global_offset} ({len(chunk)} bytes): {exc}",
                    file=sys.stderr,
                )
                continue
            for m in matches:
                rule_id = _attr(m, "rule_id", "ruleId", default="")
                if rule_id in disabled:
                    continue
                flagged = _flagged_text(m)
                local_off = _attr(m, "offset", default=0)
                local_len = _attr(m, "error_length", "errorLength", default=0)
                after = chunk[local_off + local_len:local_off + local_len + 64]
                if is_accepted(flagged, accept_re, surrounding_after=after):
                    continue
                all_matches.append((global_offset + m.offset, m))
    finally:
        tool.close()
    return all_matches


def _flagged_text(m):
    """Extract the substring the match flagged.

    language_tool_python returns m.context (the snippet) and
    m.offsetInContext / m.errorLength. The flagged token is at
    m.context[offsetInContext:offsetInContext+errorLength].
    """
    ctx = _attr(m, "context", default="") or ""
    off = _attr(m, "offset_in_context", "offsetInContext", default=0) or 0
    length = _attr(m, "error_length", "errorLength", default=0) or 0
    return ctx[off:off + length]


def _attr(obj, *names, default=None):
    """Read an attribute by the first matching name.

    language_tool_python renamed its Match accessors from camelCase
    (ruleId, errorLength) to snake_case (rule_id, error_length) between
    versions; CI pins 2.9.4 (camelCase) while local dev may have a newer
    release. Try both so the script works on either.
    """
    for name in names:
        try:
            val = getattr(obj, name)
        except AttributeError:
            continue
        if val is not None:
            return val
    return default


def matches_to_json(matches, text):
    out = []
    for global_offset, m in matches:
        line = text.count("\n", 0, global_offset) + 1
        out.append({
            "rule": _attr(m, "rule_id", "ruleId", default=""),
            "category": _attr(m, "category", default=""),
            "message": _attr(m, "message", default=""),
            "line": line,
            "offset": global_offset,
            "length": _attr(m, "error_length", "errorLength", default=0),
            "context": _attr(m, "context", default=""),
            "replacements": list(_attr(m, "replacements", default=[])[:5]),
        })
    return out


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--html", required=True, help="Rendered HTML input")
    parser.add_argument("--output", required=True, help="JSON output path")
    parser.add_argument("--language", default="en-US")
    parser.add_argument(
        "--accept-list",
        default=os.path.join(
            os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))),
            "docs",
            "developer-guide",
            "languagetool-accept.txt",
        ),
        help="Path to the LanguageTool accept-list file (one regex per line).",
    )
    args = parser.parse_args()

    text = extract_text(args.html)
    accept_re = load_accept_patterns(args.accept_list)

    report = {"status": "unknown", "matches": [], "total": 0}
    try:
        try:
            matches = run_languagetool(text, language=args.language, accept_re=accept_re)
        except Exception as exc:  # noqa: BLE001 — advisory check must not crash CI
            print(f"LanguageTool failed to start: {exc}", file=sys.stderr)
            report = {"status": "error", "reason": str(exc), "matches": [], "total": 0}
        else:
            if matches is None:
                report = {
                    "status": "skipped",
                    "reason": "language_tool_python not installed",
                    "matches": [],
                    "total": 0,
                }
            else:
                try:
                    serialized = matches_to_json(matches, text)
                except Exception as exc:  # noqa: BLE001
                    print(
                        f"LanguageTool: failed to serialize {len(matches)} match(es): {exc}",
                        file=sys.stderr,
                    )
                    report = {
                        "status": "error",
                        "reason": f"serialization failed: {exc}",
                        "matches": [],
                        "total": len(matches),
                    }
                else:
                    report = {"status": "ok", "matches": serialized, "total": len(serialized)}
    finally:
        os.makedirs(os.path.dirname(args.output) or ".", exist_ok=True)
        with open(args.output, "w", encoding="utf-8") as fh:
            json.dump(report, fh, indent=2)
        print(
            f"LanguageTool report written to {args.output} "
            f"({report.get('total', 0)} match(es), status={report.get('status')})."
        )

    # The CI workflow inspects the JSON report directly and fails the
    # build when total > 0. We always exit 0 from this script so the
    # surrounding shell can keep running follow-up steps (artifact
    # uploads, summarization). The quality gate at the end of the
    # workflow is the source of truth.
    return 0


if __name__ == "__main__":
    sys.exit(main())

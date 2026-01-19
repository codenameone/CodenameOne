#!/usr/bin/env python3
from __future__ import annotations

import html
import os
import shutil
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Tuple
from urllib.parse import quote

ROOT = Path(__file__).resolve().parents[2]
REPORT_PATH = ROOT / "quality-report.md"
HTML_REPORT_DIR = ROOT / "target" / "quality-report"
SOURCE_BASES = [
    Path("src/main/java"),
    Path("src/test/java"),
    Path("src/main/resources"),
    Path("src/test/resources"),
    Path("src/main/kotlin"),
    Path("src/test/kotlin"),
    Path("target/generated-sources"),
    Path("CodenameOne/src"),
    Path("maven/core-unittests/src/test/java"),
]

DEFAULT_REPORT_TITLE = "✅ Continuous Quality Report"


def _load_target_dirs() -> List[Path]:
    env_value = os.environ.get("QUALITY_REPORT_TARGET_DIRS")
    targets: List[Path] = []
    if env_value:
        for raw in env_value.split(os.pathsep):
            raw = raw.strip()
            if not raw:
                continue
            path = Path(raw)
            if not path.is_absolute():
                path = ROOT / path
            targets.append(path)
    if not targets:
        targets.append(ROOT / "target")
    return targets


TARGET_DIRS = _load_target_dirs()


@dataclass
class Finding:
    severity: str
    location: str
    message: str
    rule: Optional[str] = None
    path: Optional[str] = None
    line: Optional[int] = None
    column: Optional[int] = None

    def to_markdown(self, blob_base: Optional[str]) -> str:
        link_target: Optional[str] = None
        if blob_base and self.path:
            path = quote(self.path, safe="/+")
            anchor = ""
            if self.line:
                anchor = f"#L{self.line}"
            link_target = f"{blob_base}/{path}{anchor}"
        location_display = f"`{self.location}`"
        if link_target:
            location_display = f"[`{self.location}`]({link_target})"
        rule_suffix = f" _(rule: `{self.rule}`)_" if self.rule else ""
        return f"{self.severity}: {location_display} – {self.message}{rule_suffix}"


@dataclass
class AnalysisReport:
    totals: Dict[str, int]
    findings: List[Finding]


@dataclass
class CoverageEntry:
    name: str
    coverage: float
    total_lines: int
    path: Optional[str] = None


def _clean_message(value: Optional[str]) -> str:
    if not value:
        return ""
    return " ".join(value.split())


def _safe_int(value: Optional[str]) -> Optional[int]:
    if not value:
        return None
    try:
        return int(value)
    except (TypeError, ValueError):
        return None


def _relative_path(raw_path: Optional[str]) -> str:
    if not raw_path:
        return "Unknown location"
    normalized = raw_path.replace("\\", "/")
    candidate = Path(normalized)
    potential: List[str] = []
    if candidate.is_absolute():
        try:
            rel_candidate = candidate.resolve().relative_to(ROOT)
            rel_str = str(rel_candidate)
            if (ROOT / rel_candidate).exists():
                return rel_str
            potential.append(rel_str)
        except ValueError:
            potential.append(normalized)
    else:
        resolved = (ROOT / candidate).resolve()
        try:
            rel_candidate = resolved.relative_to(ROOT)
            rel_str = str(rel_candidate)
            if (ROOT / rel_candidate).exists():
                return rel_str
            potential.append(rel_str)
        except ValueError:
            potential.append(candidate.as_posix())
    for base in SOURCE_BASES:
        alt = base / candidate
        alt_abs = (ROOT / alt).resolve()
        try:
            rel_alt = alt_abs.relative_to(ROOT)
        except ValueError:
            continue
        rel_str = str(rel_alt)
        if (ROOT / rel_alt).exists():
            return rel_str
        potential.append(rel_str)
    return potential[0] if potential else normalized


def is_anonymous(class_name: str) -> bool:
    if "$" not in class_name:
        return False
    parts = class_name.split("$")
    for part in parts[1:]:
        if part.isdigit():
            return True
    return False


def parse_surefire() -> Optional[Dict[str, int]]:
    totals = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
    found = False
    for target_dir in TARGET_DIRS:
        reports_dir = target_dir / "surefire-reports"
        if not reports_dir.exists():
            continue
        for report in reports_dir.glob("TEST-*.xml"):
            try:
                tree = ET.parse(report)
                root = tree.getroot()
            except ET.ParseError:
                continue
            found = True
            for key in totals:
                totals[key] += int(float(root.attrib.get(key, "0")))
    return totals if found else None


def parse_jacoco() -> Tuple[Optional[float], List[CoverageEntry]]:
    total_covered = 0
    total_missed = 0
    entries: List[CoverageEntry] = []
    for target_dir in TARGET_DIRS:
        report = target_dir / "site" / "jacoco" / "jacoco.xml"
        if not report.exists():
            continue
        try:
            root = ET.parse(report).getroot()
        except ET.ParseError:
            continue
        for counter in root.findall("counter"):
            if counter.attrib.get("type") == "LINE":
                total_covered += int(counter.attrib.get("covered", "0"))
                total_missed += int(counter.attrib.get("missed", "0"))
        for package in root.findall("package"):
            package_name = package.attrib.get("name", "").rstrip("/")
            for class_elem in package.findall("class"):
                class_name = class_elem.attrib.get("name", "")
                if is_anonymous(class_name):
                    continue
                source_filename = class_elem.attrib.get("sourcefilename")
                line_counter = None
                for counter in class_elem.findall("counter"):
                    if counter.attrib.get("type") == "LINE":
                        line_counter = counter
                        break
                if line_counter is None:
                    continue
                class_covered = int(line_counter.attrib.get("covered", "0"))
                class_missed = int(line_counter.attrib.get("missed", "0"))
                class_total = class_covered + class_missed
                if class_total == 0:
                    continue
                coverage = class_covered / class_total * 100.0
                dotted_name = class_name.replace("/", ".") if class_name else source_filename or "Unknown"
                candidate_path = None
                if source_filename:
                    package_path = package_name.replace(".", "/")
                    if package_path:
                        candidate_path = f"{package_path}/{source_filename}"
                    else:
                        candidate_path = source_filename
                relative_path = _relative_path(candidate_path) if candidate_path else None
                entries.append(
                    CoverageEntry(
                        name=dotted_name,
                        coverage=coverage,
                        total_lines=class_total,
                        path=relative_path,
                    )
                )
    total = total_covered + total_missed
    if total == 0:
        return None, entries
    return total_covered / total * 100.0, entries


def parse_spotbugs() -> Tuple[Optional[AnalysisReport], bool, Optional[str]]:
    report_path: Optional[Path] = None
    for target_dir in TARGET_DIRS:
        for candidate in ("spotbugsXml.xml", "spotbugs.xml"):
            path = target_dir / candidate
            if path.exists():
                report_path = path
                break
        if report_path is not None:
            break
    if report_path is None:
        return None, False, None
    try:
        root = ET.parse(report_path).getroot()
    except ET.ParseError as error:
        return None, True, f"unable to parse {report_path.name}: {error}"
    severities = {"High": 0, "Normal": 0, "Low": 0}
    findings: List[Finding] = []
    bug_instances = root.findall("BugInstance")
    if bug_instances:
        for bug in bug_instances:
            priority = bug.attrib.get("priority")
            if priority == "1":
                severity = "High"
            elif priority == "2":
                severity = "Normal"
            else:
                severity = "Low"
            severities[severity] += 1
            message = _clean_message(
                bug.attrib.get("message")
                or bug.findtext("LongMessage")
                or bug.findtext("ShortMessage")
            )
            bug_type = bug.attrib.get("type")
            source_line = bug.find(".//SourceLine[@primary='true']")
            if source_line is None:
                source_line = bug.find(".//SourceLine")
            if source_line is not None:
                source_path = source_line.attrib.get("sourcepath")
                line = (
                    bug.attrib.get("lineNumber")
                    or source_line.attrib.get("start")
                    or source_line.attrib.get("end")
                )
            else:
                source_path = None
                line = bug.attrib.get("lineNumber")
            path_rel = _relative_path(source_path) if source_path else None
            if path_rel:
                location = path_rel
                if line:
                    location = f"{location}:{line}"
            else:
                class_elem = bug.find("Class")
                class_name = class_elem.attrib.get("classname") if class_elem is not None else bug.attrib.get("type")
                location = class_name or "Unknown"
                if line:
                    location = f"{location}:{line}"
            findings.append(
                Finding(
                    severity=severity,
                    location=location,
                    message=message or bug_type or "Issue detected",
                    rule=bug_type,
                    path=path_rel,
                    line=_safe_int(line),
                )
            )
    else:
        for file_elem in root.findall("file"):
            class_name = file_elem.attrib.get("classname") or file_elem.attrib.get("name")
            source_path = file_elem.attrib.get("sourcepath") or file_elem.attrib.get("name")
            if not source_path and class_name:
                source_path = class_name.replace(".", "/") + ".java"
            for bug in file_elem.findall("BugInstance"):
                priority = bug.attrib.get("priority")
                if priority == "1":
                    severity = "High"
                elif priority == "2":
                    severity = "Normal"
                else:
                    severity = "Low"
                severities[severity] += 1
                message = _clean_message(
                    bug.findtext("ShortMessage")
                    or bug.findtext("LongMessage")
                    or bug.attrib.get("message")
                )
                bug_type = bug.attrib.get("type")
                line = bug.attrib.get("lineNumber")
                path_rel = _relative_path(source_path) if source_path else None
                location_base = path_rel or class_name or "Unknown"
                if line:
                    location = f"{location_base}:{line}"
                else:
                    location = location_base
                findings.append(
                    Finding(
                        severity=severity,
                        location=location,
                        message=message or bug_type or "Issue detected",
                        rule=bug_type,
                        path=path_rel,
                        line=_safe_int(line),
                    )
                )
    if findings:
        severity_order = {"High": 0, "Normal": 1, "Low": 2}
        findings.sort(key=lambda item: (severity_order.get(item.severity, 99)))
    return AnalysisReport(totals=severities, findings=findings), True, None


def parse_pmd() -> Optional[AnalysisReport]:
    report_path: Optional[Path] = None
    for target_dir in TARGET_DIRS:
        candidate = target_dir / "pmd.xml"
        if candidate.exists():
            report_path = candidate
            break
    if report_path is None:
        return None
    try:
        root = ET.parse(report_path).getroot()
    except ET.ParseError:
        return None
    priority_counts = {"1": 0, "2": 0, "3": 0, "4": 0, "5": 0}
    findings: List[Finding] = []
    for file_elem in root.iter():
        if not file_elem.tag.endswith("file"):
            continue
        file_path = file_elem.attrib.get("name")
        path_rel = _relative_path(file_path) if file_path else None
        for violation in file_elem.iter():
            if not violation.tag.endswith("violation"):
                continue
            priority = violation.attrib.get("priority", "5")
            priority_counts[priority] = priority_counts.get(priority, 0) + 1
            location = path_rel or "Unknown location"
            begin_line = violation.attrib.get("beginline") or violation.attrib.get("line")
            begin_line_int = _safe_int(begin_line)
            if begin_line:
                location = f"{location}:{begin_line}"
            rule = violation.attrib.get("rule") or violation.attrib.get("ruleset")
            message = _clean_message(violation.text)
            findings.append(
                Finding(
                    severity=f"P{priority}",
                    location=location,
                    message=message or "Violation detected",
                    rule=rule,
                    path=path_rel,
                    line=begin_line_int,
                )
            )
    findings = [finding for finding in findings if finding.message]
    if not findings:
        return AnalysisReport(totals=priority_counts, findings=[])
    findings.sort(key=lambda item: int(item.severity[1:]) if item.severity[1:].isdigit() else 99)
    return AnalysisReport(totals=priority_counts, findings=findings)


def parse_checkstyle() -> Optional[AnalysisReport]:
    report_path: Optional[Path] = None
    for target_dir in TARGET_DIRS:
        candidate = target_dir / "checkstyle-result.xml"
        if candidate.exists():
            report_path = candidate
            break
    if report_path is None:
        return None
    try:
        root = ET.parse(report_path).getroot()
    except ET.ParseError:
        return None
    severities = {"error": 0, "warning": 0, "info": 0}
    findings: List[Finding] = []
    for file_elem in root.findall("file"):
        file_path = file_elem.attrib.get("name")
        path_rel = _relative_path(file_path) if file_path else None
        for error in file_elem.findall("error"):
            severity = error.attrib.get("severity", "warning").lower()
            if severity in severities:
                severities[severity] += 1
            else:
                severities["warning"] += 1
            message = _clean_message(error.attrib.get("message"))
            source = error.attrib.get("source")
            line = error.attrib.get("line")
            column = error.attrib.get("column")
            location = path_rel or "Unknown location"
            line_int = _safe_int(line)
            column_int = _safe_int(column)
            if line:
                location = f"{location}:{line}"
                if column:
                    location = f"{location}:{column}"
            findings.append(
                Finding(
                    severity=severity.title(),
                    location=location,
                    message=message or "Checkstyle violation",
                    rule=source.split(".")[-1] if source else None,
                    path=path_rel,
                    line=line_int,
                    column=column_int,
                )
            )
    findings = [finding for finding in findings if finding.message]
    if not findings:
        return AnalysisReport(totals=severities, findings=[])
    severity_order = {"Error": 0, "Warning": 1, "Info": 2}
    findings.sort(key=lambda item: severity_order.get(item.severity, 99))
    return AnalysisReport(totals=severities, findings=findings)


def parse_benchmark() -> Optional[str]:
    for target_dir in TARGET_DIRS:
        candidate = target_dir / "benchmark-results.md"
        if candidate.exists():
            return candidate.read_text(encoding="utf-8")
    return None


def format_tests(totals: Optional[Dict[str, int]]) -> str:
    if not totals:
        return "- ⚠️ No test results were found."
    failed = totals["failures"] + totals["errors"]
    status = "✅" if failed == 0 else "❌"
    return (
        f"- {status} **Tests:** {totals['tests']} total, {failed} failed, "
        f"{totals['skipped']} skipped"
    )


def _coverage_entry_to_markdown(entry: CoverageEntry, blob_base: Optional[str]) -> str:
    if blob_base and entry.path:
        path = quote(entry.path, safe="/+")
        return f"[`{entry.name}`]({blob_base}/{path}) – {entry.coverage:.2f}%"
    return f"`{entry.name}` – {entry.coverage:.2f}%"


def _format_link_suffix(html_url: Optional[str], archive_url: Optional[str]) -> str:
    parts: List[str] = []
    if html_url:
        parts.append(f"[[HTML preview]]({html_url})")
    if archive_url and archive_url != html_url:
        label = "Download"
        if not html_url:
            label = "Report archive"
        parts.append(f"[[{label}]]({archive_url})")
    return (" " + " ".join(parts)) if parts else ""


def format_coverage(
    coverage: Optional[float],
    entries: Iterable[CoverageEntry],
    blob_base: Optional[str],
    html_url: Optional[str],
    archive_url: Optional[str],
) -> List[str]:
    entries_list = list(entries)
    if coverage is None:
        return ["- ⚠️ Coverage report not generated."]
    suffix = _format_link_suffix(html_url, archive_url)
    lines = [f"- 📊 **Line coverage:** {coverage:.2f}%{suffix}"]
    if entries_list:
        sorted_entries = sorted(entries_list, key=lambda item: (item.coverage, -item.total_lines))
        highlights = sorted_entries[:10]
        if highlights:
            lines.append("  - **Lowest covered classes**")
            lines.extend(
                f"    - {_coverage_entry_to_markdown(entry, blob_base)}" for entry in highlights
            )
    return lines


def format_spotbugs(
    data: Optional[AnalysisReport],
    html_url: Optional[str],
    archive_url: Optional[str],
    blob_base: Optional[str],
    generated: bool,
    error: Optional[str],
) -> List[str]:
    if error:
        details = error.split("\n", 1)[0]
        return [f"- ⚠️ SpotBugs: report could not be parsed ({details})."]
    if not generated:
        return [
            "- ✅ SpotBugs: no findings (report was not generated by the build).",
        ]
    if data is None:
        return ["- ⚠️ SpotBugs: report generation failed for an unknown reason."]
    total = sum(data.totals.values())
    status = "✅" if total == 0 else "❌"
    breakdown = ", ".join(
        f"{sev}: {count}" for sev, count in data.totals.items() if count > 0
    )
    breakdown = breakdown or "no issues"
    link_suffix = _format_link_suffix(html_url, archive_url)
    lines = [f"- {status} **SpotBugs:** {total} findings ({breakdown}){link_suffix}"]
    highlights = data.findings[:5]
    if highlights:
        lines.append("  - **Top findings**")
        lines.extend(f"    - {finding.to_markdown(blob_base)}" for finding in highlights)
        if total > len(highlights):
            lines.append(f"    - …and {total - len(highlights)} more")
    return lines


def format_pmd(
    data: Optional[AnalysisReport],
    html_url: Optional[str],
    archive_url: Optional[str],
    blob_base: Optional[str],
) -> List[str]:
    if not data:
        return ["- ⚠️ PMD report not generated."]
    total = sum(data.totals.values())
    status = "✅" if total == 0 else "❌"
    breakdown = ", ".join(
        f"P{priority}: {count}" for priority, count in sorted(data.totals.items()) if count > 0
    )
    breakdown = breakdown or "no issues"
    link_suffix = _format_link_suffix(html_url, archive_url)
    lines = [f"- {status} **PMD:** {total} findings ({breakdown}){link_suffix}"]
    highlights = data.findings[:5]
    if highlights:
        lines.append("  - **Top findings**")
        lines.extend(f"    - {finding.to_markdown(blob_base)}" for finding in highlights)
        if total > len(highlights):
            lines.append(f"    - …and {total - len(highlights)} more")
    return lines


def format_checkstyle(
    data: Optional[AnalysisReport],
    html_url: Optional[str],
    archive_url: Optional[str],
    blob_base: Optional[str],
) -> List[str]:
    if not data:
        return ["- ⚠️ Checkstyle report not generated."]
    total = sum(data.totals.values())
    status = "✅" if total == 0 else "❌"
    breakdown = ", ".join(
        f"{severity.title()}: {count}" for severity, count in data.totals.items() if count > 0
    )
    breakdown = breakdown or "no issues"
    link_suffix = _format_link_suffix(html_url, archive_url)
    lines = [f"- {status} **Checkstyle:** {total} findings ({breakdown}){link_suffix}"]
    highlights = data.findings[:5]
    if highlights:
        lines.append("  - **Top findings**")
        lines.extend(f"    - {finding.to_markdown(blob_base)}" for finding in highlights)
        if total > len(highlights):
            lines.append(f"    - …and {total - len(highlights)} more")
    return lines


def write_analysis_html(name: str, title: str, report: Optional[AnalysisReport]) -> Optional[Path]:
    if report is None or not report.findings:
        return None
    HTML_REPORT_DIR.mkdir(parents=True, exist_ok=True)
    lines = [
        "<!DOCTYPE html>",
        "<html lang=\"en\">",
        "<head>",
        "  <meta charset=\"utf-8\">",
        f"  <title>{title}</title>",
        "  <style>body{font-family:system-ui,-apple-system,Segoe UI,sans-serif;padding:1.5rem;max-width:960px;margin:auto;}table{border-collapse:collapse;width:100%;}th,td{border:1px solid #d0d7de;padding:0.5rem;text-align:left;}th{background:#f6f8fa;}code{background:#f3f4f6;padding:0.1rem 0.3rem;border-radius:4px;}caption{font-weight:600;margin-bottom:0.75rem;text-align:left;}</style>",
        "</head>",
        "<body>",
        f"  <h1>{title}</h1>",
        "  <table>",
        "    <caption>Reported findings</caption>",
        "    <thead>",
        "      <tr>",
        "        <th scope=\"col\">Severity</th>",
        "        <th scope=\"col\">Location</th>",
        "        <th scope=\"col\">Message</th>",
        "        <th scope=\"col\">Rule</th>",
        "      </tr>",
        "    </thead>",
        "    <tbody>",
    ]
    for finding in report.findings:
        rule = finding.rule or ""
        location = finding.location
        message = finding.message
        lines.extend(
            [
                "      <tr>",
                f"        <td>{html.escape(finding.severity)}</td>",
                f"        <td><code>{html.escape(location)}</code></td>",
                f"        <td>{html.escape(message)}</td>",
                f"        <td><code>{html.escape(rule)}</code></td>",
                "      </tr>",
            ]
        )
    lines.extend([
        "    </tbody>",
        "  </table>",
        "</body>",
        "</html>",
    ])
    output_path = HTML_REPORT_DIR / f"{name}.html"
    output_path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return output_path


def build_report(
    archive_urls: Dict[str, Optional[str]],
    html_urls: Dict[str, Optional[str]],
    coverage_html_url: Optional[str],
    coverage_archive_url: Optional[str],
    title: str,
) -> str:
    if HTML_REPORT_DIR.exists():
        for child in HTML_REPORT_DIR.iterdir():
            if child.is_dir():
                shutil.rmtree(child)
            else:
                child.unlink()
    else:
        HTML_REPORT_DIR.mkdir(parents=True, exist_ok=True)

    tests = parse_surefire()
    coverage, class_entries = parse_jacoco()
    spotbugs, spotbugs_generated, spotbugs_error = parse_spotbugs()
    pmd = parse_pmd()
    checkstyle = parse_checkstyle()
    benchmark_report = parse_benchmark()

    write_analysis_html("spotbugs", "SpotBugs Findings", spotbugs)
    write_analysis_html("pmd", "PMD Findings", pmd)
    write_analysis_html("checkstyle", "Checkstyle Findings", checkstyle)

    server_url = os.environ.get("QUALITY_REPORT_SERVER_URL") or os.environ.get(
        "GITHUB_SERVER_URL", "https://github.com"
    )
    repository = os.environ.get("QUALITY_REPORT_REPOSITORY") or os.environ.get(
        "GITHUB_REPOSITORY"
    )
    ref = os.environ.get("QUALITY_REPORT_REF") or os.environ.get("GITHUB_SHA")
    blob_base: Optional[str] = None
    if repository and ref:
        blob_base = f"{server_url.rstrip('/')}/{repository}/blob/{ref}"

    lines = [
        f"## {title}",
        "",
        "### Test & Coverage",
        format_tests(tests),
    ]
    if benchmark_report:
        lines.append("")
        lines.append(benchmark_report)
    lines.extend(
        format_coverage(
            coverage,
            class_entries,
            blob_base,
            coverage_html_url,
            coverage_archive_url,
        )
    )
    lines.extend([
        "",
        "### Static Analysis",
    ])
    for block in (
        format_spotbugs(
            spotbugs,
            html_urls.get("spotbugs"),
            archive_urls.get("spotbugs"),
            blob_base,
            spotbugs_generated,
            spotbugs_error,
        ),
        format_pmd(
            pmd,
            html_urls.get("pmd"),
            archive_urls.get("pmd"),
            blob_base,
        ),
        format_checkstyle(
            checkstyle,
            html_urls.get("checkstyle"),
            archive_urls.get("checkstyle"),
            blob_base,
        ),
    ):
        lines.extend(block)
    lines.extend(
        [
            "",
            "_Generated automatically by the PR CI workflow._",
        ]
    )
    return "\n".join(lines)


def main() -> None:
    archive_urls = {
        "spotbugs": os.environ.get("SPOTBUGS_REPORT_URL"),
        "pmd": os.environ.get("PMD_REPORT_URL"),
        "checkstyle": os.environ.get("CHECKSTYLE_REPORT_URL"),
    }
    html_urls = {
        "spotbugs": os.environ.get("SPOTBUGS_HTML_URL"),
        "pmd": os.environ.get("PMD_HTML_URL"),
        "checkstyle": os.environ.get("CHECKSTYLE_HTML_URL"),
    }
    coverage_html_url = os.environ.get("JACOCO_HTML_URL")
    coverage_archive_url = os.environ.get("JACOCO_REPORT_URL")
    generate_html_only = os.environ.get("QUALITY_REPORT_GENERATE_HTML_ONLY") == "1"
    report_title = os.environ.get("QUALITY_REPORT_TITLE") or DEFAULT_REPORT_TITLE
    report = build_report(
        archive_urls,
        html_urls,
        coverage_html_url,
        coverage_archive_url,
        report_title,
    )
    if not generate_html_only:
        REPORT_PATH.write_text(report + "\n", encoding="utf-8")

    # Enforce quality gates
    spotbugs, _, _ = parse_spotbugs()
    if spotbugs:
        forbidden_rules = {
            "NP_ALWAYS_NULL",
            "NP_NULL_PARAM_DEREF",
            "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
            "RCN_REDUNDANT_NULLCHECK_OF_NULL_VALUE",
            "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR",
            "SF_SWITCH_NO_DEFAULT",
            "DM_DEFAULT_ENCODING",
            "EQ_CHECK_FOR_OPERAND_NOT_COMPATIBLE_WITH_THIS",
            "IA_AMBIGUOUS_INVOCATION_OF_INHERITED_OR_OUTER_METHOD",
            "LI_LAZY_INIT_STATIC",
            "RpC_REPEATED_CONDITIONAL_TEST",
            "NS_NON_SHORT_CIRCUIT",
            "ES_COMPARING_PARAMETER_STRING_WITH_EQ",
            "FE_FLOATING_POINT_EQUALITY",
            "FE_TEST_IF_EQUAL_TO_NOT_A_NUMBER",
            "ICAST_IDIV_CAST_TO_DOUBLE",
            "ICAST_QUESTIONABLE_UNSIGNED_RIGHT_SHIFT",
            "SA_FIELD_SELF_ASSIGNMENT",
            "UC_USELESS_CONDITION",
            "UC_USELESS_OBJECT",
            "UCF_USELESS_CONTROL_FLOW",
            "EC_UNRELATED_TYPES",
            "EQ_ALWAYS_FALSE",
            "SBSC_USE_STRINGBUFFER_CONCATENATION",
            "SIC_INNER_SHOULD_BE_STATIC",
            "EQ_DOESNT_OVERRIDE_EQUALS",
            "CO_COMPARETO_INCORRECT_FLOATING",
            "DL_SYNCHRONIZATION_ON_SHARED_CONSTANT",
            "SSD_DO_NOT_USE_INSTANCE_LOCK_ON_SHARED_STATIC_DATA",
            "DLS_DEAD_LOCAL_STORE",
            "DLS_DEAD_LOCAL_STORE_OF_NULL",
            "DM_NUMBER_CTOR",
            "DMI_INVOKING_TOSTRING_ON_ARRAY",
            "EC_NULL_ARG",
            "EC_UNRELATED_TYPES_USING_POINTER_EQUALITY",
            "EQ_GETCLASS_AND_CLASS_CONSTANT",
            "EQ_UNUSUAL",
            "ES_COMPARING_STRINGS_WITH_EQ",
            "FI_EMPTY",
            "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
            "DM_GC",
            "CN_IMPLEMENTS_CLONE_BUT_NOT_CLONEABLE",
            "BC_UNCONFIRMED_CAST",
            "BC_UNCONFIRMED_CAST_OF_RETURN_VALUE",
            "CN_IDIOM_NO_SUPER_CALL",
            "DM_BOOLEAN_CTOR",
            "DM_FP_NUMBER_CTOR",
            "DM_EXIT",
            "DC_DOUBLECHECK",
            "DB_DUPLICATE_SWITCH_CLAUSES",
            "EI_EXPOSE_REP", 
            "EI_EXPOSE_REP2",
            "EI_EXPOSE_STATIC_REP2",
            "EQ_COMPARETO_USE_OBJECT_EQUALS",
            "MS_EXPOSE_REP",
            "NM_CONFUSING",
            "NM_FIELD_NAMING_CONVENTION",
            "NM_METHOD_NAMING_CONVENTION",
            "NN_NAKED_NOTIFY",
            "NO_NOTIFY_NOT_NOTIFYALL",
            "NP_LOAD_OF_KNOWN_NULL_VALUE",
            "NP_BOOLEAN_RETURN_NULL",
            "RC_REF_COMPARISON_BAD_PRACTICE_BOOLEAN",
            "OS_OPEN_STREAM",
            "REFLC_REFLECTION_MAY_INCREASE_ACCESSIBILITY_OF_CLASS",
            "REC_CATCH_EXCEPTION",
            "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE",
            "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT",
            "INT_VACUOUS_COMPARISON",
            "DM_STRING_TOSTRING",
            "HE_HASHCODE_USE_OBJECT_EQUALS",
            "IM_BAD_CHECK_FOR_ODD",
            "IM_AVERAGE_COMPUTATION_COULD_OVERFLOW",
            "INT_VACUOUS_BIT_OPERATION",
            "ICAST_INT_2_LONG_AS_INSTANT",
            "ICAST_INT_CAST_TO_FLOAT_PASSED_TO_ROUND",
            "IT_NO_SUCH_ELEMENT",
            "FL_FLOATS_AS_LOOP_COUNTERS",
            "UI_INHERITANCE_UNSAFE_GETRESOURCE",
            "IS2_INCONSISTENT_SYNC",
            "RR_NOT_CHECKED",
            "URF_UNREAD_FIELD",
            "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
            "UR_UNINIT_READ",
            "UUF_UNUSED_FIELD",
            "UWF_NULL_FIELD",
            "UW_UNCOND_WAIT",
            "LI_LAZY_INIT_UPDATE_STATIC",
            "SIC_INNER_SHOULD_BE_STATIC_ANON",
            "SS_SHOULD_BE_STATIC",
            "UPM_UNCALLED_PRIVATE_METHOD",
            "RV_RETURN_VALUE_IGNORED_INFERRED",
            "RV_CHECK_FOR_POSITIVE_INDEXOF",
            "SF_SWITCH_FALLTHROUGH",
            "SIC_INNER_SHOULD_BE_STATIC_NEEDS_THIS",
            "SA_FIELD_DOUBLE_ASSIGNMENT",
            "SA_FIELD_SELF_COMPARISON",
            "SR_NOT_CHECKED",
            "SWL_SLEEP_WITH_LOCK_HELD",
            "UC_USELESS_CONDITION_TYPE"
        }

        def _is_exempt(f: Finding) -> bool:
            loc = f.path or f.location or ""
            if f.rule == "SA_FIELD_SELF_ASSIGNMENT" and "InfBlocks.java" in loc:
                return True
            if f.rule == "SF_SWITCH_FALLTHROUGH" and "InfBlocks.java" in loc:
                return True
            if f.rule == "SF_SWITCH_FALLTHROUGH" and "InfCodes.java" in loc:
                return True
            if f.rule == "SF_SWITCH_FALLTHROUGH" and "Inflate.java" in loc:
                return True
            if f.rule == "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD" and "TarEntry.java" in loc:
                return True
            if f.rule == "URF_UNREAD_FIELD" and "GridBagLayoutInfo" in loc:
                return True
            if f.rule == "NN_NAKED_NOTIFY" and "Display.java" in loc:
                return True
            if f.rule == "ICAST_QUESTIONABLE_UNSIGNED_RIGHT_SHIFT" and "Deflate.java" in loc:
                return True
            return False


        violations = [
            f for f in spotbugs.findings
            if f.rule in forbidden_rules and not _is_exempt(f)
        ]
        if violations:
            print("\n❌ Build failed due to forbidden SpotBugs violations:")
            for v in violations:
                print(f"  - {v.rule}: {v.location} - {v.message}")
            exit(1)

    pmd = parse_pmd()
    if pmd:
        forbidden_pmd_rules = {
            "ClassWithOnlyPrivateConstructorsShouldBeFinal",
            "CompareObjectsWithEquals",
            "FormalParameterNamingConventions",
            "LiteralsFirstInComparisons",
            "LocalVariableNamingConventions",
            "MethodNamingConventions",
            "MissingOverride",
            "ControlStatementBraces"
        }
        violations = [f for f in pmd.findings if f.rule in forbidden_pmd_rules]
        if violations:
            print("\n❌ Build failed due to forbidden PMD violations:")
            for v in violations:
                print(f"  - {v.rule}: {v.location} - {v.message}")
            exit(1)


if __name__ == "__main__":
    main()

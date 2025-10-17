#!/usr/bin/env python3
"""Ensure an Xcode scheme exists that wires the UI test bundle for Codename One CI.

The Codename One iOS template historically only shipped a user-specific scheme,
which means fresh CI machines don't see any test actions when invoking
``xcodebuild test``.  This helper inspects the generated project, discovers the
primary application target and any associated unit/UI test bundles, and emits a
shared scheme that drives them.

Usage:
    create-shared-scheme.py <project_dir> [scheme_name]

The script writes the shared scheme into both the .xcodeproj and any sibling
.xcworkspace directories so either entry point exposes the test action.
"""

from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, List, Optional


@dataclass
class Target:
    identifier: str
    name: str
    product_type: str
    product_name: Optional[str]

    @property
    def buildable_name(self) -> str:
        if self.product_name:
            return self.product_name
        if self.product_type.endswith(".application"):
            return f"{self.name}.app"
        if self.product_type.endswith(".bundle.ui-testing") or self.product_type.endswith(".bundle.unit-test"):
            return f"{self.name}.xctest"
        return self.name


TARGET_BLOCK_RE = re.compile(
    r"""
    ^\s*(?P<identifier>[0-9A-F]{24})\s+/\*\s+(?P<comment>[^*]+)\s+\*/\s+=\s+\{
    (?P<body>.*?)
    ^\s*\};
    """,
    re.MULTILINE | re.DOTALL | re.VERBOSE,
)


def parse_targets(project_file: Path) -> List[Target]:
    content = project_file.read_text(encoding="utf-8")
    targets: List[Target] = []
    for match in TARGET_BLOCK_RE.finditer(content):
        body = match.group("body")
        if "isa = PBXNativeTarget;" not in body:
            continue
        name = _search_value(body, r"name = (?P<value>[^;]+);")
        product_type = _search_value(body, r"productType = \"(?P<value>[^\"]+)\";")
        product_name = _search_value(body, r"productReference = [0-9A-F]{24} /\* (?P<value>[^*]+) \*/;")
        if not name or not product_type:
            continue
        targets.append(
            Target(
                identifier=match.group("identifier"),
                name=name.strip().strip('"'),
                product_type=product_type.strip(),
                product_name=product_name.strip() if product_name else None,
            )
        )
    return targets


def _search_value(text: str, pattern: str) -> Optional[str]:
    m = re.search(pattern, text)
    return m.group("value") if m else None


def choose_targets(targets: Iterable[Target]) -> tuple[Optional[Target], Optional[Target], Optional[Target]]:
    app = None
    ui = None
    unit = None
    for target in targets:
        if target.product_type.endswith(".application") and app is None:
            app = target
        elif target.product_type.endswith(".bundle.ui-testing") and ui is None:
            ui = target
        elif target.product_type.endswith(".bundle.unit-test") and unit is None:
            unit = target
    return app, unit, ui


def render_testable(target: Target, container: str) -> str:
    return f"""         <TestableReference
            skipped = \"NO\">
            <BuildableReference
               BuildableIdentifier = \"primary\"
               BlueprintIdentifier = \"{target.identifier}\"
               BuildableName = \"{target.buildable_name}\"
               BlueprintName = \"{target.name}\"
               ReferencedContainer = \"container:{container}\">
            </BuildableReference>
         </TestableReference>"""


def render_scheme(
    scheme_name: str,
    project_container: str,
    app: Target,
    testables: List[str],
) -> str:
    testables_block = "\n".join(testables) if testables else ""
    return f"""<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<Scheme
   LastUpgradeVersion = \"0500\"
   version = \"1.3\">
   <BuildAction
      parallelizeBuildables = \"YES\"
      buildImplicitDependencies = \"YES\">
      <BuildActionEntries>
         <BuildActionEntry
            buildForTesting = \"YES\"
            buildForRunning = \"YES\"
            buildForProfiling = \"YES\"
            buildForArchiving = \"YES\"
            buildForAnalyzing = \"YES\">
            <BuildableReference
               BuildableIdentifier = \"primary\"
               BlueprintIdentifier = \"{app.identifier}\"
               BuildableName = \"{app.buildable_name}\"
               BlueprintName = \"{app.name}\"
               ReferencedContainer = \"container:{project_container}\">
            </BuildableReference>
         </BuildActionEntry>
      </BuildActionEntries>
   </BuildAction>
   <TestAction
      selectedDebuggerIdentifier = \"Xcode.DebuggerFoundation.Debugger.LLDB\"
      selectedLauncherIdentifier = \"Xcode.DebuggerFoundation.Launcher.LLDB\"
      shouldUseLaunchSchemeArgsEnv = \"YES\"
      buildConfiguration = \"Debug\">
      <Testables>
{testables_block}
      </Testables>
      <MacroExpansion>
         <BuildableReference
            BuildableIdentifier = \"primary\"
            BlueprintIdentifier = \"{app.identifier}\"
            BuildableName = \"{app.buildable_name}\"
            BlueprintName = \"{app.name}\"
            ReferencedContainer = \"container:{project_container}\">
         </BuildableReference>
      </MacroExpansion>
   </TestAction>
   <LaunchAction
      selectedDebuggerIdentifier = \"Xcode.DebuggerFoundation.Debugger.LLDB\"
      selectedLauncherIdentifier = \"Xcode.DebuggerFoundation.Launcher.LLDB\"
      launchStyle = \"0\"
      useCustomWorkingDirectory = \"NO\"
      buildConfiguration = \"Debug\"
      ignoresPersistentStateOnLaunch = \"NO\"
      debugDocumentVersioning = \"YES\"
      allowLocationSimulation = \"YES\">
      <BuildableProductRunnable>
         <BuildableReference
            BuildableIdentifier = \"primary\"
            BlueprintIdentifier = \"{app.identifier}\"
            BuildableName = \"{app.buildable_name}\"
            BlueprintName = \"{app.name}\"
            ReferencedContainer = \"container:{project_container}\">
         </BuildableReference>
      </BuildableProductRunnable>
      <AdditionalOptions>
      </AdditionalOptions>
   </LaunchAction>
   <ProfileAction
      shouldUseLaunchSchemeArgsEnv = \"YES\"
      savedToolIdentifier = \"\"
      useCustomWorkingDirectory = \"NO\"
      buildConfiguration = \"Release\"
      debugDocumentVersioning = \"YES\">
      <BuildableProductRunnable>
         <BuildableReference
            BuildableIdentifier = \"primary\"
            BlueprintIdentifier = \"{app.identifier}\"
            BuildableName = \"{app.buildable_name}\"
            BlueprintName = \"{app.name}\"
            ReferencedContainer = \"container:{project_container}\">
         </BuildableReference>
      </BuildableProductRunnable>
   </ProfileAction>
   <AnalyzeAction
      buildConfiguration = \"Debug\">
   </AnalyzeAction>
   <ArchiveAction
      buildConfiguration = \"Release\"
      revealArchiveInOrganizer = \"YES\">
   </ArchiveAction>
</Scheme>
"""


def ensure_scheme(destination: Path, scheme_name: str, xml: str) -> None:
    destination.mkdir(parents=True, exist_ok=True)
    scheme_path = destination / f"{scheme_name}.xcscheme"
    scheme_path.write_text(xml, encoding="utf-8")


def main(argv: List[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("project_dir", type=Path)
    parser.add_argument("scheme_name", nargs="?", help="Override the generated scheme name")
    args = parser.parse_args(argv[1:])

    project_dir: Path = args.project_dir.resolve()
    if not project_dir.is_dir():
        print(f"error: project directory not found: {project_dir}", file=sys.stderr)
        return 1

    try:
        xcodeproj = next(project_dir.glob("*.xcodeproj"))
    except StopIteration:
        print(f"error: unable to locate an .xcodeproj under {project_dir}", file=sys.stderr)
        return 1

    project_container = xcodeproj.name
    project_file = xcodeproj / "project.pbxproj"
    if not project_file.is_file():
        print(f"error: missing project file: {project_file}", file=sys.stderr)
        return 1

    targets = parse_targets(project_file)
    if not targets:
        print(f"error: no build targets discovered in {project_file}", file=sys.stderr)
        return 1

    app, unit, ui = choose_targets(targets)
    if not app:
        print("error: unable to find application target", file=sys.stderr)
        return 1

    scheme_name = args.scheme_name or app.name

    testables: List[str] = []
    for target in (unit, ui):
        if target is not None:
            testables.append(render_testable(target, project_container))

    if not testables:
        print("warning: no unit or UI test targets discovered; emitting app-only scheme", file=sys.stderr)

    xml = render_scheme(scheme_name, project_container, app, testables)

    destinations = [xcodeproj / "xcshareddata" / "xcschemes"]
    destinations.extend(ws / "xcshareddata" / "xcschemes" for ws in project_dir.glob("*.xcworkspace"))

    for dest in destinations:
        ensure_scheme(dest, scheme_name, xml)

    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))

#!/usr/bin/env python3
#
# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Codename One designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Codename One through http://www.codenameone.com/ if you
# need additional information or have any questions.

"""Derives the artifacts a release resolves but does not publish.

Prints one `artifactId:version:packaging:[classifier,...]` per line, for
seed-frozen-artifacts.sh (which copies them into R2) and check-frozen-artifacts.sh
(which refuses to release while one of them is absent).

This is DERIVED, never a hand-maintained list, and that is the whole point. The
list was hand-maintained for one commit and was already wrong twice: it named
sqlite-jdbc, which is an ordinary reactor module published on every tag, and it
omitted cn1-builder-resources-common and cn1-builder-resources-android, which are
`runtime` dependencies of codenameone-maven-plugin pinned at an old version and
published nowhere but Central. The second omission would have broken the plugin's
own runtime classpath for every R2-only project -- every build, not one goal.

An artifact is frozen when a release resolves it at a version that is not the
release's own. Two ways that happens, and both are read from where the value
actually lives so that bumping a pin cannot leave the seeding behind:

  1. A `com.codenameone` dependency with a literal version. `${project.version}`
     and friends move with the release and are published by it, so only a
     hardcoded version marks a pin. `system` scope is excluded because Maven
     resolves it from systemPath on disk and never asks a repository -- that is
     what jfxrt and codenameone-buildclient are. `test` scope never reaches a
     user.

  2. A version the plugin resolves programmatically, which no pom declares:
     cn1.designer.version on AbstractCN1Mojo. codenameone-javase-svg is published
     in lockstep with the designer and shares its version -- it exists only to
     give that editor Batik SVG support.

Classifiers matter here: the plugin asks for the designer as
`jar-with-dependencies`, so checking only the main jar would pass while the
artifact actually consumed is missing. They are scraped from the same
getArtifact call that names them.

The TRANSITIVE CLOSURE of those pins is included, not just the pins themselves.
`findArtifactFile` resolves with `setResolveTransitively(true)`, so resolving
codenameone-designer:7.0.263 also resolves codenameone-core, codenameone-javase,
codenameone-javase-svg, codenameone-css-compiler and sqlite-jdbc at that same
version -- none of which any release publishes, and all of which predate the
first version in this repository. Seeding only the two named pins therefore left
the goal still depending on Maven Central, which is the exact dependency the
seeding exists to remove. Verified with `dependency:get`: transitive resolution
of the designer pulls six jars, non-transitive pulls one.

Parent poms are part of that closure. Maven reads a pom's parent to build the
effective model, so resolving codenameone-designer:7.0.263 also fetches
com.codenameone:codenameone:7.0.263 -- a `pom` packaging artifact with no jar,
which is why the emitted coordinate carries its packaging. This was found by
diffing the derived set against what `dependency:get` actually pulled into an
empty local repository, not by reading the poms harder; that diff is worth
re-running when this logic changes.

The closure is walked over the published poms, preferring this repository and
falling back to Central, so once a set has been seeded the walk no longer needs
Central at all. It is not cached in a checked-in list: that would be the same
hand-maintained set this file exists to replace.

Fails loudly rather than defaulting. Seeding the wrong set is worse than seeding
nothing, because the immutability guard then refuses to repair it.
"""

import glob
import os
import re
import sys
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET

NS = "{http://maven.apache.org/POM/4.0.0}"
GROUP = "com.codenameone"

# Versions that track the release itself. Anything else is a pin.
RELEASE_VERSIONS = ("${project.version}", "${cn1.version}", "${cn1.plugin.version}")

# Poms are fetched from here, in order. R2 first so that a seeded set needs no Central.
POM_SOURCES = (
    os.environ.get("R2_BASE_URL", "https://repo.codenameone.com/maven2") + "/com/codenameone/",
    "https://repo1.maven.org/maven2/com/codenameone/",
)

# Cloudflare's Browser Integrity Check 403s "Python-urllib/*" on the repository's
# custom domain. A Configuration Rule exempts /maven2/*, but this does not depend on
# that rule staying in place.
POM_HEADERS = {"User-Agent": "codenameone-frozen-coordinates"}

# Scopes Maven does not resolve from a repository, or does not resolve for a consumer.
NON_RESOLVED_SCOPES = ("test", "provided", "system")

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
MOJO = os.path.join(REPO_ROOT, "maven", "codenameone-maven-plugin", "src", "main",
                    "java", "com", "codename1", "maven", "AbstractCN1Mojo.java")
PLUGIN_SRC = os.path.join(REPO_ROOT, "maven", "codenameone-maven-plugin", "src", "main", "java")


def fail(message):
    sys.stderr.write("ERROR: %s\n" % message)
    sys.stderr.write("Update frozen-coordinates.py rather than hardcoding a version, "
                     "or the seeding and the release check will disagree.\n")
    sys.exit(1)


def read_designer_version():
    try:
        with open(MOJO, "r") as handle:
            source = handle.read()
    except IOError as err:
        fail("could not read %s: %s" % (MOJO, err))
    match = re.search(r'"cn1\.designer\.version",\s*defaultValue\s*=\s*"([^"]+)"', source)
    if not match:
        fail("could not read the pinned designer version from %s; it has moved or been renamed" % MOJO)
    return match.group(1)


def read_classifiers():
    """artifactId -> [classifier], from the plugin's own getArtifact calls."""
    found = {}
    pattern = re.compile(r'getArtifact\(\s*"com\.codenameone"\s*,\s*"([^"]+)"\s*,\s*"([^"]+)"\s*\)')
    for path in glob.glob(os.path.join(PLUGIN_SRC, "**", "*.java"), recursive=True):
        with open(path, "r") as handle:
            for artifact, classifier in pattern.findall(handle.read()):
                found.setdefault(artifact, [])
                if classifier not in found[artifact]:
                    found[artifact].append(classifier)
    return found


def read_pinned_dependencies():
    """(artifactId, version) for every com.codenameone dependency at a literal version."""
    pinned = set()
    roots = [os.path.join(REPO_ROOT, "maven"), os.path.join(REPO_ROOT, "scripts")]
    for root in roots:
        for pom in glob.glob(os.path.join(root, "**", "pom.xml"), recursive=True):
            # target/ is build output; archetype-resources are templates for a
            # generated project, whose versions are the user's, not ours.
            if os.sep + "target" + os.sep in pom or "archetype-resources" in pom:
                continue
            try:
                tree = ET.parse(pom).getroot()
            except ET.ParseError as err:
                fail("could not parse %s: %s" % (pom, err))
            for dependency in tree.iter(NS + "dependency"):
                group = (dependency.findtext(NS + "groupId") or "").strip()
                artifact = (dependency.findtext(NS + "artifactId") or "").strip()
                version = (dependency.findtext(NS + "version") or "").strip()
                scope = (dependency.findtext(NS + "scope") or "").strip()
                if group != GROUP or not artifact or not version:
                    continue
                if version in RELEASE_VERSIONS or version.startswith("${"):
                    continue
                if scope in ("system", "test"):
                    continue
                pinned.add((artifact, version))
    return pinned


def fetch_pom(artifact, version):
    """The artifact's pom bytes, from the first source that has it, or None."""
    last_error = None
    for source in POM_SOURCES:
        url = "%s%s/%s/%s-%s.pom" % (source, artifact, version, artifact, version)
        request = urllib.request.Request(url, headers=POM_HEADERS)
        try:
            return urllib.request.urlopen(request, timeout=30).read()
        except urllib.error.HTTPError as err:
            if err.code == 404:
                continue  # not here; try the next source
            last_error = "%s -> HTTP %s" % (url, err.code)
        except Exception as err:  # noqa: BLE001 - any transport problem is the same here
            last_error = "%s -> %s" % (url, err)
    if last_error:
        # A transport failure is not the same as "absent everywhere". Failing is right:
        # deriving a short closure from a bad minute would seed an incomplete set and
        # then pass its own check.
        fail("could not read the pom for %s:%s (%s)" % (artifact, version, last_error))
    return None


def properties_of(root):
    values = {}
    node = root.find(NS + "properties")
    if node is not None:
        for child in node:
            values[child.tag.replace(NS, "")] = (child.text or "").strip()
    return values


def closure(roots):
    """roots -> {(artifactId, version): packaging} for everything reachable."""
    resolved = {}
    pending = list(roots)
    while pending:
        artifact, version = pending.pop()
        if (artifact, version) in resolved:
            continue
        resolved[(artifact, version)] = "jar"
        body = fetch_pom(artifact, version)
        if body is None:
            # A root that exists in no repository is a broken pin, not an empty closure.
            if (artifact, version) in roots:
                fail("%s:%s is in no repository, so the pin cannot be satisfied"
                     % (artifact, version))
            continue
        try:
            root = ET.fromstring(body)
        except ET.ParseError as err:
            fail("could not parse the pom for %s:%s: %s" % (artifact, version, err))
        resolved[(artifact, version)] = (root.findtext(NS + "packaging") or "jar").strip() or "jar"

        # The parent is resolved too -- Maven needs it to build the effective model --
        # and it is easy to miss because it is not a <dependency>.
        parent = root.find(NS + "parent")
        if parent is not None and (parent.findtext(NS + "groupId") or "").strip() == GROUP:
            parent_artifact = (parent.findtext(NS + "artifactId") or "").strip()
            parent_version = (parent.findtext(NS + "version") or "").strip()
            if parent_artifact and parent_version and not parent_version.startswith("${"):
                pending.append((parent_artifact, parent_version))

        values = properties_of(root)
        values["project.version"] = version
        # project/dependencies only -- NOT root.iter(), which also returns
        # <dependencyManagement> entries. Those are version constraints, not
        # dependencies: Maven never resolves one unless something declares it. Walking
        # them turned a 7-coordinate closure into 20, because the reactor's parent pom
        # manages every module in the build.
        declared = root.find(NS + "dependencies")
        for dependency in (declared.findall(NS + "dependency") if declared is not None else []):
            group = (dependency.findtext(NS + "groupId") or "").strip()
            child = (dependency.findtext(NS + "artifactId") or "").strip()
            child_version = (dependency.findtext(NS + "version") or "").strip()
            scope = (dependency.findtext(NS + "scope") or "").strip()
            optional = (dependency.findtext(NS + "optional") or "").strip()
            if group != GROUP or not child:
                continue
            if scope in NON_RESOLVED_SCOPES or optional == "true":
                continue
            while child_version.startswith("${") and child_version.endswith("}"):
                replacement = values.get(child_version[2:-1])
                if replacement is None or replacement == child_version:
                    break
                child_version = replacement
            if not child_version or child_version.startswith("${"):
                # Inherited from the parent's dependencyManagement, which for these poms
                # always tracks the release's own version.
                child_version = version
            pending.append((child, child_version))
    return resolved


def main():
    designer_version = read_designer_version()
    classifiers = read_classifiers()

    roots = set()
    roots.add(("codenameone-designer", designer_version))
    roots.add(("codenameone-javase-svg", designer_version))
    roots |= read_pinned_dependencies()

    if not roots:
        fail("no frozen coordinates were derived, which cannot be right")

    coordinates = closure(roots)

    for (artifact, version), packaging in sorted(coordinates.items()):
        print("%s:%s:%s:%s" % (artifact, version, packaging,
                               ",".join(sorted(classifiers.get(artifact, [])))))


if __name__ == "__main__":
    main()

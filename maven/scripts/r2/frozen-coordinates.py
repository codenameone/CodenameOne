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

Prints one `artifactId:version[:classifier,...]` per line, for
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

Fails loudly rather than defaulting. Seeding the wrong set is worse than seeding
nothing, because the immutability guard then refuses to repair it.
"""

import glob
import os
import re
import sys
import xml.etree.ElementTree as ET

NS = "{http://maven.apache.org/POM/4.0.0}"
GROUP = "com.codenameone"

# Versions that track the release itself. Anything else is a pin.
RELEASE_VERSIONS = ("${project.version}", "${cn1.version}", "${cn1.plugin.version}")

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


def main():
    designer_version = read_designer_version()
    classifiers = read_classifiers()

    coordinates = set()
    coordinates.add(("codenameone-designer", designer_version))
    coordinates.add(("codenameone-javase-svg", designer_version))
    coordinates |= read_pinned_dependencies()

    if not coordinates:
        fail("no frozen coordinates were derived, which cannot be right")

    for artifact, version in sorted(coordinates):
        line = "%s:%s" % (artifact, version)
        if artifact in classifiers:
            line += ":" + ",".join(sorted(classifiers[artifact]))
        print(line)


if __name__ == "__main__":
    main()

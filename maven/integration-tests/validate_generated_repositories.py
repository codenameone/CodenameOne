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

"""Guard that every project-scaffolding path declares the Codename One repository.

Codename One releases are moving off Maven Central to the repository at
https://repo.codenameone.com/maven2. A generated project that does not declare it
resolves normally today -- Central still has every published version -- and then
silently stops seeing new releases after the cutover. The failure is therefore
invisible at generation time, which is exactly why it needs a build-time gate.

Both lists are checked because Maven resolves ordinary dependencies through
<repositories> and build plugins through <pluginRepositories>. A project holding only
the first can download codenameone-core and still fail to find a newer
codenameone-maven-plugin, which reads as a corrupt install rather than a missing
repository.

The three scaffolds must agree; they are the only ways a user gets a new project:

  1. cn1app-archetype  -- mvn archetype:generate / cn1:generate-app-project
  2. cn1lib-archetype  -- library projects
  3. common.zip        -- the Initializr root pom (start.codenameone.com)
"""

from pathlib import Path
import sys
import xml.etree.ElementTree as ET
import zipfile


MAVEN_NS = "http://maven.apache.org/POM/4.0.0"
NS = {"m": MAVEN_NS}
REPOSITORY_URL = "https://repo.codenameone.com/maven2"

# (container element, child element) for the two independent resolution paths.
REQUIRED_LISTS = (
    ("repositories", "repository"),
    ("pluginRepositories", "pluginRepository"),
)

APP_ARCHETYPE_POM = "maven/cn1app-archetype/src/main/resources/archetype-resources/pom.xml"
LIB_ARCHETYPE_POM = "maven/cn1lib-archetype/src/main/resources/archetype-resources/pom.xml"
INITIALIZR_ZIP = "scripts/initializr/common/src/main/resources/common.zip"
INITIALIZR_ZIP_POM = "pom.xml"


def fail(message):
    print("ERROR: " + message, file=sys.stderr)
    raise SystemExit(1)


def child_text(element, name):
    child = element.find("m:" + name, NS)
    if child is None or child.text is None:
        return None
    return child.text.strip()


def validate_pom(label, data):
    try:
        project = ET.fromstring(data)
    except ET.ParseError as error:
        fail(label + " is not valid XML: " + str(error))

    for list_name, entry_name in REQUIRED_LISTS:
        container = project.find("m:" + list_name, NS)
        if container is None:
            fail(label + " has no <" + list_name + ">; generated projects stop seeing "
                 + "Codename One releases once publication to Maven Central ends")

        matches = [entry for entry in container.findall("m:" + entry_name, NS)
                   if child_text(entry, "url") == REPOSITORY_URL]
        if not matches:
            fail(label + " does not declare " + REPOSITORY_URL + " in <" + list_name + ">")

        for entry in matches:
            releases = entry.find("m:releases", NS)
            if releases is not None and child_text(releases, "enabled") == "false":
                fail(label + " declares " + REPOSITORY_URL + " in <" + list_name
                     + "> with releases disabled, so no release can resolve from it")


def main():
    repo_root = Path(__file__).resolve().parents[2]

    for relative_path in (APP_ARCHETYPE_POM, LIB_ARCHETYPE_POM):
        pom_path = repo_root / relative_path
        if not pom_path.is_file():
            fail("archetype POM not found: " + str(pom_path))
        validate_pom(relative_path, pom_path.read_bytes())

    archive_path = repo_root / INITIALIZR_ZIP
    if not archive_path.is_file():
        fail("Initializr artifact not found: " + str(archive_path))
    with zipfile.ZipFile(str(archive_path), "r") as archive:
        try:
            data = archive.read(INITIALIZR_ZIP_POM)
        except KeyError:
            fail(INITIALIZR_ZIP + " is missing " + INITIALIZR_ZIP_POM)
        validate_pom(INITIALIZR_ZIP + "!" + INITIALIZR_ZIP_POM, data)

    print("All project scaffolds declare " + REPOSITORY_URL
          + " for both dependencies and plugins.")


if __name__ == "__main__":
    main()

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

"""Validate Maven identities in the Initializr's committed download artifact."""

from pathlib import Path
import sys
import xml.etree.ElementTree as ET
import zipfile


MAVEN_NS = "http://maven.apache.org/POM/4.0.0"
NS = {"m": MAVEN_NS}
ROOT_GROUP_ID = "com.example.myapp"
ROOT_ARTIFACT_ID = "myappname"
ROOT_VERSION = "1.0-SNAPSHOT"
PLATFORMS = ("android", "ios", "javase", "javascript", "linux", "win")


def fail(message):
    print("ERROR: " + message, file=sys.stderr)
    raise SystemExit(1)


def direct_text(element, child_name, path):
    child = element.find("m:" + child_name, NS)
    if child is None or child.text is None:
        fail(path + " is missing direct <" + child_name + ">")
    return child.text.strip()


def require_equal(actual, expected, path, field):
    if actual != expected:
        fail(path + " has " + field + "=" + repr(actual) + "; expected " + repr(expected))


def read_pom(archive, path):
    try:
        data = archive.read(path)
    except KeyError:
        fail("Initializr artifact is missing " + path)
    try:
        return data, ET.fromstring(data)
    except ET.ParseError as error:
        fail(path + " is not valid XML: " + str(error))


def validate_root_pom(archive):
    data, project = read_pom(archive, "pom.xml")
    require_equal(direct_text(project, "groupId", "pom.xml"), ROOT_GROUP_ID, "pom.xml", "groupId")
    require_equal(direct_text(project, "artifactId", "pom.xml"), ROOT_ARTIFACT_ID, "pom.xml", "artifactId")
    require_equal(direct_text(project, "version", "pom.xml"), ROOT_VERSION, "pom.xml", "version")

    properties = project.find("m:properties", NS)
    if properties is None:
        fail("pom.xml is missing <properties>")
    require_equal(direct_text(properties, "cn1app.name", "pom.xml/properties"),
                  ROOT_ARTIFACT_ID, "pom.xml/properties", "cn1app.name")
    modules = project.find("m:modules", NS)
    if modules is None or "common" not in {
            module.text.strip() for module in modules.findall("m:module", NS) if module.text}:
        fail("pom.xml does not declare the common module")
    reject_initializr_coordinates(data, "pom.xml")


def validate_platform_pom(archive, platform):
    path = platform + "/pom.xml"
    data, project = read_pom(archive, path)
    parent = project.find("m:parent", NS)
    if parent is None:
        fail(path + " is missing <parent>")

    require_equal(direct_text(parent, "groupId", path + "/parent"),
                  ROOT_GROUP_ID, path + "/parent", "groupId")
    require_equal(direct_text(parent, "artifactId", path + "/parent"),
                  ROOT_ARTIFACT_ID, path + "/parent", "artifactId")
    require_equal(direct_text(parent, "version", path + "/parent"),
                  ROOT_VERSION, path + "/parent", "version")
    require_equal(direct_text(project, "groupId", path), ROOT_GROUP_ID, path, "groupId")
    require_equal(direct_text(project, "artifactId", path),
                  ROOT_ARTIFACT_ID + "-" + platform, path, "artifactId")
    require_equal(direct_text(project, "version", path), ROOT_VERSION, path, "version")
    require_equal(direct_text(project, "name", path),
                  ROOT_ARTIFACT_ID + "-" + platform, path, "name")

    common_dependency_found = False
    dependencies = project.find("m:dependencies", NS)
    if dependencies is not None:
        for dependency in dependencies.findall("m:dependency", NS):
            artifact_id = direct_text(dependency, "artifactId", path + "/dependencies/dependency")
            if artifact_id == "${cn1app.name}-common":
                common_dependency_found = True
                require_equal(direct_text(dependency, "groupId", path + "/common-dependency"),
                              "${project.groupId}", path + "/common-dependency", "groupId")
                require_equal(direct_text(dependency, "version", path + "/common-dependency"),
                              "${project.version}", path + "/common-dependency", "version")
    if not common_dependency_found:
        fail(path + " does not depend on ${project.groupId}:${cn1app.name}-common:${project.version}")

    reject_initializr_coordinates(data, path)


def reject_initializr_coordinates(data, path):
    text = data.decode("utf-8")
    if "<groupId>com.codename1.initializr</groupId>" in text:
        fail(path + " leaks the Initializr application's groupId into a user download")
    if "<artifactId>initializr" in text:
        fail(path + " leaks an Initializr application artifactId into a user download")


def main():
    repo_root = Path(__file__).resolve().parents[2]
    archive_path = repo_root / "scripts/initializr/common/src/main/resources/common.zip"
    if not archive_path.is_file():
        fail("Initializr artifact not found: " + str(archive_path))

    with zipfile.ZipFile(str(archive_path), "r") as archive:
        embedded_poms = {name for name in archive.namelist()
                         if name == "pom.xml" or name.endswith("/pom.xml")}
        # common/pom.xml is deliberately not stored in common.zip: GeneratorModel
        # injects the selected template's common POM after reading this artifact.
        # The generated common POM is covered by its runtime guard and matrix tests.
        expected_poms = {"pom.xml"} | {platform + "/pom.xml" for platform in PLATFORMS}
        if embedded_poms != expected_poms:
            fail("Initializr artifact POM set differs from the validated platform set: found "
                 + repr(sorted(embedded_poms)) + ", expected " + repr(sorted(expected_poms)))

        validate_root_pom(archive)
        for platform in PLATFORMS:
            validate_platform_pom(archive, platform)

    print("Initializr embedded POM coordinates are consistent across all platform modules.")


if __name__ == "__main__":
    main()

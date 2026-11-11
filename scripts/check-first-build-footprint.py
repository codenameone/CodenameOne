#!/usr/bin/env python3
"""Hold the size of a new project's first build.

A developer's first contact with Codename One is a project from the
initializr and `./run.sh`. Maven then downloads everything that build
resolves, and for a long time that was 346 MB in 869 files of which the
simulator opened a fraction:

  * 157 MB was org.bytedeco's ffmpeg for seven platforms, two of them
    Android, on a machine that can run one.
  * 55 MB was the iOS, Android, macOS, Windows and Linux ports and the
    ParparVM translator. They were dependencies of the Maven plugin, and
    Maven resolves a plugin's whole class path before its first goal, so
    a Hello World started in the simulator fetched all of them.

Nothing failed, so nothing noticed: a dependency added to the plugin is
invisible in every test, and shows only as a slower first run on a
machine nobody on the project has -- an empty one.

This script is that machine. It generates an application from the
archetype this tree builds, runs the build `run.sh` runs (stopping
before the simulator window) against an EMPTY local repository, and
reads what arrived. It is a floor, not the whole bill: the Maven wrapper's
own distribution, and whatever the simulator fetches once it is up (the
browser component's natives), are not in a local repository and are not
counted.

  1. No port artifact. They are resolved when a target that reads one is
     built (AppBuilder.portArtifacts); a port in a simulator build means
     something depends on it again.
  2. No org.bytedeco native for a platform other than the host's.
  3. The total stays under a budget.

Before any of that it reads maven/cn1-binaries/pom.xml and holds the
profile of EVERY host to the table below. CI runs this on one operating
system; a profile for another that named the wrong native, or lost its
activation, would otherwise reach the developers on that system as a
simulator with no ffmpeg and no error.

The empty repository is filled from the workspace's own, offered to Maven
as a `file://` remote. That is what makes the run cheap and exact: the
snapshot artifacts this tree just built are the ones measured, and
nothing third party is fetched twice. Anything the workspace repository
does not have still comes from the network, as in any build.

    scripts/check-first-build-footprint.py --repo "$PWD/.m2-repo"
    scripts/check-first-build-footprint.py --repo ... --platform-modules all

Needs the workspace built and installed into --repo first: core, javase,
the Maven plugin and the app archetype.
"""

import argparse
import os
import platform
import re
import shutil
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# The ports a local build reads, and only a local build. Kept as a list of
# its own rather than derived from AppBuilder: this is the independent
# check on that table, and a port added there has to be added here by hand.
PORT_ARTIFACTS = (
    "codenameone-android",
    "codenameone-ios",
    "codenameone-mac",
    "codenameone-windows",
    "codenameone-linux",
    "codenameone-parparvm",
)

# Artifacts the build tools use at build time only, in the same position the
# ports were in: provided and optional in codenameone-maven-plugin or
# codenameone-build-engine, where a change of scope sends them to every
# developer. None is read by a simulator run of a plain project.
BUILD_ONLY_ARTIFACTS = (
    "codenameone-android-compat",
    "codenameone-swing-compat",
    "codenameone-javafx-compat",
    "cn1-hardening",
)

# The profiles of maven/cn1-binaries/pom.xml: id -> (what activates it, the
# natives it adds). Each native is added for ffmpeg and for javacpp.
HOST_PROFILES = {
    "ffmpeg-macosx": ({"family": "mac"}, {"macosx-arm64", "macosx-x86_64"}),
    "ffmpeg-windows": ({"family": "windows"}, {"windows-x86_64"}),
    "ffmpeg-linux-x86_64": ({"name": "Linux", "arch": "amd64"}, {"linux-x86_64"}),
    "ffmpeg-linux-arm64": ({"name": "Linux", "arch": "aarch64"}, {"linux-arm64"}),
}

# The profile a desktop jar is built with, because that jar runs on machines
# other than the one that built it.
ALL_PLATFORMS_PROFILE = "ffmpeg-all-platforms"
ALL_PLATFORMS_PROPERTY = "cn1.ffmpeg.allPlatforms"

# What a first build may download, in MB, counting every file in the local
# repository. Measured at 174 MB on a Mac when this gate was written, against
# 346 MB before it; a Mac takes ffmpeg for both of its architectures (see
# maven/cn1-binaries/pom.xml), 43 MB where a Linux or Windows host takes 25,
# which is the difference between the two rows. The margin is room for
# ordinary dependency updates, not for a new port or a new native: a change
# that needs this raised should say what the megabytes are and why a
# simulator run has to carry them.
BUDGET_MB = {"mac": 195, "other": 180}

# Fewer artifacts than this and the build did not really run: the report
# would be a clean bill for an empty directory.
MIN_ARTIFACTS = 150


def host_ffmpeg_platforms():
    """The bytedeco platforms this machine may be handed.

    The same table as the profiles in maven/cn1-binaries/pom.xml and
    PluginInfo.hostFfmpegPlatforms in the Gradle plugin, written out a
    third time on purpose: it is what those two are checked against.
    """
    system = platform.system().lower()
    machine = platform.machine().lower()
    if system == "darwin":
        return {"macosx-arm64", "macosx-x86_64"}
    if system == "windows":
        return {"windows-x86_64"}
    if system == "linux":
        if machine in ("x86_64", "amd64"):
            return {"linux-x86_64"}
        if machine in ("aarch64", "arm64"):
            return {"linux-arm64"}
    return set()


def check_host_profiles():
    """Holds every host's profile in cn1-binaries-javase to HOST_PROFILES."""
    path = os.path.join(ROOT, "maven", "cn1-binaries", "pom.xml")
    ns = {"m": "http://maven.apache.org/POM/4.0.0"}
    root = ET.parse(path).getroot()
    failures = []
    unconditional = {(d.findtext("m:artifactId", "", ns), d.findtext("m:classifier", "", ns))
                     for d in root.findall("m:dependencies/m:dependency", ns)
                     if d.findtext("m:groupId", "", ns) == "org.bytedeco"}
    for artifact in ("ffmpeg", "javacpp"):
        if (artifact, "") not in unconditional:
            failures.append("maven/cn1-binaries/pom.xml no longer depends on org.bytedeco:%s "
                            "unconditionally; the natives are useless without it" % artifact)
    for artifact, classifier in sorted(unconditional):
        if classifier or artifact.endswith("-platform"):
            failures.append("maven/cn1-binaries/pom.xml depends on org.bytedeco:%s%s on every host; "
                            "natives belong in the profile of the host that runs them"
                            % (artifact, ":" + classifier if classifier else ""))
    profiles = {p.findtext("m:id", "", ns): p for p in root.findall("m:profiles/m:profile", ns)}
    for name, (activation, natives) in sorted(HOST_PROFILES.items()):
        profile = profiles.get(name)
        if profile is None:
            failures.append("maven/cn1-binaries/pom.xml has no profile %s" % name)
            continue
        os_el = profile.find("m:activation/m:os", ns)
        actual = {} if os_el is None else {c.tag.split("}")[-1]: (c.text or "").strip() for c in os_el}
        if actual != activation:
            failures.append("profile %s is activated by %s, expected %s" % (name, actual or "nothing", activation))
        wanted = {(a, n) for a in ("ffmpeg", "javacpp") for n in natives}
        got = {(d.findtext("m:artifactId", "", ns), d.findtext("m:classifier", "", ns))
               for d in profile.findall("m:dependencies/m:dependency", ns)}
        if got != wanted:
            failures.append("profile %s adds %s, expected %s" % (name, sorted(got), sorted(wanted)))
    # The desktop jar's profile: every command that builds one passes the
    # property, and what it adds is every host's natives and no others.
    everything = profiles.get(ALL_PLATFORMS_PROFILE)
    if everything is None:
        failures.append("maven/cn1-binaries/pom.xml has no profile %s" % ALL_PLATFORMS_PROFILE)
    else:
        name = everything.findtext("m:activation/m:property/m:name", "", ns).strip()
        if name != ALL_PLATFORMS_PROPERTY:
            failures.append("profile %s is activated by property %s, expected %s; the launchers pass "
                            "-D%s=true" % (ALL_PLATFORMS_PROFILE, name or "nothing",
                                           ALL_PLATFORMS_PROPERTY, ALL_PLATFORMS_PROPERTY))
        if everything.findtext("m:activation/m:property/m:value", "", ns).strip() != "true":
            failures.append("profile %s must be activated by %s=true, which is what the launchers pass "
                            "and what the plugin's own check reads"
                            % (ALL_PLATFORMS_PROFILE, ALL_PLATFORMS_PROPERTY))
        wanted = {(a, n) for a in ("ffmpeg", "javacpp")
                  for _, natives in HOST_PROFILES.values() for n in natives}
        got = {(d.findtext("m:artifactId", "", ns), d.findtext("m:classifier", "", ns))
               for d in everything.findall("m:dependencies/m:dependency", ns)}
        if got != wanted:
            failures.append("profile %s adds %s, expected every host's natives: %s"
                            % (ALL_PLATFORMS_PROFILE, sorted(got), sorted(wanted)))
    return failures


def workspace_version():
    with open(os.path.join(ROOT, "maven", "pom.xml"), encoding="utf-8") as f:
        text = f.read()
    # The project's own <version>: the first one that is not inside <parent>.
    text = re.sub(r"<parent>.*?</parent>", "", text, flags=re.S)
    m = re.search(r"<version>([^<]+)</version>", text)
    if not m:
        sys.exit("could not read the version from maven/pom.xml")
    return m.group(1).strip()


def run(cmd, cwd, log):
    with open(log, "ab") as out:
        out.write(("\n$ " + " ".join(cmd) + "\n").encode())
        out.flush()
        code = subprocess.call(cmd, cwd=cwd, stdout=out, stderr=subprocess.STDOUT,
                               stdin=subprocess.DEVNULL)
    if code != 0:
        with open(log, "rb") as f:
            tail = f.read().decode("utf-8", "replace").splitlines()[-40:]
        print("\n".join(tail))
        sys.exit("FAIL: `%s` exited %d (log: %s)" % (" ".join(cmd[:3]), code, log))


def write_settings(path, repo):
    url = Path(repo).as_uri()
    block = """
        <{tag}>
          <id>workspace</id>
          <url>{url}</url>
          <releases><enabled>true</enabled><checksumPolicy>ignore</checksumPolicy></releases>
          <snapshots><enabled>true</enabled><checksumPolicy>ignore</checksumPolicy></snapshots>
        </{tag}>"""
    with open(path, "w", encoding="utf-8") as f:
        f.write("""<settings>
  <profiles>
    <profile>
      <id>workspace</id>
      <repositories>%s
      </repositories>
      <pluginRepositories>%s
      </pluginRepositories>
    </profile>
  </profiles>
  <activeProfiles><activeProfile>workspace</activeProfile></activeProfiles>
</settings>
""" % (block.format(tag="repository", url=url), block.format(tag="pluginRepository", url=url)))


def artifacts(repo):
    """Every artifact file in a local repository as (group, artifact, version, classifier, bytes)."""
    found = []
    total = 0
    for base, _dirs, files in os.walk(repo):
        for name in files:
            size = os.path.getsize(os.path.join(base, name))
            total += size
            rel = os.path.relpath(base, repo).split(os.sep)
            if len(rel) < 3:
                continue
            version, artifact, group = rel[-1], rel[-2], ".".join(rel[:-2])
            stem, ext = os.path.splitext(name)
            if ext not in (".jar", ".zip", ".aar", ".so", ".dll", ".dylib"):
                continue
            prefix = artifact + "-" + version
            if not stem.startswith(prefix):
                continue
            classifier = stem[len(prefix):].lstrip("-")
            found.append((group, artifact, version, classifier, size))
    return found, total


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--repo", required=True,
                    help="the workspace's local Maven repository, with this tree installed")
    ap.add_argument("--version", help="the Codename One version to build against (default: this tree's)")
    ap.add_argument("--platform-modules", default=None,
                    help="the archetype's platformModules (default: the archetype's own)")
    ap.add_argument("--budget-mb", type=float, help="override the budget")
    ap.add_argument("--work", help="a directory to work in, kept afterwards (default: a temporary one)")
    args = ap.parse_args()

    profile_failures = check_host_profiles()
    if profile_failures:
        for f in profile_failures:
            print("FAIL: " + f)
        return 1

    repo = os.path.abspath(args.repo)
    version = args.version or workspace_version()
    marker = os.path.join(repo, "com", "codenameone", "codenameone-maven-plugin", version)
    if not os.path.isdir(marker):
        sys.exit("FAIL: %s has no codenameone-maven-plugin %s; build and install the workspace first"
                 % (repo, version))

    work = os.path.abspath(args.work) if args.work else tempfile.mkdtemp(prefix="cn1-first-build-")
    os.makedirs(work, exist_ok=True)
    log = os.path.join(work, "first-build.log")
    fresh = os.path.join(work, "empty-local-repo")
    if os.path.exists(fresh) and os.listdir(fresh):
        sys.exit("FAIL: %s is not empty; the measurement needs a repository nothing has filled" % fresh)
    mvn = shutil.which("mvn") or shutil.which("mvn.cmd")
    if not mvn:
        sys.exit("FAIL: mvn is not on PATH")

    settings = os.path.join(work, "settings.xml")
    write_settings(settings, repo)
    # Generated from a repository of its own. Not from --repo, which is the
    # remote of this run and must stay as the workspace build left it; and not
    # from the empty one, which would count the archetype plugin as a download
    # of the first build.
    generate = [mvn, "-B", "-ntp", "-s", settings,
                "-Dmaven.repo.local=" + os.path.join(work, "generate-local-repo"), "archetype:generate",
                "-DarchetypeGroupId=com.codenameone", "-DarchetypeArtifactId=cn1app-archetype",
                "-DarchetypeVersion=" + version, "-DgroupId=com.example.firstbuild",
                "-DartifactId=firstbuild", "-Dversion=1.0-SNAPSHOT", "-DmainName=FirstBuild",
                "-Dcn1Version=" + version, "-Dcn1PluginVersion=" + version, "-DinteractiveMode=false"]
    if args.platform_modules:
        generate.append("-DplatformModules=" + args.platform_modules)
    project = os.path.join(work, "firstbuild")
    if os.path.exists(project):
        sys.exit("FAIL: %s already exists" % project)
    run(generate, work, log)

    # What run.sh runs, less the last phase: `verify` is where the simulator
    # is started. The application's whole runtime class path, which is what
    # the simulator runs from, is resolved by `package`.
    run([mvn, "-B", "-ntp", "-s", settings, "-Dmaven.repo.local=" + fresh, "package", "-Psimulator",
         "-DskipTests", "-Dcodename1.platform=javase"], project, log)

    found, total = artifacts(fresh)
    failures = []

    if len(found) < MIN_ARTIFACTS:
        failures.append("only %d artifacts arrived; the build did not resolve what a first build resolves"
                        % len(found))
    present = {(g, a) for g, a, _v, _c, _s in found}
    for needed in ("codenameone-core", "codenameone-javase", "codenameone-maven-plugin"):
        if ("com.codenameone", needed) not in present:
            failures.append("%s was not resolved, so this run measured something other than a "
                            "simulator build" % needed)

    for g, a, v, c, size in found:
        if g == "com.codenameone" and a in PORT_ARTIFACTS:
            failures.append("%s:%s:%s%s (%.1f MB) was downloaded by a simulator build. A port is "
                            "resolved by the first build of a target that reads it "
                            "(AppBuilder.portArtifacts); something depends on this one again -- "
                            "look at the dependencies of codenameone-maven-plugin and "
                            "codenameone-build-engine, where it must be provided and optional."
                            % (g, a, v, ":" + c if c else "", size / 1e6))

        if g == "com.codenameone" and a in BUILD_ONLY_ARTIFACTS:
            failures.append("%s:%s:%s (%.1f MB) was downloaded by a simulator build. It is used at "
                            "build time by the projects that need it and is provided and optional "
                            "in the build tools' poms; something made it a dependency of every project."
                            % (g, a, v, size / 1e6))

    allowed = host_ffmpeg_platforms()
    natives = [(a, c, size) for g, a, _v, c, size in found if g == "org.bytedeco" and c]
    for a, c, size in natives:
        if c not in allowed:
            failures.append("org.bytedeco:%s:%s (%.1f MB) is a native for another platform. "
                            "cn1-binaries-javase selects the host's alone (maven/cn1-binaries/pom.xml); "
                            "something reaches org.bytedeco's -platform aggregator again."
                            % (a, c, size / 1e6))
    if allowed and not any(a == "ffmpeg" for a, _c, _s in natives):
        failures.append("no ffmpeg native arrived for this host (%s). The simulator would have no "
                        "bundled ffmpeg: the host profile in maven/cn1-binaries/pom.xml did not "
                        "activate." % ", ".join(sorted(allowed)))

    budget = args.budget_mb if args.budget_mb is not None else \
        BUDGET_MB["mac" if platform.system() == "Darwin" else "other"]
    mb = total / 1e6
    if mb > budget:
        failures.append("the first build downloaded %.0f MB, over the budget of %.0f MB. See BUDGET_MB "
                        "in this script before raising it." % (mb, budget))

    by_group = {}
    for g, a, _v, _c, size in found:
        key = ".".join(g.split(".")[:2])
        by_group[key] = by_group.get(key, 0) + size
    print("First build of a new project: %.0f MB in %d artifacts (budget %.0f MB)" % (mb, len(found), budget))
    for key, size in sorted(by_group.items(), key=lambda kv: -kv[1])[:8]:
        print("  %7.1f MB  %s" % (size / 1e6, key))
    for g, a, _v, c, size in sorted(found, key=lambda t: -t[4])[:8]:
        print("  %7.1f MB  %s:%s%s" % (size / 1e6, g, a, ":" + c if c else ""))

    if failures:
        print()
        for f in failures:
            print("FAIL: " + f)
        print("\nlog: %s\nrepository: %s" % (log, fresh))
        return 1
    if not args.work:
        shutil.rmtree(work, ignore_errors=True)
    print("OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())

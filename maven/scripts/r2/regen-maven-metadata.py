#!/usr/bin/env python3
"""Rebuild every maven-metadata.xml in the Codename One R2 repository from the
bucket's own key listing, plus the group-level plugin metadata and the archetype
catalog.

Why derive from the bucket rather than from the build: a staging tree only holds
the version just built, so any metadata it produced would list exactly one
version and would clobber the accumulated history on upload. Deriving from the
listing is idempotent, self-healing (a re-run repairs a partial upload) and
gives the retention job the same code path when it *removes* versions.

Usage:
    regen-maven-metadata.py [--dry-run] [--group com/codenameone]

Required environment: R2_ACCOUNT_ID, R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY,
R2_BUCKET.
"""

import argparse
import functools
import hashlib
import json
import os
import re
import subprocess
import sys
import tempfile
import time
from datetime import datetime, timezone
from xml.sax.saxutils import escape

GROUP_PATH_DEFAULT = "com/codenameone"
PREFIX = "maven2"

# Artifacts whose maven-metadata must advertise a plugin prefix, so that
# `mvn cn1:run` works for anyone who adds com.codenameone to <pluginGroups>.
PLUGIN_PREFIXES = {"codenameone-maven-plugin": "cn1"}

ARCHETYPES = {
    "cn1app-archetype": "Codename One bare-bones application project.",
    "cn1lib-archetype": "Codename One cn1lib (library) project.",
}

REPO_URL = "https://repo.codenameone.com/maven2"

# Written by mark-release-complete.sh once every upload for a tag has succeeded.
#
# Completeness is per release, not per directory. Everything a tag publishes shares one
# version and the plugin resolves its editors at its own version, so advertising core
# 7.0.x while codenameone-gamebuilder 7.0.x is missing hands a consumer a plugin whose
# cn1:gamebuilder goal cannot resolve. Per-directory markers could not express that:
# the core directory is complete in exactly the case that matters.
#
# Skipping metadata on the run that failed is also not enough, because every later run
# rebuilds metadata from the same bucket listing and would advertise the abandoned tag.
RELEASES_PSEUDO_ARTIFACT = "_cn1-releases"
RELEASE_MARKER = "complete"


def aws_with_retry(*args, attempts=4):
    """aws() with bounded backoff.

    A metadata set is a body plus four checksum objects, and object storage has no
    multi-object atomic write: if one upload fails after another succeeded, the public
    repository briefly holds a body and checksums that disagree. That cannot be designed
    away here, so it is made unlikely and self-healing instead -- transient failures are
    retried, a failure that survives them fails the release loudly, and this script is
    idempotent so a re-run repairs the set. maven-metadata.xml is served with a 60s TTL,
    and both Maven's default checksumPolicy and the one in Codename One's generated poms
    are `warn`, so a consumer hitting the window is warned rather than broken.
    """
    delay = 2
    for attempt in range(1, attempts + 1):
        try:
            return aws(*args, capture=False)
        except subprocess.CalledProcessError:
            if attempt == attempts:
                raise
            print("    upload failed (attempt %d/%d), retrying in %ds"
                  % (attempt, attempts, delay))
            time.sleep(delay)
            delay *= 2


def aws(*args, capture=True):
    env = dict(os.environ)
    env["AWS_ACCESS_KEY_ID"] = env["R2_ACCESS_KEY_ID"]
    env["AWS_SECRET_ACCESS_KEY"] = env["R2_SECRET_ACCESS_KEY"]
    env["AWS_DEFAULT_REGION"] = "auto"
    # R2 rejects the CRC checksums recent aws-cli v2 sends by default.
    env["AWS_REQUEST_CHECKSUM_CALCULATION"] = "when_required"
    env["AWS_RESPONSE_CHECKSUM_VALIDATION"] = "when_required"
    endpoint = "https://%s.r2.cloudflarestorage.com" % env["R2_ACCOUNT_ID"]
    cmd = ["aws"] + list(args) + ["--endpoint-url", endpoint]
    return subprocess.run(cmd, env=env, check=True,
                          stdout=subprocess.PIPE if capture else None,
                          text=True).stdout


def list_keys(bucket, group_path):
    """All object keys under maven2/<group_path>/."""
    keys, token = [], None
    while True:
        args = ["s3api", "list-objects-v2", "--bucket", bucket,
                "--prefix", "%s/%s/" % (PREFIX, group_path),
                "--max-items", "1000", "--output", "json"]
        if token:
            args += ["--starting-token", token]
        payload = json.loads(aws(*args) or "{}")
        keys += [o["Key"] for o in payload.get("Contents", [])]
        token = payload.get("NextToken")
        if not token:
            return keys


@functools.total_ordering
class ComparableVersion:
    """Maven-ish version ordering: numeric segments compare numerically, so
    7.0.9 < 7.0.10 rather than sorting lexically."""

    _TOKEN = re.compile(r"(\d+|[A-Za-z]+)")

    def __init__(self, text):
        self.text = text
        self.parts = [int(p) if p.isdigit() else p
                      for p in self._TOKEN.findall(text)]

    def _key(self):
        # Ints sort before strings within a position; tag each part so
        # comparison never crosses types.
        return [(0, p, "") if isinstance(p, int) else (1, 0, p)
                for p in self.parts]

    def __lt__(self, other):
        return self._key() < other._key()

    def __eq__(self, other):
        return self._key() == other._key()


def discover(keys, group_path):
    """key list -> {artifactId: sorted [versions]}, for releases marked complete."""
    released = set()
    found = {}
    base = "%s/%s/" % (PREFIX, group_path)
    for key in keys:
        rest = key[len(base):]
        bits = rest.split("/")
        if len(bits) < 3:
            continue  # group-level file, not <artifact>/<version>/<file>
        artifact, version, name = bits[0], bits[1], bits[-1]
        if artifact == RELEASES_PSEUDO_ARTIFACT:
            if name == RELEASE_MARKER:
                released.add(version)
            continue
        if version.endswith("-SNAPSHOT"):
            continue
        found.setdefault(artifact, set()).add(version)

    incomplete = sorted({v for versions in found.values() for v in versions} - released)
    for version in incomplete:
        print("    skipping %s -- release not marked complete, so some artifact for it "
              "never finished uploading" % version)

    return {a: sorted(v & released, key=ComparableVersion)
            for a, v in found.items() if v & released}


def metadata_xml(group_id, artifact_id, versions, stamp):
    latest = versions[-1]
    lines = [
        '<?xml version="1.0" encoding="UTF-8"?>',
        "<metadata>",
        "  <groupId>%s</groupId>" % escape(group_id),
        "  <artifactId>%s</artifactId>" % escape(artifact_id),
        "  <versioning>",
        # Both are emitted deliberately: UpdateCodenameOneMojo reads <latest>,
        # scripts/initializr/update-cn1-version.sh prefers <release>.
        "    <latest>%s</latest>" % escape(latest),
        "    <release>%s</release>" % escape(latest),
        "    <versions>",
    ]
    lines += ["      <version>%s</version>" % escape(v) for v in versions]
    lines += [
        "    </versions>",
        "    <lastUpdated>%s</lastUpdated>" % stamp,
        "  </versioning>",
        "</metadata>",
        "",
    ]
    return "\n".join(lines)


def group_metadata_xml(plugins):
    lines = ['<?xml version="1.0" encoding="UTF-8"?>', "<metadata>", "  <plugins>"]
    for artifact_id, prefix in sorted(plugins.items()):
        lines += [
            "    <plugin>",
            "      <name>%s</name>" % escape(artifact_id),
            "      <prefix>%s</prefix>" % escape(prefix),
            "      <artifactId>%s</artifactId>" % escape(artifact_id),
            "    </plugin>",
        ]
    lines += ["  </plugins>", "</metadata>", ""]
    return "\n".join(lines)


def archetype_catalog_xml(group_id, artifacts):
    """`mvn archetype:generate -DarchetypeCatalog=remote` is the only supported
    way to reach an archetype that is not on Central: -DarchetypeRepository was
    removed, and a URL is rejected. The resolver looks for the repository with
    id `archetype` (falling back to `central`) and fetches
    <repoUrl>/archetype-catalog.xml."""
    namespace = ("http://maven.apache.org/plugins/maven-archetype-plugin"
                 "/archetype-catalog/1.0.0")
    lines = ['<?xml version="1.0" encoding="UTF-8"?>',
             '<archetype-catalog xmlns="%s">' % namespace,
             "  <archetypes>"]
    for artifact_id, description in sorted(ARCHETYPES.items()):
        versions = artifacts.get(artifact_id)
        if not versions:
            continue
        lines += [
            "    <archetype>",
            "      <groupId>%s</groupId>" % escape(group_id),
            "      <artifactId>%s</artifactId>" % escape(artifact_id),
            "      <version>%s</version>" % escape(versions[-1]),
            "      <repository>%s</repository>" % escape(REPO_URL),
            "      <description>%s</description>" % escape(description),
            "    </archetype>",
        ]
    lines += ["  </archetypes>", "</archetype-catalog>", ""]
    return "\n".join(lines)


def put(bucket, key, body, dry_run, cache_seconds=60):
    digests = {
        "md5": hashlib.md5(body.encode()).hexdigest(),
        "sha1": hashlib.sha1(body.encode()).hexdigest(),
        "sha256": hashlib.sha256(body.encode()).hexdigest(),
        "sha512": hashlib.sha512(body.encode()).hexdigest(),
    }
    if dry_run:
        print("    would write %s (%d bytes) + 4 checksums" % (key, len(body)))
        return
    cache = "public, max-age=%d, must-revalidate" % cache_seconds
    with tempfile.TemporaryDirectory() as tmp:
        # Checksums first, body last: if the run dies mid-set the body is still the
        # previous one, so the metadata that is served remains parseable.
        for name, content, ctype in [
            (os.path.basename(key) + "." + ext, digest, "text/plain")
            for ext, digest in digests.items()
        ] + [(os.path.basename(key), body, "text/xml")]:
            path = os.path.join(tmp, name)
            with open(path, "w") as handle:
                handle.write(content)
            aws_with_retry("s3", "cp", path,
                           "s3://%s/%s" % (bucket, os.path.dirname(key) + "/" + name),
                           "--cache-control", cache, "--content-type", ctype,
                           "--only-show-errors")
    print("    wrote %s + 4 checksums" % key)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--group", default=GROUP_PATH_DEFAULT,
                        help="group path, e.g. com/codenameone")
    args = parser.parse_args()

    for var in ("R2_ACCOUNT_ID", "R2_ACCESS_KEY_ID", "R2_SECRET_ACCESS_KEY", "R2_BUCKET"):
        if not os.environ.get(var):
            sys.exit("%s is required" % var)

    bucket = os.environ["R2_BUCKET"]
    group_path = args.group.strip("/")
    group_id = group_path.replace("/", ".")

    print("==> listing s3://%s/%s/%s/" % (bucket, PREFIX, group_path))
    keys = list_keys(bucket, group_path)
    artifacts = discover(keys, group_path)
    if not artifacts:
        sys.exit("no release artifacts found under %s -- refusing to write "
                 "metadata that would erase the repository's history" % group_path)
    print("    %d objects, %d artifacts" % (len(keys), len(artifacts)))

    stamp = datetime.now(timezone.utc).strftime("%Y%m%d%H%M%S")

    for artifact_id, versions in sorted(artifacts.items()):
        print("  %s (%d versions, latest %s)" % (artifact_id, len(versions), versions[-1]))
        put(bucket,
            "%s/%s/%s/maven-metadata.xml" % (PREFIX, group_path, artifact_id),
            metadata_xml(group_id, artifact_id, versions, stamp),
            args.dry_run)

    plugins = {a: p for a, p in PLUGIN_PREFIXES.items() if a in artifacts}
    if plugins:
        print("  group-level plugin metadata: %s" % ", ".join(sorted(plugins)))
        put(bucket, "%s/%s/maven-metadata.xml" % (PREFIX, group_path),
            group_metadata_xml(plugins), args.dry_run)

    if any(a in artifacts for a in ARCHETYPES):
        print("  archetype-catalog.xml")
        put(bucket, "%s/archetype-catalog.xml" % PREFIX,
            archetype_catalog_xml(group_id, artifacts), args.dry_run)

    print("==> done%s" % (" (dry run)" if args.dry_run else ""))


if __name__ == "__main__":
    main()

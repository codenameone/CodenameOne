# Codename One R2 Maven repository

Scripts that publish Codename One releases to the Cloudflare R2 bucket served at
<https://repo.codenameone.com/maven2>.

Background: Sonatype rate-limits Maven Central consumption for commercial open-source
projects, which makes Central a product availability risk rather than just a CI one.

**The migration is complete.** Dual publication ran from 7.0.264 to 7.0.267 and this is
now the only repository new releases go to. Central keeps every version published up to
the cutover -- nothing is removed from it, and a project pinned to one of those versions
resolves from Central exactly as before -- but it receives no new ones. Generated
projects have declared this repository since the archetype and Initializr change, in
both `<repositories>` and `<pluginRepositories>`.

## Layout

```
maven2/com/codenameone/<artifactId>/<version>/...   releases
maven2/com/codenameone/<artifactId>/maven-metadata.xml
maven2/com/codenameone/maven-metadata.xml           plugin-prefix metadata (cn1)
maven2/archetype-catalog.xml                        for archetype:generate
```

## Scripts

### `publish-staging-to-r2.sh <staging-dir>...`

Uploads a `central-staging` tree produced by `central-publishing-maven-plugin`. That plugin
stages every module's artifacts in standard Maven layout, with MD5/SHA-1/SHA-256/SHA-512
checksums and `.asc` signatures, as a step before its upload — so running it with
`-DskipPublishing=true` yields exactly the tree R2 needs, from the build that produced the
artifacts, with no second Maven pass that could emit different bytes. The directory name
is the plugin's; it is not a statement about where the tree goes.

Where the tree lands:

| Build | Path |
|---|---|
| `maven/` reactor | `maven/target/central-staging` |
| out-of-reactor editor | `scripts/<editor>/target/central-staging` |
| **SNAPSHOT builds** | `target/central-deferred` (different directory, and it contains `maven-metadata-central-staging.xml` files) |

### `seed-frozen-artifacts.sh [<artifactId>:<version> ...]`

Copies the frozen artifacts from Maven Central into R2, once. Some artifacts stopped
being published because their content does not change per release (`<excludeArtifacts>`
in `maven/pom.xml`), and consumers are pinned to the last version that *was* published —
which lives only on Central. That is not history that can be left behind:
`codenameone-maven-plugin` resolves `codenameone-designer` at `cn1.designer.version` for
the Resource Editor goals, and declares `cn1-builder-resources-common` and
`cn1-builder-resources-android` at `7.0` in `runtime` scope, so ordinary plugin
resolution reaches all of them. They are live dependencies of every future release, not
history that can be left behind.

Coordinates come from `frozen-coordinates.py`, which **derives** them, so the seeding and
the release-time check cannot disagree and a newly pinned artifact is picked up without
anyone remembering to add it. It verifies every file against its `.sha1` from Central
before uploading, and is idempotent. Every file the coordinate has on Central is copied,
not a chosen subset -- the designer is consumed as its `jar-with-dependencies`
attachment, and guessing which attachments matter is how the pom gets seeded and the
artifact does not.

Run it through the **Seed frozen artifacts to R2** workflow (`workflow_dispatch`) after
bumping a pin, and before pushing the next release tag. It is manual on purpose: it reads
from Central, and a release must not depend on Central being reachable at that moment.

### `frozen-coordinates.py`

Prints `artifactId:version[:classifier,...]` for each frozen artifact. Derived, never
hand-maintained, and that is load-bearing: the list was hand-written for exactly one
commit and was already wrong twice. It named `sqlite-jdbc`, which is an ordinary reactor
module published on every tag, and it omitted `cn1-builder-resources-common` and
`cn1-builder-resources-android` -- `runtime` dependencies of `codenameone-maven-plugin`
pinned at `7.0` and published nowhere but Central. That second omission would have broken
the plugin's own runtime classpath for every R2-only project: every build, not one goal.

An artifact is frozen when a release resolves it at a version that is not the release's
own. Two ways that happens, both read from where the value actually lives:

1. A `com.codenameone` dependency with a **literal** version in any reactor pom.
   `${project.version}` and friends move with the release and are published by it, so
   only a hardcoded version marks a pin. `system` scope is excluded -- Maven resolves it
   from `systemPath` on disk and never asks a repository, which is what `jfxrt` and
   `codenameone-buildclient` are. `test` scope never reaches a user.
2. A version the plugin resolves programmatically, which no pom declares:
   `cn1.designer.version` on `AbstractCN1Mojo`. `codenameone-javase-svg` shares it --
   it exists only to give that editor Batik SVG support.

Classifiers are scraped from the same `getArtifact` calls that name them.

Seeded artifacts deliberately carry no release marker: they are resolved by exact pinned
version rather than discovered, so `regen-maven-metadata.py` knows them as frozen and
generates their metadata from what is present.

### `check-frozen-artifacts.sh [<artifactId>:<version> ...]`

Fails while any frozen artifact is absent from R2. The release workflow runs it *before*
it builds anything.

Up to the cutover a missing frozen artifact was invisible: every consumer still had
Central in its resolution path, so `codenameone-designer` resolved from there whatever R2
held. With R2 as the only repository a generated project declares, the same absence means
`cn1:design` cannot resolve -- and by the time anyone runs the goal, the release that
shipped it is immutable. Hence a gate at the one moment nothing has been published yet.

Reads the public URL, so it needs no credentials.

### `mark-release-complete.sh <version>`

Marks a tag fully published. Run it only once every upload for that tag has succeeded;
see the completeness rule below for why this is per release rather than per directory.

### `regen-maven-metadata.py [--dry-run]`

Rebuilds every `maven-metadata.xml` from the bucket's own key listing, plus the group-level
plugin-prefix metadata and `archetype-catalog.xml`.

Derived from the bucket, never from the build: a staging tree only holds the version just
built, so metadata generated from it would list exactly one version and clobber the
accumulated history. Deriving from the listing is idempotent, repairs partial uploads on
re-run, and gives the retention job the same code path when it *removes* versions.

Its set of frozen artifacts — the ones exempt from the release-marker requirement, because
they are seeded rather than released — comes from `frozen-coordinates.py` too. The
hardcoded set this replaced was wrong in both directions: it listed `sqlite-jdbc`, an
ordinary reactor module that therefore had its *incomplete* releases advertised, and it
omitted `cn1-builder-resources-{common,android}`, which would have been seeded and then
withheld, printing a "release not marked complete" line about their pinned version on
every future run forever.

Run it after every `publish-staging-to-r2.sh`.

## Rules

**Never `aws s3 sync`.** `sync --delete` would erase every previous version, because a
staging tree only holds the release being built. The upload script uses `aws s3 cp
--recursive`, which cannot delete. Do not "optimise" this.

**Released versions are immutable.** The upload script refuses to replace an existing object
whose bytes differ, and exits non-zero. Re-cut under a new version rather than setting
`R2_ALLOW_OVERWRITE=1`; R2, unlike Central, will happily accept a silent overwrite.

**Never generate `maven-metadata.xml` in a build.** The upload script deletes any it finds
in the staging tree. Only `regen-maven-metadata.py` writes it.

**A release is only real once it is marked complete, and completeness is per release,
not per directory.** Everything a tag publishes shares one version, and the plugin
resolves its editors at its own version — so advertising `codenameone-core:7.0.x` while
`codenameone-gamebuilder:7.0.x` is missing hands a consumer a plugin whose
`cn1:gamebuilder` goal cannot resolve. A per-directory marker cannot express that: the
core directory is complete in exactly the case that matters.

`mark-release-complete.sh` writes `com/codenameone/_cn1-releases/<version>/complete` only
once the core reactor *and* all four editors are up, and `regen-maven-metadata.py` will
not advertise any artifact at a version lacking it. That holds on every future run too,
which matters because each run rebuilds metadata from the same bucket listing — gating
only the run that failed would let the next tag advertise the abandoned one. Re-running
the release uploads what is missing, marks it, and it then appears normally.

**A requested staging tree that is missing or empty is a failure, not a no-op.** Skipping
it would report success for a component whose build never produced anything, let the
workflow's object check pass against artifacts left by an earlier partial upload, and let
the release be marked complete without that component.

**Metadata checksum sets cannot be written atomically.** A set is a body plus four
checksum objects, and object storage has no multi-object atomic write, so a failure
part-way leaves a body and checksums that disagree. That is mitigated rather than solved:
uploads retry with backoff, checksums are written before the body so an interrupted run
still serves the previous parseable metadata, `regen-maven-metadata.py` is idempotent so a
re-run repairs the set, and the 60s TTL means the window is short. Both Maven's default
`checksumPolicy` and the one in Codename One's generated poms are `warn`, so a consumer
hitting the window is warned rather than broken.

**Metadata is written last, on purpose.** It is what makes a version discoverable, so a
failed release leaves its artifacts orphaned but invisible — the recoverable direction.
Advertised-but-incomplete cannot be undone.

**A build that failed does not get marked complete.** While Central was the authority a
failed deploy was ambiguous -- `central-publishing-maven-plugin` reports failure for
bundles it accepted -- so the Central confirmation poll, not the deploy, was the verdict.
With `skipPublishing` there is no upload to misreport, so a failed build is simply a
failed build, and its staging tree may be missing whichever modules came after the
failure. The release marker therefore requires every build *and* every upload to have
succeeded. The uploads still run after a failed build, deliberately: nothing is
discoverable without the marker, so uploading collects the whole diagnosis in one run.

**Known limitation: tag bursts can lose a release.** The workflow serialises on a single
`concurrency` group so the metadata read-modify-write cannot interleave. GitHub allows one
running and one pending member per group, and a third queued run *replaces* the pending one
even with `cancel-in-progress: false` — so if three tags are pushed while a release is
running, the middle tag is silently cancelled and never published. Push release tags one at
a time, and treat the presence of `_cn1-releases/<version>/complete` as the check that a tag
actually published. A proper fix is external FIFO dispatch, or a reconciliation job that
compares git tags against release markers; neither is in this change.

**Cloudflare caches 404s.** A Cache Rule sets Status Code TTL 400-599 to no-store, because
otherwise a probe for a not-yet-published artifact caches the negative and the artifact stays
invisible until it expires. CI polls also append a cache-busting query parameter, so a
release cannot be blocked even if that rule is removed.

**Browser Integrity Check must not apply to `/maven2/*`.** BIC is a heuristic on the
request's browser signature, and a package repository has no browsers in front of it. It
currently rejects `Python-urllib/*` and -- the one that matters -- the JDK's default
`Java/1.8.0_x` agent, with HTTP 403 and Cloudflare error 1010. `Java/11` and later pass,
as do Maven, Gradle, sbt, Ivy, Coursier, Artifactory, Nexus, curl, wget and okhttp, all
verified against the live domain.

Two reasons this is not "fine because Maven works":

1. The rule set is Cloudflare's, not ours, and it changes without notice. This domain is
   now the only source of every Codename One release, so a signature Cloudflare decides
   to distrust breaks builds globally with a 403 nobody can attribute.
2. Anything of ours that reaches the repository over plain `HttpURLConnection` inherits
   the JDK agent and fails on JDK 8. `UpdateCodenameOneMojo.readLatestVersion` did
   exactly this, and the failure was silent: it falls back to Maven Central, which no
   longer receives releases, so a JDK 8 user was told the last pre-cutover version was
   the newest one, indefinitely. It now sends an explicit `User-Agent`, as
   `ToolingHelpClient` already had to for another Cloudflare-fronted endpoint.

The code fix stands on its own, but it only covers our own callers. Turning BIC off for
this path is the part that covers everyone else's toolchain, and it cannot be done from
this repository -- the R2 API token is deliberately scoped to object read/write on the
bucket and cannot touch zone settings.

## Configuration

GitHub secrets — note these are deliberately *not* named `CLOUDFLARE_*`, because
`CLOUDFLARE_TOKEN` and `CLOUDFLARE_ACCOUNT_ID` already exist and belong to the Cloudflare
Pages website deploy. Overwriting them breaks the site build.

| Secret | Contents |
|---|---|
| `R2_ACCOUNT_ID` | Cloudflare account id; forms the S3 endpoint |
| `R2_ACCESS_KEY_ID` | R2 API token access key, scoped to this bucket |
| `R2_SECRET_ACCESS_KEY` | R2 API token secret |

| Variable | Value |
|---|---|
| `R2_BUCKET` | `cn1-maven` |

The `MAVEN_CENTRAL_USERNAME` / `MAVEN_CENTRAL_PASSWORD` secrets and the
`CN1_DUAL_PUBLISH` variable are no longer read by any workflow, and the variable can be
deleted from the repository settings. `MAVEN_GPG_PRIVATE_KEY` and `MAVEN_GPG_PASSPHRASE` still are: signatures are
verified against the artifact wherever it is served from, so releases are still signed.

`central-publishing-maven-plugin` is still what builds a release, which is not a leftover
-- it is what stages a complete Maven layout with all four checksums and `.asc`
signatures, which is exactly the tree R2 needs, with no second Maven pass that could
produce different bytes. Every build passes `-DskipPublishing=true`, so it stages and
never uploads. It does still require a `central` server entry in `settings.xml` or it
NPEs before staging, which is why the workflow's `setup-java` steps keep `server-id:
central` with no credentials attached.

The R2 token is scoped to Object Read & Write on `cn1-maven` only, so it deliberately cannot
list buckets or read zones through the Cloudflare REST API. That is correct least privilege,
not a misconfiguration.

## Running locally

```bash
export R2_ACCOUNT_ID=... R2_ACCESS_KEY_ID=... R2_SECRET_ACCESS_KEY=... R2_BUCKET=cn1-maven

# Produce a staging tree without uploading to Central. A release version needs a `central`
# server in settings.xml even with skipPublishing, or the plugin NPEs before it stages.
mvn -s /tmp/central-dummy-settings.xml -DskipPublishing=true deploy

bash maven/scripts/r2/publish-staging-to-r2.sh maven/target/central-staging
# ...repeat for each scripts/<editor>/target/central-staging, then:
bash maven/scripts/r2/mark-release-complete.sh <version>
python3 maven/scripts/r2/regen-maven-metadata.py
```

The order matters: nothing uploaded is discoverable until the release is marked, and
`regen-maven-metadata.py` silently ignores an unmarked version rather than failing.

`aws` CLI is used rather than rclone: it is preinstalled on GitHub runners, and `aws s3 cp`
cannot delete. R2 needs `AWS_REQUEST_CHECKSUM_CALCULATION=when_required` with recent
aws-cli v2 or uploads fail on checksum negotiation; the scripts set this themselves.

# Codename One R2 Maven repository

Scripts that publish Codename One releases to the Cloudflare R2 bucket served at
<https://repo.codenameone.com/maven2>.

Background: Sonatype rate-limits Maven Central consumption for commercial open-source
projects, which makes Central a product availability risk rather than just a CI one. The
migration dual-publishes to Central and R2 for three releases, then cuts over.

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
checksums and `.asc` signatures, *before* it uploads to Central — so the tree exists even
when the Central upload fails, and needs no second Maven pass.

Where the tree lands:

| Build | Path |
|---|---|
| `maven/` reactor | `maven/target/central-staging` |
| out-of-reactor editor | `scripts/<editor>/target/central-staging` |
| **SNAPSHOT builds** | `target/central-deferred` (different directory, and it contains `maven-metadata-central-staging.xml` files) |

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
once the core reactor *and* all three editors are up, and `regen-maven-metadata.py` will
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

**Do not publish an editor to Central when the core release did not get there.** The
editors declare core/plugin at their own version, and Central releases are immutable, so
publishing one against a core that is absent leaves a permanently unresolvable artifact.
The release workflow passes `-DskipPublishing` in that case; staging still happens, so the
R2 upload is unaffected.

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
| `CN1_DUAL_PUBLISH` | `true` while still publishing to Central as well |

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

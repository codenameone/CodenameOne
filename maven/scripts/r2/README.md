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
python3 maven/scripts/r2/regen-maven-metadata.py
```

`aws` CLI is used rather than rclone: it is preinstalled on GitHub runners, and `aws s3 cp`
cannot delete. R2 needs `AWS_REQUEST_CHECKSUM_CALCULATION=when_required` with recent
aws-cli v2 or uploads fail on checksum negotiation; the scripts set this themselves.

---
title: "Why Codename One Is Moving Beyond Maven Central"
slug: maven-central-cloudflare-r2
url: /blog/maven-central-cloudflare-r2/
date: '2026-08-04'
author: Shai Almog
description: "Codename One is starting a staged move from Maven Central to a free Cloudflare R2 repository, with dual publishing, a smaller release payload, and an explicit cutover plan."
feed_html: '<img src="https://www.codenameone.com/blog/maven-central-cloudflare-r2.jpg" alt="Codename One Maven artifacts move through a staged Central and Cloudflare R2 migration" /> Codename One is starting a staged move from Maven Central to a free Cloudflare R2 repository, with dual publishing, a smaller release payload, and an explicit cutover plan.'
series: ["release-2026-07-31"]
---

![Codename One Maven artifacts move through a staged Central and Cloudflare R2 migration](/blog/maven-central-cloudflare-r2.jpg)

Codename One is starting a staged move from Maven Central to a repository we operate on Cloudflare R2.

This is not a story about Maven Central being bad. Sonatype runs expensive public infrastructure and has every right to define usage limits or sell a commercial service. Our release shape is simply a bad fit for those limits, and passing that infrastructure bill to Codename One users would make less sense than serving the same signed Maven layout ourselves.

[PR #5497](https://github.com/codenameone/CodenameOne/pull/5497) implements phase one. It reduces what each release publishes and adds dual publication to R2. Maven Central remains authoritative during this phase.

We first talked about the repository move in [Friday's release post](/blog/push-v3-new-cloud/).

## Our dashboard is not a near miss

![Maven Central publishing usage shows Codename One far beyond the soft guidelines](/blog/maven-central-cloudflare-r2/maven-central-limit.png)

The current Central Publishing usage dashboard reports:

| Measure | Codename One | Dashboard guideline | Reported use |
| --- | ---: | ---: | ---: |
| Storage | 2.12 GB | 80 MB | 2,652% |
| Files | 19,962 | 1,000 | 1,996% |
| Releases | 27 | 7 | 386% |

That screenshot also explains Sonatype's position. It says the guidelines are soft, describes adjustments for qualifying open-source projects, and offers a commercial Publisher Pro route. We are an unusually heavy publisher, not an innocent bystander being charged for one small JAR.

The question is who should carry the cost. In [Open Source Bait and Switch](https://debugagent.com/open-source-bait-and-switch), I argued that monetization pressure often lands on small open-source vendors while the largest companies capture much of the value. Maven hosting is a different product and Sonatype is not changing our license. The same asymmetry is still relevant: charging a small framework company does not necessarily collect from the enterprise organizations receiving the largest downstream benefit.

We can provide package hosting free to Codename One developers on infrastructure that matches our release process. That is the more sustainable answer for us.

## The migration has three visible dates

| Date | Phase | User impact |
| --- | --- | --- |
| July 31, 2026 | Reduce release payload and start dual publishing | None. Central remains authoritative. |
| August 7, 2026 | Generated projects and Initializr add the Codename One repository | New projects receive the repository automatically. Existing projects can add it manually. |
| August 28, 2026 | Stop publishing new Codename One versions to Central, if the dual-publish period is clean | Existing projects need the Codename One repository to discover future versions. |

Dates are more useful than “next week” in a migration document. If validation changes the schedule, we will update this post before changing the source of new releases.

{{< mermaid >}}
flowchart LR
    A["Phase 1<br/>Shrink and dual publish"] --> B["Phase 2<br/>Generated POMs use R2"]
    B --> C["Three-week observation window"]
    C --> D["New releases on R2"]
    A --> E["Maven Central remains authoritative"]
    B --> E
    E --> F["Existing Central versions remain available"]
{{< /mermaid >}}

## The POM change

Existing projects do not need to change during phase one. To prepare a project for new releases after the cutover, add both a repository and a plugin repository:

```xml
<repositories>
    <repository>
        <id>codenameone</id>
        <url>https://repo.codenameone.com/maven2</url>
        <releases>
            <enabled>true</enabled>
        </releases>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
    </repository>
</repositories>

<pluginRepositories>
    <pluginRepository>
        <id>codenameone-plugins</id>
        <url>https://repo.codenameone.com/maven2</url>
        <releases>
            <enabled>true</enabled>
        </releases>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
    </pluginRepository>
</pluginRepositories>
```

The plugin repository is not redundant. Maven resolves build plugins and ordinary dependencies through separate repository lists. A project with only `<repositories>` can download a runtime artifact and still fail to discover a future `codenameone-maven-plugin`.

Next week's Initializr and archetype change will generate this configuration. Existing projects can add it at any time; the repository uses standard Maven layout and does not change dependency coordinates.

## We were publishing the same bytes repeatedly

The real Codename One 7.0.258 release published 44 artifacts and 127 JARs totaling 229.5 MB. The core JAR itself is only 4.49 MB. Most of the weight came from shaded fat JARs that republished the same third-party libraries each week.

Phase one reduces the measured release payload to 76.9 MB, a 66 percent reduction:

| | Per release | Versions in 8 GB | Approximate history at 65 versions per year |
| --- | ---: | ---: | ---: |
| Before | 229.5 MB | 35 | 6 months |
| After | 76.9 MB | 106 | 1.6 years |

The changes are deliberately targeted:

- Five unused `jar-with-dependencies` attachments stop being published. Builder bundles still contain what they need.
- Stable `designer`, `javase-svg`, and `sqlite-jdbc` artifacts are frozen at pinned versions instead of being copied into every release.
- The headless CSS command moves out of the 43.5 MB Resource Editor fat JAR.
- A new `css-cli` module carries that command in a 28 KB JAR.

The CSS compiler itself had already been separated. Its command-line driver had not. Moving that driver avoids launching the complete Swing editor dependency graph for a headless build.

This optimization would not get us under the dashboard guidelines. It is still worth doing. It reduces upload time, metadata churn, retention pressure, and the number of bytes a failed release has to retry.

## R2 is static hosting, which is what Maven needs

A release repository does not need a database-driven artifact server in the request path. Maven needs immutable files in a known directory layout, metadata, checksums, and signatures.

The release pipeline uploads:

```text
maven2/com/codenameone/<artifactId>/<version>/...
maven2/com/codenameone/<artifactId>/maven-metadata.xml
maven2/com/codenameone/maven-metadata.xml
maven2/archetype-catalog.xml
```

The R2 publication reuses the tree already staged for Central. It includes MD5, SHA-1, SHA-256, SHA-512, and `.asc` signatures. There is no second Maven build that could produce different bytes.

Cloudflare's edge cache and object storage should reduce download latency and remove Central throttling from our release path. We also expect releases and CI to become faster and more stable because publication no longer depends on a second service accepting our volume at that moment. Those are expectations, not measurements yet. We will compare publish time, artifact resolution time, cache behavior, and failure rate during dual publication.

## Static hosting still needs release semantics

Object storage will happily accept a partial or overwritten release unless the publishing scripts stop it. The phase-one tooling adds those rules:

1. Released bytes are immutable. An upload fails if the same path already contains different content.
2. A release is invisible until every core and editor artifact is present.
3. A per-release completion marker is written only after all uploads succeed.
4. `maven-metadata.xml` is rebuilt from the bucket listing, never from the one-version staging tree.
5. Metadata advertises only versions with a completion marker.
6. Upload uses a copy operation that cannot delete older versions.
7. Frozen dependencies are seeded from Central and verified against their checksums.

{{< mermaid >}}
sequenceDiagram
    participant Build as Release build
    participant Stage as Central staging tree
    participant R2 as R2 bucket
    participant Meta as Metadata generator
    Build->>Stage: Produce signed artifacts and checksums
    Stage->>R2: Copy without delete
    R2->>R2: Reject different bytes at an existing path
    Build->>R2: Mark complete after every component succeeds
    Meta->>R2: List complete versions
    Meta->>R2: Write metadata last
    Note over R2: Partial releases remain undiscoverable
{{< /mermaid >}}

Cloudflare can cache a 404 just before a new artifact appears. The runbook therefore requires a cache rule that does not store 4xx and 5xx responses. Release polling also uses a cache-busting query so a stale negative cannot turn a successful upload into a false release failure.

## Retention: the guarantee and the likely window

We will guarantee at least six months of historical Codename One versions in the new repository. The optimized payload currently fits roughly 106 releases in the planned capacity, which is about 1.6 years at the recent pace.

The guarantee is shorter than the estimate because release contents and frequency can change. We would rather guarantee a window we can defend and retain more in practice than publish a long promise that quietly fails.

Freezing stable packages also means “history” is no longer one full copy of every byte per weekly tag. A future version can depend on a pinned stable artifact while the frequently changing core keeps its own version cadence.

Versions already on Maven Central are not deleted by this move. The retention policy applies to the repository we operate and to future availability, not to removing the public history that Central already stores.

## What can still go wrong

The red-team list is part of the design:

- **A partial upload is advertised.** Completion markers and metadata-last publication prevent it.
- **A retry changes released bytes.** The overwrite guard fails the release.
- **A stale cached 404 hides a new file.** The cache rule and cache-busted poll address it.
- **A project adds only the dependency repository.** Generated POMs include `<pluginRepositories>` too.
- **A frozen artifact remains available only on Central.** The seed step copies and verifies every pinned dependency before the final cutover.
- **R2 is slower in a real region.** The observation window measures this before Central publication stops.
- **Three release tags arrive while one is running.** GitHub Actions can replace a pending member of a concurrency group. The current runbook requires pushing release tags one at a time until dispatch is made durable.

The last item is not polished away. The repository removes one availability dependency, but it does not turn GitHub Actions into a release queue.

## Why this should be mostly seamless

Coordinates, version numbers, signatures, checksums, and normal Maven resolution remain unchanged. New projects receive the repository automatically next week. Existing projects need one POM block before they request a post-cutover version.

That is the whole user-facing migration. The work underneath it is larger because package hosting is part of the product supply chain. If we do this well, developers will notice faster and more reliable resolution, not a new thing they have to learn.

---

## Discussion

_If your CI has repository mirrors, allowlists, or strict checksum policies, please test the new endpoint during dual publication and tell us what fails._

{{< giscus >}}

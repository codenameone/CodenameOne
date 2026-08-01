---
title: "Why Codename One is moving beyond Maven Central"
slug: 2026-08-04-1200-shai-maven-central-cloudflare-r2
platform: linkedin
account: shai
source_slug: maven-central-cloudflare-r2
publish_at: '2026-08-04T12:00:00'
timezone: Asia/Jerusalem
image: /blog/maven-central-cloudflare-r2.jpg
---

Codename One is starting a staged move from Maven Central to a repository we operate on Cloudflare R2.

I do not think Maven Central is doing something wrong by setting commercial usage limits. We are simply a terrible fit for them.

Our dashboard reports 2.12 GB against an 80 MB guideline, 19,962 files against 1,000, and 27 releases against 7.

The first thing we found was our own waste. A real release publishes 229.5 MB. After removing unused fat JAR attachments, freezing stable packages, and extracting a 28 KB CSS command from the 43.5 MB Resource Editor JAR, that falls to 76.9 MB.

This week we start dual publishing. Next week new projects will include the Codename One repository. Three weeks later, if the observation period is clean, new versions will stop going to Central.

Coordinates and normal Maven resolution stay the same. Existing projects will need a small `<repositories>` and `<pluginRepositories>` addition for future updates.

The article includes the POM block, exact dates, retention guarantee, release integrity design, known risks, and why this is not an attack on Sonatype.

{{canonical}}

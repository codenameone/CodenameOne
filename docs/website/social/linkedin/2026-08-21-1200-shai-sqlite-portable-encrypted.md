---
title: "SQLite now behaves the same across Codename One ports"
slug: 2026-08-21-1200-shai-sqlite-portable-encrypted
platform: linkedin
account: shai
source_slug: sqlite-portable-encrypted
publish_at: '2026-08-21T12:00:00'
timezone: Asia/Jerusalem
image: /blog/sqlite-portable-encrypted.jpg
---

Codename One originally delegated SQLite calls to the database supplied by each operating system. We knew the result was less portable than the rest of the framework, but owning SQLite on every target looked like a deep rabbit hole. In 2018 we documented a pluggable SpatiaLite library as one escape hatch.

Windows and Linux returned `null` when an application opened a database. JavaScript depended on WebSQL, which Chrome removed and Firefox never implemented. iOS and the simulator disagreed on cursor positions, blobs, scripts, and transaction behavior.

Database encryption forced us to solve the underlying differences instead of adding another port-specific path.

We fixed the contract first. Seven device tests now run lifecycle, statement, cursor, transaction, encryption, and compatibility behavior on every port-status target.

Encryption then becomes one choice in application code: a user or server passphrase, a random key kept by the platform key store, or 32 bytes managed by an existing protocol. Every port writes one SQLCipher 4 compatible format.

The database API now has a behavior contract we test on every supported target.

{{canonical}}

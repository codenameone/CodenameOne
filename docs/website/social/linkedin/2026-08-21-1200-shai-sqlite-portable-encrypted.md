---
title: "A shared database interface was not enough"
slug: 2026-08-21-1200-shai-sqlite-portable-encrypted
platform: linkedin
account: shai
source_slug: sqlite-portable-encrypted
publish_at: '2026-08-21T12:00:00'
timezone: Asia/Jerusalem
image: /blog/sqlite-portable-encrypted.jpg
---

We have called Codename One's SQLite API portable since 2012. The interface was portable. Its behavior was not.

Windows and Linux returned `null` when an application opened a database. JavaScript depended on WebSQL, which Chrome removed and Firefox never implemented. iOS and the simulator disagreed on cursor positions, blobs, scripts, and transaction behavior.

Database encryption exposed the deeper problem. There was no single contract underneath the API where a key could belong.

We fixed the contract first. Seven device tests now run lifecycle, statement, cursor, transaction, encryption, and compatibility behavior on every port-status target.

Encryption then becomes one choice in application code: a user or server passphrase, a random key kept by the platform key store, or 32 bytes managed by an existing protocol. Every port writes one SQLCipher 4 compatible format.

Portable should describe behavior we test, not method names that happen to match.

{{canonical}}

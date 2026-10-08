---
title: "Update DB Schema in Production with Flyway like Syntax"
slug: database-upgrades-without-lost-data
url: /blog/database-upgrades-without-lost-data/
date: '2026-10-11'
author: Shai Almog
description: "Use ordered SQL migrations on client and server, validate schema history and handle skipped releases. Understand baselines, repeatable scripts and transactional limits."
feed_html: '<img src="https://www.codenameone.com/blog/database-upgrades-without-lost-data.jpg" alt="Versioned database changes bring old installations forward" /> Use ordered SQL migrations on client and server, validate schema history and handle skipped releases. Understand baselines, repeatable scripts and transactional limits.'
series: ["release-2026-10-09"]
---

![Versioned database changes bring old installations forward](/blog/database-upgrades-without-lost-data.jpg)

The database on your laptop has today's schema. The database on a customer's phone might be six releases old. Both have to work after the next update, and deleting the customer's data is not a migration strategy.

Codename One now uses the same migration engine for client databases and the native backend. It follows Flyway-style versioned scripts and history, with build-time registration so the scripts travel inside the app or server. This continues our [weekly release series](/blog/android-apps-beyond-android/) and supplies the tables behind [yesterday's security stack](/blog/sign-in-client-server-contract/).

## Give each schema change a place in history

Client scripts live in `common/src/main/db/migration`. Backend scripts live in `src/main/resources/db/migration` inside the backend module. For a notes application, the first script might be:

```sql
-- V1__create_note.sql
CREATE TABLE note (
    id INTEGER PRIMARY KEY,
    body VARCHAR(2000) NOT NULL
);
CREATE INDEX note_body ON note (body);
```

A later release adds a timestamp through another file:

```sql
-- V2__add_note_created.sql
ALTER TABLE note ADD COLUMN created BIGINT;
UPDATE note SET created = 0 WHERE created IS NULL;
```

The double underscore separates the version from the description. Versions sort numerically, so `2.10` comes after `2.9`. Never edit a versioned SQL script that has shipped. Its recorded checksum lets the engine detect that two installations would otherwise have different definitions of the same version.

{{< mermaid >}}
flowchart LR;
    fresh["New database"] --> v1["Apply V1"];
    v1 --> v2["Apply V2"];
    existing["Database already at V1"] --> v2;
    v2 --> ready["Current schema"];
    ready --> reopen["Validate history on next open"];
{{< /mermaid >}}

Skipping an app release is now ordinary input to the migration engine. It applies the missing versions in order. A repeatable `R__description.sql` runs after versioned migrations and again when its checksum changes, which suits a view definition. The build catches duplicate versions, malformed filenames and empty scripts before packaging.

## Upgrade before the app starts using the database

For the client, migrate immediately after opening the database:

```java
Database db = Display.getInstance().openOrCreate("notes.db");
Migrations.migrate(db);
```

The imports are `com.codename1.db.Database`, `com.codename1.db.Migrations` and `com.codename1.ui.Display`. Run database work on its owning thread. If a migration moves substantial data, do it off the event dispatch thread and show progress before opening the normal UI.

The annotation ORM's `EntityManager.open("notes.db")` performs migration for you. The build compiles scripts into generated code, so there is no folder of migration resources for an iOS package to discover at runtime.

An older binary also needs protection from newer data. By default, a future schema causes migration to refuse before changing it. The [client guide](/developer-guide/#client-schema-migrations) shows how to handle `MigrationException.FUTURE_SCHEMA` and ask the user to update. This is safer than hoping an old query happens to survive a downgrade.

## Make server startup an ordering guarantee

The backend applies pending migrations before opening its entity manager or accepting application requests. Its database lock coordinates instances starting against the same database. A successful transactional migration records its history row with the schema change.

Use the Maven goals to inspect and validate a deployment before starting it:

```bash
mvn -pl backend -Dcodename1.platform=backend cn1:migrate-info
mvn -pl backend -Dcodename1.platform=backend cn1:migrate-validate
```

The [server migration guide](/developer-guide/backend-data/#backend-schema-migrations) also covers applying and repairing migrations. `migrate-info` reads history without changing it. Keep connection settings pointed at the intended environment before running a mutating goal.

SQL common to SQLite, PostgreSQL and MySQL can live directly in `db/migration`. Engine-specific variants belong under `sqlite`, `postgresql` or `mysql`, using the same migration filename. MariaDB uses the MySQL variant. Shared machinery does not make different database dialects interchangeable.

## The failure case determines the recovery plan

SQLite and PostgreSQL can roll a transactional migration back with its history update. MySQL and MariaDB can commit DDL statements as they execute. A failed script there can leave part of its schema change behind. The engine records failure and refuses to proceed; inspect and correct the database before repairing history.

A SQLite script marked `executeInTransaction=false` has another boundary: the normal transaction cannot serialize several starting processes across the whole script. Run such a migration from one process before starting the rest. Do not apply the normal locking guarantee to that exception.

A populated database without migration history is also refused by default. Adopt it by explicitly recording a baseline for the version it already implements. A baseline records your assertion about the schema; it does not inspect every column and prove the assertion correct.

The security library registers its own named migration set and separate history table, then runs before application migrations. That lets your scripts depend on its tables without mixing your application's version numbers with the library's.

There are no undo scripts. Write a forward repair migration, and keep tested backups for changes that cannot be reversed from the remaining data. Java migrations have no automatic checksum, so changing an already-applied Java migration will not get the same SQL checksum protection.

## Test the upgrade your customers will take

A fresh-database test only proves the easiest path. Keep fixtures for an older supported schema, upgrade them to current, then reopen and verify that nothing applies twice. Add a newer-than-app fixture to test refusal. For important data, assert the values after migration as well as the table shape.

The [implementation in PR #5961](https://github.com/codenameone/CodenameOne/pull/5961) provides the engine. Your release history provides the cases that matter. Tomorrow's testing article shows how to keep backend behavior under test while changing runtimes.

## Discussion

_How far back does your oldest supported installation's database go?_

{{< giscus >}}

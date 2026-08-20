# Bundled SQLite engine

The SQLite build shared by every Codename One target that compiles C: iOS, native Windows and
native Linux. It is [SQLite3 Multiple Ciphers](https://github.com/utelle/SQLite3MultipleCiphers),
which is SQLite plus a set of encryption schemes, one of which is byte compatible with SQLCipher 4.

| | |
| --- | --- |
| Upstream release | `v2.5.0`, built on SQLite 3.53.4 |
| Download | <https://github.com/utelle/SQLite3MultipleCiphers/releases/download/v2.5.0/sqlite3mc-2.5.0-sqlite-3.53.4-amalgamation.zip> |
| Licence | MIT (SQLite itself is public domain) |

## Why this rather than SQLCipher

The on-disk format has to be identical everywhere, or a database written on a phone could not be
opened in the simulator. SQLite3MC gives us that from a single source: the simulator's JDBC driver
is built from this same project, so four of the five platforms run the same engine at the same
version. SQLCipher would also have worked on the C targets, but it publishes no prebuilt
amalgamation, so every build would have to run its configure script first.

Android is the exception. It cannot compile C in our build, so it consumes a prebuilt SQLCipher
AAR. That is a different engine, but it writes the same format, which is what actually matters.

## Why the files are named .h

`cn1_sqlite3_amalgamation.h` is upstream's `sqlite3mc_amalgamation.c`, renamed. The extension is
load bearing, and all three C targets depend on it:

- The iOS project generator lists `.h` files but excludes them from the compile phase, and routes
  unknown extensions into the resources phase. Named `.c` it would be compiled a second time
  without our defines; named `.inc` it would be copied into the .ipa as 13MB of dead weight.
- CMake globs `*.c` for sources and `*.h` for headers, so the same reasoning holds on Windows and
  Linux.
- The ParparVM native symbol scanner reads every `.c` and `.m` into memory and tokenises it. A 13MB
  translation unit would multiply the work of the heaviest structure in the translator.

It is compiled exactly once, by `cn1_sqlite3.c`, which sets the build options first.

## Updating

Download the amalgamation zip for the new release, copy `sqlite3mc_amalgamation.c` over
`cn1_sqlite3_amalgamation.h` and `sqlite3mc_amalgamation.h` over `cn1_sqlite3.h`, and update the
version and checksums here. Keep the simulator's `io.github.willena:sqlite-jdbc` version in step,
or the two will drift apart on cipher defaults.

## Checksums

Of the upstream files, before renaming:

    sqlite3mc_amalgamation.c  d28339d7a56f3b465720aa9c729f3ca9d429705ea8fda45bb8d034536cecd579
    sqlite3mc_amalgamation.h  959f37e52c004f179ac0e2a5ce28f7bc58271c1a7d532ce9654484641fc31855

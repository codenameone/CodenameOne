# Bundled SQLite engine for the JavaScript port

`sqlite3mc.js`, `sqlite3.wasm` and `sqlite3-opfs-async-proxy.js` are vendored, not built here.

| | |
|---|---|
| Upstream | SQLite3MultipleCiphers |
| Release | v2.4.0 (SQLite 3.53.4) |
| Asset | `sqlite3mc-2.4.0-sqlite-3.53.4-wasm.zip`, `jswasm/` directory |
| URL | <https://github.com/utelle/SQLite3MultipleCiphers/releases/tag/v2.4.0> |
| Licence | MIT (SQLite3MultipleCiphers); SQLite itself is public domain |

`sqlite3mc.js` is upstream's `jswasm/sqlite3.js` renamed. The other two keep their names,
and `port.js` loads the pair with an explicit `locateFile` pointing back at this directory --
`importScripts` does not move a worker's base URL, so without it the loader fetches
`/sqlite3.wasm` from the site root.

SHA-256 of the files as vendored:

```
df09e0c2b1534f3c6aa7614f3834f7d11ea8ee354fb4287d553e1dec4b5d80c5  sqlite3mc.js
51e490633f1913682bbbcda867caf5b4d41da0295b5b976b9c84c1369a66db6b  sqlite3.wasm
4ea2bcbd715b0d56089fc871ea241f8c5985d8669d1ddecaab4d56a8da806ce9  sqlite3-opfs-async-proxy.js
```

## Why 2.4.0 rather than the newest release

v2.5.0 ships the same SQLite version but its `sqlite3.wasm` is built with a toolchain that
emits the WebAssembly compact-imports encoding: the first import carries kind `0x7f` where a
standard module expects `0x00`-`0x03`. No shipping browser accepts that, and neither does
Node -- `WebAssembly.Module()` rejects it outright with "unknown import kind 0x7f". A build
that cannot be compiled anywhere is not a candidate however current its version number, so
this stays on the newest release whose module validates.

Validate any replacement before committing it, because the failure is invisible until a
database is opened at runtime:

```bash
node -e "new WebAssembly.Module(require('fs').readFileSync('sqlite3.wasm')); console.log('ok')"
```

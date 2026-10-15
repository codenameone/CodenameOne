# PWA regression checks

The Java `JavaScriptPwaTest` checks the final deployment files, icon dimensions,
manifest hints, push-handler preservation, and content-based cache versions.
The service-worker contract runs in the Java 8 PR job:

```sh
node tests/pwa/service-worker-contract.mjs
```

For a local Chromium check, run the build-engine tests with
`-Dcn1.pwa.fixture=/absolute/path/to/fixture`, then install the dependencies from
`scripts/package.json` and the matching Playwright Chromium browser. Run:

```sh
node tests/pwa/browser.mjs /absolute/path/to/fixture
```

The browser check serves the generated fixture on localhost under two paths. It
checks Chrome's manifest/installability diagnostics, offline navigation with a
query string, cached chunks/resources, API exclusion, waiting updates, and cache
isolation. It does not automate the Android or iOS installation UI.

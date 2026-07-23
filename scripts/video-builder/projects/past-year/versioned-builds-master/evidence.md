# Evidence map

Source: `docs/website/content/blog/versioned-builds-master.md`
Canonical: https://www.codenameone.com/blog/versioned-builds-master/

## Thesis

Pinning released builds while using master builds as a fast verification channel

## Supported beats

- **Pin A Released Version:** By default, a cloud build uses the current Codename One release. With a versioned build, you can pin to a specific published version.
- **Build Against Master:** This builds against the current development head of Codename One. Community developers recently asked for nightly or daily builds, but pushing nightly artifacts through Maven Central and support channels is messier than it sounds.
- **Why It Is Tiered:** The small overhead of fetching versioned artifacts is noticeable, but that is not the real reason this is limited by account level.
- **A Practical Workflow:** If that works, the regression is likely in the framework or build server. If it still fails, the issue is probably in your app, dependencies, native configuration, or a platform toolchain change that affects the old version too.
- **What This Does Not Solve:** Versioned builds are not a time machine for every external dependency. Apple, Google, OS SDKs, certificates, signing rules, app store requirements, Maven repositories and native tools all move. A six-month-old framework can still be affected by a current Xcode or Android requirement.

## Independent problem evidence

- Maven Dependency Mechanism: https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html — Maven's dependency mechanism resolves artifacts by group, artifact, and version so a build can name the framework state it expects.
- Reproducible Builds: https://reproducible-builds.org/docs/ — Reproducible-build guidance records how timestamps, toolchains, and environment inputs can affect otherwise identical source builds.

## Product proof

- `docs/website/static/blog/versioned-builds-master.jpg`

# Evidence map

Source: `docs/website/content/blog/standalone-codename-one-settings.md`
Canonical: https://www.codenameone.com/blog/standalone-codename-one-settings/

## Thesis

Why project settings became a focused standalone Maven tool

## Supported beats

- **One command, one project:** The Maven plugin resolves the com.codenameone:codenameone-settings artifact, launches it against the current project, and writes changes back to that project's codenameone_settings.properties and Maven configuration. The tool has its own release lifecycle instead of borrowing the GUI Builder's jar and version.
- **Build hints are searchable project data:** Build hints used to feel like an untyped text file with a dialog in front of it. The new editor preserves direct key-value control, but adds descriptions, known value types, filtering, and a focused editing flow.
- **Extensions keep compatibility warnings:** The Extensions screen browses the live catalog and installs or removes both Maven dependencies and legacy cn1lib packages. It also retains bundled compatibility metadata when the live catalog omits it.
- **What moved out:** Accounts do not belong inside a project-property editor. Certificates have enough platform rules to justify their own tool. Build monitoring belongs next to the builds. Removing those sections leaves Settings with one durable responsibility: edit the project you launched it from.
- **The migration cost:** The new editor is distributed as a separate Maven artifact and its application is built on Java 17. The release workflow now has to publish that reactor after the core release and keep its versions aligned with the Maven plugin.

## Referenced evidence

- https://github.com/codenameone/CodenameOne/pull/5359
- https://cloud.codenameone.com/account/security

## Independent problem evidence

- Guide to Configuring Plug-ins: https://maven.apache.org/guides/mini/guide-configuring-plugins.html — A plugin goal can run directly from the command line against the current project.
- About repositories: https://docs.github.com/en/repositories/creating-and-managing-repositories/about-repositories — Repository-managed files make important setup reviewable and repeatable for the team.

## Product proof

- `docs/website/static/blog/standalone-codename-one-settings/settings-basic.png`
- `docs/website/static/blog/standalone-codename-one-settings/settings-build-hints.png`
- `docs/website/static/blog/standalone-codename-one-settings/settings-extensions.png`

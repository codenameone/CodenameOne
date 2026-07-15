JavaScriptPort is the browser-runtime work area for the new Codename One JavaScript port.

Scope of this bootstrap:
- imported HTML5/runtime code from the former browser port as the behavioral/source baseline
- GPLv2 + Classpath Exception licensing consistent with the rest of Codename One
- ParparVM-oriented smoke fixtures under `Ports/JavaScriptPort/tests/**`
- executable translator/runtime coverage through the local ParparVM test suite in `vm/tests`

License boundary:
- first-party files under `Ports/JavaScriptPort/**` use Codename One's GPLv2 + Classpath Exception license
- bundled third-party material retains its upstream terms; source-header exceptions are identified by exact path in `scripts/copyright-header-exclusions.txt`

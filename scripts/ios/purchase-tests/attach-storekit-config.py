#!/usr/bin/env python3
"""Point every shared scheme of a generated project at a StoreKit configuration.

Usage: attach-storekit-config.py <project_dir> <Products.storekit>

Run after create-shared-scheme.py, which rewrites the schemes whole.

The configuration is set on the test action and on the launch action. A hosted
test creates its SKTestSession only after the app has started, and an app using
StoreKit 2 listens to Transaction.updates from launch, as Apple requires.
StoreKit then settles on the sandbox environment ("Error updating Storefront
... environment: Sandbox" in the app's log) and never sees the session's local
store: every product comes back without a name or price, and no transaction
reaches the app. With the configuration on the scheme the app is launched
against the local store from its first StoreKit call. StoreKit 1 does not touch
the store before the app makes a call, which is why its test never needed this.
"""
import sys
from pathlib import Path


def main(argv):
    if len(argv) != 3:
        print(__doc__, file=sys.stderr)
        return 2
    project_dir = Path(argv[1]).resolve()
    config = Path(argv[2]).resolve()
    if not config.is_file():
        print(f"attach-storekit-config: no configuration at {config}", file=sys.stderr)
        return 2
    ref = ("      <StoreKitConfigurationFileReference\n"
           f"         identifier = \"{config}\">\n"
           "      </StoreKitConfigurationFileReference>\n")
    schemes = sorted(project_dir.glob("*.xc*/xcshareddata/xcschemes/*.xcscheme"))
    if not schemes:
        print(f"attach-storekit-config: no shared schemes under {project_dir}", file=sys.stderr)
        return 1
    for scheme in schemes:
        xml = scheme.read_text(encoding="utf-8")
        if "StoreKitConfigurationFileReference" in xml:
            continue
        for tag in ("TestAction", "LaunchAction"):
            close = f"   </{tag}>"
            if close not in xml:
                print(f"attach-storekit-config: {scheme} has no {tag}", file=sys.stderr)
                return 1
            xml = xml.replace(close, ref + close, 1)
        scheme.write_text(xml, encoding="utf-8")
        print(f"attach-storekit-config: {scheme.name} -> {config.name}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))

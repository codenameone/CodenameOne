#!/usr/bin/env python3
"""Install the Central-mirror profile into a Maven settings.xml.

With no settings file, the shipped one is copied. actions/setup-java writes its own
(a <servers> block for publishing), and a release writes credentials there, so an
existing file is merged into rather than replaced: the profile goes into its
<profiles> and its id into <activeProfiles>, creating either element when absent.
Running twice is a no-op.
"""
import os
import re
import sys

PROFILE_ID = "cn1-ci-central-mirror"


def main(source, dest):
    with open(source, encoding="utf-8") as f:
        shipped = f.read()
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    if not os.path.exists(dest):
        with open(dest, "w", encoding="utf-8") as f:
            f.write(shipped)
        print("Installed " + dest)
        return
    with open(dest, encoding="utf-8") as f:
        existing = f.read()
    if PROFILE_ID in existing:
        print(dest + " already has the mirror profile")
        return
    profile = re.search(r"<profile>.*?</profile>", shipped, re.S).group(0)
    active = "<activeProfile>" + PROFILE_ID + "</activeProfile>"
    if "</settings>" not in existing:
        sys.exit("error: " + dest + " has no </settings>; refusing to guess where the profile goes")
    if "<profiles>" in existing:
        existing = existing.replace("<profiles>", "<profiles>\n" + profile, 1)
    else:
        existing = existing.replace("</settings>", "<profiles>\n" + profile + "\n</profiles>\n</settings>", 1)
    if "<activeProfiles>" in existing:
        existing = existing.replace("<activeProfiles>", "<activeProfiles>\n" + active, 1)
    else:
        existing = existing.replace("</settings>", "<activeProfiles>" + active + "</activeProfiles>\n</settings>", 1)
    with open(dest, "w", encoding="utf-8") as f:
        f.write(existing)
    print("Merged the mirror profile into " + dest)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])

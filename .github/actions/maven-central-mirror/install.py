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


def outside_comments(text, pattern):
    """First match of pattern in text that is not inside an XML comment, or None."""
    masked = re.sub(r"<!--.*?-->", lambda m: " " * len(m.group(0)), text, flags=re.S)
    return re.search(pattern, masked)


def insert_into(text, tag, content, dest):
    """Put content first inside <tag> (open or self-closing), creating it if absent."""
    m = outside_comments(text, r"<" + tag + r"\s*/>")
    if m:
        return text[:m.start()] + "<" + tag + ">\n" + content + "\n</" + tag + ">" + text[m.end():]
    m = outside_comments(text, r"<" + tag + r"(\s[^>]*)?>")
    if m:
        return text[:m.end()] + "\n" + content + text[m.end():]
    m = outside_comments(text, r"</settings\s*>")
    if not m:
        sys.exit("error: " + dest + " has no </settings>; refusing to guess where the profile goes")
    return text[:m.start()] + "<" + tag + ">\n" + content + "\n</" + tag + ">\n" + text[m.start():]


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
    existing = insert_into(existing, "profiles", profile, dest)
    existing = insert_into(existing, "activeProfiles", active, dest)
    with open(dest, "w", encoding="utf-8") as f:
        f.write(existing)
    print("Merged the mirror profile into " + dest)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])

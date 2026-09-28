#!/usr/bin/env python3
"""Compare the native project a Maven build generated with the one a Gradle
build generated from the same application (android-source, ios-source).

The two builds run the same engine, so the trees must match file for file.
What legitimately differs is normalized, and nothing else is:

  * a Properties file's first comment is the time Properties.store() ran;
  * the Android signing keystore is generated with a fresh random key;
  * Maven's jars carry META-INF/maven/** and a MANIFEST.MF, which the builder
    copies along with the rest of the jar;
  * the iOS source manifest records where each file came from as an absolute
    path, which names each build's own work directory;
  * jars and zips are compared by entry name and content, not by bytes, since
    their headers hold timestamps.

Usage: compare_generated_projects.py MAVEN_DIR GRADLE_DIR
Exits 1 and lists every difference when the trees are not equivalent.
"""
import os
import re
import sys
import zipfile

TIMESTAMP = re.compile(rb'^#[A-Z][a-z]{2} [A-Z][a-z]{2} [ 0-9]\d \d\d:\d\d:\d\d [A-Z+0-9:-]+ \d{4}\r?$', re.M)
SOURCE_MANIFEST_PATH = re.compile(rb'\|/[^|\n]*/antProject/')
KEYSTORES = {'app/keyStore'}


def maven_bookkeeping(rel):
    return '/META-INF/maven/' in '/' + rel or rel.endswith('META-INF/MANIFEST.MF')


def files_under(root):
    out = {}
    for base, dirs, files in os.walk(root):
        for name in files:
            full = os.path.join(base, name)
            rel = os.path.relpath(full, root).replace(os.sep, '/')
            if rel in KEYSTORES or maven_bookkeeping(rel):
                continue
            out[rel] = full
    return out


def normalized(rel, data):
    if rel.endswith('.properties'):
        data = TIMESTAMP.sub(b'#<timestamp>', data)
    if rel.endswith('cn1-source-manifest.txt'):
        data = SOURCE_MANIFEST_PATH.sub(b'|<work>/antProject/', data)
    return data


def archive_entries(path):
    with zipfile.ZipFile(path) as z:
        return {n: z.read(n) for n in z.namelist() if not n.endswith('/') and not maven_bookkeeping(n)}


def main(maven_dir, gradle_dir):
    maven = files_under(maven_dir)
    gradle = files_under(gradle_dir)
    problems = []
    for rel in sorted(set(maven) - set(gradle)):
        problems.append('only in Maven:  ' + rel)
    for rel in sorted(set(gradle) - set(maven)):
        problems.append('only in Gradle: ' + rel)
    for rel in sorted(set(maven) & set(gradle)):
        a = open(maven[rel], 'rb').read()
        b = open(gradle[rel], 'rb').read()
        if a == b:
            continue
        if zipfile.is_zipfile(maven[rel]) and zipfile.is_zipfile(gradle[rel]):
            ea, eb = archive_entries(maven[rel]), archive_entries(gradle[rel])
            if ea != eb:
                names = sorted(n for n in set(ea) | set(eb) if ea.get(n) != eb.get(n))
                problems.append('archive differs: %s (%s)' % (rel, ', '.join(names[:5])))
            continue
        if normalized(rel, a) != normalized(rel, b):
            problems.append('differs:        ' + rel)
    compared = len(set(maven) & set(gradle))
    if problems:
        print('%d difference(s) between %s and %s:' % (len(problems), maven_dir, gradle_dir))
        for p in problems[:60]:
            print('  ' + p)
        return 1
    if compared == 0:
        # Two empty trees are "equal" and prove nothing.
        print('no files to compare under %s and %s' % (maven_dir, gradle_dir))
        return 1
    print('%d files equivalent' % compared)
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1], sys.argv[2]))

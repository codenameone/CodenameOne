#!/usr/bin/env python3
"""Install the local legacy-runner LinkedIn handoff; never starts a browser.

The runner lives outside this repository. Keep a private backup and diff before
installing. Article platforms keep their existing behavior; queue/state are untouched.
"""
import argparse
import difflib
from pathlib import Path

MARKER = "LINKEDIN_COORDINATOR_HANDOFF_V1"
MESSAGE = "LinkedIn is owned by the overnight Firefox coordinator; submit this queue item for exact approval there."


def guarded(source):
    if MARKER in source:
        return source
    anchors = [
        ('    site = task["site"]\n    driver_fn = DRIVERS.get(site)',
         f'    # {MARKER}: never launch the legacy browser for LinkedIn.\n'
         '    site = task["site"]\n'
         '    if site == "linkedin":\n'
         f'        return False, None, {MESSAGE!r}\n'
         '    driver_fn = DRIVERS.get(site)'),
        ('    """Publish one due LinkedIn post as Shai or the Codename One page."""\n',
         '    """Legacy entry retained for callers; all LinkedIn writes are centralized."""\n'
         f'    raise RuntimeError({MESSAGE!r})\n'),
        ('        pending = [t for t in unfinished if is_due(t, now)]',
         '        delegated = [t for t in unfinished if t.get("site") == "linkedin"]\n'
         '        for t in delegated:\n'
         f'            print("Delegated " + t["id"] + ": " + {MESSAGE!r})\n'
         '        unfinished = [t for t in unfinished if t.get("site") != "linkedin"]\n'
         '        pending = [t for t in unfinished if is_due(t, now)]'),
    ]
    for before, after in anchors:
        if source.count(before) != 1:
            raise ValueError("Runner changed: inspect before installing; no files modified")
        source = source.replace(before, after)
    compile(source, '<guarded-runner>', 'exec')
    return source


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('runner', type=Path)
    p.add_argument('--backup-dir', type=Path, required=True)
    p.add_argument('--apply', action='store_true')
    a = p.parse_args()
    before = a.runner.read_text()
    after = guarded(before)
    if before == after:
        print('Coordinator handoff already installed')
        return
    diff = ''.join(difflib.unified_diff(before.splitlines(True), after.splitlines(True),
                                      fromfile=str(a.runner), tofile=str(a.runner)))
    if not a.apply:
        print(diff)
        return
    a.backup_dir.mkdir(parents=True, exist_ok=True)
    backup = a.backup_dir / 'syndicate_browser.before-linkedin-handoff.py'
    with backup.open('x') as f: f.write(before)
    (a.backup_dir / 'linkedin-handoff.patch').write_text(diff)
    a.runner.write_text(after)
    print('Installed coordinator handoff; backup: ' + str(backup))


if __name__ == '__main__': main()

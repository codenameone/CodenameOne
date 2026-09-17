#!/usr/bin/env python3
"""Snapshot the existing HelloCodenameOne macOS build's application + port classes.

The build supplies the real application, Kotlin dependencies, generated entrypoint,
and framework. The benchmark supplies the freshly built ParparVM JavaAPI separately.
This does not claim to rebuild HelloCodenameOne from source.
"""
import hashlib
import json
from pathlib import Path
import shutil
import sys

repo = Path(__file__).resolve().parents[2]
build = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else (
    repo / 'scripts/hellocodenameone/mac/target/codenameone/antProject/dist/macos-build')
output = repo / 'vm/selfhost/target/hello-corpus'
javaapi = repo / 'vm/selfhost/target/javaapi-classes'
inputs = [build / 'macPort/classes', build / 'classes']
if not all(p.is_dir() for p in inputs) or not javaapi.is_dir():
    sys.exit('Missing built HelloCodenameOne macOS classes or selfhost JavaAPI')
if output.exists():
    shutil.rmtree(output)
output.mkdir()
manifest = {'source_build': str(build), 'classes': {}}
for directory in inputs:
    for source in sorted(directory.rglob('*.class')):
        relative = source.relative_to(directory)
        if relative.parts[0] in ('java', 'javax') or (javaapi / relative).exists():
            continue
        destination = output / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, destination)
        manifest['classes'][str(relative)] = {
            'source': str(source), 'sha256': hashlib.sha256(source.read_bytes()).hexdigest()}
entry = output / 'com/codenameone/examples/hellocodenameone/HelloCodenameOneStub.class'
if not entry.exists():
    sys.exit('Missing generated HelloCodenameOneStub entrypoint')
(output.parent / 'hello-corpus.json').write_text(json.dumps(manifest, indent=2) + '\n')
print(str(output))
print('%d application, dependency and framework classes' % len(manifest['classes']))

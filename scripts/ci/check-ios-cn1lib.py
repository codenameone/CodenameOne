#!/usr/bin/env python3
"""Compile one shipped iOS bridge under MRR and ARC, sharing SDK/pod setup.

Keep both compilations even when one fails. Link coverage is a separate step
using check-admob-ios-link.sh, since these targets are static libraries.
"""
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[2]
LIBRARIES = ('cn1-admob', 'cn1-applovin', 'cn1-unity-levelplay',
             'cn1-ai-whisper', 'cn1-ai-stablediffusion')


def project_spec(ad):
    header = '''                  GCC_PREFIX_HEADER: CN1Probe-Prefix.pch
                  GCC_PRECOMPILE_PREFIX_HEADER: NO
                  HEADER_SEARCH_PATHS: $(inherited) $(SRCROOT)
''' if ad else ''
    return '''name: CN1Probe
options:
  bundleIdPrefix: com.codenameone.ciprobe
  deploymentTarget:
    iOS: "DEPLOYMENT"
configs:
  DebugMRR: debug
  DebugARC: debug
targets:
  CN1Probe:
    type: library.static
    platform: iOS
    sources:
      - path: Sources
    settings:
      base:
                  CLANG_ENABLE_MODULES: YES
                  CODE_SIGNING_ALLOWED: NO
'''.replace('DEPLOYMENT', '15.0' if ad else '14.0') + header + '''      configs:
        DebugMRR:
          CLANG_ENABLE_OBJC_ARC: NO
        DebugARC:
          CLANG_ENABLE_OBJC_ARC: YES
'''


def check(lib, work):
    if lib not in LIBRARIES:
        raise ValueError('Unsupported cn1lib: ' + lib)
    work.mkdir(parents=True, exist_ok=True)
    sources = work / 'Sources'
    sources.mkdir(exist_ok=True)
    for source in (ROOT / 'maven' / lib / 'ios/src/main/objectivec').iterdir():
        if source.suffix in ('.h', '.m', '.mm'):
            shutil.copy2(source, sources)
    if not any(source.suffix in ('.m', '.mm') for source in sources.iterdir()):
        raise ValueError('No Objective-C sources staged for ' + lib)
    ad = not lib.startswith('cn1-ai-')
    if ad:
        for header in ('cn1_globals.h', 'cn1_virtual_thread.h'):
            shutil.copy2(ROOT / 'vm/ByteCodeTranslator/src' / header, work)
        (work / 'cn1_class_method_index.h').write_text('#pragma once\n')
        (work / 'CN1Probe-Prefix.pch').write_text(
            '#ifdef __OBJC__\n#import <UIKit/UIKit.h>\n#import <Foundation/Foundation.h>\n#endif\n'
            '#include "cn1_globals.h"\n')
    (work / 'project.yml').write_text(project_spec(ad))
    subprocess.run(['xcodegen', 'generate', '--spec', 'project.yml'], cwd=work, check=True)
    if ad:
        props = ROOT / 'maven' / lib / 'common/codenameone_library_required.properties'
        pod = next(line.split('=', 1)[1] for line in props.read_text().splitlines()
                   if line.startswith('codename1.arg.ios.pods='))
        name, version = pod.split(' ', 1)
        (work / 'Podfile').write_text(
            "platform :ios, '15.0'\n"
            "project 'CN1Probe', 'DebugMRR' => :debug, 'DebugARC' => :debug\n"
            "target 'CN1Probe' do\n  use_frameworks!\n  pod '%s', '%s'\nend\n" % (name, version))
        subprocess.run(['pod', 'install', '--repo-update'], cwd=work, check=True)
    project = ['-workspace', 'CN1Probe.xcworkspace'] if ad else ['-project', 'CN1Probe.xcodeproj']
    failed = False
    for config in ('DebugMRR', 'DebugARC'):
        print('::group::' + lib + ' ' + config, flush=True)
        rc = subprocess.run(['xcodebuild', *project, '-scheme', 'CN1Probe', '-configuration', config,
                             '-derivedDataPath', str(work / 'DerivedData'),
                             '-sdk', 'iphonesimulator', '-destination', 'generic/platform=iOS Simulator',
                             'CODE_SIGNING_ALLOWED=NO', 'build'], cwd=work).returncode
        print('::endgroup::', flush=True)
        if rc:
            print('::error::' + lib + ' failed ' + config, flush=True)
            failed = True
    return int(failed)


if __name__ == '__main__':
    lib = sys.argv[1]
    work = Path(tempfile.mkdtemp(prefix=lib + '-', dir=os.environ.get('RUNNER_TEMP')))
    sys.exit(check(lib, work))

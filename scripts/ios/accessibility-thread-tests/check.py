#!/usr/bin/env python3
"""Run the production accessibility gate with main-thread-checking UIKit stubs.

Requires macOS and Xcode command-line tools; no generated app or signing needed.
The Java ABI and UIKit queries are stubbed; Foundation and GCD are real.
"""
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[3]
source = (ROOT / 'Ports/iOSPort/nativeSources/IOSNative.m').read_text()
start = source.index('#if !TARGET_OS_WATCH\nBOOL cn1AccessibilityEagerLatched')
end = source.index('JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isDirectToDrawable', start)
implementation = source[start:end]
contract = Path(__file__).with_name('contract.m')
with tempfile.TemporaryDirectory(prefix='cn1-accessibility-thread-') as directory:
    tmp = Path(directory)
    (tmp / 'implementation.h').write_text(implementation)
    for platform, flags in (
        ('ios', ['-DCN1_TEST_PLATFORM=0']),
        ('macos', ['-DCN1_TEST_PLATFORM=1']),
        ('watchos', ['-DCN1_TEST_PLATFORM=2']),
    ):
        executable = tmp / platform
        subprocess.run([
            'xcrun', 'clang', '-fblocks', '-fno-objc-arc', '-Wall', '-Wextra',
            '-Wno-unused-parameter', '-Wno-unused-function', '-Wno-unused-variable', '-Werror', '-framework', 'Foundation',
            '-I', str(tmp), *flags, str(contract), '-o', str(executable),
        ], check=True, timeout=60)
        cases = ('none', 'voiceover', 'switch', 'touch', 'unavailable', 'eager')
        for case in cases:
            subprocess.run([str(executable), case], check=True, timeout=15)
        print('PASS: ' + platform + ' (6 scenarios)', flush=True)

#!/usr/bin/env python3
"""Compile/link the production push completion block and run its native contract.

Requires macOS and Xcode command-line tools; no generated app or signing is needed.
Only the surrounding VM ABI is stubbed. Keep the production preprocessor guards,
including those on the Java entry points, so a misspelled flag cannot pass.
"""
from pathlib import Path
import re
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[3]
NATIVE = ROOT / 'Ports/iOSPort/nativeSources'
source = (NATIVE / 'IOSNative.m').read_text()
delegate = (NATIVE / 'CodenameOne_GLAppDelegate.m').read_text()
builder = (ROOT / 'maven/codenameone-maven-plugin/src/main/java/com/codename1/builders/IPhoneBuilder.java').read_text()

# Use the template's flag and the builder's actual disabling replacement.
flag = re.search(r'^#define INCLUDE_CN1_PUSH2\s*$', source, re.M).group(0)
disabling = re.search(r'str\.replace\("(#define INCLUDE_CN1_PUSH2)", "([^"]+)"\)', builder)
assert disabling, 'Builder push configuration changed; update the test staging'
start = source.index('typedef void (^CN1PushCompletionHandlerType)')
start = source.rindex('#ifdef', 0, start)
end = source.index('#ifdef INCLUDE_CN1_BACKGROUND_FETCH', start)
implementation = source[start:end]
# Declarations come from the real caller, in a separate translation unit.
start = delegate.index('typedef void (^CN1PushCompletionHandlerType)')
end = delegate.index('-(void)cn1RoutePush:', start)
declarations = delegate[start:end]
abi = '''#import <Foundation/Foundation.h>
#import <dispatch/dispatch.h>
typedef void *JAVA_OBJECT;
typedef long long JAVA_LONG;
#define CN1_THREAD_STATE_MULTI_ARG
'''
with tempfile.TemporaryDirectory(prefix='cn1-push-completion-') as tmp:
    tmp = Path(tmp)
    for enabled in (True, False):
        configuration = flag if enabled else flag.replace(*disabling.groups())
        (tmp / 'implementation.m').write_text(abi + configuration + '\n' + implementation)
        (tmp / 'caller.h').write_text(declarations)
        executable = tmp / 'check'
        subprocess.run([
            'xcrun', 'clang', '-fblocks', '-fno-objc-arc', '-framework', 'Foundation',
            '-I', str(tmp), '-DPUSH_ENABLED=' + str(int(enabled)),
            str(tmp / 'implementation.m'), str(Path(__file__).with_name('contract.m')),
            '-o', str(executable),
        ], check=True, timeout=60)
        subprocess.run([str(executable)], check=True, timeout=20)
        print('PASS: push ' + ('enabled' if enabled else 'disabled'), flush=True)

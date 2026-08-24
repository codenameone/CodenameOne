#!/usr/bin/env python3
"""Check that native-interface peer methods return a native view, not a peer.

A NativeInterface method declared to return PeerComponent is special-cased by
every builder: AndroidGradleBuilder emits

    return PeerComponent.create(impl.createBanner(handle, adUnitId, ...));

and IPhoneBuilder the long[] equivalent. The generated stub does the wrapping,
so the platform implementation has to hand back the *native* object -- an
android.view.View on Android, a void* on iOS. An implementation that returns a
PeerComponent instead gets wrapped twice, and
AndroidImplementation.createNativePeer rejects its own AndroidPeer with

    java.lang.IllegalArgumentException:
        com.codename1.impl.android.AndroidImplementation$AndroidPeer

the first time the component is shown. Nothing catches that before a device
run: the double wrap is valid Java, so the API check compiles it happily, and
it only fails when the peer is created. cn1-admob, cn1-applovin and
cn1-unity-levelplay all shipped with it.

Run with no arguments from the repository root; exits non-zero on a finding.
"""

import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# "PeerComponent createBanner(int handle, ...);" in an interface body.
IFACE_METHOD = re.compile(
    r'(?<![\w.])PeerComponent\s+(\w+)\s*\([^;{)]*\)\s*;')
# "public PeerComponent createBanner(final int handle, ...) {" in a class body.
IMPL_METHOD = re.compile(
    r'(?<![\w.])(?:public|protected)\s+PeerComponent\s+(\w+)\s*\(')
# "-(void*)createBanner:(int)param ..." in an Objective-C implementation.
OBJC_METHOD = re.compile(r'^\s*-\s*\(\s*([\w \t*]+?)\s*\)\s*(\w+)\s*:', re.M)


def java_sources(root):
    for base, _dirs, files in os.walk(root):
        if os.sep + 'target' + os.sep in base + os.sep:
            continue
        for name in files:
            if name.endswith('.java'):
                yield os.path.join(base, name)


def native_interfaces(root):
    """Yield (path, text) for every interface extending NativeInterface."""
    for path in java_sources(root):
        with open(path, encoding='utf-8', errors='replace') as f:
            text = f.read()
        if 'NativeInterface' not in text:
            continue
        if re.search(r'\binterface\s+\w+\s+extends\s+[\w.,\s]*NativeInterface\b',
                     text):
            yield path, text


def module_dirs(iface_path):
    """The library root and interface name for a cn1lib native interface."""
    marker = os.sep + 'src' + os.sep + 'main' + os.sep + 'java' + os.sep
    if marker not in iface_path:
        return None, None
    module = iface_path.split(marker)[0]
    return os.path.dirname(module), os.path.basename(iface_path)[:-len('.java')]


def check():
    findings = []
    for iface_path, iface_text in native_interfaces(os.path.join(REPO, 'maven')):
        peer_methods = set(IFACE_METHOD.findall(iface_text))
        if not peer_methods:
            continue
        lib_root, iface_name = module_dirs(iface_path)
        if lib_root is None:
            continue
        rel_iface = os.path.relpath(iface_path, REPO)

        android_root = os.path.join(lib_root, 'android', 'src', 'main', 'java')
        for impl_path in java_sources(android_root):
            if not impl_path.endswith(iface_name + 'Impl.java'):
                continue
            with open(impl_path, encoding='utf-8', errors='replace') as f:
                impl_text = f.read()
            for name in sorted(set(IMPL_METHOD.findall(impl_text)) & peer_methods):
                findings.append(
                    '%s: %s() returns PeerComponent, but %s declares it as a peer '
                    'method, so the generated stub wraps the result in '
                    'PeerComponent.create() again. Return the android.view.View '
                    'itself.' % (os.path.relpath(impl_path, REPO), name, rel_iface))

        ios_root = os.path.join(lib_root, 'ios', 'src', 'main', 'objectivec')
        if not os.path.isdir(ios_root):
            continue
        for name in sorted(os.listdir(ios_root)):
            if not name.endswith('.m'):
                continue
            impl_path = os.path.join(ios_root, name)
            with open(impl_path, encoding='utf-8', errors='replace') as f:
                impl_text = f.read()
            for ret, method in OBJC_METHOD.findall(impl_text):
                if method not in peer_methods:
                    continue
                if ret.replace(' ', '') != 'void*':
                    findings.append(
                        '%s: %s returns %s, but %s declares it as a peer method, '
                        'which the iOS builder reads back as a pointer. Return '
                        'void*.' % (os.path.relpath(impl_path, REPO), method,
                                    ret, rel_iface))
    return findings


def main():
    findings = check()
    if findings:
        sys.stderr.write('Native peer methods must return the native view:\n\n')
        for finding in findings:
            sys.stderr.write('  ' + finding + '\n')
        sys.stderr.write('\n')
        return 1
    print('check-native-peer-returns: no findings')
    return 0


if __name__ == '__main__':
    sys.exit(main())

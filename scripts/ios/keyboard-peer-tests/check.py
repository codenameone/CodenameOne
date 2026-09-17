#!/usr/bin/env python3
"""Exercise production press routing with UIKit stubs (macOS + Xcode required).

Complements input-validation-app's real WKWebView hardware-typing test.
Foundation is real; native focus, keys and the Java delivery bridge are stubbed.
"""
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[3]
main = (ROOT / 'Ports/iOSPort/nativeSources/CodenameOne_GLViewController.m').read_text()
mac = (ROOT / 'Ports/iOSPort/nativeSources/CN1MacWindows.m').read_text()
globals_start = main.index('#if !TARGET_OS_OSX\n// BrowserComponent')
methods_start = main.index('// Hardware keyboard support', globals_start)
methods_end = main.index('// Hover support', methods_start)
mac_start = mac.index('- (void)deliverPresses:')
mac_end = mac.index('- (void)viewDidLayoutSubviews', mac_start)

with tempfile.TemporaryDirectory(prefix='cn1-keyboard-peer-') as directory:
    tmp = Path(directory)
    (tmp / 'globals.h').write_text(main[globals_start:methods_start])
    (tmp / 'main.h').write_text(main[methods_start:methods_end])
    (tmp / 'mac.h').write_text(mac[mac_start:mac_end])
    executable = tmp / 'contract'
    subprocess.run([
        'xcrun', 'clang', '-fno-objc-arc', '-Wall', '-Wextra', '-Werror',
        '-Wno-unused-parameter', '-framework', 'Foundation', '-I', str(tmp),
        str(Path(__file__).with_name('contract.m')), '-o', str(executable),
    ], check=True, timeout=60)
    subprocess.run([str(executable)], check=True, timeout=15)

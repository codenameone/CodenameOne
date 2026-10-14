#!/usr/bin/env python3
"""Exercise the real Linux event pump/queue against GTK's GLib main context.

Run locally with GTK3 development packages, a C compiler, and pkg-config. No
display server or translated application is needed: a GTK idle callback models
the native callback which queues a click for Java's subsequent drainInput().
"""
import os
from pathlib import Path
import re
import shlex
import subprocess
import tempfile
import unittest

REPO = Path(__file__).resolve().parent.parent

HARNESS = r'''
#include <gtk/gtk.h>
#include <pthread.h>
#include <assert.h>
#include <string.h>
#define JAVA_BOOLEAN int
#define JAVA_TRUE 1
#define JAVA_FALSE 0
#define CODENAME_ONE_THREAD_STATE void
static int cn1WindowOpen = 1;
static int parked;
#define CN1_YIELD_THREAD do { assert(!parked); parked = 1; } while (0)
#define CN1_RESUME_THREAD do { assert(parked); parked = 0; } while (0)
void cn1LinuxPushWindowEvent(int, int, int, int, int);
'''

CHECKS = r'''
static int watchdogFired;
static int clickQueued;
static gboolean watchdog(gpointer unused) {
    (void) unused;
    assert(!parked);
    watchdogFired = 1;
    return G_SOURCE_REMOVE;
}
static gboolean click(gpointer repeat) {
    assert(!parked);
    cn1LinuxPushEvent(CN1_EVENT_POINTER_PRESSED, 40, 50, 1);
    cn1LinuxPushEvent(CN1_EVENT_POINTER_RELEASED, 40, 50, 1);
    clickQueued = 1;
    return repeat != NULL;
}
static gboolean closeWindow(gpointer unused) {
    (void) unused;
    assert(!parked);
    cn1WindowOpen = 0;
    return G_SOURCE_REMOVE;
}
int main(int argc, char** argv) {
    assert(argc == 2);
    guint source;
    int closing = strcmp(argv[1], "close") == 0;
    if (closing) {
        source = g_idle_add(closeWindow, NULL);
    } else if (strcmp(argv[1], "wake") == 0) {
        /* With no ready source the pump must wait, then dispatch unparked. */
        source = g_timeout_add(30, click, NULL);
    } else {
        source = g_idle_add(click, strcmp(argv[1], "busy") == 0 ? (gpointer) 1 : NULL);
    }
    guint guard = g_timeout_add(500, watchdog, NULL);
    int result = com_codename1_impl_linux_LinuxNative_pumpMessages___R_boolean();
    /* GLib can first wake to recheck its source list, without dispatching the
     * timer. Java's main loop repeats the pump in that case too. */
    while (strcmp(argv[1], "wake") == 0 && result && !clickQueued && !watchdogFired) {
        result = com_codename1_impl_linux_LinuxNative_pumpMessages___R_boolean();
    }
    assert(!parked);
    assert(!watchdogFired && "queued click waited for an unrelated event");
    assert(result == !closing);
    if (!closing) {
        int event[5];
        assert(cn1LinuxPopEvent(event) && event[0] == CN1_EVENT_POINTER_PRESSED);
        assert(event[1] == 40 && event[2] == 50);
        assert(cn1LinuxPopEvent(event) && event[0] == CN1_EVENT_POINTER_RELEASED);
    }
    if (strcmp(argv[1], "busy") == 0) g_source_remove(source);
    g_source_remove(guard);
    return 0;
}
'''


class LinuxEventPumpTest(unittest.TestCase):
    def test_input_is_delivered_before_waiting_again(self):
        native = REPO / 'Ports/LinuxPort/nativeSources'
        header = (native / 'cn1_linux.h').read_text()
        source = (native / 'cn1_linux_window.c').read_text()
        events = re.search(r'typedef enum\s*\{[^}]+\}\s*CN1EventType;', header).group(0)
        queue = source[source.index('#define CN1_EVENT_RING'):
                       source.index('/* ------------------------------------------------------------- globals */')]
        start = source.index('JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_pumpMessages___R_boolean(')
        pump = source[start:source.index('\n}', start) + 3]
        flags = shlex.split(subprocess.check_output(
            ['pkg-config', '--cflags', '--libs', 'gtk+-3.0'], text=True))
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'pump.c'
            binary = Path(directory) / 'pump'
            path.write_text('\n'.join((HARNESS, events, queue, pump, CHECKS)))
            subprocess.run(shlex.split(os.environ.get('CC', 'cc')) +
                           ['-std=c11', '-Wall', '-Wextra', '-Werror', '-pthread',
                            str(path), '-o', str(binary)] + flags, check=True)
            for scenario in ('pending', 'busy', 'wake', 'close'):
                with self.subTest(scenario=scenario):
                    subprocess.run([str(binary), scenario], check=True, timeout=3)


if __name__ == '__main__':
    unittest.main()

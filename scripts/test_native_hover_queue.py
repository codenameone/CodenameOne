#!/usr/bin/env python3
"""Compile the real desktop event queues and exercise overflow without a native UI.

The Linux queue uses pthreads; the Windows harness substitutes only its lock/signal
primitives. Event declarations and all queue operations come from the port sources.
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
#include <assert.h>
static void drain(void) { int out[5]; while (pop(out)) {} }
static void fillMotion(int count) {
    for (int i = 0; i < count; i++) push(0, CN1_EVENT_POINTER_HOVER, i, 12, 0);
}
int main(void) {
    for (int window = 0; window <= 7; window += 7) {
        int out[5], count, leaves, ordinary;
        drain();
        fillMotion(CAPACITY - 1);
        push(window, CN1_EVENT_POINTER_HOVER, -1, -1, 512);
        count = leaves = 0;
        while (pop(out)) {
            count++;
            if (out[0] == CN1_EVENT_POINTER_HOVER && out[1] == -1 && out[2] == -1) {
                leaves++; assert(out[3] == 512 && out[4] == window);
                assert(count == CAPACITY - 1); /* terminal state stays last */
            }
        }
        assert(count == CAPACITY - 1 && leaves == 1);

        /* A queued leave must not be the droppable event evicted for a release. */
        push(window, CN1_EVENT_POINTER_HOVER, -1, -1, 512);
        fillMotion(CAPACITY - 2);
        push(7, CN1_EVENT_KEY_RELEASED, 0, 0, 65);
        count = leaves = 0;
        while (pop(out)) {
            count++;
            if (out[0] == CN1_EVENT_POINTER_HOVER && out[1] == -1 && out[2] == -1) leaves++;
        }
        assert(count == CAPACITY - 1 && leaves == 1);

        /* Ordinary hover remains droppable. Repeated fills also wrap both cursors. */
        fillMotion(CAPACITY - 1);
        push(window, CN1_EVENT_POINTER_HOVER, 99, 99, 12345);
        count = ordinary = 0;
        while (pop(out)) { count++; if (out[3] == 12345) ordinary++; }
        assert(count == CAPACITY - 1 && ordinary == 0);
    }
    return 0;
}
'''


class NativeHoverQueueTest(unittest.TestCase):
    def compile_and_run(self, platform):
        native = REPO / 'Ports' / (platform + 'Port') / 'nativeSources'
        stem = 'cn1_' + platform.lower()
        header = (native / (stem + '.h')).read_text()
        enum = re.search(r'typedef enum\s*\{[^}]+\}\s*CN1EventType;', header).group(0)
        code = '#include <pthread.h>\n' + enum + '\n'
        if platform == 'Linux':
            source = (native / 'cn1_linux_window.c').read_text()
            queue = source[source.index('#define CN1_EVENT_RING'):source.index('/* ------------------------------------------------------------- globals */')]
            code += 'void cn1LinuxPushWindowEvent(int, int, int, int, int);\n' + queue
            code += '\n#define CAPACITY CN1_EVENT_RING\n#define push cn1LinuxPushWindowEvent\n#define pop cn1LinuxPopEvent\n'
        else:
            source = (native / 'cn1_windows_window.cpp').read_text()
            event = re.search(r'typedef struct\s*\{[^}]+\}\s*CN1Event;', header).group(0)
            capacity = re.search(r'#define CN1_EVENT_QUEUE_CAPACITY\s+\d+', header).group(0)
            code += 'typedef int JAVA_INT;\ntypedef long LONG;\n' + event + '\n' + capacity + r'''
static struct {
    CN1Event events[CN1_EVENT_QUEUE_CAPACITY];
    LONG eventHead, eventTail;
    pthread_mutex_t eventLock;
    int eventSignal;
} cn1Win = { .eventLock = PTHREAD_MUTEX_INITIALIZER };
#define EnterCriticalSection(lock) pthread_mutex_lock(lock)
#define LeaveCriticalSection(lock) pthread_mutex_unlock(lock)
#define SetEvent(signal) ((void)(signal))
void cn1WinPushWindowEvent(int, CN1EventType, int, int, int);
'''
            queue = source[source.index('void cn1WinPushEvent('):source.index('/* ------------------------------------------------------------- input helpers */')]
            code += queue + r'''
#define CAPACITY CN1_EVENT_QUEUE_CAPACITY
#define push cn1WinPushWindowEvent
static int pop(int* out) {
    CN1Event event;
    if (!cn1WinPollEvent(&event)) return 0;
    out[0] = event.type; out[1] = event.x; out[2] = event.y;
    out[3] = event.keyCode; out[4] = event.windowId;
    return 1;
}
'''
        with tempfile.TemporaryDirectory() as directory:
            source_path = Path(directory) / 'queue.c'
            binary = Path(directory) / 'queue'
            source_path.write_text(code + HARNESS)
            subprocess.run(shlex.split(os.environ.get('CC', 'cc')) +
                           ['-std=c11', '-Wall', '-Wextra', '-Werror', '-pthread', str(source_path), '-o', str(binary)], check=True)
            subprocess.run([str(binary)], check=True)

    def test_linux_queue(self):
        self.compile_and_run('Linux')

    def test_windows_queue(self):
        self.compile_and_run('Windows')


if __name__ == '__main__':
    unittest.main()

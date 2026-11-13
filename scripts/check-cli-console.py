#!/usr/bin/env python3
"""Drives vm/backend/demo/consolecheck under a real pseudo-terminal.

The unit tests (maven/backend, ConsoleApiTest) script a fake terminal; this
checks what only a real one can: that raw mode really reads keys one at a
time, that arrow-key escape sequences arrive and select, that Ctrl-C at a
prompt ends the program with 130, and that the terminal's mode is exactly what
it was before the program ran -- a CLI that leaves a terminal raw is
unusable until the user types `reset`. It also runs the program with piped
input to check the line-mode fallback and that no escape codes are written to
a pipe.

    scripts/check-cli-console.py <command...>

e.g. the native binary from `CN1_BACKEND_DEMO=demo/consolecheck vm/backend/build.sh
ConsoleCheck com.demo out/consolecheck`, or `java -cp ... com.demo.ConsoleCheck`.
POSIX only.
"""
import os
import pty
import select
import subprocess
import sys
import termios
import time

UP = b"\x1b[A"
DOWN = b"\x1b[B"


def read_until(fd, marker, timeout=30.0, buf=None):
    buf = bytearray() if buf is None else buf
    end = time.time() + timeout
    while marker not in buf:
        left = end - time.time()
        if left <= 0:
            raise SystemExit("timed out waiting for %r; got:\n%s" % (marker, buf.decode("utf-8", "replace")))
        r, _, _ = select.select([fd], [], [], left)
        if r:
            try:
                chunk = os.read(fd, 4096)
            except OSError:
                chunk = b""
            if not chunk:
                raise SystemExit("program ended waiting for %r; got:\n%s" % (marker, buf.decode("utf-8", "replace")))
            buf.extend(chunk)
    return buf


def drain(fd, proc, timeout=30.0):
    out = bytearray()
    end = time.time() + timeout
    while time.time() < end:
        r, _, _ = select.select([fd], [], [], 0.2)
        if r:
            try:
                chunk = os.read(fd, 4096)
            except OSError:
                chunk = b""
            if not chunk:
                break
            out.extend(chunk)
        elif proc.poll() is not None:
            break
    proc.wait(timeout=10)
    return out


def spawn(cmd):
    master, slave = pty.openpty()
    # A known, cooked starting mode, and a size, so both can be checked after.
    attrs = termios.tcgetattr(slave)
    name = os.ttyname(slave)
    env = dict(os.environ, TERM="xterm-256color", LANG="en_US.UTF-8", COLUMNS="", LINES="")
    env.pop("NO_COLOR", None)
    env.pop("COLUMNS")
    env.pop("LINES")
    import fcntl
    import struct
    fcntl.ioctl(slave, termios.TIOCSWINSZ, struct.pack("HHHH", 30, 100, 0, 0))
    def controlling_tty():
        # A session of its own with the pty as its controlling terminal, as a
        # shell gives every command: /dev/tty is what the JVM's stty reads.
        os.setsid()
        fcntl.ioctl(0, termios.TIOCSCTTY, 0)

    proc = subprocess.Popen(cmd, stdin=slave, stdout=slave, stderr=slave, env=env,
                            preexec_fn=controlling_tty, close_fds=True)
    os.close(slave)
    return master, name, attrs, proc


def check_mode_restored(slave, before, what):
    # The program led the session the pty belonged to, so its exit hung up
    # the descriptor opened before it; a fresh one reads the mode it left.
    fd = os.open(slave, os.O_RDWR | os.O_NOCTTY)
    try:
        after = termios.tcgetattr(fd)
    finally:
        os.close(fd)
    # iflag, oflag, cflag, lflag and the control characters.
    names = ["iflag", "oflag", "cflag", "lflag", "ispeed", "ospeed", "cc"]
    for i, name in enumerate(names):
        if before[i] != after[i]:
            raise SystemExit("%s: terminal %s left changed: %r -> %r" % (what, name, before[i], after[i]))


def interactive(cmd):
    master, slave, before, proc = spawn(cmd)
    buf = read_until(master, b"(anon)")
    if b"SIZE 100x30 tty=true in=true" not in buf:
        raise SystemExit("size/tty line wrong:\n" + buf.decode("utf-8", "replace"))
    for needed in (b"\x1b[", b"api", b"Main.java", b"spun", b"log line"):
        if needed not in buf:
            raise SystemExit("missing %r in output:\n%s" % (needed, buf.decode("utf-8", "replace")))
    os.write(master, b"ada")
    os.write(master, b"\r")
    read_until(master, b"Database")
    time.sleep(0.1)
    os.write(master, DOWN + DOWN + UP + b"\r")
    read_until(master, b"Features")
    time.sleep(0.1)
    os.write(master, b" " + DOWN + DOWN + b" \r")
    read_until(master, b"Token")
    time.sleep(0.1)
    os.write(master, b"hunter2\r")
    read_until(master, b"Proceed?")
    time.sleep(0.1)
    os.write(master, b"y")
    out = drain(master, proc)
    text = out.decode("utf-8", "replace")
    expected = "ANSWERS name=ada db=MySQL features=[auth, jobs] secret=7 ok=true"
    if expected not in text:
        raise SystemExit("answers wrong; got:\n" + text)
    if "hunter2" in text:
        raise SystemExit("the password was echoed:\n" + text)
    if proc.returncode != 0:
        raise SystemExit("exit code %d" % proc.returncode)
    check_mode_restored(slave, before, "after answering")
    os.close(master)
    print("interactive: ok")


def cancelled(cmd):
    master, slave, before, proc = spawn(cmd)
    read_until(master, b"(anon)")
    time.sleep(0.1)
    os.write(master, b"ab\x03")
    out = drain(master, proc)
    if proc.returncode != 130:
        raise SystemExit("Ctrl-C exit code %d, wanted 130:\n%s" % (proc.returncode, out.decode("utf-8", "replace")))
    if b"CANCELLED" not in out:
        raise SystemExit("no cancellation line:\n" + out.decode("utf-8", "replace"))
    check_mode_restored(slave, before, "after Ctrl-C")
    if out.rfind(b"\x1b[?25l") > out.rfind(b"\x1b[?25h"):
        raise SystemExit("cursor left hidden after Ctrl-C")
    os.close(master)
    print("cancelled: ok")


def piped(cmd):
    env = dict(os.environ, TERM="xterm-256color")
    env.pop("NO_COLOR", None)
    p = subprocess.run(cmd, input=b"bob\n3\n1,mail\nsecret\nn\n", stdout=subprocess.PIPE,
                       stderr=subprocess.STDOUT, env=env, timeout=120)
    text = p.stdout.decode("utf-8", "replace")
    if p.returncode != 0:
        raise SystemExit("piped exit code %d:\n%s" % (p.returncode, text))
    if "\x1b" in text:
        raise SystemExit("escape codes written to a pipe:\n" + repr(text))
    expected = "ANSWERS name=bob db=SQLite features=[auth, mail] secret=6 ok=false"
    if expected not in text:
        raise SystemExit("piped answers wrong; got:\n" + text)
    if "work: done 20/20" not in text:
        raise SystemExit("no progress summary in piped output:\n" + text)
    print("piped: ok")


def main():
    cmd = sys.argv[1:]
    if not cmd:
        raise SystemExit(__doc__)
    interactive(cmd)
    cancelled(cmd)
    piped(cmd)


if __name__ == "__main__":
    main()

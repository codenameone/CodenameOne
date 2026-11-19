# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""signal: the numbers and the handler table.

A mobile application receives no POSIX signals -- the operating system
suspends and resumes it through the lifecycle callbacks -- so a handler is
recorded and answered by getsignal but never called, and raise_signal runs
it directly, which is what a program that signals itself observes.
"""

import enum as _enum


class Signals(_enum.IntEnum):
    SIGHUP = 1
    SIGINT = 2
    SIGQUIT = 3
    SIGILL = 4
    SIGTRAP = 5
    SIGABRT = 6
    SIGBUS = 7
    SIGFPE = 8
    SIGKILL = 9
    SIGUSR1 = 10
    SIGSEGV = 11
    SIGUSR2 = 12
    SIGPIPE = 13
    SIGALRM = 14
    SIGTERM = 15
    SIGCHLD = 17
    SIGCONT = 18
    SIGSTOP = 19
    SIGTSTP = 20
    SIGWINCH = 28


class Handlers(_enum.IntEnum):
    SIG_DFL = 0
    SIG_IGN = 1


SIG_DFL = Handlers.SIG_DFL
SIG_IGN = Handlers.SIG_IGN
NSIG = 65
SIGHUP = Signals.SIGHUP
SIGINT = Signals.SIGINT
SIGQUIT = Signals.SIGQUIT
SIGILL = Signals.SIGILL
SIGTRAP = Signals.SIGTRAP
SIGABRT = Signals.SIGABRT
SIGBUS = Signals.SIGBUS
SIGFPE = Signals.SIGFPE
SIGKILL = Signals.SIGKILL
SIGUSR1 = Signals.SIGUSR1
SIGSEGV = Signals.SIGSEGV
SIGUSR2 = Signals.SIGUSR2
SIGPIPE = Signals.SIGPIPE
SIGALRM = Signals.SIGALRM
SIGTERM = Signals.SIGTERM
SIGCHLD = Signals.SIGCHLD
SIGCONT = Signals.SIGCONT
SIGSTOP = Signals.SIGSTOP
SIGTSTP = Signals.SIGTSTP
SIGWINCH = Signals.SIGWINCH

_handlers = {}


def default_int_handler(signalnum, frame):
    raise KeyboardInterrupt


def signal(signalnum, handler):
    signalnum = int(signalnum)
    if signalnum in (9, 19):
        raise OSError(22, "Invalid argument")
    old = _handlers.get(signalnum, default_int_handler if signalnum == 2 else SIG_DFL)
    _handlers[signalnum] = handler
    return old


def getsignal(signalnum):
    signalnum = int(signalnum)
    return _handlers.get(signalnum, default_int_handler if signalnum == 2 else SIG_DFL)


def raise_signal(signalnum):
    h = getsignal(signalnum)
    if callable(h):
        h(int(signalnum), None)


def strsignal(signalnum):
    try:
        return Signals(signalnum).name
    except ValueError:
        return None


def valid_signals():
    return set(Signals)


def set_wakeup_fd(fd, *, warn_on_full_buffer=True):
    return -1


def alarm(seconds):
    return 0


def pause():
    pass


def siginterrupt(signalnum, flag):
    pass

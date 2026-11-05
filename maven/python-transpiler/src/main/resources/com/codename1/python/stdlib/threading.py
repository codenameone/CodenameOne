# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Threads over Java threads, with CPython's global interpreter lock.

Python code runs on one thread at a time. A thread lets the others run
while it waits: in time.sleep(), on a lock, condition, event or queue, and
during an HTTP request. A thread that computes without waiting keeps the
others waiting, as under CPython.
"""

import _thread
import time as _time

__all__ = ["Thread", "Lock", "RLock", "Condition", "Semaphore", "BoundedSemaphore",
           "Event", "Barrier", "BrokenBarrierError", "Timer", "local", "current_thread",
           "main_thread", "active_count", "enumerate", "get_ident", "TIMEOUT_MAX",
           "ThreadError", "excepthook"]

TIMEOUT_MAX = 9223372036.0
ThreadError = RuntimeError
get_ident = _thread.get_ident


class Lock:
    def __init__(self):
        self._lock = _thread.allocate_lock()

    def acquire(self, blocking=True, timeout=-1):
        if not blocking and timeout != -1:
            raise ValueError("can't specify a timeout for a non-blocking call")
        return _thread.lock_acquire(self._lock, blocking, None if timeout < 0 else timeout)

    def release(self):
        _thread.lock_release(self._lock)

    def locked(self):
        return _thread.lock_locked(self._lock)

    __enter__ = acquire

    def __exit__(self, *exc):
        self.release()

    def __repr__(self):
        return "<%s _thread.lock object>" % ("locked" if self.locked() else "unlocked")


class RLock:
    def __init__(self):
        self._block = Lock()
        self._owner = None
        self._count = 0

    def acquire(self, blocking=True, timeout=-1):
        me = get_ident()
        if self._owner == me:
            self._count += 1
            return True
        if not self._block.acquire(blocking, timeout):
            return False
        self._owner = me
        self._count = 1
        return True

    def release(self):
        if self._owner != get_ident():
            raise RuntimeError("cannot release un-acquired lock")
        self._count -= 1
        if self._count == 0:
            self._owner = None
            self._block.release()

    def _is_owned(self):
        return self._owner == get_ident()

    def _release_save(self):
        state = (self._count, self._owner)
        self._count = 0
        self._owner = None
        self._block.release()
        return state

    def _acquire_restore(self, state):
        self._block.acquire()
        self._count, self._owner = state

    __enter__ = acquire

    def __exit__(self, *exc):
        self.release()


class Condition:
    def __init__(self, lock=None):
        self._lock = lock if lock is not None else RLock()
        self._waiters = []

    def __enter__(self):
        return self._lock.__enter__()

    def __exit__(self, *exc):
        return self._lock.__exit__(*exc)

    def acquire(self, *args):
        return self._lock.acquire(*args)

    def release(self):
        self._lock.release()

    def _owned(self):
        if isinstance(self._lock, RLock):
            return self._lock._is_owned()
        return self._lock.locked()

    def wait(self, timeout=None):
        if not self._owned():
            raise RuntimeError("cannot wait on un-acquired lock")
        waiter = Lock()
        waiter.acquire()
        self._waiters.append(waiter)
        if isinstance(self._lock, RLock):
            saved = self._lock._release_save()
        else:
            saved = None
            self._lock.release()
        try:
            if timeout is None:
                got = waiter.acquire()
            else:
                got = waiter.acquire(True, timeout) if timeout > 0 else waiter.acquire(False)
            return got
        finally:
            if saved is not None:
                self._lock._acquire_restore(saved)
            else:
                self._lock.acquire()
            if not got:
                try:
                    self._waiters.remove(waiter)
                except ValueError:
                    pass

    def wait_for(self, predicate, timeout=None):
        end = None if timeout is None else _time.monotonic() + timeout
        result = predicate()
        while not result:
            if end is not None:
                left = end - _time.monotonic()
                if left <= 0:
                    break
                self.wait(left)
            else:
                self.wait()
            result = predicate()
        return result

    def notify(self, n=1):
        if not self._owned():
            raise RuntimeError("cannot notify on un-acquired lock")
        for w in self._waiters[:n]:
            self._waiters.remove(w)
            w.release()

    def notify_all(self):
        self.notify(len(self._waiters))

    notifyAll = notify_all


class Semaphore:
    def __init__(self, value=1):
        if value < 0:
            raise ValueError("semaphore initial value must be >= 0")
        self._cond = Condition(Lock())
        self._value = value

    def acquire(self, blocking=True, timeout=None):
        if not blocking and timeout is not None:
            raise ValueError("can't specify timeout for non-blocking acquire")
        with self._cond:
            if not blocking and self._value == 0:
                return False
            if not self._cond.wait_for(lambda: self._value > 0, timeout):
                return False
            self._value -= 1
            return True

    __enter__ = acquire

    def release(self, n=1):
        if n < 1:
            raise ValueError("n must be one or more")
        with self._cond:
            self._value += n
            self._cond.notify(n)

    def __exit__(self, *exc):
        self.release()


class BoundedSemaphore(Semaphore):
    def __init__(self, value=1):
        Semaphore.__init__(self, value)
        self._initial_value = value

    def release(self, n=1):
        if n < 1:
            raise ValueError("n must be one or more")
        with self._cond:
            if self._value + n > self._initial_value:
                raise ValueError("Semaphore released too many times")
            self._value += n
            self._cond.notify(n)


class Event:
    def __init__(self):
        self._cond = Condition(Lock())
        self._flag = False

    def is_set(self):
        return self._flag

    isSet = is_set

    def set(self):
        with self._cond:
            self._flag = True
            self._cond.notify_all()

    def clear(self):
        with self._cond:
            self._flag = False

    def wait(self, timeout=None):
        with self._cond:
            if not self._flag:
                self._cond.wait_for(lambda: self._flag, timeout)
            return self._flag


class BrokenBarrierError(RuntimeError):
    pass


class Barrier:
    def __init__(self, parties, action=None, timeout=None):
        if parties < 1:
            raise ValueError("parties must be >= 1")
        self._cond = Condition(Lock())
        self._parties = parties
        self._action = action
        self._timeout = timeout
        self._count = 0
        self._generation = 0
        self._broken = False

    @property
    def parties(self):
        return self._parties

    @property
    def n_waiting(self):
        return self._count

    @property
    def broken(self):
        return self._broken

    def wait(self, timeout=None):
        if timeout is None:
            timeout = self._timeout
        with self._cond:
            if self._broken:
                raise BrokenBarrierError
            gen = self._generation
            index = self._count
            self._count += 1
            if self._count == self._parties:
                try:
                    if self._action:
                        self._action()
                except BaseException:
                    self._break()
                    raise
                self._count = 0
                self._generation += 1
                self._cond.notify_all()
                return index
            if not self._cond.wait_for(lambda: self._generation != gen or self._broken, timeout):
                self._break()
            if self._broken:
                raise BrokenBarrierError
            return index

    def _break(self):
        self._broken = True
        self._cond.notify_all()

    def abort(self):
        with self._cond:
            self._break()

    def reset(self):
        with self._cond:
            self._broken = False
            self._count = 0
            self._generation += 1
            self._cond.notify_all()


class local:
    """Attributes per thread, kept in a dictionary keyed by the thread."""

    def __init__(self):
        object.__setattr__(self, "_local__d", {})

    def _local__ns(self):
        d = object.__getattribute__(self, "_local__d")
        me = get_ident()
        ns = d.get(me)
        if ns is None:
            ns = {}
            d[me] = ns
        return ns

    def __getattr__(self, name):
        ns = self._local__ns()
        if name in ns:
            return ns[name]
        raise AttributeError("'_thread._local' object has no attribute '%s'" % name)

    def __setattr__(self, name, value):
        self._local__ns()[name] = value

    def __delattr__(self, name):
        ns = self._local__ns()
        if name not in ns:
            raise AttributeError(name)
        del ns[name]


_active = {}
_counter = 0


def _newname(prefix="Thread-%d"):
    global _counter
    _counter += 1
    return prefix % _counter


class ExceptHookArgs:
    def __init__(self, exc_type, exc_value, exc_traceback, thread):
        self.exc_type = exc_type
        self.exc_value = exc_value
        self.exc_traceback = exc_traceback
        self.thread = thread


def excepthook(args):
    if args.exc_type is SystemExit:
        return
    import sys
    name = args.thread.name if args.thread is not None else get_ident()
    print("Exception in thread %s:" % name, file=sys.stderr)
    print("%s: %s" % (args.exc_type.__name__, args.exc_value), file=sys.stderr)


class Thread:
    def __init__(self, group=None, target=None, name=None, args=(), kwargs=None, *, daemon=None):
        self._target = target
        self._args = args
        self._kwargs = kwargs if kwargs is not None else {}
        if name is None:
            name = _newname()
            if target is not None:
                tname = getattr(target, "__name__", None)
                if tname:
                    name += " (%s)" % tname
        self._name = name
        self._daemonic = bool(daemon) if daemon is not None else False
        self._ident = None
        self._started = Event()
        self._done = Event()

    @property
    def name(self):
        return self._name

    @name.setter
    def name(self, value):
        self._name = str(value)

    def getName(self):
        return self._name

    def setName(self, value):
        self._name = str(value)

    @property
    def ident(self):
        return self._ident

    native_id = ident

    @property
    def daemon(self):
        return self._daemonic

    @daemon.setter
    def daemon(self, value):
        if self._started.is_set():
            raise RuntimeError("cannot set daemon status of active thread")
        self._daemonic = bool(value)

    def isDaemon(self):
        return self._daemonic

    def setDaemon(self, value):
        self.daemon = value

    def start(self):
        if self._started.is_set():
            raise RuntimeError("threads can only be started once")
        _thread.start_new_thread(self._bootstrap, ())
        self._started.wait()

    def _bootstrap(self):
        self._ident = get_ident()
        _active[self._ident] = self
        self._started.set()
        try:
            self.run()
        except BaseException as e:
            excepthook(ExceptHookArgs(type(e), e, None, self))
        finally:
            _active.pop(self._ident, None)
            self._done.set()

    def run(self):
        try:
            if self._target is not None:
                self._target(*self._args, **self._kwargs)
        finally:
            del self._target, self._args, self._kwargs

    def join(self, timeout=None):
        if not self._started.is_set():
            raise RuntimeError("cannot join thread before it is started")
        if self is current_thread():
            raise RuntimeError("cannot join current thread")
        self._done.wait(timeout)

    def is_alive(self):
        return self._started.is_set() and not self._done.is_set()

    def __repr__(self):
        status = "initial"
        if self._started.is_set():
            status = "started"
        if self._done.is_set():
            status = "stopped"
        if self._daemonic:
            status += " daemon"
        if self._ident is not None:
            status += " %s" % self._ident
        return "<%s(%s, %s)>" % (type(self).__name__, self._name, status)


class _MainThread(Thread):
    def __init__(self):
        Thread.__init__(self, name="MainThread")
        self._ident = get_ident()
        self._started.set()
        _active[self._ident] = self


class _DummyThread(Thread):
    def __init__(self):
        Thread.__init__(self, name=_newname("Dummy-%d"), daemon=True)
        self._ident = get_ident()
        self._started.set()

    def is_alive(self):
        return True

    def join(self, timeout=None):
        raise RuntimeError("cannot join a dummy thread")


_main = _MainThread()


def current_thread():
    t = _active.get(get_ident())
    if t is None:
        t = _DummyThread()
    return t


currentThread = current_thread


def main_thread():
    return _main


def active_count():
    return len(_active)


activeCount = active_count


def enumerate():
    return list(_active.values())


class Timer(Thread):
    def __init__(self, interval, function, args=None, kwargs=None):
        Thread.__init__(self)
        self.interval = interval
        self.function = function
        self.args = args if args is not None else []
        self.kwargs = kwargs if kwargs is not None else {}
        self.finished = Event()

    def cancel(self):
        self.finished.set()

    def run(self):
        self.finished.wait(self.interval)
        if not self.finished.is_set():
            self.function(*self.args, **self.kwargs)
        self.finished.set()

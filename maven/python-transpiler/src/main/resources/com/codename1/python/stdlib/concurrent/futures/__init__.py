# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Futures and executors over threading.

ProcessPoolExecutor runs its calls on threads: an application is one
process on a device, so it is a ThreadPoolExecutor under the process
pool's name, and its calls take turns at the interpreter lock.
"""

import threading
import time
import queue as _queue

__all__ = ["Future", "Executor", "ThreadPoolExecutor", "ProcessPoolExecutor", "wait",
           "as_completed", "CancelledError", "TimeoutError", "InvalidStateError",
           "BrokenExecutor", "FIRST_COMPLETED", "FIRST_EXCEPTION", "ALL_COMPLETED"]

FIRST_COMPLETED = "FIRST_COMPLETED"
FIRST_EXCEPTION = "FIRST_EXCEPTION"
ALL_COMPLETED = "ALL_COMPLETED"

_PENDING = "PENDING"
_RUNNING = "RUNNING"
_CANCELLED = "CANCELLED"
_FINISHED = "FINISHED"


class Error(Exception):
    pass


class CancelledError(Error):
    pass


TimeoutError = TimeoutError


class InvalidStateError(Error):
    pass


class BrokenExecutor(RuntimeError):
    pass


class Future:
    def __init__(self):
        self._condition = threading.Condition()
        self._state = _PENDING
        self._result = None
        self._exception = None
        self._callbacks = []
        self._waiters = []

    def __repr__(self):
        with self._condition:
            if self._state == _FINISHED:
                if self._exception is not None:
                    return "<Future at 0x%x state=finished raised %s>" % (id(self), type(self._exception).__name__)
                return "<Future at 0x%x state=finished returned %s>" % (id(self), type(self._result).__name__)
            return "<Future at 0x%x state=%s>" % (id(self), self._state.lower())

    def _invoke_callbacks(self):
        for cb in self._callbacks:
            try:
                cb(self)
            except Exception:
                pass

    def cancel(self):
        with self._condition:
            if self._state in (_RUNNING, _FINISHED):
                return False
            if self._state == _CANCELLED:
                return True
            self._state = _CANCELLED
            self._condition.notify_all()
            self._notify_waiters()
        self._invoke_callbacks()
        return True

    def cancelled(self):
        return self._state == _CANCELLED

    def running(self):
        return self._state == _RUNNING

    def done(self):
        return self._state in (_CANCELLED, _FINISHED)

    def _get(self):
        if self._state == _CANCELLED:
            raise CancelledError()
        if self._exception is not None:
            raise self._exception
        return self._result

    def result(self, timeout=None):
        with self._condition:
            if self.done():
                return self._get()
            self._condition.wait(timeout)
            if self.done():
                return self._get()
            raise TimeoutError()

    def exception(self, timeout=None):
        with self._condition:
            if not self.done():
                self._condition.wait(timeout)
            if self._state == _CANCELLED:
                raise CancelledError()
            if not self.done():
                raise TimeoutError()
            return self._exception

    def add_done_callback(self, fn):
        with self._condition:
            if not self.done():
                self._callbacks.append(fn)
                return
        try:
            fn(self)
        except Exception:
            pass

    def set_running_or_notify_cancel(self):
        with self._condition:
            if self._state == _CANCELLED:
                return False
            if self._state == _PENDING:
                self._state = _RUNNING
                return True
            raise RuntimeError("Future in unexpected state")

    def set_result(self, result):
        with self._condition:
            if self.done():
                raise InvalidStateError("%s: %r" % (self._state, self))
            self._result = result
            self._state = _FINISHED
            self._condition.notify_all()
            self._notify_waiters()
        self._invoke_callbacks()

    def set_exception(self, exception):
        with self._condition:
            if self.done():
                raise InvalidStateError("%s: %r" % (self._state, self))
            self._exception = exception
            self._state = _FINISHED
            self._condition.notify_all()
            self._notify_waiters()
        self._invoke_callbacks()

    def _notify_waiters(self):
        for w in self._waiters:
            w.put(self)


class Executor:
    def submit(self, fn, /, *args, **kwargs):
        raise NotImplementedError()

    def map(self, fn, *iterables, timeout=None, chunksize=1):
        end = None if timeout is None else time.monotonic() + timeout
        fs = [self.submit(fn, *args) for args in zip(*iterables)]

        def results():
            try:
                fs.reverse()
                while fs:
                    f = fs.pop()
                    if end is None:
                        yield f.result()
                    else:
                        yield f.result(end - time.monotonic())
            finally:
                for f in fs:
                    f.cancel()
        return results()

    def shutdown(self, wait=True, *, cancel_futures=False):
        pass

    def __enter__(self):
        return self

    def __exit__(self, *exc):
        self.shutdown(wait=True)
        return False


class _WorkItem:
    def __init__(self, future, fn, args, kwargs):
        self.future = future
        self.fn = fn
        self.args = args
        self.kwargs = kwargs

    def run(self):
        if not self.future.set_running_or_notify_cancel():
            return
        try:
            result = self.fn(*self.args, **self.kwargs)
        except BaseException as exc:
            self.future.set_exception(exc)
        else:
            self.future.set_result(result)


class ThreadPoolExecutor(Executor):
    _counter = 0

    def __init__(self, max_workers=None, thread_name_prefix="", initializer=None, initargs=()):
        if max_workers is None:
            max_workers = 8
        if max_workers <= 0:
            raise ValueError("max_workers must be greater than 0")
        ThreadPoolExecutor._counter += 1
        self._max_workers = max_workers
        self._work = _queue.SimpleQueue()
        self._threads = []
        self._idle = threading.Semaphore(0)
        self._shutdown = False
        self._lock = threading.Lock()
        self._prefix = thread_name_prefix or "ThreadPoolExecutor-%d" % (ThreadPoolExecutor._counter - 1)
        self._initializer = initializer
        self._initargs = initargs

    def submit(self, fn, /, *args, **kwargs):
        with self._lock:
            if self._shutdown:
                raise RuntimeError("cannot schedule new futures after shutdown")
            f = Future()
            self._work.put(_WorkItem(f, fn, args, kwargs))
            self._adjust()
            return f

    def _adjust(self):
        if self._idle.acquire(False):
            return
        if len(self._threads) < self._max_workers:
            t = threading.Thread(name="%s_%d" % (self._prefix, len(self._threads)), target=self._worker)
            t.daemon = True
            t.start()
            self._threads.append(t)

    def _worker(self):
        if self._initializer is not None:
            self._initializer(*self._initargs)
        while True:
            item = self._work.get()
            if item is None:
                self._work.put(None)
                return
            item.run()
            self._idle.release()

    def shutdown(self, wait=True, *, cancel_futures=False):
        with self._lock:
            self._shutdown = True
            if cancel_futures:
                while True:
                    try:
                        item = self._work.get_nowait()
                    except _queue.Empty:
                        break
                    if item is not None:
                        item.future.cancel()
            self._work.put(None)
        if wait:
            for t in self._threads:
                t.join()


class ProcessPoolExecutor(ThreadPoolExecutor):
    def __init__(self, max_workers=None, mp_context=None, initializer=None, initargs=(), **kw):
        ThreadPoolExecutor.__init__(self, max_workers, "ProcessPoolExecutor", initializer, initargs)


def wait(fs, timeout=None, return_when=ALL_COMPLETED):
    fs = set(fs)
    end = None if timeout is None else time.monotonic() + timeout
    q = _queue.SimpleQueue()
    for f in fs:
        with f._condition:
            f._waiters.append(q)
    try:
        while True:
            done = set(f for f in fs if f.done())
            if return_when == FIRST_COMPLETED and done:
                break
            if return_when == FIRST_EXCEPTION and any(
                    not f.cancelled() and f._exception is not None for f in done):
                break
            if len(done) == len(fs):
                break
            left = None if end is None else end - time.monotonic()
            if left is not None and left <= 0:
                break
            try:
                q.get(True, left)
            except _queue.Empty:
                break
    finally:
        for f in fs:
            with f._condition:
                f._waiters.remove(q)
    done = set(f for f in fs if f.done())
    return DoneAndNotDoneFutures(done, fs - done)


class DoneAndNotDoneFutures(tuple):
    def __new__(cls, done, not_done):
        return tuple.__new__(cls, (done, not_done))

    @property
    def done(self):
        return self[0]

    @property
    def not_done(self):
        return self[1]


def as_completed(fs, timeout=None):
    fs = list(dict.fromkeys(fs))
    end = None if timeout is None else time.monotonic() + timeout
    q = _queue.SimpleQueue()
    pending = []
    for f in fs:
        with f._condition:
            if f.done():
                q.put(f)
            else:
                f._waiters.append(q)
                pending.append(f)
    try:
        for _ in range(len(fs)):
            left = None if end is None else end - time.monotonic()
            try:
                yield q.get(True, left) if left is None or left > 0 else q.get(False)
            except _queue.Empty:
                raise TimeoutError("%d (of %d) futures unfinished" % (len(fs) - _, len(fs)))
    finally:
        for f in pending:
            with f._condition:
                if q in f._waiters:
                    f._waiters.remove(q)

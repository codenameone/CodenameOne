# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Coroutines on a single-threaded event loop.

The loop runs callbacks, timers and tasks in the thread that called run().
There is no selector: a loop with nothing ready sleeps until its next
timer. Networking, subprocesses and executors are not part of this module.
"""

import time as _time
from heapq import heappush as _heappush, heappop as _heappop

__all__ = [
    "AbstractEventLoop", "BaseEventLoop", "CancelledError", "Condition", "Event", "Future",
    "InvalidStateError", "Lock", "Queue", "QueueEmpty", "QueueFull", "Semaphore", "BoundedSemaphore",
    "Task", "TimeoutError", "all_tasks", "as_completed", "create_task", "current_task", "ensure_future",
    "gather", "get_event_loop", "get_running_loop", "iscoroutine", "iscoroutinefunction", "new_event_loop",
    "run", "set_event_loop", "shield", "sleep", "wait", "wait_for", "FIRST_COMPLETED", "FIRST_EXCEPTION",
    "ALL_COMPLETED", "timeout",
]

FIRST_COMPLETED = "FIRST_COMPLETED"
FIRST_EXCEPTION = "FIRST_EXCEPTION"
ALL_COMPLETED = "ALL_COMPLETED"


class CancelledError(BaseException):
    pass


class InvalidStateError(Exception):
    pass


class QueueEmpty(Exception):
    pass


class QueueFull(Exception):
    pass


_PENDING = "PENDING"
_CANCELLED = "CANCELLED"
_FINISHED = "FINISHED"


def iscoroutine(obj):
    return type(obj).__name__ == "coroutine"


def iscoroutinefunction(func):
    return False


# ---- futures --------------------------------------------------------------

class Future:
    _asyncio_future_blocking = False

    def __init__(self, *, loop=None):
        self._loop = loop if loop is not None else get_event_loop()
        self._state = _PENDING
        self._result = None
        self._exception = None
        self._callbacks = []
        self._cancel_message = None

    def get_loop(self):
        return self._loop

    def done(self):
        return self._state != _PENDING

    def cancelled(self):
        return self._state == _CANCELLED

    def cancel(self, msg=None):
        if self._state != _PENDING:
            return False
        self._state = _CANCELLED
        self._cancel_message = msg
        self._schedule_callbacks()
        return True

    def result(self):
        if self._state == _CANCELLED:
            raise self._make_cancelled_error()
        if self._state != _FINISHED:
            raise InvalidStateError("Result is not ready.")
        if self._exception is not None:
            raise self._exception
        return self._result

    def exception(self):
        if self._state == _CANCELLED:
            raise self._make_cancelled_error()
        if self._state != _FINISHED:
            raise InvalidStateError("Exception is not set.")
        return self._exception

    def set_result(self, result):
        if self._state != _PENDING:
            raise InvalidStateError("invalid state")
        self._result = result
        self._state = _FINISHED
        self._schedule_callbacks()

    def set_exception(self, exception):
        if self._state != _PENDING:
            raise InvalidStateError("invalid state")
        if isinstance(exception, type):
            exception = exception()
        self._exception = exception
        self._state = _FINISHED
        self._schedule_callbacks()

    def add_done_callback(self, fn, *, context=None):
        if self._state != _PENDING:
            self._loop.call_soon(fn, self)
        else:
            self._callbacks.append(fn)

    def remove_done_callback(self, fn):
        kept = [f for f in self._callbacks if f != fn]
        removed = len(self._callbacks) - len(kept)
        self._callbacks = kept
        return removed

    def _schedule_callbacks(self):
        callbacks = self._callbacks
        self._callbacks = []
        for fn in callbacks:
            self._loop.call_soon(fn, self)

    def _make_cancelled_error(self):
        if self._cancel_message is None:
            return CancelledError()
        return CancelledError(self._cancel_message)

    def __await__(self):
        if not self.done():
            self._asyncio_future_blocking = True
            yield self
        if not self.done():
            raise RuntimeError("await wasn't used with future")
        return self.result()

    __iter__ = __await__

    def __repr__(self):
        return "<Future " + self._state.lower() + ">"


class _Yield:
    """`await _Yield()` gives every other ready callback one turn."""

    def __await__(self):
        yield


_current_tasks = {}
_all_tasks = []


class Task(Future):
    _counter = 0

    def __init__(self, coro, *, loop=None, name=None, context=None):
        Future.__init__(self, loop=loop)
        if not iscoroutine(coro):
            raise TypeError("a coroutine was expected, got " + repr(coro))
        self._coro = coro
        self._fut_waiter = None
        self._must_cancel = False
        Task._counter += 1
        self._name = name if name is not None else "Task-" + str(Task._counter)
        self._loop.call_soon(self._step)
        _all_tasks.append(self)

    def get_name(self):
        return self._name

    def set_name(self, value):
        self._name = str(value)

    def get_coro(self):
        return self._coro

    def cancel(self, msg=None):
        if self.done():
            return False
        if self._fut_waiter is not None:
            if self._fut_waiter.cancel(msg):
                return True
        self._must_cancel = True
        self._cancel_message = msg
        return True

    def _step(self, exc=None):
        if self.done():
            return
        if self._must_cancel:
            if not isinstance(exc, CancelledError):
                exc = self._make_cancelled_error()
            self._must_cancel = False
        self._fut_waiter = None
        coro = self._coro
        _current_tasks[id(self._loop)] = self
        try:
            if exc is None:
                result = coro.send(None)
            else:
                result = coro.throw(exc)
        except StopIteration as e:
            if self._must_cancel:
                self._must_cancel = False
                Future.cancel(self, self._cancel_message)
            else:
                Future.set_result(self, e.value)
        except CancelledError as e:
            self._cancel_message = e.args[0] if e.args else None
            Future.cancel(self, self._cancel_message)
        except (KeyboardInterrupt, SystemExit) as e:
            Future.set_exception(self, e)
            raise
        except BaseException as e:
            Future.set_exception(self, e)
        else:
            if getattr(result, "_asyncio_future_blocking", False):
                result._asyncio_future_blocking = False
                result.add_done_callback(self._wakeup)
                self._fut_waiter = result
                if self._must_cancel:
                    if self._fut_waiter.cancel(self._cancel_message):
                        self._must_cancel = False
            elif result is None:
                self._loop.call_soon(self._step)
            else:
                self._loop.call_soon(self._step, RuntimeError("Task got bad yield: " + repr(result)))
        finally:
            _current_tasks.pop(id(self._loop), None)
            if self.done() and self in _all_tasks:
                _all_tasks.remove(self)

    def _wakeup(self, future):
        try:
            future.result()
        except BaseException as e:
            self._step(e)
        else:
            self._step()

    def __repr__(self):
        return "<Task " + self._state.lower() + " name='" + self._name + "'>"


# ---- the loop -------------------------------------------------------------

class Handle:
    def __init__(self, callback, args, loop):
        self._callback = callback
        self._args = args
        self._loop = loop
        self._cancelled = False

    def cancel(self):
        self._cancelled = True

    def cancelled(self):
        return self._cancelled

    def _run(self):
        self._callback(*self._args)


class TimerHandle(Handle):
    def __init__(self, when, callback, args, loop):
        Handle.__init__(self, callback, args, loop)
        self._when = when

    def when(self):
        return self._when


class AbstractEventLoop:
    pass


class BaseEventLoop(AbstractEventLoop):
    def __init__(self):
        self._ready = []
        self._scheduled = []
        self._seq = 0
        self._stopping = False
        self._running = False
        self._closed = False

    def time(self):
        return _time.monotonic()

    def call_soon(self, callback, *args, context=None):
        h = Handle(callback, args, self)
        self._ready.append(h)
        return h

    call_soon_threadsafe = call_soon

    def call_later(self, delay, callback, *args, context=None):
        return self.call_at(self.time() + delay, callback, *args)

    def call_at(self, when, callback, *args, context=None):
        h = TimerHandle(when, callback, args, self)
        self._seq += 1
        _heappush(self._scheduled, (when, self._seq, h))
        return h

    def create_future(self):
        return Future(loop=self)

    def create_task(self, coro, *, name=None, context=None):
        return Task(coro, loop=self, name=name)

    def is_running(self):
        return self._running

    def is_closed(self):
        return self._closed

    def stop(self):
        self._stopping = True

    def close(self):
        if self._running:
            raise RuntimeError("Cannot close a running event loop")
        self._closed = True
        self._ready = []
        self._scheduled = []

    def run_forever(self):
        if self._running:
            raise RuntimeError("This event loop is already running")
        if self._closed:
            raise RuntimeError("Event loop is closed")
        global _running_loop
        previous = _running_loop
        _running_loop = self
        self._running = True
        try:
            while True:
                self._run_once()
                if self._stopping:
                    break
        finally:
            self._stopping = False
            self._running = False
            _running_loop = previous

    def run_until_complete(self, future):
        future = ensure_future(future, loop=self)

        def done(f):
            self.stop()

        future.add_done_callback(done)
        self.run_forever()
        if not future.done():
            raise RuntimeError("Event loop stopped before Future completed.")
        return future.result()

    def _run_once(self):
        scheduled = self._scheduled
        while scheduled and scheduled[0][2]._cancelled:
            _heappop(scheduled)
        if not self._ready and scheduled and not self._stopping:
            wait = scheduled[0][0] - self.time()
            if wait > 0:
                _time.sleep(wait)
        now = self.time()
        while scheduled and scheduled[0][0] <= now:
            h = _heappop(scheduled)[2]
            if not h._cancelled:
                self._ready.append(h)
        ready = self._ready
        self._ready = []
        for h in ready:
            if not h._cancelled:
                h._run()


_running_loop = None
_event_loop = None


def get_running_loop():
    if _running_loop is None:
        raise RuntimeError("no running event loop")
    return _running_loop


def get_event_loop():
    global _event_loop
    if _running_loop is not None:
        return _running_loop
    if _event_loop is None or _event_loop.is_closed():
        _event_loop = BaseEventLoop()
    return _event_loop


def new_event_loop():
    return BaseEventLoop()


def set_event_loop(loop):
    global _event_loop
    _event_loop = loop


def current_task(loop=None):
    if loop is None:
        loop = get_running_loop()
    return _current_tasks.get(id(loop))


def all_tasks(loop=None):
    if loop is None:
        loop = get_running_loop()
    return set(t for t in _all_tasks if t._loop is loop and not t.done())


def ensure_future(coro_or_future, *, loop=None):
    if isinstance(coro_or_future, Future):
        return coro_or_future
    if loop is None:
        loop = get_event_loop()
    if iscoroutine(coro_or_future):
        return loop.create_task(coro_or_future)
    if hasattr(coro_or_future, "__await__"):
        return loop.create_task(_wrap_awaitable(coro_or_future))
    raise TypeError("An asyncio.Future, a coroutine or an awaitable is required")


async def _wrap_awaitable(awaitable):
    return await awaitable


def create_task(coro, *, name=None, context=None):
    return get_running_loop().create_task(coro, name=name)


def run(main, *, debug=None):
    if _running_loop is not None:
        raise RuntimeError("asyncio.run() cannot be called from a running event loop")
    if not iscoroutine(main):
        raise ValueError("a coroutine was expected, got " + repr(main))
    loop = new_event_loop()
    set_event_loop(loop)
    try:
        return loop.run_until_complete(main)
    finally:
        try:
            pending = [t for t in _all_tasks if t._loop is loop and not t.done()]
            for t in pending:
                t.cancel()
            if pending:
                loop.run_until_complete(gather(*pending, return_exceptions=True))
        finally:
            set_event_loop(None)
            loop.close()


async def sleep(delay, result=None):
    if delay <= 0:
        await _Yield()
        return result
    loop = get_running_loop()
    future = loop.create_future()
    h = loop.call_later(delay, _set_result_unless_cancelled, future, result)
    try:
        return await future
    finally:
        h.cancel()


def _set_result_unless_cancelled(fut, result):
    if not fut.cancelled():
        fut.set_result(result)


def gather(*coros_or_futures, return_exceptions=False):
    loop = get_event_loop() if _running_loop is None else _running_loop
    children = [ensure_future(c, loop=loop) for c in coros_or_futures]
    outer = loop.create_future()
    if not children:
        outer.set_result([])
        return outer
    state = [0]

    def child_done(fut):
        state[0] += 1
        if outer.done():
            return
        if not return_exceptions:
            if fut.cancelled():
                outer.set_exception(fut._make_cancelled_error())
                return
            if fut.exception() is not None:
                outer.set_exception(fut.exception())
                return
        if state[0] == len(children):
            results = []
            for c in children:
                if c.cancelled():
                    results.append(c._make_cancelled_error())
                elif c.exception() is not None:
                    results.append(c.exception())
                else:
                    results.append(c.result())
            outer.set_result(results)

    for c in children:
        c.add_done_callback(child_done)
    return outer


def shield(arg):
    inner = ensure_future(arg)
    if inner.done():
        return inner
    outer = inner._loop.create_future()

    def done(f):
        if outer.cancelled():
            return
        if f.cancelled():
            outer.cancel()
        elif f.exception() is not None:
            outer.set_exception(f.exception())
        else:
            outer.set_result(f.result())

    inner.add_done_callback(done)
    return outer


async def wait_for(fut, timeout):
    fut = ensure_future(fut)
    if timeout is None:
        return await fut
    if fut.done():
        return fut.result()
    loop = get_running_loop()
    timed_out = [False]

    def expire():
        timed_out[0] = True
        fut.cancel()

    h = loop.call_later(timeout, expire)
    try:
        return await fut
    except CancelledError:
        if timed_out[0]:
            raise TimeoutError()
        raise
    finally:
        h.cancel()


async def wait(fs, *, timeout=None, return_when=ALL_COMPLETED):
    fs = set(ensure_future(f) for f in fs)
    if not fs:
        raise ValueError("Set of Tasks/Futures is empty.")
    loop = get_running_loop()
    waiter = loop.create_future()
    counter = [len(fs)]

    def check(f):
        counter[0] -= 1
        if waiter.done():
            return
        if (counter[0] <= 0 or return_when == FIRST_COMPLETED
                or return_when == FIRST_EXCEPTION and not f.cancelled() and f.exception() is not None):
            waiter.set_result(None)

    h = None
    if timeout is not None:
        h = loop.call_later(timeout, _set_result_unless_cancelled, waiter, None)
    for f in fs:
        f.add_done_callback(check)
    try:
        await waiter
    finally:
        if h is not None:
            h.cancel()
        for f in fs:
            f.remove_done_callback(check)
    done = set(f for f in fs if f.done())
    return done, fs - done


def as_completed(fs, *, timeout=None):
    todo = [ensure_future(f) for f in fs]
    loop = get_event_loop() if _running_loop is None else _running_loop
    queue = Queue()

    def on_done(f):
        queue.put_nowait(f)

    for f in todo:
        f.add_done_callback(on_done)

    async def next_done():
        f = await queue.get()
        return f.result()

    return [next_done() for _ in todo]


class timeout:
    """`async with asyncio.timeout(delay):` cancels the block after `delay`."""

    def __init__(self, delay):
        self._delay = delay
        self._handle = None
        self._task = None
        self._expired = False

    def reschedule(self, when):
        if self._handle is not None:
            self._handle.cancel()
            self._handle = None
        if when is not None:
            loop = get_running_loop()
            self._handle = loop.call_at(when, self._on_timeout)

    def expired(self):
        return self._expired

    def _on_timeout(self):
        self._expired = True
        self._task.cancel()

    async def __aenter__(self):
        self._task = current_task()
        if self._delay is not None:
            self._handle = get_running_loop().call_later(self._delay, self._on_timeout)
        return self

    async def __aexit__(self, exc_type, exc, tb):
        if self._handle is not None:
            self._handle.cancel()
            self._handle = None
        if self._expired and exc_type is CancelledError:
            raise TimeoutError()
        return False


# ---- synchronization primitives --------------------------------------------

class Event:
    def __init__(self):
        self._waiters = []
        self._value = False

    def is_set(self):
        return self._value

    def set(self):
        if not self._value:
            self._value = True
            for fut in self._waiters:
                if not fut.done():
                    fut.set_result(True)

    def clear(self):
        self._value = False

    async def wait(self):
        if self._value:
            return True
        fut = get_running_loop().create_future()
        self._waiters.append(fut)
        try:
            await fut
            return True
        finally:
            self._waiters.remove(fut)


class Lock:
    def __init__(self):
        self._waiters = []
        self._locked = False

    def locked(self):
        return self._locked

    async def acquire(self):
        if not self._locked and not self._waiters:
            self._locked = True
            return True
        fut = get_running_loop().create_future()
        self._waiters.append(fut)
        try:
            await fut
        finally:
            self._waiters.remove(fut)
        self._locked = True
        return True

    def release(self):
        if not self._locked:
            raise RuntimeError("Lock is not acquired.")
        self._locked = False
        for fut in self._waiters:
            if not fut.done():
                fut.set_result(True)
                break

    async def __aenter__(self):
        await self.acquire()
        return None

    async def __aexit__(self, exc_type, exc, tb):
        self.release()
        return False


class Condition:
    def __init__(self, lock=None):
        self._lock = lock if lock is not None else Lock()
        self._waiters = []

    def locked(self):
        return self._lock.locked()

    async def acquire(self):
        return await self._lock.acquire()

    def release(self):
        self._lock.release()

    async def __aenter__(self):
        await self._lock.acquire()
        return None

    async def __aexit__(self, exc_type, exc, tb):
        self._lock.release()
        return False

    async def wait(self):
        fut = get_running_loop().create_future()
        self._waiters.append(fut)
        self._lock.release()
        try:
            await fut
            return True
        finally:
            self._waiters.remove(fut)
            await self._lock.acquire()

    async def wait_for(self, predicate):
        result = predicate()
        while not result:
            await self.wait()
            result = predicate()
        return result

    def notify(self, n=1):
        count = 0
        for fut in self._waiters:
            if count >= n:
                break
            if not fut.done():
                count += 1
                fut.set_result(False)

    def notify_all(self):
        self.notify(len(self._waiters))


class Semaphore:
    def __init__(self, value=1):
        if value < 0:
            raise ValueError("Semaphore initial value must be >= 0")
        self._value = value
        self._waiters = []

    def locked(self):
        return self._value == 0

    async def acquire(self):
        while self._value <= 0:
            fut = get_running_loop().create_future()
            self._waiters.append(fut)
            try:
                await fut
            finally:
                self._waiters.remove(fut)
        self._value -= 1
        return True

    def release(self):
        self._value += 1
        for fut in self._waiters:
            if not fut.done():
                fut.set_result(True)
                break

    async def __aenter__(self):
        await self.acquire()
        return None

    async def __aexit__(self, exc_type, exc, tb):
        self.release()
        return False


class BoundedSemaphore(Semaphore):
    def __init__(self, value=1):
        Semaphore.__init__(self, value)
        self._bound_value = value

    def release(self):
        if self._value >= self._bound_value:
            raise ValueError("BoundedSemaphore released too many times")
        Semaphore.release(self)


class Queue:
    def __init__(self, maxsize=0):
        self._maxsize = maxsize
        self._items = []
        self._head = 0
        self._getters = []
        self._putters = []
        self._unfinished = 0
        self._finished = Event()
        self._finished.set()

    @property
    def maxsize(self):
        return self._maxsize

    def qsize(self):
        return len(self._items) - self._head

    def empty(self):
        return self.qsize() == 0

    def full(self):
        if self._maxsize <= 0:
            return False
        return self.qsize() >= self._maxsize

    def _wake(self, waiters):
        while waiters:
            fut = waiters.pop(0)
            if not fut.done():
                fut.set_result(None)
                break

    def put_nowait(self, item):
        if self.full():
            raise QueueFull
        self._items.append(item)
        self._unfinished += 1
        self._finished.clear()
        self._wake(self._getters)

    def get_nowait(self):
        if self.empty():
            raise QueueEmpty
        item = self._items[self._head]
        self._items[self._head] = None
        self._head += 1
        if self._head > 32 and self._head * 2 > len(self._items):
            self._items = self._items[self._head:]
            self._head = 0
        self._wake(self._putters)
        return item

    async def put(self, item):
        while self.full():
            fut = get_running_loop().create_future()
            self._putters.append(fut)
            try:
                await fut
            except BaseException:
                fut.cancel()
                if fut in self._putters:
                    self._putters.remove(fut)
                raise
        self.put_nowait(item)

    async def get(self):
        while self.empty():
            fut = get_running_loop().create_future()
            self._getters.append(fut)
            try:
                await fut
            except BaseException:
                fut.cancel()
                if fut in self._getters:
                    self._getters.remove(fut)
                raise
        return self.get_nowait()

    def task_done(self):
        if self._unfinished <= 0:
            raise ValueError("task_done() called too many times")
        self._unfinished -= 1
        if self._unfinished == 0:
            self._finished.set()

    async def join(self):
        if self._unfinished > 0:
            await self._finished.wait()

# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Utilities for with-statement contexts."""

from functools import wraps
from collections import deque


class AbstractContextManager:
    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_value, traceback):
        return None


class AbstractAsyncContextManager:
    async def __aenter__(self):
        return self

    async def __aexit__(self, exc_type, exc_value, traceback):
        return None


class ContextDecorator:
    def _recreate_cm(self):
        return self

    def __call__(self, func):
        @wraps(func)
        def inner(*args, **kwds):
            with self._recreate_cm():
                return func(*args, **kwds)
        return inner


class _GeneratorContextManager(ContextDecorator, AbstractContextManager):
    def __init__(self, func, args, kwds):
        self.gen = func(*args, **kwds)
        self.func, self.args, self.kwds = func, args, kwds

    def _recreate_cm(self):
        return self.__class__(self.func, self.args, self.kwds)

    def __enter__(self):
        try:
            return next(self.gen)
        except StopIteration:
            raise RuntimeError("generator didn't yield") from None

    def __exit__(self, typ, value, traceback):
        if typ is None:
            try:
                next(self.gen)
            except StopIteration:
                return False
            raise RuntimeError("generator didn't stop")
        if value is None:
            value = typ()
        try:
            self.gen.throw(value)
        except StopIteration as exc:
            return exc is not value
        except RuntimeError as exc:
            if exc is value:
                return False
            if isinstance(value, StopIteration) and exc.__cause__ is value:
                return False
            raise
        except BaseException as exc:
            if exc is not value:
                raise
            return False
        raise RuntimeError("generator didn't stop after throw()")


class _AsyncGeneratorContextManager(AbstractAsyncContextManager):
    def __init__(self, func, args, kwds):
        self.gen = func(*args, **kwds)

    async def __aenter__(self):
        try:
            return await self.gen.__anext__()
        except StopAsyncIteration:
            raise RuntimeError("generator didn't yield") from None

    async def __aexit__(self, typ, value, traceback):
        if typ is None:
            try:
                await self.gen.__anext__()
            except StopAsyncIteration:
                return False
            raise RuntimeError("generator didn't stop")
        if value is None:
            value = typ()
        try:
            await self.gen.athrow(value)
        except StopAsyncIteration as exc:
            return exc is not value
        except BaseException as exc:
            if exc is not value:
                raise
            return False
        raise RuntimeError("generator didn't stop after athrow()")


def contextmanager(func):
    @wraps(func)
    def helper(*args, **kwds):
        return _GeneratorContextManager(func, args, kwds)
    return helper


def asynccontextmanager(func):
    @wraps(func)
    def helper(*args, **kwds):
        return _AsyncGeneratorContextManager(func, args, kwds)
    return helper


class closing(AbstractContextManager):
    def __init__(self, thing):
        self.thing = thing

    def __enter__(self):
        return self.thing

    def __exit__(self, *exc_info):
        self.thing.close()


class aclosing(AbstractAsyncContextManager):
    def __init__(self, thing):
        self.thing = thing

    async def __aenter__(self):
        return self.thing

    async def __aexit__(self, *exc_info):
        await self.thing.aclose()


class suppress(AbstractContextManager):
    def __init__(self, *exceptions):
        self._exceptions = exceptions

    def __enter__(self):
        return None

    def __exit__(self, exctype, excinst, exctb):
        if exctype is None:
            return False
        if issubclass(exctype, self._exceptions):
            return True
        if isinstance(excinst, BaseExceptionGroup):
            match, rest = excinst.split(self._exceptions)
            if rest is None:
                return True
            raise rest
        return False


class nullcontext(AbstractContextManager, AbstractAsyncContextManager):
    def __init__(self, enter_result=None):
        self.enter_result = enter_result

    def __enter__(self):
        return self.enter_result

    def __exit__(self, *excinfo):
        return None

    async def __aenter__(self):
        return self.enter_result

    async def __aexit__(self, *excinfo):
        return None


class _RedirectStream(AbstractContextManager):
    _stream = None

    def __init__(self, new_target):
        self._new_target = new_target
        self._old_targets = []

    def __enter__(self):
        import sys
        self._old_targets.append(getattr(sys, self._stream))
        setattr(sys, self._stream, self._new_target)
        return self._new_target

    def __exit__(self, exctype, excinst, exctb):
        import sys
        setattr(sys, self._stream, self._old_targets.pop())


class redirect_stdout(_RedirectStream):
    _stream = "stdout"


class redirect_stderr(_RedirectStream):
    _stream = "stderr"


class ExitStack(AbstractContextManager):
    def __init__(self):
        self._exit_callbacks = deque()

    def pop_all(self):
        new_stack = type(self)()
        new_stack._exit_callbacks = self._exit_callbacks
        self._exit_callbacks = deque()
        return new_stack

    def push(self, exit):
        if hasattr(exit, "__exit__"):
            cm = exit
            self._exit_callbacks.append(lambda t, v, tb: cm.__exit__(t, v, tb))
        else:
            self._exit_callbacks.append(exit)
        return exit

    def enter_context(self, cm):
        result = cm.__enter__()
        self._exit_callbacks.append(lambda t, v, tb: cm.__exit__(t, v, tb))
        return result

    def callback(self, callback, /, *args, **kwds):
        def _exit_wrapper(exc_type, exc, tb):
            callback(*args, **kwds)
        self._exit_callbacks.append(_exit_wrapper)
        return callback

    def close(self):
        self.__exit__(None, None, None)

    def __exit__(self, *exc_details):
        received_exc = exc_details[0] is not None
        pending_raise = False
        suppressed = False
        exc = exc_details[1]
        while self._exit_callbacks:
            cb = self._exit_callbacks.pop()
            try:
                if exc is None:
                    details = (None, None, None)
                else:
                    details = (type(exc), exc, None)
                if cb(*details):
                    suppressed = True
                    pending_raise = False
                    exc = None
            except BaseException as new_exc:
                if exc is not None and new_exc is not exc:
                    new_exc.__context__ = exc
                pending_raise = True
                exc = new_exc
        if pending_raise:
            raise exc
        return received_exc and suppressed


class AsyncExitStack(AbstractAsyncContextManager):
    def __init__(self):
        self._exit_callbacks = deque()

    async def enter_async_context(self, cm):
        result = await cm.__aenter__()
        self._exit_callbacks.append((True, cm))
        return result

    def enter_context(self, cm):
        result = cm.__enter__()
        self._exit_callbacks.append((False, cm))
        return result

    def push_async_callback(self, callback, /, *args, **kwds):
        self._exit_callbacks.append((True, (callback, args, kwds)))
        return callback

    def callback(self, callback, /, *args, **kwds):
        self._exit_callbacks.append((False, (callback, args, kwds)))
        return callback

    async def aclose(self):
        await self.__aexit__(None, None, None)

    async def __aexit__(self, *exc_details):
        received_exc = exc_details[0] is not None
        suppressed = False
        pending_raise = False
        exc = exc_details[1]
        while self._exit_callbacks:
            is_async, cb = self._exit_callbacks.pop()
            try:
                details = (None, None, None) if exc is None else (type(exc), exc, None)
                if isinstance(cb, tuple):
                    r = cb[0](*cb[1], **cb[2])
                    if is_async:
                        await r
                    r = False
                elif is_async:
                    r = await cb.__aexit__(*details)
                else:
                    r = cb.__exit__(*details)
                if r:
                    suppressed = True
                    pending_raise = False
                    exc = None
            except BaseException as new_exc:
                if exc is not None and new_exc is not exc:
                    new_exc.__context__ = exc
                pending_raise = True
                exc = new_exc
        if pending_raise:
            raise exc
        return received_exc and suppressed


class chdir(AbstractContextManager):
    def __init__(self, path):
        self.path = path

    def __enter__(self):
        return None

    def __exit__(self, *excinfo):
        return None

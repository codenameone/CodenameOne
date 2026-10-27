# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Queues: FIFO, LIFO and priority.

A translated program runs its Python code on one thread, so nothing can
fill an empty queue while get() waits on it. A get() on an empty queue or a
put() on a full one therefore raises Empty or Full at once, where CPython
would wait for another thread (forever, without one).
"""

from collections import deque
import heapq

__all__ = ["Empty", "Full", "ShutDown", "Queue", "PriorityQueue", "LifoQueue", "SimpleQueue"]


class Empty(Exception):
    pass


class Full(Exception):
    pass


class ShutDown(Exception):
    pass


class Queue:
    def __init__(self, maxsize=0):
        self.maxsize = maxsize
        self._init(maxsize)
        self.unfinished_tasks = 0
        self.is_shutdown = False

    def task_done(self):
        unfinished = self.unfinished_tasks - 1
        if unfinished < 0:
            raise ValueError("task_done() called too many times")
        self.unfinished_tasks = unfinished

    def join(self):
        if self.unfinished_tasks:
            raise RuntimeError("join() would wait forever: %d unfinished tasks" % self.unfinished_tasks)

    def qsize(self):
        return self._qsize()

    def empty(self):
        return not self._qsize()

    def full(self):
        return 0 < self.maxsize <= self._qsize()

    def put(self, item, block=True, timeout=None):
        if self.is_shutdown:
            raise ShutDown
        if block and timeout is not None and timeout < 0:
            raise ValueError("'timeout' must be a non-negative number")
        if 0 < self.maxsize <= self._qsize():
            raise Full
        self._put(item)
        self.unfinished_tasks += 1

    def get(self, block=True, timeout=None):
        if block and timeout is not None and timeout < 0:
            raise ValueError("'timeout' must be a non-negative number")
        if not self._qsize():
            if self.is_shutdown:
                raise ShutDown
            raise Empty
        return self._get()

    def put_nowait(self, item):
        return self.put(item, block=False)

    def get_nowait(self):
        return self.get(block=False)

    def shutdown(self, immediate=False):
        self.is_shutdown = True
        if immediate:
            while self._qsize():
                self._get()
                if self.unfinished_tasks > 0:
                    self.unfinished_tasks -= 1

    def _init(self, maxsize):
        self.queue = deque()

    def _qsize(self):
        return len(self.queue)

    def _put(self, item):
        self.queue.append(item)

    def _get(self):
        return self.queue.popleft()


class PriorityQueue(Queue):
    def _init(self, maxsize):
        self.queue = []

    def _qsize(self):
        return len(self.queue)

    def _put(self, item):
        heapq.heappush(self.queue, item)

    def _get(self):
        return heapq.heappop(self.queue)


class LifoQueue(Queue):
    def _init(self, maxsize):
        self.queue = []

    def _qsize(self):
        return len(self.queue)

    def _put(self, item):
        self.queue.append(item)

    def _get(self):
        return self.queue.pop()


class SimpleQueue:
    def __init__(self):
        self._queue = deque()

    def put(self, item, block=True, timeout=None):
        self._queue.append(item)

    def get(self, block=True, timeout=None):
        if timeout is not None and timeout < 0:
            raise ValueError("'timeout' must be a non-negative number")
        if not self._queue:
            raise Empty
        return self._queue.popleft()

    def put_nowait(self, item):
        return self.put(item, block=False)

    def get_nowait(self):
        return self.get(False)

    def empty(self):
        return len(self._queue) == 0

    def qsize(self):
        return len(self._queue)

# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""_interpqueues: the queues between subinterpreters, which a translated
program cannot start (see _interpreters); creating one raises QueueError."""


class QueueError(RuntimeError):
    pass


class QueueNotFoundError(QueueError):
    pass


class QueueEmpty(QueueError):
    pass


class QueueFull(QueueError):
    pass


def create(maxsize, unboundop=0, fallback=0):
    raise QueueError("interpreter queues are not available in a translated program")


def destroy(qid):
    raise QueueNotFoundError(qid)


def get(qid):
    raise QueueNotFoundError(qid)


def put(qid, obj, unboundop=0, fallback=0):
    raise QueueNotFoundError(qid)


def list_all():
    return []

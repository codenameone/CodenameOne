# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Background tasks, run after the response has been produced."""
import asyncio
import inspect


class BackgroundTask:
    def __init__(self, func, *args, **kwargs):
        self.func = func
        self.args = args
        self.kwargs = kwargs

    def __call__(self):
        r = self.func(*self.args, **self.kwargs)
        if inspect.iscoroutine(r):
            asyncio.run(r)


class BackgroundTasks(BackgroundTask):
    def __init__(self, tasks=None):
        self.tasks = list(tasks) if tasks else []

    def add_task(self, func, *args, **kwargs):
        self.tasks.append(BackgroundTask(func, *args, **kwargs))

    def __call__(self):
        for t in self.tasks:
            t()

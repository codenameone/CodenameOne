# Runtime behaviour the llm and MarkItDown ports depend on (anyio, click,
# sqlite-utils, the openai SDK).
import asyncio
import collections.abc
import contextvars
import functools
import inspect
import sqlite3
import threading
import typing

print("debug", __debug__)


# collections.abc recognises native coroutines and generators structurally.
async def coro():
    return 1


c = coro()
print("coroutine", isinstance(c, collections.abc.Coroutine), isinstance(c, collections.abc.Awaitable))
c.close()


def gen():
    yield 1


print("generator", isinstance(gen(), collections.abc.Generator), isinstance([], collections.abc.Generator))


# Attributes on functions can be deleted, as click does with its params.
def f():
    pass


f.__click_params__ = [1]
del f.__click_params__
print("deleted", hasattr(f, "__click_params__"))
try:
    del f.missing
except AttributeError as e:
    print("AttributeError", e)


# get_type_hints follows __wrapped__ to the function's own globals.
def hinted(x: int) -> str:
    return str(x)


@functools.wraps(hinted)
def wrapper(*a, **k):
    return hinted(*a, **k)


print("hints", sorted(typing.get_type_hints(wrapper).items(), key=lambda kv: kv[0]))
print("object init", inspect.signature(object.__init__))

# executescript runs statements as written: no implicit transaction.
con = sqlite3.connect(":memory:")
con.execute("create table t (x)")
con.executescript("insert into t values (1); insert into t values (2);")
print("in transaction after script", con.in_transaction)
con.execute("insert into t values (3)")
print("in transaction after insert", con.in_transaction)
con.commit()

# User-defined SQL functions.
con.create_function("double", 1, lambda v: v * 2)
print("udf", con.execute("select double(x) from t order by x").fetchall())


def boom(v):
    raise ValueError("no")


con.create_function("boom", 1, boom)
try:
    con.execute("select boom(1)").fetchall()
except sqlite3.OperationalError as e:
    print("OperationalError", e)
con.close()

# Each thread has its own context; Thread(context=) runs in the one given.
var = contextvars.ContextVar("var", default="unset")
var.set("main")
seen = []
t = threading.Thread(target=lambda: seen.append(var.get()))
t.start()
t.join()
ctx = contextvars.copy_context()
t = threading.Thread(target=lambda: seen.append(var.get()), context=ctx)
t.start()
t.join()
print("threads saw", seen, "main still", var.get())


# asyncio: Runner, cancelling/uncancel, callbacks in their context, task factory.
async def child():
    await asyncio.sleep(10)


async def main():
    task = asyncio.ensure_future(child())
    await asyncio.sleep(0)
    task.cancel()
    print("cancelling", task.cancelling())
    print("uncancel", task.uncancel())
    try:
        await task
    except asyncio.CancelledError:
        print("cancelled", task.cancelled())
    loop = asyncio.get_running_loop()
    done = loop.create_future()
    ctx = contextvars.copy_context()
    ctx.run(var.set, "in callback context")
    loop.call_soon(lambda: done.set_result(var.get()), context=ctx)
    print("callback saw", await done)
    made = []

    def factory(loop, coro, **kwargs):
        made.append(1)
        return asyncio.Task(coro, loop=loop, **kwargs)

    loop.set_task_factory(factory)
    await loop.create_task(asyncio.sleep(0))
    print("factory used", made, loop.get_task_factory() is factory)
    return "done"


with asyncio.Runner() as runner:
    print("runner", runner.run(main()))
print("asyncio.run", asyncio.run(asyncio.sleep(0, "slept")))


def positional(a, b, /, c, *, d):
    pass


def only_positional(a, /):
    pass


print("signatures", inspect.signature(positional), inspect.signature(only_positional))
try:
    only_positional(a=1)
except TypeError as e:
    print("TypeError", e)


def posonly_kwargs(a, /, **kw):
    return a, kw


print("to kwargs", posonly_kwargs(1, a=2), positional.__code__.co_posonlyargcount)


# Code points of long strings: pairs are one, a lone surrogate is one too,
# and indexing a long string in a loop sees the same answer each time.
long_pairs = "x\U0001F600y" * 20
print(len(long_pairs), long_pairs[1] == "\U0001F600", long_pairs[-1], long_pairs[30:33] == "\U0001F600yx")
lone = "a\ud800b" * 15
print(len(lone), lone[1] == "\ud800", lone[44], len(lone[3:9]))
mixed = "\ud83d" + "\U0001F600" * 12 + "\ude00" + "z" * 30
print(len(mixed), mixed[0] == "\ud83d", mixed[13] == "\ude00", mixed[1] == "\U0001F600")
print(sum(1 for i in range(len(long_pairs)) if long_pairs[i] == "y"))


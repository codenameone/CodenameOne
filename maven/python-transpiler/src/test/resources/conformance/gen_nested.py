# A generator defined inside another generator, after a loop. The inner
# body is emitted inline in the outer's state machine, and each has its
# own numbered states; the outer's jump threading must leave the inner's
# jumps alone.
import asyncio


def outer(k):
    i = 0
    while i < k:
        yield i
        i += 1

    def inner(n):
        j = 0
        while j < n:
            yield j * 10
            j += 1
        yield -1

    for v in inner(3):
        yield v
    yield 99


print(list(outer(2)))


async def ticker(q):
    await asyncio.sleep(0)
    stop = asyncio.Event()
    got = []

    async def sender():
        n = 0
        while not stop.is_set():
            got.append("tick %d" % n)
            n += 1
            await asyncio.sleep(0.01)

    task = asyncio.create_task(sender())
    try:
        while True:
            text = await q.get()
            if text == "stop":
                break
    except KeyError:
        pass
    stop.set()
    await task
    return len(got) > 0


async def drive():
    q = asyncio.Queue()
    t = asyncio.create_task(ticker(q))
    await asyncio.sleep(0.05)
    await q.put("x")
    await q.put("stop")
    return await t


print(asyncio.run(drive()))

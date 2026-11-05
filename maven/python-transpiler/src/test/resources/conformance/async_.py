import asyncio
import heapq

h = []
for x in [5, 1, 4, 2, 3]:
    heapq.heappush(h, x)
print([heapq.heappop(h) for _ in range(5)])
print(list(heapq.merge([1, 4, 7], [2, 5, 8], [3, 6])))
print(heapq.nsmallest(2, [9, 3, 7, 1]), heapq.nlargest(2, [9, 3, 7, 1]))

log = []

async def worker(name, n):
    for i in range(n):
        log.append(name + str(i))
        await asyncio.sleep(0)
    return name * n

async def fails():
    await asyncio.sleep(0)
    raise ValueError("boom")

async def producer(q):
    for i in range(4):
        await q.put(i)
    await q.put(None)

async def consumer(q):
    total = 0
    while True:
        v = await q.get()
        if v is None:
            return total
        total += v

async def main():
    r = await asyncio.gather(worker("a", 3), worker("b", 2))
    print(r, log)
    try:
        await asyncio.gather(fails())
    except ValueError as e:
        print("caught", e)
    print(await asyncio.gather(fails(), worker("c", 1), return_exceptions=True))
    q = asyncio.Queue(maxsize=2)
    t = asyncio.create_task(producer(q))
    print("sum", await consumer(q))
    await t
    try:
        await asyncio.wait_for(asyncio.sleep(10), 0.01)
    except TimeoutError:
        print("timeout")
    ev = asyncio.Event()
    async def setter():
        await asyncio.sleep(0.001)
        ev.set()
    asyncio.create_task(setter())
    print("event", await ev.wait())
    lock = asyncio.Lock()
    order = []
    async def crit(k):
        async with lock:
            order.append(k)
            await asyncio.sleep(0)
            order.append(k)
    await asyncio.gather(crit(1), crit(2))
    print(order)
    t2 = asyncio.create_task(asyncio.sleep(5))
    await asyncio.sleep(0)
    t2.cancel()
    try:
        await t2
    except asyncio.CancelledError:
        print("cancelled", t2.cancelled())
    return 42

print(asyncio.run(main()))

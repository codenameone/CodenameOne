import asyncio


async def ticker(n):
    try:
        for i in range(n):
            await asyncio.sleep(0)
            yield i
    finally:
        print("ticker closed", n)


async def doubled(src):
    async for v in src:
        yield v * 2


async def slow_sq(x):
    await asyncio.sleep(0)
    return x * x


async def main():
    out = []
    async for v in ticker(3):
        out.append(v)
    print(out)
    print([v async for v in doubled(ticker(4))])
    print({v: await slow_sq(v) async for v in ticker(3)})
    print({v % 2 async for v in ticker(5) if v > 0})
    print([await slow_sq(x) for x in range(4)])
    g = (v + 100 async for v in ticker(2))
    print(type(g).__name__, [x async for x in g])
    agen = ticker(10)
    print(await agen.__anext__(), await agen.__anext__())
    await agen.aclose()
    try:
        await agen.__anext__()
    except StopAsyncIteration:
        print("exhausted")

    async def echo():
        received = None
        while True:
            received = yield received
    e = echo()
    print(await e.asend(None), await e.asend("hi"), await e.asend(5))
    try:
        await e.athrow(ValueError("x"))
    except ValueError as ex:
        print("athrow", ex)
    print(type(ticker).__name__, type(ticker(1)).__name__)


asyncio.run(main())

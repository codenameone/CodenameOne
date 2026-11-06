import asyncio

from fastapi import FastAPI, Request, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
from fastapi.middleware.gzip import GZipMiddleware
from fastapi.middleware.trustedhost import TrustedHostMiddleware
from fastapi.testclient import TestClient
from starlette.middleware.base import BaseHTTPMiddleware

app = FastAPI()
seen = []


@app.middleware("http")
async def http_only(request: Request, call_next):
    seen.append("http " + request.url.path)
    return await call_next(request)


class Tagger(BaseHTTPMiddleware):
    async def dispatch(self, request, call_next):
        seen.append("tagger " + request.url.path)
        return await call_next(request)


app.add_middleware(Tagger)
app.add_middleware(GZipMiddleware, minimum_size=10)
app.add_middleware(CORSMiddleware, allow_origins=["*"])


@app.websocket("/sleepy")
async def sleepy(websocket: WebSocket):
    await websocket.accept()
    while True:
        try:
            text = await websocket.receive_text()
        except WebSocketDisconnect as e:
            seen.append("sleepy gone %d" % e.code)
            return
        await asyncio.sleep(0.05)
        await websocket.send_text("slept " + text)


@app.websocket("/ticker")
async def ticker(websocket: WebSocket):
    await websocket.accept()
    n = int(await websocket.receive_text())

    async def push():
        for i in range(n):
            await asyncio.sleep(0.02)
            await websocket.send_json({"tick": i})
        await websocket.send_text("done")

    task = asyncio.create_task(push())
    await task
    try:
        await websocket.receive_text()
    except WebSocketDisconnect:
        seen.append("ticker gone")


@app.websocket("/timeout")
async def timeout(websocket: WebSocket):
    await websocket.accept()
    for _ in range(2):
        try:
            text = await asyncio.wait_for(websocket.receive_text(), 0.1)
            await websocket.send_text("got " + text)
        except asyncio.TimeoutError:
            await websocket.send_text("timed out")
    await websocket.close(code=1000)


@app.websocket("/both")
async def both(websocket: WebSocket):
    await websocket.accept()
    results = await asyncio.gather(asyncio.sleep(0.01, "a"), asyncio.sleep(0.02, "b"))
    await websocket.send_json(results)
    await websocket.close()


@app.websocket("/boom")
async def boom(websocket: WebSocket):
    await websocket.accept()
    await asyncio.sleep(0.01)
    raise ValueError("endpoint failed")


@app.get("/plain")
def plain():
    return {"ok": True}


client = TestClient(app)
print(client.get("/plain").json(), seen)
del seen[:]

with client.websocket_connect("/sleepy") as ws:
    ws.send_text("a")
    ws.send_text("b")
    print(ws.receive_text())
    print(ws.receive_text())

with client.websocket_connect("/ticker") as ws:
    ws.send_text("3")
    print(ws.receive_json(), ws.receive_json(), ws.receive_json(), ws.receive_text())

with client.websocket_connect("/timeout") as ws:
    print(ws.receive_text())
    ws.send_text("late")
    print(ws.receive_text())
    try:
        ws.receive_text()
    except WebSocketDisconnect as e:
        print("closed", e.code)

with client.websocket_connect("/both") as ws:
    print(ws.receive_json())

try:
    with client.websocket_connect("/boom") as ws:
        ws.receive_text()
except ValueError as e:
    print("raised", e)
print(sorted(seen))

# Trusted hosts apply to websockets too
app2 = FastAPI()
app2.add_middleware(TrustedHostMiddleware, allowed_hosts=["example.com"])


@app2.websocket("/ws")
async def ws2(websocket: WebSocket):
    await websocket.accept()
    await websocket.send_text("host " + websocket.headers["host"])
    await websocket.close()


c2 = TestClient(app2)
with c2.websocket_connect("/ws", headers={"host": "example.com"}) as ws:
    print(ws.receive_text())
try:
    with c2.websocket_connect("/ws", headers={"host": "evil.com"}) as ws:
        print("connected?")
except Exception as e:
    print("bad host refused", type(e).__name__, e.status_code, e.text)

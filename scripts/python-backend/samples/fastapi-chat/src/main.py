"""A FastAPI chat service: HTTP routes, both kinds of middleware, websockets
and a streamed response. Served by the Codename One backend; driven end to
end by scripts/python-backend/e2e-test.sh.

    python main.py [port]
"""
import asyncio
import sys
import time

from fastapi import BackgroundTasks, FastAPI, WebSocket, WebSocketDisconnect
from fastapi.responses import StreamingResponse, JSONResponse

app = FastAPI(title="chat")
messages = []


class StampMiddleware:
    """Pure ASGI middleware: tags every HTTP response, refuses websockets
    to /ws/private without a token, and leaves the rest alone."""

    def __init__(self, app):
        self.app = app

    async def __call__(self, scope, receive, send):
        if scope["type"] == "websocket" and scope["path"] == "/ws/private":
            query = scope.get("query_string", b"")
            if b"token=secret" not in query:
                await send({"type": "websocket.http.response.start", "status": 401,
                            "headers": [(b"content-type", b"text/plain")]})
                await send({"type": "websocket.http.response.body", "body": b"no token"})
                return
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return

        async def stamped(message):
            if message["type"] == "http.response.start":
                headers = list(message.get("headers", []))
                headers.append((b"x-asgi", b"stamped"))
                message = dict(message)
                message["headers"] = headers
            await send(message)
        await self.app(scope, receive, stamped)


app.add_middleware(StampMiddleware)


@app.middleware("http")
async def timing(request, call_next):
    if request.url.path == "/blocked":
        return JSONResponse({"detail": "blocked by middleware"}, status_code=401)
    response = await call_next(request)
    response.headers["x-handled-by"] = "timing"
    return response


@app.get("/health")
def health():
    return {"ok": True}


@app.post("/messages", status_code=201)
def post_message(body: dict):
    messages.append(body.get("text", ""))
    return {"count": len(messages)}


@app.get("/messages")
def list_messages():
    return {"messages": messages}


@app.get("/stream")
def stream():
    def gen():
        for i in range(3):
            yield "chunk %d\n" % i
            if i < 2:
                time.sleep(1)
    return StreamingResponse(gen(), media_type="text/plain")


@app.get("/astream")
async def astream():
    async def gen():
        for i in range(3):
            yield "async %d\n" % i
            if i < 2:
                await asyncio.sleep(1)
    return StreamingResponse(gen(), media_type="text/plain")


events = []


def _late(name):
    # Slow enough that a server running it before (or while holding up) the
    # response would be seen by the client's clock.
    time.sleep(0.5)
    events.append("task " + name)


@app.get("/background")
def background(tasks: BackgroundTasks):
    events.append("handler")
    tasks.add_task(_late, "plain")
    return {"queued": True}


@app.get("/background-stream")
def background_stream(tasks: BackgroundTasks):
    def gen():
        for i in range(2):
            yield "bg %d\n" % i
            time.sleep(0.3)
        events.append("stream done")
    tasks.add_task(_late, "stream")
    return StreamingResponse(gen(), media_type="text/plain", background=tasks)


@app.get("/events")
def list_events():
    return {"events": events}


@app.delete("/events")
def clear_events():
    del events[:]
    return {"events": events}


@app.websocket("/ws/echo")
async def echo(ws: WebSocket):
    await ws.accept()
    try:
        while True:
            text = await ws.receive_text()
            await ws.send_text("echo: " + text)
    except WebSocketDisconnect:
        pass


@app.websocket("/ws/ticker")
async def ticker(ws: WebSocket):
    await ws.accept()
    stop = asyncio.Event()

    async def sender():
        n = 0
        while not stop.is_set():
            await ws.send_text("tick %d" % n)
            n += 1
            await asyncio.sleep(0.2)

    task = asyncio.create_task(sender())
    try:
        while True:
            text = await ws.receive_text()
            if text == "stop":
                break
    except WebSocketDisconnect:
        pass
    stop.set()
    await task
    await ws.close(1000)


@app.websocket("/ws/forbidden")
async def forbidden(ws: WebSocket):
    await ws.close()


@app.websocket("/ws/private")
async def private(ws: WebSocket):
    await ws.accept()
    await ws.send_text("welcome")
    await ws.close()


@app.websocket("/ws/proto")
async def proto(ws: WebSocket):
    await ws.accept(subprotocol="chat.v1", headers=[(b"x-room", b"lobby")])
    await ws.send_text("proto ok")
    await ws.close()


if __name__ == "__main__":
    import uvicorn
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8080
    uvicorn.run(app, host="127.0.0.1", port=port)

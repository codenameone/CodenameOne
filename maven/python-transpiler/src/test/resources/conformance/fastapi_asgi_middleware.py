from fastapi import FastAPI, Request, WebSocket
from fastapi.testclient import TestClient
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.websockets import WebSocketDisconnect

log = []


class AddHeader:
    """Pure ASGI middleware: adds a header to every HTTP response."""

    def __init__(self, app, value="asgi"):
        self.app = app
        self.value = value

    async def __call__(self, scope, receive, send):
        log.append("scope " + scope["type"] + " " + scope["path"])
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return

        async def send_wrapper(message):
            if message["type"] == "http.response.start":
                headers = list(message.get("headers", []))
                headers.append((b"x-asgi", self.value.encode("latin-1")))
                message = dict(message)
                message["headers"] = headers
                log.append("start %d" % message["status"])
            await send(message)

        await self.app(scope, receive, send_wrapper)


class Gate:
    """Pure ASGI middleware: refuses /ws/secret, and counts websocket messages."""

    def __init__(self, app):
        self.app = app

    async def __call__(self, scope, receive, send):
        if scope["type"] == "websocket" and scope["path"] == "/ws/secret":
            await send({"type": "websocket.http.response.start", "status": 401,
                        "headers": [(b"content-type", b"text/plain"), (b"x-why", b"gate")]})
            await send({"type": "websocket.http.response.body", "body": b"no entry"})
            return
        if scope["type"] == "websocket":
            async def counting_receive():
                m = await receive()
                log.append("ws in " + m["type"])
                return m

            async def counting_send(m):
                log.append("ws out " + m["type"])
                await send(m)

            await self.app(scope, counting_receive, counting_send)
            return
        body = []

        async def read():
            m = await receive()
            body.append(m.get("body", b""))
            return m

        await self.app(scope, read, send)
        log.append("gate body %r" % b"".join(body))


app = FastAPI()
app.add_middleware(Gate)
app.add_middleware(AddHeader, value="v1")


class Tag(BaseHTTPMiddleware):
    async def dispatch(self, request, call_next):
        resp = await call_next(request)
        resp.headers["x-tag"] = "base"
        return resp


app.add_middleware(Tag)


@app.get("/hello")
def hello():
    return {"hi": 1}


@app.post("/echo")
async def echo(request: Request):
    return {"len": len(await request.body())}


@app.websocket("/ws/chat")
async def chat(ws: WebSocket):
    await ws.accept(headers=[(b"x-room", b"lobby")])
    msg = await ws.receive_text()
    await ws.send_text("echo " + msg)
    await ws.close()


@app.websocket("/ws/secret")
async def secret(ws: WebSocket):
    await ws.accept()


client = TestClient(app)

r = client.get("/hello")
print(r.status_code, r.json(), r.headers.get("x-asgi"), r.headers.get("x-tag"))
print(log)
del log[:]

r = client.post("/echo", content=b"abcdef")
print(r.status_code, r.json(), r.headers.get("x-asgi"))
print(log)
del log[:]

r = client.get("/nope")
print(r.status_code, r.json(), r.headers.get("x-asgi"))
del log[:]

with client.websocket_connect("/ws/chat") as ws:
    ws.send_text("hey")
    print(ws.receive_text(), ws.extra_headers)
print(log)
del log[:]

try:
    with client.websocket_connect("/ws/secret") as ws:
        print("connected?")
except Exception as e:
    print(type(e).__name__, e.status_code, e.text, e.headers.get("x-why"))
print(log)
del log[:]

try:
    with client.websocket_connect("/ws/none") as ws:
        print("connected?")
except WebSocketDisconnect as e:
    print("disconnect", e.code)
print(log)

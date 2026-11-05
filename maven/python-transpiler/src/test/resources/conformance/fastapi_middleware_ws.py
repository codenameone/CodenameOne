from fastapi import Depends, FastAPI, HTTPException, Request, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
from fastapi.middleware.gzip import GZipMiddleware
from fastapi.middleware.trustedhost import TrustedHostMiddleware
from fastapi.responses import PlainTextResponse
from fastapi.testclient import TestClient
from starlette.middleware.base import BaseHTTPMiddleware

app = FastAPI()
order = []


@app.middleware("http")
async def timing(request: Request, call_next):
    order.append("timing-in " + request.url.path)
    response = await call_next(request)
    response.headers["X-Timing"] = "1"
    order.append("timing-out %d" % response.status_code)
    return response


class Tagger(BaseHTTPMiddleware):
    def __init__(self, app, tag):
        super().__init__(app)
        self.tag = tag

    async def dispatch(self, request, call_next):
        order.append("tagger-in")
        if request.url.path == "/blocked":
            return PlainTextResponse("blocked by " + self.tag, status_code=403)
        request.state.tag = self.tag
        response = await call_next(request)
        response.headers["X-Tag"] = self.tag
        order.append("tagger-out")
        return response


app.add_middleware(Tagger, tag="t1")
app.add_middleware(CORSMiddleware, allow_origins=["https://good.example"], allow_methods=["GET", "POST"],
                   allow_headers=["X-Custom"], allow_credentials=True, expose_headers=["X-Tag"])
app.add_middleware(GZipMiddleware, minimum_size=100)


@app.get("/hello")
def hello(request: Request):
    return {"msg": "hi", "tag": request.state.tag}


@app.get("/big")
def big():
    return PlainTextResponse("x" * 1000)


@app.get("/missing")
def missing():
    raise HTTPException(404, "nope")


@app.get("/blocked")
def blocked():
    return {"never": True}


client = TestClient(app)

r = client.get("/hello")
print(r.status_code, r.json(), r.headers["x-timing"], r.headers["x-tag"])
print(order)
del order[:]
print("cors" , r.headers.get("access-control-allow-origin"))

r = client.get("/missing")
print(r.status_code, r.json(), r.headers.get("x-tag"), order)
del order[:]

r = client.get("/blocked")
print(r.status_code, r.text, r.headers.get("x-timing"), order)
del order[:]

r = client.get("/big")
print(r.status_code, len(r.text), r.headers.get("content-encoding"), r.headers.get("vary"))
r = client.get("/big", headers={"accept-encoding": "identity"})
print(r.status_code, len(r.text), r.headers.get("content-encoding"), r.headers.get("content-length"))
r = client.get("/hello", headers={"accept-encoding": "gzip"})
print(r.status_code, r.headers.get("content-encoding"))
del order[:]

# CORS simple and preflight requests
r = client.get("/hello", headers={"Origin": "https://good.example"})
print(r.status_code, r.headers.get("access-control-allow-origin"), r.headers.get("access-control-allow-credentials"),
      r.headers.get("access-control-expose-headers"), r.headers.get("vary"))
r = client.get("/hello", headers={"Origin": "https://evil.example"})
print(r.status_code, r.headers.get("access-control-allow-origin"), r.headers.get("access-control-allow-credentials"))
r = client.options("/hello", headers={"Origin": "https://good.example", "Access-Control-Request-Method": "POST",
                                      "Access-Control-Request-Headers": "x-custom"})
print(r.status_code, r.text, r.headers.get("access-control-allow-origin"), r.headers.get("access-control-allow-methods"),
      r.headers.get("access-control-allow-headers"), r.headers.get("access-control-max-age"), r.headers.get("vary"))
r = client.options("/hello", headers={"Origin": "https://evil.example", "Access-Control-Request-Method": "DELETE",
                                      "Access-Control-Request-Headers": "x-other"})
print(r.status_code, r.text)
print(order)
del order[:]

# A wildcard CORS app and trusted hosts
app2 = FastAPI()
app2.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"], max_age=60)
app2.add_middleware(TrustedHostMiddleware, allowed_hosts=["example.com", "*.example.org", "www.site.com"])


@app2.get("/")
def root():
    return "ok"


c2 = TestClient(app2)
print(c2.get("/").status_code, c2.get("/").text)
print(c2.get("/", headers={"host": "example.com"}).json())
print(c2.get("/", headers={"host": "api.example.org"}).json())
r = c2.get("/", headers={"host": "site.com"}, follow_redirects=False)
print(r.status_code, r.headers.get("location"))
r = c2.get("/", headers={"host": "example.com", "Origin": "https://any.where"})
print(r.headers.get("access-control-allow-origin"), r.headers.get("vary"))
r = c2.options("/", headers={"host": "example.com", "Origin": "https://any.where",
                             "Access-Control-Request-Method": "PATCH", "Access-Control-Request-Headers": "X-A, X-B"})
print(r.status_code, r.headers.get("access-control-allow-origin"), r.headers.get("access-control-allow-methods"),
      r.headers.get("access-control-allow-headers"), r.headers.get("access-control-max-age"))


@app2.get("/boom")
def boom():
    raise ValueError("boom")


try:
    c2.get("/boom", headers={"host": "example.com"})
except ValueError as e:
    print("raised", e)
print(TestClient(app2, raise_server_exceptions=False).get("/boom", headers={"host": "example.com"}).status_code)

# WebSockets
log = []


def get_prefix():
    return ">"


@app.websocket("/ws")
async def echo(websocket: WebSocket):
    await websocket.accept()
    try:
        while True:
            text = await websocket.receive_text()
            await websocket.send_text("echo " + text)
    except WebSocketDisconnect as e:
        log.append("disconnect %d" % e.code)


@app.websocket("/rooms/{room}")
async def room(websocket: WebSocket, room: str, prefix: str = Depends(get_prefix), limit: int = 2):
    await websocket.accept()
    await websocket.send_json({"room": room, "limit": limit, "path": websocket.url.path})
    for _ in range(limit):
        data = await websocket.receive_json()
        await websocket.send_json({"got": data, "prefix": prefix})
    b = await websocket.receive_bytes()
    await websocket.send_bytes(b[::-1])
    await websocket.close(code=1001)


@app.websocket("/reject")
async def reject(websocket: WebSocket):
    await websocket.close(code=1008)


with client.websocket_connect("/ws") as ws:
    ws.send_text("a")
    print(ws.receive_text())
    ws.send_text("b")
    print(ws.receive_text())
print(log)

with client.websocket_connect("/ws") as ws:
    ws.send_text("c")
    print(ws.receive_text())
    ws.close(code=4000)
print(log)

with client.websocket_connect("/rooms/lobby?limit=1") as ws:
    print(ws.receive_json())
    ws.send_json({"n": 1})
    print(ws.receive_json())
    ws.send_bytes(b"abc")
    print(ws.receive_bytes())
    try:
        ws.receive_text()
    except WebSocketDisconnect as e:
        print("closed", e.code)

for path in ("/reject", "/nowhere"):
    try:
        with client.websocket_connect(path) as ws:
            print("connected?")
    except WebSocketDisconnect as e:
        print(path, "refused", e.code)
print(order)

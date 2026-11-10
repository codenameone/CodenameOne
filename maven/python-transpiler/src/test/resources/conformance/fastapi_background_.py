from fastapi import BackgroundTasks, FastAPI
from fastapi.responses import JSONResponse, StreamingResponse
from fastapi.testclient import TestClient
from starlette.background import BackgroundTask

app = FastAPI()
log = []


def note(msg):
    log.append(msg)


def boom():
    raise ValueError("task failed")


@app.get("/injected")
def injected(tasks: BackgroundTasks):
    tasks.add_task(note, "injected")
    log.append("handler")
    return {"ok": True}


@app.get("/own")
def own(tasks: BackgroundTasks):
    tasks.add_task(note, "injected-dropped")
    return JSONResponse({"own": True}, background=BackgroundTask(note, "own"))


@app.get("/raising")
def raising(tasks: BackgroundTasks):
    tasks.add_task(boom)
    return {"ok": 1}


def chunks():
    yield b"a"
    yield "b"
    yield b""
    yield b"c"


def failing():
    try:
        yield b"start"
        raise ValueError("stream failed")
    finally:
        log.append("stream finally")


def bad_type():
    yield b"x"
    yield 5


@app.get("/stream")
def stream():
    return StreamingResponse(chunks())


@app.get("/failing")
def failing_route():
    return StreamingResponse(failing())


@app.get("/badtype")
def bad_route():
    return StreamingResponse(bad_type())


client = TestClient(app)
r = client.get("/injected")
print(r.status_code, r.json(), log)
del log[:]
r = client.get("/own")
print(r.status_code, r.json(), log)
del log[:]
try:
    client.get("/raising")
    print("no error")
except ValueError as e:
    print("ValueError", e)
r = client.get("/stream")
print(r.status_code, r.content)
try:
    client.get("/failing")
    print("no error")
except ValueError as e:
    print("ValueError", e, log)
try:
    client.get("/badtype")
    print("no error")
except AttributeError as e:
    print("AttributeError", e)

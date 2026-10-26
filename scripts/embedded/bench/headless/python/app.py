# The Python half of the headless comparison: the same two routes on FastAPI,
# served by uvicorn -- the usual way a Python service is written today.
from fastapi import FastAPI
from fastapi.responses import PlainTextResponse

app = FastAPI()


@app.get("/plaintext", response_class=PlainTextResponse)
def plaintext():
    return "Hello, World!"


@app.get("/json")
def json():
    return {"message": "Hello, World!"}

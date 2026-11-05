from enum import Enum
from typing import Annotated, List, Optional

from fastapi import (APIRouter, Body, Depends, FastAPI, Header, HTTPException, Path, Query,
                     Request, Response)
from fastapi.exceptions import RequestValidationError
from fastapi.responses import HTMLResponse, JSONResponse, PlainTextResponse, RedirectResponse
from fastapi.testclient import TestClient
from pydantic import BaseModel, Field

app = FastAPI(title="Demo", version="1.2.3")
log = []


class Color(str, Enum):
    red = "red"
    green = "green"


class Address(BaseModel):
    city: str
    zip: Optional[str] = None


class Item(BaseModel):
    name: str
    price: float
    tags: List[str] = []
    address: Optional[Address] = None


class UserIn(BaseModel):
    username: str
    password: str
    age: int = Field(default=18, ge=0)


class UserOut(BaseModel):
    username: str
    age: int


@app.get("/")
def root():
    return {"hello": "world"}


@app.get("/items/{item_id}")
def read_item(item_id: int, q: Optional[str] = None):
    return {"item_id": item_id, "q": q}


@app.get("/float/{x}")
def read_float(x: float):
    return {"x": x}


@app.get("/color/{c}")
def read_color(c: Color):
    return {"color": c, "value": c.value}


@app.get("/files/{file_path:path}")
def read_file(file_path: str):
    return {"path": file_path}


@app.get("/search")
def search(q: str, limit: int = 10, flag: bool = False, tags: List[str] = Query(default=[])):
    return {"q": q, "limit": limit, "flag": flag, "tags": tags}


@app.get("/constrained")
def constrained(name: str = Query(min_length=3, max_length=5), n: int = Query(default=1, ge=1, le=10)):
    return {"name": name, "n": n}


@app.get("/pathc/{pid}")
def pathc(pid: int = Path(gt=0, description="positive"), q: Annotated[Optional[str], Query(alias="query")] = None):
    return {"pid": pid, "q": q}


@app.get("/headers")
def headers(x_token: Optional[str] = Header(default=None), user_agent: Optional[str] = Header(default=None)):
    return {"x_token": x_token, "ua": user_agent}


@app.post("/items", status_code=201)
def create_item(item: Item):
    return item


@app.put("/items/{item_id}")
def update_item(item_id: int, item: Item, notify: bool = False):
    return {"item_id": item_id, "name": item.name, "notify": notify,
            "city": item.address.city if item.address else None}


@app.post("/users", response_model=UserOut, status_code=201)
def create_user(user: UserIn):
    return user


@app.post("/embed")
def embed(item: Item = Body(embed=True), importance: int = Body(default=1)):
    return {"name": item.name, "importance": importance}


@app.post("/list")
def body_list(values: List[int]):
    return {"sum": sum(values)}


def common(skip: int = 0, limit: int = 100):
    log.append("common")
    return {"skip": skip, "limit": limit}


def nested(commons: dict = Depends(common), token: Optional[str] = Header(default=None)):
    log.append("nested")
    return {"commons": commons, "token": token}


@app.get("/deps")
def deps(n: dict = Depends(nested)):
    return n


def resource():
    log.append("open")
    try:
        yield "res"
    finally:
        log.append("close")


@app.get("/yield")
def use_yield(r: str = Depends(resource)):
    log.append("handler")
    return {"r": r, "log": list(log)}


class Fancy:
    def __init__(self, a: int = 1):
        self.a = a


@app.get("/cls")
def use_class(f: Fancy = Depends()):
    return {"a": f.a}


@app.get("/http-error/{code}")
def http_error(code: int):
    raise HTTPException(status_code=code, detail="custom detail", headers={"X-Err": "1"})


@app.get("/http-error-dict")
def http_error_dict():
    raise HTTPException(status_code=400, detail={"a": 1})


class MyError(Exception):
    def __init__(self, name):
        self.name = name


@app.exception_handler(MyError)
async def my_error_handler(request: Request, exc: MyError):
    return JSONResponse(status_code=418, content={"message": "oops " + exc.name})


@app.get("/myerror")
def myerror():
    raise MyError("bob")


@app.get("/json-response")
def json_response():
    return JSONResponse({"a": 1}, status_code=202, headers={"X-A": "b"})


@app.get("/plain", response_class=PlainTextResponse)
def plain():
    return "plain text"


@app.get("/html", response_class=HTMLResponse)
def html():
    return "<h1>hi</h1>"


@app.get("/nocontent", status_code=204)
def nocontent():
    return Response(status_code=204)


@app.get("/redirect")
def redirect():
    return RedirectResponse("/", status_code=307)


@app.get("/async")
async def async_endpoint():
    return {"async": True}


async def async_dep():
    return "async-dep"


@app.get("/async-dep")
async def use_async_dep(d: str = Depends(async_dep)):
    return {"d": d}


@app.get("/req")
def use_request(request: Request):
    return {"method": request.method, "path": request.url.path, "q": dict(request.query_params),
            "ua": request.headers.get("user-agent")}


@app.get("/unset", response_model=Item, response_model_exclude_unset=True)
def unset():
    return {"name": "n", "price": 1.5}


@app.get("/model-list", response_model=List[UserOut])
def model_list():
    return [{"username": "a", "age": 1, "password": "x"}, UserIn(username="b", password="y")]


@app.get("/needhdr")
def needhdr(x_api_key: str = Header(), c: Optional[str] = Header(default=None, alias="x-c")):
    return {"k": x_api_key, "c": c}


router = APIRouter(prefix="/v1", tags=["v1"])


def router_dep():
    log.append("router_dep")


@router.get("/things/{tid}")
def get_thing(tid: int):
    return {"tid": tid}


@router.post("/things", status_code=201)
def post_thing(item: Item):
    return {"created": item.name}


app.include_router(router)
sub = APIRouter()


@sub.get("/ping")
def ping():
    return "pong"


app.include_router(sub, prefix="/sub", dependencies=[Depends(router_dep)])

client = TestClient(app)


def show(r, *hdrs):
    print(r.status_code)
    for h in hdrs:
        v = r.headers.get(h)
        if v is not None:
            print(h + ":", v)
    print(r.text)
    print("--")


show(client.get("/"), "content-type")
show(client.get("/items/5?q=abc"))
show(client.get("/items/abc"))
show(client.get("/items/5.5"))
show(client.get("/float/2"))
show(client.get("/color/red"))
show(client.get("/color/blue"))
show(client.get("/files/a/b/c.txt"))
show(client.get("/search?q=x&limit=3&flag=true&tags=a&tags=b"))
show(client.get("/search"))
show(client.get("/search?q=x&limit=abc&flag=maybe"))
show(client.get("/constrained?name=ab&n=0"))
show(client.get("/constrained?name=abcdefg&n=11"))
show(client.get("/constrained?name=abc"))
show(client.get("/pathc/0"))
show(client.get("/pathc/3?query=zz"))
show(client.get("/needhdr"))
show(client.get("/needhdr", headers={"X-Api-Key": "k", "x-c": "cc"}))
show(client.get("/headers", headers={"X-Token": "tok", "User-Agent": "ua1"}))
show(client.post("/items", json={"name": "n", "price": 2, "tags": ["a"], "address": {"city": "X"}}))
show(client.post("/items", json={"name": "n"}))
show(client.post("/items", json={"name": 5, "price": "abc", "address": {}}))
show(client.post("/items", json=[1]))
show(client.post("/items"))
show(client.post("/items", content="not json", headers={"content-type": "application/json"}))
show(client.put("/items/7?notify=1", json={"name": "z", "price": 1, "address": {"city": "C"}}))
show(client.put("/items/x", json={"name": "z", "price": 1}))
show(client.post("/users", json={"username": "u", "password": "secret"}))
show(client.post("/users", json={"username": "u", "password": "secret", "age": -1}))
show(client.post("/embed", json={"item": {"name": "e", "price": 1}, "importance": 5}))
show(client.post("/embed", json={"item": {"name": "e", "price": 1}}))
show(client.post("/embed", json={"name": "e", "price": 1}))
show(client.post("/list", json=[1, 2, 3]))
show(client.post("/list", json=[1, "x"]))
show(client.get("/deps?skip=2", headers={"token": "t"}))
print(log)
log.clear()
show(client.get("/yield"))
print(log)
show(client.get("/cls"))
show(client.get("/http-error/403"), "x-err")
show(client.get("/http-error-dict"))
show(client.get("/myerror"))
show(client.get("/json-response"), "x-a", "content-type")
show(client.get("/plain"), "content-type")
show(client.get("/html"), "content-type")
show(client.get("/nocontent"))
show(client.get("/redirect", follow_redirects=False), "location")
show(client.get("/async"))
show(client.get("/async-dep"))
show(client.get("/req?a=1&b=2"))
show(client.get("/unset"))
show(client.get("/model-list"))
show(client.get("/v1/things/3"))
show(client.post("/v1/things", json={"name": "t", "price": 1}))
log.clear()
show(client.get("/sub/ping"))
print(log)
show(client.get("/nope"))
show(client.post("/"), "allow")
show(client.delete("/items/1"), "allow")
show(client.get("/items"), "allow")
show(client.get("/items/1/"), "location")
show(client.get("/sub/ping/", follow_redirects=False), "location")


app2 = FastAPI()


@app2.exception_handler(RequestValidationError)
async def validation_handler(request: Request, exc: RequestValidationError):
    return JSONResponse(status_code=400, content={"custom": True, "n": len(exc.errors())})


@app2.get("/x/{i}")
def x(i: int):
    return i


show(TestClient(app2).get("/x/abc"))
schema = app.openapi()
print(schema["info"])
print(sorted(schema["paths"]))
print(sorted(schema["components"]["schemas"]))
print(schema["paths"]["/items/{item_id}"]["get"]["parameters"])

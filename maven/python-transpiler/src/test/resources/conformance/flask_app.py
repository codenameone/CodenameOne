from flask import (Flask, Blueprint, request, jsonify, make_response, abort, redirect,
                   url_for, g, Response)

app = Flask(__name__)


@app.route("/")
def index():
    return "Hello, World!"


@app.route("/user/<name>")
def user(name):
    return "User " + name


@app.route("/item/<int:item_id>")
def item(item_id):
    return {"id": item_id, "type": type(item_id).__name__}


@app.route("/price/<float:p>")
def price(p):
    return jsonify(price=p)


@app.route("/files/<path:fp>")
def files(fp):
    return "file=" + fp


@app.route("/uid/<uuid:u>")
def uid(u):
    return str(u.version)


@app.route("/echo", methods=["GET", "POST", "PUT"])
def echo():
    return jsonify(method=request.method, path=request.path, args=request.args.to_dict(),
                   tags=request.args.getlist("tag"), page=request.args.get("page", 1, type=int))


@app.post("/form")
def form():
    return jsonify(request.form.to_dict())


@app.post("/json")
def js():
    data = request.get_json()
    return jsonify(got=data, silent=request.get_json(silent=True) is not None)


@app.route("/raw", methods=["POST"])
def raw():
    return request.data


@app.route("/hdr")
def hdr():
    return jsonify(ua=request.headers.get("user-agent"), x=request.headers["X-Token"],
                   cookie=request.cookies.get("sid"), ct=request.headers.get("Content-Type"))


@app.route("/slash/")
def slash():
    return "slash"


@app.route("/noslash")
def noslash():
    return "noslash"


@app.route("/tuple")
def tup():
    return "created", 201


@app.route("/tuple3")
def tup3():
    return {"a": 1}, 202, {"X-Extra": "yes"}


@app.route("/resp")
def resp():
    r = make_response("custom", 203)
    r.headers["X-Foo"] = "bar"
    r.set_cookie("sid", "abc")
    r.mimetype = "text/plain"
    return r


@app.route("/lst")
def lst():
    return [1, 2, {"a": None}]


@app.route("/abort/<int:code>")
def do_abort(code):
    abort(code)


@app.route("/boom")
def boom():
    raise RuntimeError("boom")


@app.route("/redir")
def redir():
    return redirect(url_for("user", name="bob"))


@app.errorhandler(418)
def teapot(e):
    return "I am a teapot: " + str(e.code), 418


@app.errorhandler(ValueError)
def value_error(e):
    return jsonify(error=str(e)), 400


@app.route("/value")
def value():
    raise ValueError("bad value")


@app.errorhandler(404)
def not_found(e):
    if request.path.startswith("/api"):
        return jsonify(error="not found", path=request.path), 404
    return e


@app.before_request
def before():
    g.seen = request.path
    if request.path == "/blocked":
        return "blocked", 403


@app.after_request
def after(resp):
    resp.headers["X-Seen"] = g.seen
    return resp


@app.route("/blocked")
def blocked():
    return "never"


bp = Blueprint("api", __name__, url_prefix="/api/v1")


@bp.route("/things/<int:n>")
def thing(n):
    return jsonify(n=n, endpoint=request.endpoint, url=url_for(".thing", n=n + 1))


@bp.route("/ping", methods=["POST"])
def ping():
    return "pong"


app.register_blueprint(bp)


def show(r, *hdrs):
    print(r.status_code, r.status)
    for h in hdrs:
        v = r.headers.get(h)
        if v is not None:
            if h == "allow":
                v = ", ".join(sorted(v.split(", ")))
            print(h + ":", v)
    print(r.get_data(as_text=True).rstrip("\n"))
    print("--")


c = app.test_client()
show(c.get("/"), "content-type", "content-length")
show(c.get("/user/alice"))
show(c.get("/item/42"), "content-type")
show(c.get("/item/abc"))
show(c.get("/price/3.5"))
show(c.get("/files/a/b/c.txt"))
show(c.get("/uid/12345678-1234-4234-8234-123456789abc"))
show(c.get("/echo?page=3&tag=a&tag=b&x=%20y"))
show(c.post("/echo", query_string={"k": "v"}))
show(c.post("/form", data={"a": "1", "b": "two words"}))
show(c.post("/json", json={"x": [1, 2], "y": "z"}))
show(c.post("/json", data="not json", content_type="application/json"))
show(c.post("/json", data="x", content_type="text/plain"))
show(c.post("/raw", data=b"bytes"))
c.set_cookie("sid", "zz")
show(c.get("/hdr", headers={"X-Token": "t1", "User-Agent": "ua"}))
show(c.get("/slash"), "location")
show(c.get("/slash/"))
show(c.get("/noslash/"))
show(c.get("/slash?a=1"), "location")
show(c.get("/tuple"))
show(c.get("/tuple3"), "x-extra", "content-type")
show(c.get("/resp"), "x-foo", "content-type", "set-cookie")
show(c.get("/lst"))
show(c.get("/abort/404"))
show(c.get("/abort/403"))
show(c.get("/abort/418"))
show(c.get("/abort/400"))
show(c.get("/abort/500"))
show(c.get("/boom"))
show(c.get("/value"))
show(c.get("/nowhere"))
show(c.get("/api/nowhere"), "content-type")
show(c.post("/"), "allow")
show(c.delete("/echo"), "allow")
show(c.open("/echo", method="OPTIONS"), "allow")
show(c.head("/"), "content-type")
show(c.get("/redir"), "location")
show(c.get("/redir", follow_redirects=True))
show(c.get("/blocked"), "x-seen")
show(c.get("/", headers={}), "x-seen")
show(c.get("/api/v1/things/7"))
show(c.post("/api/v1/ping"))
show(c.get("/api/v1/ping"), "allow")
with app.test_request_context("/"):
    print(url_for("item", item_id=5), url_for("echo", a=1, b="x y"), url_for("files", fp="p/q"))

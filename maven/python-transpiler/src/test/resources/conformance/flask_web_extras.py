import io

from flask import (Flask, Response, request, session, render_template, render_template_string,
                   stream_with_context, flash, get_flashed_messages)
from jinja2 import DictLoader

app = Flask(__name__)
app.secret_key = "conformance-secret"
app.jinja_env.loader = DictLoader({
    "layout.html": "<h1>{% block title %}Site{% endblock %}</h1>{% block content %}{% endblock %}",
    "items.html": "{% extends 'layout.html' %}{% block title %}{{ title|upper }}{% endblock %}"
                  "{% block content %}<ul>{% for i in items %}<li class=\"{{ 'first' if loop.first }}\">"
                  "{{ loop.index }}. {{ i }}</li>{% endfor %}</ul>{% endblock %}",
})


@app.route("/login", methods=["POST"])
def login():
    session["user"] = request.form["user"]
    session["visits"] = 0
    flash("welcome " + request.form["user"])
    return "ok"


@app.route("/visit")
def visit():
    session["visits"] = session.get("visits", 0) + 1
    return "%s visits=%d" % (session.get("user"), session["visits"])


@app.route("/peek")
def peek():
    msgs = get_flashed_messages()
    return "user=%s msgs=%s" % (session.get("user"), msgs)


@app.route("/logout")
def logout():
    who = session.pop("user", None)
    session.clear()
    return "bye " + str(who)


@app.route("/plain")
def plain():
    return "no session"


@app.route("/items")
def items():
    return render_template("items.html", title="list", items=["<a>", "b"])


@app.route("/inline")
def inline():
    return render_template_string("{{ greeting }}, {{ name|title }}! {{ request.path }} {{ session.get('user') }}",
                                  greeting="Hi & bye", name="ann lee")


@app.route("/upload", methods=["POST"])
def upload():
    f = request.files["doc"]
    data = f.read()
    others = request.files.getlist("extra")
    return "%s %s %d %r form=%s extras=%s missing=%s" % (
        f.filename, f.content_type, len(data), data, request.form.to_dict(),
        [(o.filename, o.read()) for o in others], "nope" in request.files)


@app.route("/stream")
def stream():
    def gen():
        for i in range(3):
            yield "chunk%d;" % i
        yield request.args.get("tail", "end")
    return Response(stream_with_context(gen()), mimetype="text/plain")


@app.route("/gen")
def gen_view():
    def g():
        yield b"a"
        yield "b"
    return g()


c = app.test_client()
r = c.get("/plain")
print(r.status_code, r.get_data(as_text=True), r.headers.get("Vary"), "Set-Cookie" in r.headers)
r = c.post("/login", data={"user": "ann"})
print(r.status_code, r.get_data(as_text=True), r.headers.get("Vary"), "Set-Cookie" in r.headers)
for _ in range(2):
    print(c.get("/visit").get_data(as_text=True))
print(c.get("/peek").get_data(as_text=True))
print(c.get("/peek").get_data(as_text=True))
r = c.get("/logout")
print(r.get_data(as_text=True), "Set-Cookie" in r.headers)
print(c.get("/visit").get_data(as_text=True))

c2 = app.test_client()
c2.set_cookie("session", "forged.value")
print(c2.get("/peek").get_data(as_text=True))

print(c.get("/items").get_data(as_text=True))
print(c.get("/inline").get_data(as_text=True))

r = c.post("/upload", data={"doc": (io.BytesIO(b"hello\r\nworld"), "notes.txt"), "title": "T1",
                            "extra": [(io.BytesIO(b"x1"), "a.bin", "application/x-test"),
                                      (io.BytesIO(b"x2"), "b.bin")]})
print(r.status_code, r.get_data(as_text=True))

r = c.get("/stream?tail=done")
print(r.status_code, r.mimetype, r.get_data(as_text=True))
print(c.get("/gen").get_data(as_text=True))

app2 = Flask("nokey")


@app2.route("/")
def nokey():
    try:
        session["x"] = 1
    except RuntimeError as e:
        return "error: " + str(e)
    return "stored"


print(app2.test_client().get("/").get_data(as_text=True))

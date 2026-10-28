# A small JSON API in plain Flask. Built by scripts/python-backend/build.sh
# into one native executable that serves through the backend's HTTP server;
# `python3 main.py` with Flask installed runs the same file unchanged.
import time

from flask import Flask, Response, jsonify, request, abort, stream_with_context

app = Flask(__name__)
todos = {}
next_id = [1]


@app.get("/todos")
def list_todos():
    done = request.args.get("done")
    items = list(todos.values())
    if done is not None:
        items = [t for t in items if t["done"] == (done == "true")]
    return jsonify(items)


@app.post("/todos")
def create_todo():
    body = request.get_json()
    if not body or "title" not in body:
        abort(400)
    todo = {"id": next_id[0], "title": body["title"], "done": False}
    todos[todo["id"]] = todo
    next_id[0] += 1
    return jsonify(todo), 201


@app.put("/todos/<int:todo_id>")
def update_todo(todo_id):
    todo = todos.get(todo_id)
    if todo is None:
        abort(404)
    todo.update(request.get_json() or {})
    return jsonify(todo)


@app.delete("/todos/<int:todo_id>")
def delete_todo(todo_id):
    if todos.pop(todo_id, None) is None:
        abort(404)
    return "", 204


@app.get("/fib/<int:n>")
def fib(n):
    a, b = 0, 1
    for _ in range(n):
        a, b = b, a + b
    return jsonify({"n": n, "fib": str(a)})


@app.route("/stream")
def stream():
    """Streamed as it is produced: one line a second, naming the request."""
    who = request.args.get("who", "anon")

    def gen():
        for i in range(3):
            yield "%s %d\n" % (request.args.get("who", who), i)
            if i < 2:
                time.sleep(1)
    return Response(stream_with_context(gen()), mimetype="text/plain")


@app.route("/badge.<fmt>")
def badge(fmt):
    """A PNG or JPEG drawn on the server: shapes, then text in the embedded
    TrueType font (com.codename1.image underneath)."""
    import io
    from PIL import Image, ImageDraw, ImageFont
    who = request.args.get("who", "todo")
    font = ImageFont.load_default(24)
    im = Image.new("RGB", (240, 64), (24, 32, 48))
    d = ImageDraw.Draw(im)
    d.rectangle((4, 4, 235, 59), outline=(250, 200, 40), width=3)
    d.text((120, 32), "Hi, " + who, fill=(255, 255, 255), font=font, anchor="mm")
    ink = Image.new("L", (240, 64), 0)
    ImageDraw.Draw(ink).text((120, 32), "Hi, " + who, fill=255, font=font, anchor="mm")
    if fmt == "json":
        return jsonify(bbox=list(ink.getbbox() or ()), length=font.getlength("Hi, " + who),
                       metrics=list(font.getmetrics()), name=list(font.getname()))
    out = io.BytesIO()
    if fmt == "png":
        im.save(out, "PNG")
        mime = "image/png"
    elif fmt == "jpg":
        im.save(out, "JPEG", quality=85)
        mime = "image/jpeg"
    else:
        abort(404)
    return Response(out.getvalue(), mimetype=mime)


if __name__ == "__main__":
    import sys
    app.run(host="127.0.0.1", port=int(sys.argv[1]) if len(sys.argv) > 1 else 8080)

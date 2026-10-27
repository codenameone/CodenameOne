# A small JSON API in plain Flask. Built by scripts/python-backend/build.sh
# into one native executable that serves through the backend's HTTP server;
# `python3 main.py` with Flask installed runs the same file unchanged.
from flask import Flask, jsonify, request, abort

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


if __name__ == "__main__":
    app.run(host="127.0.0.1", port=8080)

# Developer guide, Python chapter. Transpiled and compiled with the demos
# so the guide's code cannot drift from what builds.

# tag::python-interop-python-004[]
from flask import Flask, jsonify

app = Flask(__name__)


@app.get("/fib/<int:n>")
def fib(n):
    a, b = 0, 1
    for _ in range(n):
        a, b = b, a + b
    return jsonify({"n": n, "fib": str(a)})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8080)
# end::python-interop-python-004[]

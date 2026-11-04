#!/usr/bin/env python3
# What one Flask request costs an interpreter, with no server and no socket:
# Flask's test client calls the WSGI application directly. Set beside the
# server rows of compare.py, it shows how much of a served request is Flask
# and how much is the server around it.
#
#   <python> flask_cost.py [app dir]   (default: the flask-todo sample)
import os
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
APP = os.path.join(os.path.dirname(HERE), "python-backend", "samples", "flask-todo", "src")
sys.path.insert(0, sys.argv[1] if len(sys.argv) > 1 else APP)
from main import app  # noqa: E402

c = app.test_client()
for _ in range(3000):           # warm-up, so PyPy's and GraalPy's JITs have compiled it
    c.get("/fib/30")
n = 20000
t = time.perf_counter()
for _ in range(n):
    c.get("/fib/30")
dt = time.perf_counter() - t
print("%s: %.1f us a request, %.0f requests a second on one core"
      % (sys.implementation.name, dt / n * 1e6, n / dt))

import itsdangerous
from datetime import timedelta
from flask import Flask, session
from itsdangerous import (Signer, TimestampSigner, URLSafeSerializer, URLSafeTimedSerializer,
                          BadSignature, SignatureExpired)

# Freeze the clock so signatures are reproducible; both runtimes read it here.
itsdangerous.TimestampSigner.get_timestamp = lambda self: 1900000000

# Written by CPython Flask 3.1 with secret_key "conformance-secret".
REAL_A = (".eJyrVkpSsqpWUgCSSo7uGQVKtTpKuWCRlEwwVRIfD5SqUKoFSuQpWRnrKJVAxJWsog11jGKBwqXFqUVA"
          "NYk5mcmpSjpK5RmpeRATgIK--Xk6CgZmCr6JlQpGBkYmCgbmVgYWVgaWCu6-IUBDAb4uIlE.auHChw"
          ".Ots5ttF1cd5_5fecrJTMlAjFK4k")
REAL_Z = (".eJyrVkpSsqpWUgCSSo7uGQVKtTpKSZnpQF5iUvIgREo6SrlgB6dkgqmS-HigWyuUaoHuzlOyMtZRKoGI"
          "K1lFG-oYxQKFS4tTi0D-yclMTgVqL89IzYOYABT0zc_TUTAwU_BNrFQwMjAyUTAwtzKwsDKwVHD3DQEa"
          "CgDLtF3d.auHChw.po3LysCQmOeBe6wd5DPLQKiNJAI")

app = Flask(__name__)
app.secret_key = "conformance-secret"
app.permanent_session_lifetime = timedelta(days=36500)
app.config["PERMANENT_SESSION_LIFETIME"] = timedelta(days=36500)


@app.route("/show")
def show():
    out = []
    for k in sorted(session.keys()):
        v = session[k]
        out.append("%s=%s:%r" % (k, type(v).__name__, v))
    return "|".join(out) or "empty"


@app.route("/set")
def set_it():
    session["user"] = "bob"
    session["t"] = (1, "x")
    return "ok"


@app.route("/big")
def big():
    session["big"] = "xyz" * 60
    return "ok"


def cookie_of(resp):
    h = resp.headers.get("Set-Cookie")
    return h.split(";")[0].split("=", 1)[1]


c = app.test_client()
c.set_cookie("session", REAL_A)
print(c.get("/show").get_data(as_text=True))
c.set_cookie("session", REAL_Z)
print(c.get("/show").get_data(as_text=True))
c.set_cookie("session", REAL_A[:-2] + "xx")
print(c.get("/show").get_data(as_text=True))

c2 = app.test_client()
made = cookie_of(c2.get("/set"))
print(made)
print(c2.get("/show").get_data(as_text=True))
big_cookie = cookie_of(c2.get("/big"))
print(big_cookie.startswith("."), len(big_cookie) < 200)
print(c2.get("/show").get_data(as_text=True))

s = Signer("k")
print(s.sign("hello"), s.unsign(s.sign("hello")), s.validate(b"hello.bad"))
ts = TimestampSigner("k", salt="x")
v = ts.sign("v")
print(v, ts.unsign(v, max_age=5))
ser = URLSafeSerializer("k")
print(ser.dumps({"a": [1, 2]}), ser.loads(ser.dumps({"a": [1, 2]})))
tser = URLSafeTimedSerializer("k", salt="s")
tok = tser.dumps("payload")
print(tok, tser.loads(tok))
try:
    URLSafeTimedSerializer("other", salt="s").loads(tok)
except BadSignature as e:
    print("bad", type(e).__name__)
itsdangerous.TimestampSigner.get_timestamp = lambda self: 1900000100
try:
    tser.loads(tok, max_age=10)
except SignatureExpired as e:
    print("expired", e)
rot = URLSafeSerializer(["old", "new"])
print(URLSafeSerializer("old").loads(rot.dumps(1)) if False else rot.loads(URLSafeSerializer("old").dumps(7)))

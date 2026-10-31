# requests against HttpTestServer (see ConformanceTest); the expected output
# is the real requests' against the same server.
import requests
from _testbase import BASE

r = requests.get(BASE + "/get", params={"q": "a b", "n": 3, "skip": None, "many": [1, 2]},
                 headers={"X-Test": "yes"})
print(r.status_code, r.ok, r.reason, r.encoding, r.headers["content-type"], "Content-Type" in r.headers)
print(r.json())
print(r.url == BASE + "/get?q=a+b&n=3&many=1&many=2", r.request.method, bool(r))

r = requests.post(BASE + "/anything", json={"b": [1, 2], "a": None, "s": "x"})
print(r.json())
r = requests.post(BASE + "/anything", data={"name": "py thon", "tags": ["a", "b"]})
print(r.json())
r = requests.put(BASE + "/anything", data="raw text", headers={"Content-Type": "text/plain", "X-A": "1"})
print(r.json())
r = requests.patch(BASE + "/anything?x=1", data=b"bytes")
print(r.json()["method"], r.json()["args"], r.json()["body"])
r = requests.delete(BASE + "/anything")
print(r.json()["method"], r.json()["body"] == "")

r = requests.get(BASE + "/status/404")
print(r.status_code, r.ok, r.reason, r.text, repr(r))
try:
    r.raise_for_status()
except requests.HTTPError as e:
    print("HTTPError:", str(e).replace(BASE, "BASE"), e.response.status_code)
r = requests.get(BASE + "/status/503")
try:
    r.raise_for_status()
except requests.exceptions.RequestException as e:
    print(type(e).__name__, str(e).replace(BASE, "BASE"))

r = requests.get(BASE + "/redirect")
print(r.status_code, r.json()["args"], r.url.replace(BASE, "BASE"))
r = requests.get(BASE + "/redirect", allow_redirects=False)
print(r.status_code, r.headers["Location"], r.is_redirect)

r = requests.get(BASE + "/headers-out")
print(r.headers["x-one"], r.headers.get("X-TWO"), r.headers.get("missing", "-"), r.text, r.encoding)

print(requests.get(BASE + "/basic-auth", auth=("user", "pass")).json())
print(requests.get(BASE + "/basic-auth", auth=("user", "nope")).status_code)

try:
    requests.get(BASE + "/bad-json").json()
except requests.JSONDecodeError as e:
    print("JSONDecodeError", e, isinstance(e, ValueError))

with requests.Session() as s:
    s.headers.update({"X-Session": "s1"})
    s.params = {"fixed": "1"}
    r = s.get(BASE + "/get", params={"extra": "2"}, headers={"X-Call": "c"})
    print(r.json()["args"], r.json()["headers"])

for bad in ["nohost", "ftp://example.com/x"]:
    try:
        requests.get(bad)
    except (requests.exceptions.MissingSchema, requests.exceptions.InvalidSchema) as e:
        print(type(e).__name__, e)
try:
    requests.get("http://127.0.0.1:9/nothing", timeout=2)
except requests.ConnectionError as e:
    print("ConnectionError", isinstance(e, requests.RequestException))
print(requests.codes.ok, requests.codes.not_found, requests.codes["teapot"])

# urllib.request and http.client against HttpTestServer (see ConformanceTest);
# the expected output is CPython's against the same server.
import json
import urllib.request
import urllib.error
import urllib.parse
import http.client
from http import HTTPStatus
from _testbase import BASE

with urllib.request.urlopen(BASE + "/get?q=" + urllib.parse.quote("a b")) as r:
    print(r.status, r.reason, r.getcode(), r.headers.get_content_type(), r.url == BASE + "/get?q=a%20b")
    d = json.loads(r.read())
    print(d["method"], d["args"])

req = urllib.request.Request(BASE + "/anything", data=json.dumps({"k": [1, 2]}).encode(),
                             headers={"Content-Type": "application/json", "X-Mine": "1"}, method="PUT")
print(req.get_method(), req.host == BASE[7:], req.selector, req.has_header("X-mine"))
with urllib.request.urlopen(req) as r:
    d = json.loads(r.read().decode("utf-8"))
    print(d["method"], d["body"], d["content_type"], d["headers"])

form = urllib.parse.urlencode({"name": "py thon", "n": 3}).encode("ascii")
with urllib.request.urlopen(BASE + "/anything", data=form) as r:
    d = json.load(r)
    print(d["method"], d["body"], d["content_type"])

try:
    urllib.request.urlopen(BASE + "/status/404")
except urllib.error.HTTPError as e:
    print("HTTPError", e.code, e.reason, str(e), isinstance(e, urllib.error.URLError), isinstance(e, OSError))
    e.close()

with urllib.request.urlopen(BASE + "/redirect") as r:
    print("redirect", r.status, r.url.replace(BASE, "BASE"), json.loads(r.read())["args"])

with urllib.request.urlopen(BASE + "/headers-out") as r:
    print(sorted(k.lower() for k in r.headers.keys() if k.lower().startswith("x-")))

try:
    urllib.request.urlopen("ftp-nope://example")
except urllib.error.URLError as e:
    print("URLError", e.reason)

try:
    urllib.request.urlopen(BASE + "/basic-auth")
except urllib.error.HTTPError as e:
    print("auth needed", e.code)
mgr = urllib.request.HTTPPasswordMgrWithDefaultRealm()
mgr.add_password(None, BASE, "user", "pass")
opener = urllib.request.build_opener(urllib.request.HTTPBasicAuthHandler(mgr))
try:
    opener.open(BASE + "/basic-auth")
except urllib.error.HTTPError as e:
    print("no challenge, no retry", e.code)
import base64
req = urllib.request.Request(BASE + "/basic-auth")
req.add_header("Authorization", "Basic " + base64.b64encode(b"user:pass").decode())
with urllib.request.urlopen(req) as r:
    print("auth", r.status, r.read())

host = BASE[7:]
c = http.client.HTTPConnection(host, timeout=10)
c.request("POST", "/anything?x=1", body=b"raw", headers={"Content-Type": "text/plain"})
r = c.getresponse()
print(r.status, r.reason, r.getheader("Content-Type"), json.loads(r.read())["body"])
c.close()
c = http.client.HTTPConnection(host.split(":")[0], int(host.split(":")[1]))
c.request("GET", "/status/503")
r = c.getresponse()
print(r.status, r.reason, r.read(5), http.client.responses[503])
print(HTTPStatus.NOT_FOUND, HTTPStatus(201).phrase, HTTPStatus.OK == 200, HTTPStatus.IM_A_TEAPOT.is_client_error)
print(http.client.NOT_FOUND, repr(HTTPStatus.OK))

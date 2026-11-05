# httpx against HttpTestServer (see ConformanceTest); the expected output is
# the real httpx's against the same server.
import asyncio
import httpx
from _testbase import BASE

r = httpx.get(BASE + "/get", params={"q": "a b", "n": 3}, headers={"X-Test": "yes"})
print(r.status_code, r.reason_phrase, r.is_success, r.is_error, r.encoding, r.headers["content-type"])
print(r.json(), r.url.path, r.url.params["q"], str(r.url).replace(BASE, "BASE"))
print(r.http_version, r.request.method, repr(r))

r = httpx.post(BASE + "/anything", json={"b": [1, 2], "a": None})
print(r.json())
r = httpx.post(BASE + "/anything", data={"k": "v w"})
print(r.json())
r = httpx.put(BASE + "/anything", content="raw", headers={"X-Put": "1"})
print(r.json())

r = httpx.get(BASE + "/redirect")
print(r.status_code, r.is_redirect, r.headers["location"])
r = httpx.get(BASE + "/redirect", follow_redirects=True)
print(r.status_code, r.json()["args"])

r = httpx.get(BASE + "/status/404")
print(r.status_code, r.is_client_error, r.text)
try:
    r.raise_for_status()
except httpx.HTTPStatusError as e:
    print(str(e).replace(BASE, "BASE"))
    print(e.response.status_code, e.request.method)
print(httpx.get(BASE + "/get").raise_for_status().status_code)

with httpx.Client(base_url=BASE, headers={"X-Client": "c"}, params={"p": "1"}) as client:
    r = client.get("/get", params={"extra": "e"})
    print(r.json()["args"], r.json()["headers"])
    r = client.get("basic-auth", auth=("user", "pass"))
    print(r.status_code, r.json())


async def main():
    async with httpx.AsyncClient(base_url=BASE) as client:
        r = await client.get("/get", params={"async": "yes"})
        print("async", r.status_code, r.json()["args"])
        r = await client.post("/anything", json=[1, 2, 3])
        print("async", r.json()["body"])

asyncio.run(main())

try:
    httpx.get("nohost/x")
except httpx.UnsupportedProtocol as e:
    print("UnsupportedProtocol", e)
try:
    httpx.get("http://127.0.0.1:9/nothing", timeout=2)
except httpx.ConnectError as e:
    print("ConnectError", isinstance(e, httpx.RequestError), isinstance(e, httpx.HTTPError))
print(httpx.codes.OK, httpx.codes.NOT_FOUND, httpx.Timeout(3.0))
h = httpx.Headers({"A": "1"})
h["b"] = "2"
print(h["a"], h["B"], list(h.keys()), "a" in h)

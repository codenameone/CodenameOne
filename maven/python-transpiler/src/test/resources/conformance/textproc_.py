# warnings, shlex, html, urllib.parse, secrets, queue.
import warnings, shlex, html, secrets, queue
from urllib.parse import (urlparse, urlsplit, urlunparse, urljoin, urldefrag, quote, quote_plus, unquote,
                          unquote_plus, urlencode, parse_qs, parse_qsl, unquote_to_bytes)

with warnings.catch_warnings(record=True) as w:
    warnings.simplefilter("always")
    warnings.warn("first")
    warnings.warn("dep", DeprecationWarning)
    warnings.warn(RuntimeWarning("rt"))
    print([(type(x.message).__name__, str(x.message), x.category.__name__) for x in w])
with warnings.catch_warnings(record=True) as w:
    warnings.simplefilter("once")
    for i in range(3):
        warnings.warn("same")
    warnings.filterwarnings("ignore", message="skip.*")
    warnings.warn("skip me")
    warnings.warn("keep me")
    print(len(w), [str(x.message) for x in w])
with warnings.catch_warnings():
    warnings.simplefilter("error")
    try:
        warnings.warn("boom", UserWarning)
    except UserWarning as e:
        print("raised", e)
print(warnings.formatwarning("msg", UserWarning, "f.py", 3).strip())
try:
    warnings.simplefilter("bogus")
except ValueError as e:
    print(e)

print(shlex.split('ls -l "my file.txt" \'single q\' a\\ b # c'), shlex.split("a # b", comments=True))
print(shlex.quote("safe"), shlex.quote("it's here"), shlex.quote(""), shlex.join(["echo", "a b", "c"]))
lex = shlex.shlex("a=1; b='x y' && c", posix=True, punctuation_chars=True)
print(list(lex))
print(list(shlex.shlex("foo(bar, 'baz')")))
for bad in ['"unclosed', "trail\\"]:
    try:
        shlex.split(bad)
    except ValueError as e:
        print(e)

print(html.escape("<a href=\"x\">'&'</a>"), html.escape("<b>", quote=False))
print(html.unescape("&lt;p&gt; &amp;amp; &copy; &#169; &#xA9; &notit; &ampx &#0; &#x80; &nbsp;| &NotEqualTilde; &bogus;"))

u = urlparse("https://user:pw@Example.COM:8080/a/b;p?q=1&r=2#frag")
print(u, u.hostname, u.port, u.username, u.password, u.geturl())
print(urlsplit("//net/loc?x"), urlsplit("mailto:a@b.c"), urlsplit("http://[::1]:80/x").hostname, urlparse(b"http://h/p").netloc)
for rel in ["g", "./g", "g/", "/g", "//g", "?y", "g?y", "#s", "g#s", "..", "../g", "../..", "../../../g", "g;x", "", ".", "./"]:
    print(repr(rel), urljoin("http://a/b/c/d;p?q", rel))
print(urldefrag("http://x/y#z"), urlunparse(("http", "h", "/p", "", "a=1", "f")))
print(quote("a b&c/d?é"), quote("/x y", safe=""), quote_plus("a b+c"), unquote("%E2%82%AC%2"), unquote_plus("a+b%20c"), unquote_to_bytes("%41%zz"))
print(urlencode({"a": 1, "b": "x y", "c": "é"}), urlencode([("k", [1, 2])], doseq=True), urlencode({"k": [1, 2]}))
print(parse_qs("a=1&a=2&b=&c=x+y%21"), parse_qsl("a=1&b=&c", keep_blank_values=True), parse_qs("x=1;y=2", separator=";"))

print(len(secrets.token_bytes(16)), len(secrets.token_hex(8)), len(secrets.token_urlsafe(9)), 0 <= secrets.randbelow(10) < 10,
      secrets.choice("ab") in "ab", secrets.compare_digest("abc", "abc"), secrets.compare_digest(b"abc", b"abd"), secrets.randbits(8) < 256)

q = queue.Queue(maxsize=2)
q.put(1)
q.put(2)
print(q.full(), q.qsize())
try:
    q.put_nowait(3)
except queue.Full:
    print("full")
print(q.get(), q.get(), q.empty())
try:
    q.get_nowait()
except queue.Empty:
    print("empty")
pq = queue.PriorityQueue()
for x in [5, 1, 4, 2]:
    pq.put((x, str(x)))
print([pq.get() for _ in range(4)])
lq = queue.LifoQueue()
for x in "abc":
    lq.put(x)
print(lq.get(), lq.get(), lq.qsize())
sq = queue.SimpleQueue()
sq.put(9)
print(sq.get(), sq.empty())

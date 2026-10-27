# weakref and atexit. Referents stay alive throughout: when a dead one is
# cleared is the collector's business, not something a program can rely on.
import weakref, atexit

class Node:
    def __init__(self, name):
        self.name = name
    def hello(self):
        return "hi " + self.name
    def __repr__(self):
        return "Node(%s)" % self.name

a, b, c = Node("a"), Node("b"), Node("c")
r = weakref.ref(a)
print(r() is a, r() == a, type(r).__name__, isinstance(r, weakref.ref), r.__callback__, weakref.ref(a) == r, hash(r) == hash(weakref.ref(a)))
for bad in [1, "s", (1,), [1], {}, None]:
    try:
        weakref.ref(bad)
    except TypeError as e:
        print(e)
print(weakref.ref(len) if False else "", weakref.ref(Node)() is Node, weakref.ref(print if False else Node.hello)() is Node.hello)
p = weakref.proxy(a)
print(p.name, p.hello(), str(p))
p.name = "a2"
print(a.name)
a.name = "a"
wm = weakref.WeakMethod(a.hello)
print(wm()(), wm() == a.hello)
wvd = weakref.WeakValueDictionary()
wvd["x"] = a
wvd["y"] = b
print(len(wvd), sorted(wvd.keys()), wvd["x"], wvd.get("z", "none"), "y" in wvd, sorted(wvd.items(), key=lambda kv: kv[0]))
print(wvd.pop("y"), len(wvd), wvd.setdefault("q", c), sorted(wvd))
wkd = weakref.WeakKeyDictionary()
wkd[a] = 1
wkd[b] = 2
print(len(wkd), wkd[a], a in wkd, c in wkd, 5 in wkd, sorted(wkd.values()), wkd.get(c, 0))
ws = weakref.WeakSet([a, b])
ws.add(c)
ws.discard(b)
print(len(ws), a in ws, b in ws, sorted(n.name for n in ws))
f = weakref.finalize(c, print, "finalized", "c")
print(f.alive, f.peek()[1:] == (print, ("finalized", "c"), {}))
f()
print(f.alive, f())

def bye(who, punct="!"):
    print("bye", who + punct)

atexit.register(bye, "first")
atexit.register(bye, "second", punct="?")
@atexit.register
def last():
    print("registered last, runs first")
def gone():
    print("never")
atexit.register(gone)
atexit.unregister(gone)
weakref.finalize(a, print, "a finalized at exit")
print("main done")

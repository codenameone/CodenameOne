import threading
import time

# Contention on a plain Lock between real threads.
lock = threading.Lock()
counter = [0]


def worker(n):
    for _ in range(n):
        with lock:
            v = counter[0]
            time.sleep(0)
            counter[0] = v + 1


ts = [threading.Thread(target=worker, args=(200,)) for _ in range(4)]
for t in ts:
    t.start()
for t in ts:
    t.join()
print("counter", counter[0])

# A timeout on a lock another thread holds.
held = threading.Lock()
held.acquire()
t0 = time.monotonic()
got = held.acquire(timeout=0.05)
waited = time.monotonic() - t0
print("timed out", got, waited >= 0.04)
print("non-blocking", held.acquire(False))

# Released by another thread while this one waits.
def later():
    time.sleep(0.02)
    held.release()


threading.Thread(target=later).start()
print("acquired after release", held.acquire(timeout=5))
held.release()
try:
    held.release()
except RuntimeError as e:
    print("RuntimeError", e)

# RLock: reentrant for its owner, and other threads wait for the last release.
r = threading.RLock()
order = []


def other():
    with r:
        order.append("other")


with r:
    with r:
        t = threading.Thread(target=other)
        t.start()
        time.sleep(0.02)
        order.append("owner")
t.join()
print("rlock order", order)
try:
    threading.RLock().release()
except RuntimeError:
    print("rlock release unowned: RuntimeError")

# GIL stress: CPU-bound threads all finish, each sees its own results.
results = {}


def spin(k):
    s = 0
    for i in range(20000):
        s += i % (k + 2)
        if i % 1000 == 0:
            time.sleep(0)
    results[k] = s


ts = [threading.Thread(target=spin, args=(k,)) for k in range(6)]
for t in ts:
    t.start()
for t in ts:
    t.join()
print("spin", sorted(results.items()))

# Condition and Event wake a waiting thread.
cond = threading.Condition()
items = []


def produce():
    time.sleep(0.01)
    with cond:
        items.append(1)
        cond.notify()


threading.Thread(target=produce).start()
with cond:
    ok = cond.wait_for(lambda: items, timeout=5)
print("condition", bool(ok), items)
ev = threading.Event()
print("event timeout", ev.wait(0.01))
threading.Thread(target=ev.set).start()
print("event set", ev.wait(5))
sem = threading.Semaphore(1)
sem.acquire()
print("semaphore timeout", sem.acquire(timeout=0.01))
sem.release()

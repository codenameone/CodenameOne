import threading
import queue
import time
from concurrent.futures import ThreadPoolExecutor, ProcessPoolExecutor, as_completed, wait, FIRST_COMPLETED

# producer / consumer over a bounded queue
q = queue.Queue(maxsize=2)
got = []


def producer():
    for i in range(10):
        q.put(i)
    q.put(None)


def consumer():
    while True:
        item = q.get()
        q.task_done()
        if item is None:
            break
        got.append(item * item)


tp = threading.Thread(target=producer)
tc = threading.Thread(target=consumer, name="consumer")
tc.start()
tp.start()
tp.join()
tc.join()
print("squares", got)
print("alive", tp.is_alive(), tc.is_alive(), tc.name)

# a lock protecting a counter across threads that sleep in between
lock = threading.Lock()
counter = [0]


def bump(n):
    for _ in range(n):
        with lock:
            v = counter[0]
            time.sleep(0)
            counter[0] = v + 1


ts = [threading.Thread(target=bump, args=(50,)) for _ in range(4)]
for t in ts:
    t.start()
for t in ts:
    t.join()
print("counter", counter[0])

# events and conditions
ev = threading.Event()
order = []


def waiter():
    ev.wait()
    order.append("woke")


w = threading.Thread(target=waiter)
w.start()
time.sleep(0.01)
order.append("set")
ev.set()
w.join()
print("order", order)
print("wait timeout", threading.Event().wait(0.01))

cond = threading.Condition()
items = []


def cons():
    with cond:
        cond.wait_for(lambda: len(items) >= 3)
        print("cond saw", sorted(items))


c = threading.Thread(target=cons)
c.start()
for i in range(3):
    with cond:
        items.append(i)
        cond.notify()
c.join()

# rlock, semaphore, barrier
r = threading.RLock()
with r:
    with r:
        print("rlock nested ok")
sem = threading.BoundedSemaphore(2)
print("sem", sem.acquire(), sem.acquire(), sem.acquire(False))
sem.release()
try:
    sem.release()
    sem.release()
except ValueError as e:
    print("sem error", e)

b = threading.Barrier(3)
idx = []


def party():
    idx.append(b.wait())


ps = [threading.Thread(target=party) for _ in range(3)]
for p in ps:
    p.start()
for p in ps:
    p.join()
print("barrier", sorted(idx))

# thread-local data
data = threading.local()
data.x = "main"
seen = []


def loc():
    seen.append(hasattr(data, "x"))
    data.x = "worker"
    seen.append(data.x)


t = threading.Thread(target=loc)
t.start()
t.join()
print("local", seen, data.x)
print("main", threading.current_thread() is threading.main_thread(), threading.current_thread().name)

# timer
fired = []
tm = threading.Timer(0.01, lambda: fired.append(1))
tm.start()
tm.join()
tm2 = threading.Timer(10, lambda: fired.append(2))
tm2.start()
tm2.cancel()
tm2.join()
print("timer", fired)

# futures
with ThreadPoolExecutor(max_workers=3) as ex:
    print("map", list(ex.map(lambda x: x * 10, range(5))))
    fs = [ex.submit(pow, 2, i) for i in range(6)]
    print("completed", sorted(f.result() for f in as_completed(fs)))
    bad = ex.submit(lambda: 1 / 0)
    try:
        bad.result()
    except ZeroDivisionError as e:
        print("future raised", type(e).__name__)
    print("exception", type(bad.exception()).__name__)
    done, not_done = wait([ex.submit(time.sleep, 0)], return_when=FIRST_COMPLETED)
    print("wait", len(done), len(not_done))
    cb = []
    f = ex.submit(lambda: 7)
    f.result()
    f.add_done_callback(lambda fut: cb.append(fut.result()))
    print("callback", cb, f.done(), f.cancelled())

if __name__ == "__main__":
    with ProcessPoolExecutor(2) as px:
        print("process pool", list(px.map(abs, [-1, -2, 3])))

# queue variants
lq = queue.LifoQueue()
for i in range(3):
    lq.put(i)
print("lifo", [lq.get() for _ in range(3)])
try:
    queue.Queue().get(timeout=0.01)
except queue.Empty:
    print("empty after timeout")
sq = queue.SimpleQueue()
threading.Thread(target=lambda: sq.put("hello")).start()
print("simple", sq.get())
jq = queue.Queue()
for i in range(3):
    jq.put(i)


def drain():
    while not jq.empty():
        jq.get()
        jq.task_done()


threading.Thread(target=drain).start()
jq.join()
print("joined", jq.qsize())

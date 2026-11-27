import sqlite3
from _harness import measure

def work(n):
    c = sqlite3.connect(":memory:")
    c.execute("CREATE TABLE responses (id TEXT PRIMARY KEY, model TEXT, prompt TEXT)")
    t = 0
    with c:
        for i in range(n):
            t += c.execute("INSERT INTO [responses] ([id], [model], [prompt]) VALUES (?, ?, ?)", ["r%d" % i, "echo", "p"]).lastrowid
    return t

measure("zz_insert", work, 20000, 5)

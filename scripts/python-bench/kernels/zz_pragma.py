import sqlite3
from _harness import measure

def work(n):
    c = sqlite3.connect(":memory:")
    c.execute("CREATE TABLE responses (id TEXT PRIMARY KEY, model TEXT, prompt TEXT, system TEXT, options_json TEXT, response TEXT, conversation_id TEXT, duration_ms INTEGER, datetime_utc TEXT, input_tokens INTEGER, output_tokens INTEGER)")
    t = 0
    for i in range(n):
        t += len([r[1] for r in c.execute("PRAGMA table_info([responses])").fetchall()])
    return t

measure("zz_pragma", work, 5000, 5)

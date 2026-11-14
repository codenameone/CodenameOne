# SQLite the way sqlite-utils drives it when llm logs a reply: for each row
# the table's columns are read back (PRAGMA table_info), an INSERT is built
# as text from them, a JSON field is serialized, the row goes in with bound
# parameters and the statement's rows come back as tuples. In memory, so the
# disk's sync time stays out of it and only the Python side is measured.
import json
import sqlite3

from _harness import measure


def quote(name):
    return "[" + name.replace("]", "]]") + "]"


def columns(conn, table):
    return [row[1] for row in conn.execute("PRAGMA table_info(%s)" % quote(table)).fetchall()]


def insert(conn, table, record):
    cols = columns(conn, table)
    names = [k for k in record if k in cols]
    sql = "INSERT INTO %s (%s) VALUES (%s)" % (
        quote(table), ", ".join(quote(n) for n in names), ", ".join("?" for _ in names))
    cur = conn.execute(sql, [record[n] for n in names])
    return cur.lastrowid


def work(n):
    conn = sqlite3.connect(":memory:")
    conn.execute("CREATE TABLE conversations (id TEXT PRIMARY KEY, name TEXT, model TEXT)")
    conn.execute("CREATE TABLE responses (id TEXT PRIMARY KEY, model TEXT, prompt TEXT, "
                 "system TEXT, options_json TEXT, response TEXT, conversation_id TEXT, "
                 "duration_ms INTEGER, datetime_utc TEXT, input_tokens INTEGER, "
                 "output_tokens INTEGER)")
    total = 0
    with conn:
        insert(conn, "conversations", {"id": "c1", "name": "bench", "model": "echo"})
        for i in range(n):
            options = {"temperature": 0.5 + (i % 5) / 10.0, "stop": ["\n", "END"], "seed": i}
            total += insert(conn, "responses", {
                "id": "r%06d" % i,
                "model": "echo",
                "prompt": "prompt number %d about %s" % (i, "alpha bravo"[: 1 + i % 11]),
                "system": None,
                "options_json": json.dumps(options),
                "response": "reply %d" % (i * 7),
                "conversation_id": "c1",
                "duration_ms": i % 97,
                "datetime_utc": "2026-11-08T10:%02d:%02d" % (i // 60 % 60, i % 60),
                "input_tokens": i % 13,
                "output_tokens": i % 17,
            })
    for row in conn.execute("SELECT id, options_json, duration_ms FROM responses "
                            "WHERE conversation_id = ? ORDER BY id", ("c1",)):
        total += row[2] + len(json.loads(row[1])["stop"])
    conn.close()
    return total


measure("sqlite_rows", work, 3000, 5)

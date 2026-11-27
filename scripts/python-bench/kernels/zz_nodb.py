# SQLite the way sqlite-utils drives it when an application logs a reply: for each row
# the table's columns are read back (PRAGMA table_info), an INSERT is built
# as text from them, a JSON field is serialized, the row goes in with bound
# parameters and the statement's rows come back as tuples. In memory, so the
# disk's sync time stays out of it and only the Python side is measured.
import json
import sqlite3

from _harness import measure


def quote(name):
    return "[" + name.replace("]", "]]") + "]"


COLS = {"conversations": ["id", "name", "model"], "responses": ["id", "model", "prompt", "system", "options_json", "response", "conversation_id", "duration_ms", "datetime_utc", "input_tokens", "output_tokens"]}
def columns(conn, table):
    return [row for row in COLS[table]]


def insert(conn, table, record):
    cols = columns(conn, table)
    names = [k for k in record if k in cols]
    sql = "INSERT INTO %s (%s) VALUES (%s)" % (
        quote(table), ", ".join(quote(n) for n in names), ", ".join("?" for _ in names))
    vals = [record[n] for n in names]
    return len(sql) + len(vals)


def work(n):
    conn = None
    ("CREATE TABLE conversations (id TEXT PRIMARY KEY, name TEXT, model TEXT)")
    ("CREATE TABLE responses (id TEXT PRIMARY KEY, model TEXT, prompt TEXT, "
                 "system TEXT, options_json TEXT, response TEXT, conversation_id TEXT, "
                 "duration_ms INTEGER, datetime_utc TEXT, input_tokens INTEGER, "
                 "output_tokens INTEGER)")
    total = 0
    if True:
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
    return total


measure("sqlite_nodb", work, 3000, 5)

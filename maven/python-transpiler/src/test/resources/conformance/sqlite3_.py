import datetime
import sqlite3

con = sqlite3.connect(":memory:")
cur = con.cursor()
cur.execute("CREATE TABLE movie(id INTEGER PRIMARY KEY, title TEXT NOT NULL UNIQUE, year INTEGER, score REAL, poster BLOB)")
print(cur.description, cur.rowcount)
cur.execute("INSERT INTO movie(title, year, score) VALUES (?, ?, ?)", ("Monty Python", 1975, 8.2))
print("lastrowid", cur.lastrowid, "rowcount", cur.rowcount, "in_tx", con.in_transaction)
data = [("And Now for Something", 1971, 7.5), ("Life of Brian", 1979, 8.0), ("Meaning of Life", 1983, 7.6)]
cur.executemany("INSERT INTO movie(title, year, score) VALUES (?, ?, ?)", data)
print("many rowcount", cur.rowcount)
con.commit()
print("after commit in_tx", con.in_transaction, "total", con.total_changes)

for row in cur.execute("SELECT id, title, year, score FROM movie ORDER BY year"):
    print(row, [type(v).__name__ for v in row])
print([d[0] for d in cur.description])

cur.execute("SELECT title FROM movie WHERE year > :y AND score >= :s", {"y": 1974, "s": 7.9})
print(cur.fetchall())
cur.execute("SELECT ?2, ?1", ("a", "b"))
print(cur.fetchone())
cur.execute("SELECT count(*), max(score), min(title) FROM movie")
print(cur.fetchone())
print(cur.fetchone())

cur.execute("UPDATE movie SET score = score + 0.5 WHERE year < ?", (1980,))
print("updated", cur.rowcount)
con.rollback()
print(con.execute("SELECT score FROM movie WHERE year = 1975").fetchone())

con.execute("UPDATE movie SET poster = ? WHERE id = 1", (b"\x00\x01\xff",))
con.execute("INSERT INTO movie(title, year) VALUES ('Untitled', NULL)")
for r in con.execute("SELECT poster, year, score FROM movie WHERE id IN (1, 5) ORDER BY id"):
    print(r)

try:
    con.execute("INSERT INTO movie(title) VALUES (?)", ("Life of Brian",))
except sqlite3.IntegrityError as e:
    print("IntegrityError", e)
try:
    con.execute("SELECT * FROM no_such_table")
except sqlite3.OperationalError as e:
    print("OperationalError", e)
try:
    con.execute("SELECT ?", (1, 2))
except sqlite3.ProgrammingError as e:
    print("ProgrammingError", e)
try:
    con.execute("SELECT 1; SELECT 2")
except sqlite3.ProgrammingError as e:
    print("ProgrammingError", e)
try:
    con.execute("SELECT :a", {"b": 1})
except sqlite3.ProgrammingError as e:
    print("ProgrammingError", e)
print(issubclass(sqlite3.IntegrityError, sqlite3.DatabaseError), issubclass(sqlite3.Error, Exception))
con.commit()

con.row_factory = sqlite3.Row
r = con.execute("SELECT id, title AS name FROM movie WHERE id = 2").fetchone()
print(r["name"], r["NAME"], r[0], r.keys(), tuple(r), len(r))
con.row_factory = None

con.executescript("""
    CREATE TABLE log(id INTEGER PRIMARY KEY AUTOINCREMENT, msg TEXT);
    -- a comment; with a semicolon
    INSERT INTO log(msg) VALUES ('one; two');
    INSERT INTO log(msg) VALUES ('three');
    CREATE TRIGGER t AFTER INSERT ON log BEGIN UPDATE log SET msg = upper(msg) WHERE id = new.id; END;
    INSERT INTO log(msg) VALUES ('four');
""")
print(con.execute("SELECT msg FROM log ORDER BY id").fetchall())

with con:
    con.execute("INSERT INTO log(msg) VALUES ('ctx')")
try:
    with con:
        con.execute("INSERT INTO log(msg) VALUES ('lost')")
        raise ValueError("boom")
except ValueError:
    pass
print(con.execute("SELECT count(*) FROM log").fetchone())


class Point:
    def __init__(self, x, y):
        self.x, self.y = x, y


sqlite3.register_adapter(Point, lambda p: "%f;%f" % (p.x, p.y))
sqlite3.register_converter("point", lambda b: tuple(float(v) for v in b.split(b";")))
con2 = sqlite3.connect(":memory:", detect_types=sqlite3.PARSE_DECLTYPES | sqlite3.PARSE_COLNAMES)
con2.execute("CREATE TABLE p(pt point, d date, ts timestamp)")
con2.execute("INSERT INTO p VALUES (?, ?, ?)", (Point(1, 2.5), datetime.date(2024, 2, 29),
                                                 datetime.datetime(2024, 1, 2, 3, 4, 5, 600)))
print(con2.execute("SELECT pt, d, ts FROM p").fetchone())
print(con2.execute('SELECT pt AS "p [point]" FROM p').fetchone())
print(con2.execute("SELECT pt FROM p").description[0][0])
con2.close()
try:
    con2.execute("SELECT 1")
except sqlite3.ProgrammingError as e:
    print("ProgrammingError", e)

con.text_factory = bytes
print(con.execute("SELECT 'abc'").fetchone())
con.text_factory = str

for line in con.iterdump():
    if "movie" in line or "log" in line:
        print(line)
print(sqlite3.complete_statement("SELECT 1;"), sqlite3.complete_statement("SELECT 1"))

con3 = sqlite3.connect(":memory:", autocommit=False)
print("autocommit False in_tx", con3.in_transaction)
con3.execute("CREATE TABLE a(x)")
con3.commit()
print("still", con3.in_transaction)
con3.close()
con4 = sqlite3.connect(":memory:", isolation_level=None)
con4.execute("CREATE TABLE a(x)")
con4.execute("INSERT INTO a VALUES (1)")
print("isolation None in_tx", con4.in_transaction)
con4.execute("BEGIN")
print("explicit", con4.in_transaction)
con4.execute("INSERT INTO a VALUES (2)")
con4.execute("ROLLBACK")
print(con4.execute("SELECT * FROM a").fetchall())
print(sqlite3.paramstyle, sqlite3.apilevel)
con.close()

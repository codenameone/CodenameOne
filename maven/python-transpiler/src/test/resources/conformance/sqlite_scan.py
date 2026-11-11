# Placeholders, quoting, comments and statement ends, as sqlite3 finds them in a
# statement's text before the engine sees it.
import sqlite3

db = sqlite3.connect(":memory:")
db.execute("create table t (a, b, [c?d] text, \"e:f\" text)")
db.execute("insert into t values (?, ?, ?, ?)", (1, "x", "q", "r"))
db.execute("insert into t values (:a, @b, $c, :a)", {"a": 2, "b": "y", "c": "z"})
db.execute("insert into t values (?2, ?1, ?2, ?3)", ("p1", "p2", "p3"))
db.execute("insert into t (a, b) values ('it''s ? :not', ?) -- a ? in a comment", ("v",))
db.execute("insert into t (a, b) /* :skipped ? */ values (\"a\"\"b\", ?)", (5,))
db.execute("insert into t (a, b) values (?, 'été :x ?')", ("accent",))
for row in db.execute("select a, b, [c?d], \"e:f\" from t order by rowid"):
    print(row)

for sql, args in (
    ("select ?, ?", (1,)),
    ("select :a, :b", {"a": 1}),
    ("select 1; select 2", ()),
    ("select ?1, ?3", (1, 2)),
):
    try:
        print(db.execute(sql, args).fetchall())
    except (sqlite3.Error, ValueError) as e:
        print(type(e).__name__)

db.executescript("""
create table log (n);
create trigger tr after insert on t begin
    insert into log values (case when new.a = 'end' then 1 else 2 end);
    insert into log values (';');
end;
insert into t (a) values ('end'); insert into t (a) values ('begin');
""")
print(db.execute("select n from log order by rowid").fetchall())
print(db.execute("select 'a;b', ? ; ", (3,)).fetchall())
print(db.execute("select :x || ':x'", {"x": "k"}).fetchall())
print(sqlite3.complete_statement("select 1;"), sqlite3.complete_statement("select ';"))

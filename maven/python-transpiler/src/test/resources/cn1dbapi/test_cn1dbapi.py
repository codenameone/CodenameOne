"""CPython checks of _cn1dbapi's pure functions; run by Cn1DbApiTest."""
import datetime as dt
import sys

sys.path.insert(0, sys.argv[1])
sys.path.insert(0, sys.argv[2])
import _cn1dbapi as d  # noqa: E402

failures = []


def check(label, got, want):
    if got != want:
        failures.append("%s: got %r, want %r" % (label, got, want))


P = {"a": 1, "b": 2}
N = d._named
check("quotes", N("select :a, ':b', \"x:b\", `y:b`, 'it''s :a'", P), ("select ?, ':b', \"x:b\", `y:b`, 'it''s :a'", (1,)))
check("cast", N("select x::text, :b::int", P), ("select x::text, ?::int", (2,)))
check("positional", N("select $1, :a", P), ("select $1, ?", (1,)))
check("dollar", N("select $$ :a $$, $t$ :b $t$, :b", P), ("select $$ :a $$, $t$ :b $t$, ?", (2,)))
check("comments", N("select :a -- :b\n/* :b */ , :b", P), ("select ? -- :b\n/* :b */ , ?", (1, 2)))
check("pg backslash is literal", N("select 'C:\\', :b", P), ("select 'C:\\', ?", (2,)))
check("E-string", N("select E'x\\' :a', :b", P), ("select E'x\\' :a', ?", (2,)))
check("mysql backslash", N("select 'it\\'s :a', :b", P, True), ("select 'it\\'s :a', ?", (2,)))
check("mysql --1", N("select 1--1, :a", P, True), ("select 1--1, ?", (1,)))
check("mysql -- comment", N("select :a -- :b\n", P, True), ("select ? -- :b\n", (1,)))
check("mysql #", N("select :a # :b\n, :b", P, True), ("select ? # :b\n, ?", (1, 2)))
check("pg # is code", N("select :a # :b", P), ("select ? # ?", (1, 2)))
try:
    N("select :zz", P)
    failures.append("missing bind did not raise")
except d.ProgrammingError:
    pass

Q = d._is_query
check("select", Q("  /* c */ (SELECT 1)"), True)
check("returning newline", Q("-- x\nINSERT INTO t VALUES (1)\nRETURNING\nid"), True)
check("returning in quotes", Q("insert into t values ('returning')"), False)
check("values", Q("VALUES (1)"), True)
check("show", Q("show tables"), True)
check("update", Q("update t set a=1"), False)
check("with select", Q("WITH x AS (UPDATE t SET a=1 RETURNING a) SELECT * FROM x"), True)
check("with update", Q("WITH x AS (SELECT 1) UPDATE t SET a = (SELECT * FROM x)"), False)
check("with recursive select", Q("with recursive r(n) as (select 1 union select n+1 from r) select n from r"), True)
check("with insert", Q("WITH x AS (SELECT 1 AS v) INSERT INTO t SELECT v FROM x"), False)
check("with delete returning", Q("WITH x AS (SELECT 1) DELETE FROM t RETURNING id"), True)
check("mysql # hides keyword", Q("# select\nupdate t set a=1", True), False)

tz2 = dt.timezone(dt.timedelta(hours=2))
check("typed", d._typed([("12:00:00+02", "24:00:00", "infinity", "-infinity", "2020-01-02 03:04:05+05", "2.5")],
                        ["timetz", "time", "datetime", "date", "datetimetz", "decimal"]),
      [(dt.time(12, 0, tzinfo=tz2), dt.time(0, 0), dt.datetime.max, dt.date.min,
        dt.datetime(2020, 1, 2, 3, 4, 5, tzinfo=dt.timezone(dt.timedelta(hours=5))), d._decimal.Decimal("2.5"))])
try:
    d._typed([("0044-03-15 BC",)], ["date"])
    failures.append("BC date did not raise")
except ValueError:
    pass

if failures:
    print("\n".join(failures))
    sys.exit(1)
print("ok")

#
# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Codename One designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Codename One through http://www.codenameone.com/ if you
# need additional information or have any questions.

# The fast paths the llm hero port added, each held to what the general path
# answers: datetime's fixed-offset construction, blake2b's digest that keeps
# hashing, pydantic's cached field plan, and sqlite3 cursors that keep the
# read lock while rows are pending.
import datetime
import hashlib
import sqlite3
import time

from pydantic import BaseModel, field_validator

utc = datetime.timezone.utc
plus2 = datetime.timezone(datetime.timedelta(hours=2), "X")

# now(): a fixed offset goes the fast way, a subclass the general one.
n = datetime.datetime.now(utc)
print(type(n).__name__, n.tzinfo is utc, abs(n.timestamp() - time.time()) < 5)
n2 = datetime.datetime.now(plus2)
print(n2.utcoffset(), abs((n2 - n).total_seconds()) < 5)


class MyDT(datetime.datetime):
    pass


m = MyDT.now(utc)
print(type(m).__name__, m.tzinfo is utc)

# fromtimestamp, integral and fractional, with half-even microseconds.
for ts in (0, 1700000000, 1700000000.5, 1700000000.0000005, 1700000000.0000015, -1.25):
    for tz in (utc, plus2):
        d = datetime.datetime.fromtimestamp(ts, tz)
        print(repr(d), d.isoformat(), d.isoformat(timespec="milliseconds"))

# Addition and isoformat against a value built field by field.
base = datetime.datetime(2024, 2, 28, 23, 59, 59, 999999, tzinfo=plus2)
for delta in (datetime.timedelta(microseconds=1), datetime.timedelta(days=1, seconds=3661),
              datetime.timedelta(days=-400)):
    s = base + delta
    print(s.isoformat(), s == datetime.datetime(s.year, s.month, s.day, s.hour, s.minute, s.second,
                                                s.microsecond, tzinfo=plus2), hash(s) == hash(s.replace()))
print(datetime.datetime(2024, 1, 1, tzinfo=utc).isoformat(), datetime.datetime(2024, 1, 1, 0, 0, 0, 5).isoformat())

# blake2b: hexdigest twice, then more data, matches a fresh hash of the whole.
h = hashlib.blake2b(b"abc", digest_size=16)
first = h.hexdigest()
print(first, first == h.hexdigest())
h.update(b"def" * 50)
print(h.hexdigest() == hashlib.blake2b(b"abc" + b"def" * 50, digest_size=16).hexdigest())
big = hashlib.blake2b(b"x" * 300)
print(big.hexdigest()[:32], big.digest() == big.digest())


# pydantic: the plan is rebuilt when the class's fields change.
class Item(BaseModel):
    name: str
    size: int = 0

    @field_validator("size")
    @classmethod
    def positive(cls, v):
        if v < 0:
            raise ValueError("negative")
        return v


print(Item(name="a", size=2), Item(name="b"))
try:
    Item(name="c", size=-1)
except Exception as e:
    print(type(e).__name__, "negative" in str(e))


class Extra(Item):
    tag: str = "t"


print(Extra(name="d", size=1, tag="u"), Item(name="e", size=3))

# sqlite3: a cursor iterated while other queries run, one abandoned part way,
# and the statements a held read must never stand in the way of.
db = sqlite3.connect(":memory:")
db.execute("create table t (v integer)")
db.executemany("insert into t values (?)", [(i,) for i in range(6)])
db.commit()
out = []
for (v,) in db.execute("select v from t order by v"):
    out.append((v, db.execute("select count(*) from t where v <= ?", (v,)).fetchone()[0]))
print(out)
half = db.execute("select v from t order by v")
print(half.fetchone(), half.fetchone())
db.execute("insert into t values (99)")
db.commit()
print(half.fetchall()[:2], db.execute("select max(v) from t").fetchone())
part = db.execute("select v from t order by v")
print(part.fetchone())
part.close()
db.execute("create table gone (x)")
db.execute("drop table gone")
one = db.execute("select v from t where v = 3")
print(one.fetchone(), one.fetchone(), one.fetchall())
db.execute("vacuum")
print(db.execute("select count(*) from t").fetchone())
db.close()

# dict(zip(keys, row)): a list or tuple of keys against a tuple, read straight
# into the dict, and a zip already part consumed.
keys = ["a", "b", "a", "c"]
print(dict(zip(keys, (1, None, 3))), dict(zip(("x", "y"), (1, 2, 3))))
z = zip(["p", "q", "r"], (1, 2, 3))
print(next(z), dict(z), list(z))
print(dict(zip([], ())), dict(zip(["k"], (b"v",))))

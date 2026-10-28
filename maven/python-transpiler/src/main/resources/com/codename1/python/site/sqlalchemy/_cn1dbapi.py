"""A minimal DB-API 2.0 driver over the backend's server databases (``_cn1db``).

It is what the PostgreSQL, MySQL and MariaDB dialects connect through: the backend's own
wire-protocol drivers, reached from Python. Placeholders are ``?`` on every engine; the
backend binds them. Errors subclass the ``sqlite3`` ones, so the engine maps both the same
way into ``sqlalchemy.exc``.
"""

import sqlite3 as _sqlite3

import _cn1db

paramstyle = "qmark"


class Error(_sqlite3.Error):
    pass


class OperationalError(Error, _sqlite3.OperationalError):
    pass


class ProgrammingError(Error, _sqlite3.ProgrammingError):
    pass


class IntegrityError(Error, _sqlite3.IntegrityError):
    pass


_INTEGRITY = ("duplicate", "unique", "foreign key", "violates", "cannot be null", "not-null")


def _error(e):
    msg = str(e)
    low = msg.lower()
    for k in _INTEGRITY:
        if k in low:
            return IntegrityError(msg)
    return OperationalError(msg)


def _named(sql, params):
    """``:name`` binds (``text()``, the named style sqlite3 takes) as ``?`` and a tuple: the
    backend's drivers bind by position. Quoted text, identifiers and PostgreSQL's ``::``
    casts are left alone."""
    out = []
    values = []
    i = 0
    n = len(sql)
    while i < n:
        ch = sql[i]
        if ch in "'\"`":
            j = sql.find(ch, i + 1)
            j = n - 1 if j < 0 else j
            out.append(sql[i:j + 1])
            i = j + 1
            continue
        if ch == ":" and i + 1 < n and (sql[i + 1].isalpha() or sql[i + 1] == "_") \
                and (i == 0 or sql[i - 1] != ":"):
            j = i + 1
            while j < n and (sql[j].isalnum() or sql[j] == "_"):
                j += 1
            name = sql[i + 1:j]
            if name not in params:
                raise ProgrammingError("You did not supply a value for binding parameter :" + name + ".")
            values.append(params[name])
            out.append("?")
            i = j
            continue
        out.append(ch)
        i += 1
    return "".join(out), tuple(values)


class Cursor:
    def __init__(self, conn):
        self.connection = conn
        self.description = None
        self.rowcount = -1
        self.lastrowid = None
        self.arraysize = 1
        self._rows = []
        self._at = 0

    def execute(self, sql, params=()):
        if isinstance(params, dict):
            sql, params = _named(sql, params)
        c = self.connection
        if c._handle is None:
            raise OperationalError("Cannot operate on a closed database.")
        head = sql.lstrip()[:16].upper()
        query = head.startswith("SELECT") or head.startswith("WITH") or " RETURNING " in sql.upper()
        if not c._in_tx and not head.startswith("CREATE") and not head.startswith("DROP"):
            _cn1db.begin(c._handle)
            c._in_tx = True
        try:
            cols, rows, changes, rowid = _cn1db.run(c._handle, sql, tuple(params or ()), query)
        except OSError as e:
            raise _error(e)
        self._rows = list(rows)
        self._at = 0
        self.description = tuple((n, None, None, None, None, None, None) for n in cols) if query else None
        self.rowcount = len(self._rows) if query else changes
        self.lastrowid = rowid if rowid else None
        return self

    def executemany(self, sql, seq):
        total = 0
        for p in seq:
            self.execute(sql, p)
            total += max(self.rowcount, 0)
        self.rowcount = total
        return self

    def fetchone(self):
        if self._at >= len(self._rows):
            return None
        r = self._rows[self._at]
        self._at += 1
        return r

    def fetchmany(self, size=None):
        n = size or self.arraysize
        out = self._rows[self._at:self._at + n]
        self._at += len(out)
        return out

    def fetchall(self):
        out = self._rows[self._at:]
        self._at = len(self._rows)
        return out

    def close(self):
        self._rows = []

    def __iter__(self):
        return iter(self.fetchall())


class Connection:
    def __init__(self, url):
        try:
            self._handle = _cn1db.open(url)
        except OSError as e:
            raise _error(e)
        self._in_tx = False
        self.server_dialect = _cn1db.dialect(self._handle)

    @property
    def in_transaction(self):
        return self._in_tx

    def cursor(self):
        return Cursor(self)

    def commit(self):
        if self._in_tx:
            self._in_tx = False
            _cn1db.commit(self._handle)

    def rollback(self):
        if self._in_tx:
            self._in_tx = False
            _cn1db.rollback(self._handle)

    def close(self):
        if self._handle is not None:
            self.rollback()
            _cn1db.close(self._handle)
            self._handle = None


def connect(url):
    return Connection(url)

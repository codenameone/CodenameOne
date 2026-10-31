"""A minimal DB-API 2.0 driver over the backend's server databases (``_cn1db``).

It is what the PostgreSQL, MySQL and MariaDB dialects connect through: the backend's own
wire-protocol drivers, reached from Python. Placeholders are ``?`` on every engine; the
backend binds them. Errors subclass the ``sqlite3`` ones, so the engine maps both the same
way into ``sqlalchemy.exc``.
"""

import sqlite3 as _sqlite3

import _cn1db
import datetime as _dt
import decimal as _decimal

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


def _skip(sql, i, backslash):
    """The end of the quoted text, comment or dollar-quoted body starting at ``sql[i]``, or
    -1 when ``sql[i]`` starts none. ``backslash`` is whether a backslash escapes the next
    character inside quotes (MySQL and MariaDB always, PostgreSQL in an ``E''`` string); it
    also selects MySQL's comment rules, where ``--`` opens a comment only when followed by
    whitespace (``1--1`` is arithmetic) and ``#`` runs to the end of the line."""
    n = len(sql)
    ch = sql[i]
    if ch in "'\"`":
        esc = backslash or (ch == "'" and i > 0 and sql[i - 1] in "eE"
                            and (i < 2 or not (sql[i - 2].isalnum() or sql[i - 2] == "_")))
        j = i + 1
        while j < n:
            c = sql[j]
            if esc and c == "\\":
                j += 2
                continue
            if c == ch:
                if j + 1 < n and sql[j + 1] == ch:
                    j += 2
                    continue
                return j + 1
            j += 1
        return n
    if (ch == "-" and sql.startswith("--", i)
            and (not backslash or i + 2 >= n or sql[i + 2] in " \t\r\n\f\v")) \
            or (ch == "#" and backslash):
        j = sql.find("\n", i)
        return n if j < 0 else j + 1
    if ch == "/" and sql.startswith("/*", i):
        j = sql.find("*/", i + 2)
        return n if j < 0 else j + 2
    if ch == "$" and (i == 0 or not (sql[i - 1].isalnum() or sql[i - 1] == "_")):
        j = i + 1
        while j < n and (sql[j].isalnum() or sql[j] == "_"):
            j += 1
        if j < n and sql[j] == "$" and not sql[i + 1:j][:1].isdigit():
            tag = sql[i:j + 1]
            k = sql.find(tag, j + 1)
            return n if k < 0 else k + len(tag)
    return -1


def _named(sql, params, backslash=False):
    """``:name`` binds (``text()``, the named style sqlite3 takes) as ``?`` and a tuple: the
    backend's drivers bind by position. Quoted text and identifiers, comments, PostgreSQL's
    dollar-quoted bodies and its ``::`` casts are left alone."""
    out = []
    values = []
    i = 0
    n = len(sql)
    while i < n:
        ch = sql[i]
        j = _skip(sql, i, backslash)
        if j >= 0:
            out.append(sql[i:j])
            i = j
            continue
        if ch == ":" and i + 1 < n and sql[i + 1] == ":":
            out.append("::")
            i += 2
            continue
        if ch == ":" and i + 1 < n and (sql[i + 1].isalpha() or sql[i + 1] == "_"):
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


def _code(sql, backslash=False):
    """``sql`` upper-cased with its quoted text, comments and dollar-quoted bodies replaced by
    a space, so a keyword test sees only keywords."""
    out = []
    i = 0
    n = len(sql)
    while i < n:
        j = _skip(sql, i, backslash)
        if j >= 0:
            out.append(" ")
            i = j
            continue
        out.append(sql[i])
        i += 1
    return "".join(out).upper()


_QUERY_HEADS = ("SELECT", "WITH", "VALUES", "SHOW", "EXPLAIN", "PRAGMA", "TABLE", "DESCRIBE", "DESC")


def _words(code):
    """The words of ``code``, each paired with its parenthesis depth."""
    w = []
    cur = []
    depth = 0
    for c in code:
        if c.isalnum() or c == "_":
            cur.append(c)
            continue
        if cur:
            w.append(("".join(cur), depth))
            cur = []
        if c == "(":
            depth += 1
        elif c == ")":
            depth = max(depth - 1, 0)
    if cur:
        w.append(("".join(cur), depth))
    return w


_STATEMENTS = ("SELECT", "VALUES", "TABLE", "INSERT", "UPDATE", "DELETE", "MERGE")


def _query_words(words):
    """Whether a statement of these words answers rows: by its first keyword, or for a
    ``WITH`` by the first statement keyword after the CTE list (outside its parentheses),
    or by a ``RETURNING`` clause of the statement itself (not of a CTE)."""
    if not words:
        return False
    base = words[0][1]
    # RETURNING counts only in the statement itself, not inside a CTE: WITH d AS
    # (DELETE ... RETURNING id) DELETE FROM u USING d answers no rows.
    if any(w == "RETURNING" and d == base for w, d in words):
        return True
    head = words[0][0]
    if head == "WITH":
        head = ""
        for w, d in words[1:]:
            if d == base and w in _STATEMENTS:
                head = w
                break
        return head in ("SELECT", "VALUES", "TABLE")
    return head in _QUERY_HEADS


def _is_query(sql, backslash=False):
    """Whether ``sql`` answers rows. The backend's run() is told up front (its drivers read
    a result set only when asked), so this decides by the statement: its first keyword after
    comments and opening parentheses, or a RETURNING clause anywhere outside quotes."""
    return _query_words(_words(_code(sql, backslash)))


def _offset(text):
    # PostgreSQL writes an offset as +HH when it has no minutes; fromisoformat wants +HH:MM.
    if len(text) > 3 and text[-3] in "+-" and text[-2:].isdigit() and ":" in text[:-3]:
        text += ":00"
    return text


def _datetime(text):
    if text == "infinity":
        return _dt.datetime.max
    if text == "-infinity":
        return _dt.datetime.min
    return _dt.datetime.fromisoformat(_offset(text))


# A BC date or timestamp ("0044-03-15 BC") is outside what datetime can hold. psycopg2
# raises ValueError for it too, so it is not mapped to anything: fromisoformat raises.
def _date(text):
    if text == "infinity":
        return _dt.date.max
    if text == "-infinity":
        return _dt.date.min
    return _dt.date.fromisoformat(text)


def _time(text):
    # PostgreSQL allows 24:00:00 (the end of a day); psycopg2 answers it as 00:00:00,
    # since datetime.time cannot hold it.
    if text.startswith("24:00:00"):
        text = "00:00:00" + text[8:]
    return _dt.time.fromisoformat(_offset(text))


# A server database sends DECIMAL/NUMERIC and the date and time types as text;
# the backend reports each column's kind so they come back as the Python types
# psycopg2 and mysqlclient return.
_PARSERS = {
    "decimal": _decimal.Decimal,
    "date": _date,
    "datetime": _datetime,
    "datetimetz": _datetime,
    "time": _time,
    "timetz": _time,
}


def _typed(rows, kinds):
    parsers = [_PARSERS.get(k) if k is not None else None for k in kinds]
    if not any(p is not None for p in parsers):
        return list(rows)
    out = []
    for row in rows:
        out.append(tuple(p(v) if p is not None and isinstance(v, str) else v
                         for p, v in zip(parsers, row)))
    return out


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
        c = self.connection
        backslash = c.server_dialect in ("mysql", "mariadb")
        if isinstance(params, dict):
            sql, params = _named(sql, params, backslash)
        if c._handle is None:
            raise OperationalError("Cannot operate on a closed database.")
        words = _words(_code(sql, backslash))
        head = words[0][0] if words else ""
        query = _query_words(words)
        if not c._in_tx and head != "CREATE" and head != "DROP":
            _cn1db.begin(c._handle)
            c._in_tx = True
        try:
            cols, rows, changes, rowid, kinds = _cn1db.run(c._handle, sql, tuple(params or ()), query)
        except OSError as e:
            raise _error(e)
        self._rows = _typed(rows, kinds) if query else list(rows)
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

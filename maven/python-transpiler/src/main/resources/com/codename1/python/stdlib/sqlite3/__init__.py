# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""The DB-API 2.0 interface to SQLite, over the platform's own engine.

In an application the database is Codename One's `Database`: a bare file
name lands in the application's database directory, `:memory:` is a
private database removed when it is closed. Connections, cursors, `Row`,
qmark, numbered and named parameters, adapters and converters
(`PARSE_COLNAMES`, and `PARSE_DECLTYPES` for columns read straight from
one table), the legacy and the `autocommit` transaction control,
`executescript`, `iterdump` and the exception hierarchy behave as in
CPython.

Not provided, raising `NotSupportedError`: user-defined SQL functions,
aggregates and collations, `backup`, `blobopen`, loading extensions.
"""

import datetime as _datetime

import _cn1sql

apilevel = "2.0"
threadsafety = 1
paramstyle = "qmark"
version = "2.6.0"
version_info = (2, 6, 0)
sqlite_version = "3.45.0"
sqlite_version_info = (3, 45, 0)

PARSE_DECLTYPES = 1
PARSE_COLNAMES = 2
LEGACY_TRANSACTION_CONTROL = -1

SQLITE_OK = 0
SQLITE_DENY = 1
SQLITE_IGNORE = 2


class Warning(Exception):
    pass


class Error(Exception):
    sqlite_errorcode = None
    sqlite_errorname = None


class InterfaceError(Error):
    pass


class DatabaseError(Error):
    pass


class DataError(DatabaseError):
    pass


class OperationalError(DatabaseError):
    pass


class IntegrityError(DatabaseError):
    pass


class InternalError(DatabaseError):
    pass


class ProgrammingError(DatabaseError):
    pass


class NotSupportedError(DatabaseError):
    pass


class PrepareProtocol:
    pass


Binary = memoryview

adapters = {}
converters = {}


def register_adapter(type_, adapter):
    adapters[(type_, PrepareProtocol)] = adapter


def register_converter(name, converter):
    converters[name.upper()] = converter


def adapt(obj, proto=PrepareProtocol, alt=None):
    a = adapters.get((type(obj), proto))
    if a is not None:
        return a(obj)
    conform = getattr(obj, "__conform__", None)
    if conform is not None:
        r = conform(proto)
        if r is not None:
            return r
    if alt is not None:
        return alt
    raise ProgrammingError("can't adapt")


register_adapter(_datetime.date, lambda d: d.isoformat())
register_adapter(_datetime.datetime, lambda d: d.isoformat(" "))


def _convert_date(b):
    return _datetime.date(*map(int, b.split(b"-")))


def _convert_timestamp(b):
    datepart, timepart = b.split(b" ")
    year, month, day = map(int, datepart.split(b"-"))
    timepart_full = timepart.split(b".")
    hours, minutes, seconds = map(int, timepart_full[0].split(b":"))
    microseconds = 0
    if len(timepart_full) == 2:
        microseconds = int("{:0<6.6}".format(timepart_full[1].decode()))
    return _datetime.datetime(year, month, day, hours, minutes, seconds, microseconds)


register_converter("date", _convert_date)
register_converter("timestamp", _convert_timestamp)


def _engine_error(e):
    """The DB-API exception for an engine failure's message."""
    msg = str(e)
    # Drivers wrap SQLite's own message: "[SQLITE_ERROR] SQL error or
    # missing database (no such table: t)". The part in the last
    # parentheses is what SQLite said.
    if msg.startswith("[SQLITE_") and msg.endswith(")"):
        depth = 0
        i = len(msg) - 1
        while i >= 0:
            c = msg[i]
            if c == ")":
                depth += 1
            elif c == "(":
                depth -= 1
                if depth == 0:
                    break
            i -= 1
        if i > 0:
            msg = msg[i + 1:-1]
    low = msg.lower()
    if "constraint failed" in low or "constraint violation" in low or "not unique" in low:
        return IntegrityError(msg)
    if "datatype mismatch" in low:
        return IntegrityError(msg)
    return OperationalError(msg)


def _skip_space(sql, i):
    n = len(sql)
    while i < n:
        c = sql[i]
        if c == "-" and sql.startswith("--", i):
            j = sql.find("\n", i)
            i = n if j < 0 else j + 1
        elif c == "/" and sql.startswith("/*", i):
            j = sql.find("*/", i + 2)
            i = n if j < 0 else j + 2
        elif c in " \t\r\n\f":
            i += 1
        else:
            break
    return i


def _first_word(sql):
    i = _skip_space(sql, 0)
    j = i
    n = len(sql)
    while j < n and (sql[j].isalpha() or sql[j] == "_"):
        j += 1
    return sql[i:j].upper()


_QUERY_WORDS = ("SELECT", "PRAGMA", "WITH", "VALUES", "EXPLAIN")
_DML_WORDS = ("INSERT", "UPDATE", "DELETE", "REPLACE")


def _scan(sql):
    """Splits off the first statement and finds its parameters.

    Answers (statement with every placeholder written as ``?``, the
    placeholders in order -- None for a bare ``?``, an int for ``?NNN``, a
    name for ``:name``, ``@name`` and ``$name`` -- the rest after the
    statement's ``;``, and whether the statement creates a trigger).
    """
    out = []
    params = []
    n = len(sql)
    i = 0
    start = 0
    trigger = _first_word(sql) == "CREATE" and _is_trigger(sql)
    depth_begin = 0
    word = ""
    while i < n:
        c = sql[i]
        if c == "'" or c == '"' or c == "`" or c == "[":
            close = "]" if c == "[" else c
            j = i + 1
            while j < n:
                if sql[j] == close:
                    if close != "]" and j + 1 < n and sql[j + 1] == close:
                        j += 2
                        continue
                    break
                j += 1
            i = j + 1
            continue
        if c == "-" and sql.startswith("--", i):
            j = sql.find("\n", i)
            i = n if j < 0 else j + 1
            continue
        if c == "/" and sql.startswith("/*", i):
            j = sql.find("*/", i + 2)
            i = n if j < 0 else j + 2
            continue
        if c.isalpha() or c == "_":
            j = i
            while j < n and (sql[j].isalnum() or sql[j] == "_"):
                j += 1
            word = sql[i:j].upper()
            if trigger:
                if word == "BEGIN" or word == "CASE":
                    depth_begin += 1
                elif word == "END":
                    depth_begin -= 1
            i = j
            continue
        if c == "?":
            out.append(sql[start:i])
            j = i + 1
            while j < n and sql[j].isdigit():
                j += 1
            params.append(int(sql[i + 1:j]) if j > i + 1 else None)
            out.append("?")
            start = j
            i = j
            continue
        if (c == ":" or c == "@" or c == "$") and i + 1 < n and (sql[i + 1].isalpha() or sql[i + 1] == "_"):
            out.append(sql[start:i])
            j = i + 1
            while j < n and (sql[j].isalnum() or sql[j] == "_"):
                j += 1
            params.append(sql[i + 1:j])
            out.append("?")
            start = j
            i = j
            continue
        if c == ";" and depth_begin <= 0:
            out.append(sql[start:i])
            return "".join(out), params, sql[i + 1:], trigger
        i += 1
    out.append(sql[start:])
    return "".join(out), params, "", trigger


def _is_trigger(sql):
    words = []
    i = 0
    n = len(sql)
    while i < n and len(words) < 4:
        i = _skip_space(sql, i)
        j = i
        while j < n and (sql[j].isalnum() or sql[j] == "_"):
            j += 1
        if j == i:
            break
        words.append(sql[i:j].upper())
        i = j
    return "TRIGGER" in words


def _is_blank(sql):
    return _skip_space(sql, 0) >= len(sql)


def complete_statement(statement):
    rest = statement
    while not _is_blank(rest):
        stmt, params, after, trig = _scan(rest)
        if after == "" and not rest.rstrip().endswith(";"):
            return False
        rest = after
    return statement.rstrip().endswith(";") or _is_blank(statement)


class Row:
    """A result row indexable by position and, case-insensitively, by name."""

    def __init__(self, cursor, values):
        self._keys = tuple(d[0] for d in cursor.description)
        self._values = tuple(values)

    def keys(self):
        return list(self._keys)

    def __getitem__(self, key):
        if isinstance(key, str):
            low = key.lower()
            for i, k in enumerate(self._keys):
                if k.lower() == low:
                    return self._values[i]
            raise IndexError("No item with that key")
        return self._values[key]

    def __len__(self):
        return len(self._values)

    def __iter__(self):
        return iter(self._values)

    def __eq__(self, other):
        if not isinstance(other, Row):
            return NotImplemented
        return self._keys == other._keys and self._values == other._values

    def __ne__(self, other):
        r = self.__eq__(other)
        return r if r is NotImplemented else not r

    def __hash__(self):
        return hash(self._keys) ^ hash(self._values)

    def __repr__(self):
        return "<sqlite3.Row object>"


class Cursor:
    def __init__(self, connection):
        if not isinstance(connection, Connection):
            raise TypeError("argument 1 must be sqlite3.Connection")
        self.connection = connection
        self.arraysize = 1
        self.row_factory = None
        self.description = None
        self.rowcount = -1
        self.lastrowid = None
        self._rows = []
        self._pos = 0
        self._closed = False
        self._inited = True

    def _check(self):
        if self._closed:
            raise ProgrammingError("Cannot operate on a closed cursor.")
        self.connection._check()

    def close(self):
        self._closed = True
        self._rows = []

    def setinputsizes(self, sizes):
        pass

    def setoutputsize(self, size, column=None):
        pass

    def execute(self, sql, parameters=()):
        self._check()
        if not isinstance(sql, str):
            raise TypeError("execute() argument 1 must be str, not " + type(sql).__name__)
        stmt, names, rest, trig = _scan(sql)
        if not _is_blank(rest):
            raise ProgrammingError("You can only execute one statement at a time.")
        self._run(stmt, names, parameters, False)
        return self

    def executemany(self, sql, seq_of_parameters):
        self._check()
        stmt, names, rest, trig = _scan(sql)
        if not _is_blank(rest):
            raise ProgrammingError("You can only execute one statement at a time.")
        word = _first_word(stmt)
        if word in _QUERY_WORDS and word != "WITH":
            raise ProgrammingError("executemany() can only execute DML statements.")
        total = 0
        any_run = False
        for params in seq_of_parameters:
            self._run(stmt, names, params, True)
            any_run = True
            if self.rowcount > 0:
                total += self.rowcount
        self.rowcount = total if any_run else 0
        self.description = None
        self._rows = []
        self._pos = 0
        return self

    def executescript(self, sql_script):
        self._check()
        if not isinstance(sql_script, str):
            raise ValueError("script argument must be unicode.")
        conn = self.connection
        if conn._autocommit == LEGACY_TRANSACTION_CONTROL and conn.in_transaction:
            conn.commit()
        rest = sql_script
        while not _is_blank(rest):
            stmt, names, rest, trig = _scan(rest)
            if _is_blank(stmt):
                continue
            self._run(stmt, [], (), False, script=True)
        self.description = None
        self._rows = []
        return self

    def _bind(self, names, parameters):
        if not names:
            if parameters is None:
                return []
            if isinstance(parameters, dict) or (hasattr(parameters, "keys") and hasattr(parameters, "__getitem__")
                                               and not isinstance(parameters, (list, tuple, Row))):
                return []
            ps = list(parameters)
            if ps:
                raise ProgrammingError("Incorrect number of bindings supplied. The current statement uses 0, "
                                       "and there are %d supplied." % len(ps))
            return []
        named = [n for n in names if isinstance(n, str)]
        is_map = isinstance(parameters, dict) or (hasattr(parameters, "keys") and hasattr(parameters, "__getitem__")
                                                  and not isinstance(parameters, (list, tuple, Row)))
        values = []
        if named and is_map:
            for n in names:
                if not isinstance(n, str):
                    raise ProgrammingError("Binding %d has no name, but you supplied a dictionary (which has "
                                           "only names)." % (len(values) + 1))
                try:
                    values.append(parameters[n])
                except KeyError:
                    raise ProgrammingError("You did not supply a value for binding parameter :%s." % n) from None
            return values
        if is_map:
            raise ProgrammingError("Binding 1 has no name, but you supplied a dictionary (which has only names).")
        try:
            ps = list(parameters)
        except TypeError:
            raise ProgrammingError("parameters are of unsupported type") from None
        numbered = [n for n in names if isinstance(n, int)]
        if numbered:
            count = max(numbered)
            if len(ps) != count:
                raise ProgrammingError("Incorrect number of bindings supplied. The current statement uses %d, "
                                       "and there are %d supplied." % (count, len(ps)))
            nxt = 1
            for n in names:
                k = n if isinstance(n, int) else nxt
                values.append(ps[k - 1])
                nxt = k + 1
            return values
        if len(ps) != len(names):
            raise ProgrammingError("Incorrect number of bindings supplied. The current statement uses %d, "
                                   "and there are %d supplied." % (len(names), len(ps)))
        return ps

    def _adapt(self, v):
        if v is None or type(v) in (int, float, str, bytes):
            a = adapters.get((type(v), PrepareProtocol))
            return v if a is None else a(v)
        a = adapters.get((type(v), PrepareProtocol))
        if a is not None:
            return a(v)
        conform = getattr(v, "__conform__", None)
        if conform is not None:
            r = conform(PrepareProtocol)
            if r is not None:
                return r
        if isinstance(v, (bytearray, memoryview)):
            return bytes(v)
        if isinstance(v, bool):
            return int(v)
        if isinstance(v, int):
            return int(v)
        if isinstance(v, float):
            return float(v)
        if isinstance(v, str):
            return str(v)
        if isinstance(v, bytes):
            return bytes(v)
        raise ProgrammingError("Error binding parameter %d: type '%s' is not supported"
                               % (0, type(v).__name__))

    def _run(self, stmt, names, parameters, many, script=False):
        conn = self.connection
        values = self._bind(names, parameters)
        bound = []
        for i, v in enumerate(values):
            try:
                bound.append(self._adapt(v))
            except ProgrammingError:
                raise ProgrammingError("Error binding parameter %d: type '%s' is not supported"
                                       % (i + 1, type(v).__name__)) from None
        word = _first_word(stmt)
        self._rows = []
        self._pos = 0
        if word == "BEGIN":
            conn._begin()
            self.description = None
            self.rowcount = -1
            return
        if word == "COMMIT" or word == "END":
            conn._commit_raw()
            self.description = None
            self.rowcount = -1
            return
        if word == "ROLLBACK" and "TO" not in stmt.upper().split():
            conn._rollback_raw()
            self.description = None
            self.rowcount = -1
            return
        if word in _DML_WORDS and conn._autocommit == LEGACY_TRANSACTION_CONTROL \
                and conn._isolation_level is not None and not conn.in_transaction:
            conn._begin()
        query = word in _QUERY_WORDS or " RETURNING " in (" " + stmt.upper().replace("\n", " ") + " ")
        try:
            cols, rows, changes, rowid = _cn1sql.run(conn._handle, stmt, tuple(bound), query)
        except OSError as e:
            raise _engine_error(e) from None
        if word in ("CREATE", "DROP", "ALTER"):
            conn._decltypes = {}
        if word in _DML_WORDS:
            conn.total_changes += max(changes, 0)
        if cols:
            self.description = tuple((self._colname(c), None, None, None, None, None, None) for c in cols)
            self._rows = self._convert(stmt, cols, rows)
            self.rowcount = -1 if word in _QUERY_WORDS else len(rows)
        else:
            self.description = None
            self.rowcount = changes if word in _DML_WORDS else -1
        if word in ("INSERT", "REPLACE") and not many:
            self.lastrowid = rowid
        elif word in ("INSERT", "REPLACE") and many:
            self.lastrowid = rowid

    def _colname(self, name):
        if self.connection.detect_types & PARSE_COLNAMES:
            k = name.find(" [")
            if k >= 0 and name.endswith("]"):
                return name[:k]
        return name

    def _convert(self, stmt, cols, rows):
        conn = self.connection
        convs = [None] * len(cols)
        if conn.detect_types:
            decl = conn._declared(stmt) if conn.detect_types & PARSE_DECLTYPES else {}
            for i, c in enumerate(cols):
                kind = None
                if conn.detect_types & PARSE_COLNAMES:
                    k = c.find(" [")
                    if k >= 0 and c.endswith("]"):
                        kind = c[k + 2:-1]
                if kind is None and decl:
                    kind = decl.get(c.lower())
                if kind is not None:
                    kind = kind.split("(")[0].strip().split(" ")[0]
                    convs[i] = converters.get(kind.upper())
        tf = conn.text_factory
        out = []
        for row in rows:
            vals = list(row)
            for i, v in enumerate(vals):
                f = convs[i]
                if f is not None and v is not None:
                    if isinstance(v, str):
                        v = v.encode("utf-8")
                    elif not isinstance(v, bytes):
                        v = str(v).encode("ascii")
                    vals[i] = f(v)
                elif isinstance(v, str) and tf is not str:
                    b = v.encode("utf-8")
                    vals[i] = b if tf is bytes else tf(b)
            out.append(tuple(vals))
        return out

    def _make(self, values):
        rf = self.row_factory
        if rf is None:
            rf = self.connection.row_factory
        if rf is None:
            return values
        return rf(self, values)

    def fetchone(self):
        self._check()
        if self._pos >= len(self._rows):
            return None
        r = self._rows[self._pos]
        self._pos += 1
        return self._make(r)

    def fetchmany(self, size=None):
        if size is None:
            size = self.arraysize
        out = []
        while len(out) < size:
            r = self.fetchone()
            if r is None:
                break
            out.append(r)
        return out

    def fetchall(self):
        out = []
        while True:
            r = self.fetchone()
            if r is None:
                return out
            out.append(r)

    def __iter__(self):
        return self

    def __next__(self):
        r = self.fetchone()
        if r is None:
            raise StopIteration
        return r


class Connection:
    def __init__(self, database, timeout=5.0, detect_types=0, isolation_level="", check_same_thread=True,
                 factory=None, cached_statements=128, uri=False, *, autocommit=LEGACY_TRANSACTION_CONTROL):
        name = database
        if hasattr(name, "__fspath__"):
            name = name.__fspath__()
        if isinstance(name, bytes):
            name = name.decode("utf-8")
        if uri:
            name = _from_uri(name)
        if autocommit not in (True, False, LEGACY_TRANSACTION_CONTROL):
            raise ValueError("autocommit must be True, False, or sqlite3.LEGACY_TRANSACTION_CONTROL")
        self.detect_types = detect_types
        self.row_factory = None
        self.text_factory = str
        self.total_changes = 0
        self.in_transaction = False
        self._isolation_level = isolation_level
        self._autocommit = autocommit
        self._decltypes = {}
        self._closed = False
        try:
            self._handle = _cn1sql.open(name)
        except OSError as e:
            raise OperationalError("unable to open database file") from None
        self.Warning = Warning
        self.Error = Error
        self.InterfaceError = InterfaceError
        self.DatabaseError = DatabaseError
        self.DataError = DataError
        self.OperationalError = OperationalError
        self.IntegrityError = IntegrityError
        self.InternalError = InternalError
        self.ProgrammingError = ProgrammingError
        self.NotSupportedError = NotSupportedError
        if autocommit is False:
            self._begin()

    def _check(self):
        if self._closed:
            raise ProgrammingError("Cannot operate on a closed database.")

    @property
    def isolation_level(self):
        return self._isolation_level

    @isolation_level.setter
    def isolation_level(self, value):
        if value is not None and str(value).upper() not in ("", "DEFERRED", "IMMEDIATE", "EXCLUSIVE"):
            raise ValueError("isolation_level string must be '', 'DEFERRED', 'IMMEDIATE', or 'EXCLUSIVE'")
        if value is None and self.in_transaction and self._autocommit == LEGACY_TRANSACTION_CONTROL:
            self.commit()
        self._isolation_level = value

    @property
    def autocommit(self):
        return self._autocommit

    @autocommit.setter
    def autocommit(self, value):
        if value not in (True, False, LEGACY_TRANSACTION_CONTROL):
            raise ValueError("autocommit must be True, False, or sqlite3.LEGACY_TRANSACTION_CONTROL")
        if value is True and self.in_transaction:
            self._commit_raw()
        self._autocommit = value
        if value is False and not self.in_transaction:
            self._begin()

    def _begin(self):
        if self.in_transaction:
            raise OperationalError("cannot start a transaction within a transaction")
        try:
            _cn1sql.begin(self._handle)
        except OSError as e:
            raise _engine_error(e) from None
        self.in_transaction = True

    def _commit_raw(self):
        if not self.in_transaction:
            raise OperationalError("cannot commit - no transaction is active")
        try:
            _cn1sql.commit(self._handle)
        except OSError as e:
            raise _engine_error(e) from None
        self.in_transaction = False

    def _rollback_raw(self):
        if not self.in_transaction:
            raise OperationalError("cannot rollback - no transaction is active")
        try:
            _cn1sql.rollback(self._handle)
        except OSError as e:
            raise _engine_error(e) from None
        self.in_transaction = False

    def _declared(self, stmt):
        """The declared column types of the one table a query reads from."""
        words = stmt.replace("\n", " ").replace("\t", " ").split()
        table = None
        for i, w in enumerate(words):
            if w.upper() == "FROM" and i + 1 < len(words):
                table = words[i + 1].strip("`\"[];,()")
                if i + 2 < len(words) and words[i + 2].upper() == "JOIN":
                    return {}
                break
        if not table:
            return {}
        if table in self._decltypes:
            return self._decltypes[table]
        try:
            cols, rows, ch, rid = _cn1sql.run(self._handle, "PRAGMA table_info(\"%s\")"
                                              % table.replace('"', '""'), (), True)
        except OSError:
            rows = []
        decl = {}
        for r in rows:
            decl[str(r[1]).lower()] = str(r[2]) if r[2] is not None else ""
        decl = {k: v for k, v in decl.items() if v}
        self._decltypes[table] = decl
        return decl

    def cursor(self, factory=Cursor):
        self._check()
        c = factory(self)
        if self.row_factory is not None and c.row_factory is None:
            pass
        return c

    def execute(self, sql, parameters=()):
        return self.cursor().execute(sql, parameters)

    def executemany(self, sql, parameters):
        return self.cursor().executemany(sql, parameters)

    def executescript(self, sql_script):
        return self.cursor().executescript(sql_script)

    def commit(self):
        self._check()
        if self.in_transaction:
            self._commit_raw()
        if self._autocommit is False:
            self._begin()

    def rollback(self):
        self._check()
        if self.in_transaction:
            self._rollback_raw()
        if self._autocommit is False:
            self._begin()

    def close(self):
        if self._closed:
            return
        if self._autocommit is False and self.in_transaction:
            self._rollback_raw()
        self._closed = True
        _cn1sql.close(self._handle)

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, tb):
        if self._closed:
            raise ProgrammingError("Cannot operate on a closed database.")
        if exc_type is None:
            self.commit()
        else:
            self.rollback()
        return False

    def iterdump(self):
        """The SQL that recreates the database, as CPython's iterdump writes it."""
        yield "BEGIN TRANSACTION;"
        cols, rows, ch, rid = _cn1sql.run(
            self._handle, "SELECT name, type, sql FROM sqlite_master WHERE sql NOT NULL AND type == 'table' "
                          "ORDER BY name", (), True)
        sequence = False
        for name, kind, sql in rows:
            if name == "sqlite_sequence":
                yield "DELETE FROM \"sqlite_sequence\";"
                sequence = True
                continue
            elif name == "sqlite_stat1":
                yield "ANALYZE \"sqlite_master\";"
            elif name.startswith("sqlite_"):
                continue
            else:
                yield "%s;" % sql
            qname = name.replace('"', '""')
            c2, info, x, y = _cn1sql.run(self._handle, "PRAGMA table_info(\"%s\")" % qname, (), True)
            names = [str(r[1]) for r in info]
            q = "SELECT 'INSERT INTO \"%s\" VALUES(' || %s || ')' FROM \"%s\";" % (
                qname, "||','||".join("quote(\"%s\")" % n.replace('"', '""') for n in names), qname)
            c3, data, x, y = _cn1sql.run(self._handle, q[:-1], (), True)
            for r in data:
                yield "%s;" % r[0]
        cols, rows, ch, rid = _cn1sql.run(
            self._handle, "SELECT name, type, sql FROM sqlite_master WHERE sql NOT NULL AND type IN "
                          "('index', 'trigger', 'view') ORDER BY name", (), True)
        for name, kind, sql in rows:
            yield "%s;" % sql
        if sequence:
            c4, seq, x, y = _cn1sql.run(self._handle, "SELECT name, seq FROM sqlite_sequence", (), True)
            for name, seq_value in seq:
                yield "INSERT INTO \"sqlite_sequence\" VALUES('%s',%d);" % (name.replace("'", "''"), seq_value)
        yield "COMMIT;"

    def create_function(self, name, narg, func, *, deterministic=False):
        raise NotSupportedError("user-defined SQL functions are not supported on this platform")

    def create_aggregate(self, name, n_arg, aggregate_class):
        raise NotSupportedError("user-defined aggregates are not supported on this platform")

    def create_window_function(self, name, num_params, aggregate_class):
        raise NotSupportedError("user-defined window functions are not supported on this platform")

    def create_collation(self, name, callable):
        raise NotSupportedError("user-defined collations are not supported on this platform")

    def set_authorizer(self, authorizer_callback):
        raise NotSupportedError("authorizers are not supported on this platform")

    def set_progress_handler(self, progress_handler, n):
        if progress_handler is not None:
            raise NotSupportedError("progress handlers are not supported on this platform")

    def set_trace_callback(self, trace_callback):
        if trace_callback is not None:
            raise NotSupportedError("trace callbacks are not supported on this platform")

    def enable_load_extension(self, enabled):
        raise NotSupportedError("extensions cannot be loaded on this platform")

    def load_extension(self, path, *, entrypoint=None):
        raise NotSupportedError("extensions cannot be loaded on this platform")

    def backup(self, target, *, pages=-1, progress=None, name="main", sleep=0.25):
        raise NotSupportedError("backup is not supported on this platform")

    def blobopen(self, table, column, row, *, readonly=False, name="main"):
        raise NotSupportedError("blob I/O is not supported on this platform")

    def interrupt(self):
        pass


def _from_uri(uri):
    if not uri.startswith("file:"):
        raise OperationalError("unable to open database file")
    rest = uri[5:]
    query = ""
    if "?" in rest:
        rest, query = rest.split("?", 1)
    if rest.startswith("//"):
        rest = rest[2:]
        k = rest.find("/")
        rest = rest[k:] if k >= 0 else ""
    for part in query.split("&"):
        if part == "mode=memory":
            return ":memory:"
    return rest or ":memory:"


def connect(database, timeout=5.0, detect_types=0, isolation_level="", check_same_thread=True,
            factory=Connection, cached_statements=128, uri=False, *, autocommit=LEGACY_TRANSACTION_CONTROL):
    return factory(database, timeout, detect_types, isolation_level, check_same_thread, None,
                   cached_statements, uri, autocommit=autocommit)

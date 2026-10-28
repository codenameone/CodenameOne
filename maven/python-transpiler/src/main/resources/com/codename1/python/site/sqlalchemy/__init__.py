"""A SQLAlchemy 2.x subset for Codename One: Core over the DB-API ``sqlite3`` module.

SQLite runs over ``sqlite3``; PostgreSQL, MySQL and MariaDB URLs run over the backend's own
drivers (``_cn1dbapi``), in a server build. Statements compile to SQL with ``?`` placeholders and run
through ``sqlite3``; results are read from the cursor as they are iterated. The ORM lives in ``sqlalchemy.orm``
and plugs in through two hooks: a mapped class carries ``__sa_mapper__`` (selecting it
yields objects) and a relationship attribute answers ``_join_info()`` (joining on it).

Subqueries (FROM, scalar, ``EXISTS``, ``IN``), CTEs, unions, aliases, ``case`` and ``cast``
are supported; a scalar or ``EXISTS`` subquery correlates to the enclosing query's tables
as SQLAlchemy does. ``echo=True`` logs each statement through the ``sqlalchemy.engine.Engine``
logger.

Unsupported: Oracle and SQL Server dialects, connection pooling options, reflection, events, recursive
CTEs, ``Enum``/``JSON``/``Numeric`` decimal handling.
"""

import datetime as _dt
import logging as _logging
import sqlite3 as _sqlite3

from . import exc

__version__ = "2.0.0"


# ----------------------------------------------------------------------------- types

class TypeEngine:
    _ddl = "NULL"

    def __init__(self, *args, **kwargs):
        pass

    def ddl(self):
        return self._ddl

    def bind(self, value):
        return value

    def result(self, value):
        return value


class NullType(TypeEngine):
    _ddl = "NULL"


class Integer(TypeEngine):
    _ddl = "INTEGER"


class BigInteger(Integer):
    _ddl = "BIGINT"


class SmallInteger(Integer):
    _ddl = "SMALLINT"


class String(TypeEngine):
    _ddl = "VARCHAR"

    def __init__(self, length=None, *args, **kwargs):
        self.length = length

    def ddl(self):
        if self.length:
            return "%s(%d)" % (self._ddl, self.length)
        return self._ddl


class Unicode(String):
    pass


class Text(String):
    _ddl = "TEXT"


class UnicodeText(Text):
    pass


class Float(TypeEngine):
    _ddl = "FLOAT"

    def result(self, value):
        return None if value is None else float(value)


class Numeric(Float):
    _ddl = "NUMERIC"


class Boolean(TypeEngine):
    _ddl = "BOOLEAN"

    def bind(self, value):
        if value is None:
            return None
        return bool(value)

    def result(self, value):
        return None if value is None else bool(value)


def _num(s, a, b):
    return int(s[a:b])


class DateTime(TypeEngine):
    _ddl = "DATETIME"

    def bind(self, value):
        if value is None or isinstance(value, str):
            return value
        return "%04d-%02d-%02d %02d:%02d:%02d.%06d" % (
            value.year, value.month, value.day, value.hour, value.minute, value.second, value.microsecond)

    def result(self, value):
        if not isinstance(value, str):
            return value
        s = value.replace("T", " ")
        micro = 0
        if len(s) > 20 and s[19] == ".":
            frac = (s[20:26] + "000000")[:6]
            micro = int(frac)
        hour = _num(s, 11, 13) if len(s) >= 13 else 0
        minute = _num(s, 14, 16) if len(s) >= 16 else 0
        second = _num(s, 17, 19) if len(s) >= 19 else 0
        return _dt.datetime(_num(s, 0, 4), _num(s, 5, 7), _num(s, 8, 10), hour, minute, second, micro)


class Date(TypeEngine):
    _ddl = "DATE"

    def bind(self, value):
        if value is None or isinstance(value, str):
            return value
        return "%04d-%02d-%02d" % (value.year, value.month, value.day)

    def result(self, value):
        if not isinstance(value, str):
            return value
        return _dt.date(_num(value, 0, 4), _num(value, 5, 7), _num(value, 8, 10))


class LargeBinary(TypeEngine):
    _ddl = "BLOB"


def _to_type(t):
    if t is None:
        return NullType()
    if isinstance(t, type):
        return t()
    return t


def _is_type(a):
    if isinstance(a, TypeEngine):
        return True
    return isinstance(a, type) and issubclass(a, TypeEngine)


# ----------------------------------------------------------------------------- compiler

# The dialect statements compile for: the executing engine's, pushed by Connection.execute.
_active = []

class _Compiled:
    def __init__(self, sql, params, dialect):
        self.string = sql
        self.positiontup = list(params)
        self.dialect = dialect

    def __str__(self):
        return self.string


class _Ctx:
    def __init__(self, qualify=True):
        self.dialect = _active[-1]
        self.params = []
        self.qualify = qualify
        self.columns_clause = False
        # Tables of the enclosing queries, which a correlated subquery leaves out of its FROM.
        self.outer = []
        self.correlate = False
        self.ctes = []


class ClauseElement:
    type = NullType()

    def _compile(self, ctx):
        raise NotImplementedError(type(self).__name__)

    def compile(self, bind=None, dialect=None, **kw):
        """The statement as the given dialect (or the bind's) would run it, with ``?``
        placeholders: ``str()`` of the result is the SQL, ``positiontup`` the values."""
        if dialect is None and bind is not None:
            dialect = getattr(bind, "dialect", None)
        _active.append(dialect if dialect is not None else _active[0])
        try:
            ctx = _Ctx()
            sql = _compile_top(self, ctx) if isinstance(self, (Select, CompoundSelect)) else self._compile_any(ctx)
        finally:
            _active.pop()
        return _Compiled(sql, ctx.params, ctx.dialect)

    def _compile_any(self, ctx):
        if isinstance(self, _DML):
            if isinstance(self, Insert):
                return self._sql(self._row(None), ctx)
            raise exc.CompileError("compile() of UPDATE or DELETE is not supported")
        return self._compile(ctx)

    def _tables(self, acc):
        pass

    def __str__(self):
        return self._compile(_Ctx())


def _lit(value, typ=None):
    if isinstance(value, ClauseElement):
        return value
    return BindParameter(value, typ)


class ColumnElement(ClauseElement):
    key = None
    name = None

    def __hash__(self):
        return id(self)

    def _op(self, op, other):
        return BinaryExpression(self, op, _lit(other, self.type))

    def __eq__(self, other):
        if other is None:
            return BinaryExpression(self, "IS", Null())
        return self._op("=", other)

    def __ne__(self, other):
        if other is None:
            return BinaryExpression(self, "IS NOT", Null())
        return self._op("!=", other)

    def __lt__(self, other):
        return self._op("<", other)

    def __le__(self, other):
        return self._op("<=", other)

    def __gt__(self, other):
        return self._op(">", other)

    def __ge__(self, other):
        return self._op(">=", other)

    def __add__(self, other):
        if isinstance(self.type, String):
            return Grouping(self._op("||", other), self.type)
        return Grouping(self._op("+", other), self.type)

    def __sub__(self, other):
        return Grouping(self._op("-", other), self.type)

    def __mul__(self, other):
        return Grouping(self._op("*", other), self.type)

    def __truediv__(self, other):
        return Grouping(self._op("/", other), self.type)

    def __mod__(self, other):
        return Grouping(self._op("%", other), self.type)

    def __neg__(self):
        return UnaryExpression(self, "-", None, self.type)

    def __invert__(self):
        return not_(self)

    def in_(self, values):
        if isinstance(values, (Select, CompoundSelect, ScalarSelect)):
            return InExpression(self, [_SubqueryList(values)], False)
        return InExpression(self, [_lit(v, self.type) for v in values], False)

    def not_in(self, values):
        if isinstance(values, (Select, CompoundSelect, ScalarSelect)):
            return InExpression(self, [_SubqueryList(values)], True)
        return InExpression(self, [_lit(v, self.type) for v in values], True)

    notin_ = not_in

    def like(self, pattern, escape=None):
        return self._op("LIKE", pattern)

    def not_like(self, pattern, escape=None):
        return self._op("NOT LIKE", pattern)

    def ilike(self, pattern, escape=None):
        return BinaryExpression(Function("lower", [self]), "LIKE", Function("lower", [_lit(pattern, self.type)]))

    def startswith(self, s):
        return self._op("LIKE", s + "%")

    def endswith(self, s):
        return self._op("LIKE", "%" + s)

    def contains(self, s):
        return self._op("LIKE", "%" + s + "%")

    def between(self, lo, hi):
        return and_(self >= lo, self <= hi)

    def is_(self, other):
        return BinaryExpression(self, "IS", _lit(other) if other is not None else Null())

    def is_not(self, other):
        return BinaryExpression(self, "IS NOT", _lit(other) if other is not None else Null())

    isnot = is_not

    def desc(self):
        return UnaryExpression(self, None, "DESC", self.type)

    def asc(self):
        return UnaryExpression(self, None, "ASC", self.type)

    def label(self, name):
        return Label(name, self)


class Null(ColumnElement):
    def _compile(self, ctx):
        return "NULL"


def null():
    return Null()


class BindParameter(ColumnElement):
    def __init__(self, value, typ=None):
        self.value = value
        self.type = _to_type(typ)

    def _compile(self, ctx):
        ctx.params.append(self.type.bind(self.value))
        return "?"


def literal(value, type_=None):
    return BindParameter(value, type_)


class BinaryExpression(ColumnElement):
    def __init__(self, left, op, right):
        self.left = left
        self.op = op
        self.right = right
        self.type = Boolean() if op not in ("+", "-", "*", "/", "%") else left.type

    def _compile(self, ctx):
        return "%s %s %s" % (self.left._compile(ctx), self.op, self.right._compile(ctx))

    def _tables(self, acc):
        self.left._tables(acc)
        self.right._tables(acc)

    def __bool__(self):
        if self.op == "=":
            return self.left is self.right
        if self.op == "!=":
            return self.left is not self.right
        raise TypeError("Boolean value of this clause is not defined")


class Grouping(ColumnElement):
    def __init__(self, elem, typ):
        self.elem = elem
        self.type = typ

    def _compile(self, ctx):
        return "(" + self.elem._compile(ctx) + ")"

    def _tables(self, acc):
        self.elem._tables(acc)


class InExpression(ColumnElement):
    def __init__(self, left, items, negate):
        self.left = left
        self.items = items
        self.negate = negate
        self.type = Boolean()

    def _compile(self, ctx):
        inner = ", ".join(i._compile(ctx) for i in self.items)
        return "%s %sIN (%s)" % (self.left._compile(ctx), "NOT " if self.negate else "", inner)

    def _tables(self, acc):
        self.left._tables(acc)


class UnaryExpression(ColumnElement):
    def __init__(self, elem, prefix, suffix, typ=None):
        self.elem = elem
        self.prefix = prefix
        self.suffix = suffix
        self.type = typ if typ is not None else NullType()

    def _compile(self, ctx):
        s = self.elem._compile(ctx)
        if self.prefix:
            s = self.prefix + s if self.prefix == "-" else "%s (%s)" % (self.prefix, s)
        if self.suffix:
            s = s + " " + self.suffix
        return s

    def _tables(self, acc):
        self.elem._tables(acc)


def desc(col):
    return col.desc()


def asc(col):
    return col.asc()


class BooleanClauseList(ColumnElement):
    def __init__(self, op, items):
        self.op = op
        self.items = list(items)
        self.type = Boolean()

    def _compile(self, ctx):
        if len(self.items) == 1:
            return self.items[0]._compile(ctx)
        return "(" + (" %s " % self.op).join(i._compile(ctx) for i in self.items) + ")"

    def _tables(self, acc):
        for i in self.items:
            i._tables(acc)


def and_(*clauses):
    return BooleanClauseList("AND", clauses)


def or_(*clauses):
    return BooleanClauseList("OR", clauses)


def not_(clause):
    return UnaryExpression(clause, "NOT", None, Boolean())


class _BoolConst(ColumnElement):
    def __init__(self, value):
        self.value = value
        self.type = Boolean()

    def _compile(self, ctx):
        if ctx.dialect.supports_native_boolean:
            return "true" if self.value else "false"
        return "1" if self.value else "0"


def true():
    return _BoolConst(True)


def false():
    return _BoolConst(False)


class Label(ColumnElement):
    def __init__(self, name, elem):
        self.name = self.key = name
        self.elem = elem
        self.type = elem.type

    def _compile(self, ctx):
        if ctx.columns_clause:
            ctx.columns_clause = False
            s = self.elem._compile(ctx)
            ctx.columns_clause = True
            return "%s AS %s" % (s, self.name)
        return self.name

    def _tables(self, acc):
        self.elem._tables(acc)


class Function(ColumnElement):
    def __init__(self, name, args):
        self.name = self.key = name
        self.args = [_lit(a) for a in args]
        if name == "count":
            self.type = Integer()
        elif name in ("length", "char_length"):
            self.type = Integer()
        elif self.args and name in ("sum", "max", "min", "abs", "coalesce", "lower", "upper", "trim"):
            self.type = self.args[0].type
        elif name == "avg":
            self.type = Float()
        else:
            self.type = NullType()

    def _compile(self, ctx):
        if not self.args and self.name == "count":
            return "count(*)"
        cc = ctx.columns_clause
        ctx.columns_clause = False
        s = "%s(%s)" % (self.name, ", ".join(a._compile(ctx) for a in self.args))
        ctx.columns_clause = cc
        return s

    def _tables(self, acc):
        for a in self.args:
            a._tables(acc)


class _FuncNamespace:
    def __getattr__(self, name):
        if name.startswith("__"):
            raise AttributeError(name)

        def make(*args):
            return Function(name, args)
        return make


func = _FuncNamespace()


class TextClause(ColumnElement):
    def __init__(self, text_):
        self.text = text_

    def _compile(self, ctx):
        return self.text

    def _execute_on(self, conn, params, session=None):
        if isinstance(params, list):
            cur = conn._run(self.text, params, many=True)
        else:
            cur = conn._run(self.text, params if params is not None else {})
        return CursorResult._from_cursor(cur, None)


def text(sql):
    return TextClause(sql)


# ----------------------------------------------------------------------------- schema

class ForeignKey:
    def __init__(self, column, ondelete=None, onupdate=None, **kw):
        self._spec = column
        self.parent = None
        self.ondelete = ondelete

    def _target(self):
        if isinstance(self._spec, Column):
            return self._spec.table.name, self._spec.name
        dot = self._spec.rfind(".")
        return self._spec[:dot], self._spec[dot + 1:]

    @property
    def column(self):
        tname, cname = self._target()
        return self.parent.table.metadata.tables[tname].c[cname]

    def references(self, table):
        return self._target()[0] == table.name


class Column(ColumnElement):
    def __init__(self, *args, **kw):
        self.name = None
        self.type = None
        self.foreign_keys = []
        for a in args:
            if isinstance(a, str):
                self.name = a
            elif isinstance(a, ForeignKey):
                self.foreign_keys.append(a)
                a.parent = self
            elif _is_type(a):
                self.type = _to_type(a)
        if self.type is None:
            self.type = Integer() if self.foreign_keys else NullType()
        self.key = kw.get("key", self.name)
        self.primary_key = kw.get("primary_key", False)
        self.nullable = kw.get("nullable", not self.primary_key)
        self.default = kw.get("default")
        self.unique = kw.get("unique", False)
        self.index = kw.get("index", False)
        self.autoincrement = kw.get("autoincrement", "auto")
        self.table = None

    def _compile(self, ctx):
        q = ctx.dialect.quote
        if ctx.qualify and self.table is not None:
            return "%s.%s" % (q(self.table.name), q(self.name))
        return q(self.name)

    def _tables(self, acc):
        if self.table is not None:
            _add_unique(acc, self.table)

    def _default_value(self):
        d = self.default
        if callable(d):
            try:
                return d()
            except TypeError:
                return d(None)
        return d


def _add_unique(acc, item):
    for x in acc:
        if x is item:
            return
    acc.append(item)


class ColumnCollection:
    def __init__(self):
        self._cols = []
        self._by_key = {}

    def _add(self, col):
        self._cols.append(col)
        self._by_key[col.key] = col

    def __getattr__(self, name):
        if name.startswith("_"):
            raise AttributeError(name)
        try:
            return self._by_key[name]
        except KeyError:
            raise AttributeError(name)

    def __getitem__(self, key):
        if isinstance(key, int):
            return self._cols[key]
        return self._by_key[key]

    def __iter__(self):
        return iter(list(self._cols))

    def __len__(self):
        return len(self._cols)

    def __contains__(self, key):
        return key in self._by_key

    def keys(self):
        return [c.key for c in self._cols]

    def values(self):
        return list(self._cols)


class FromClause(ClauseElement):
    def join(self, right, onclause=None, isouter=False, full=False):
        return Join(self, _from_obj(right), onclause, isouter)

    def outerjoin(self, right, onclause=None, full=False):
        return Join(self, _from_obj(right), onclause, True)


class Table(FromClause):
    def __init__(self, name, metadata, *args, **kw):
        self.name = name
        self.metadata = metadata
        self.c = self.columns = ColumnCollection()
        self.constraints = []
        for a in args:
            if isinstance(a, Column):
                a.table = self
                self.c._add(a)
        metadata.tables[name] = self
        metadata._order.append(self)

    @property
    def primary_key(self):
        return [c for c in self.c if c.primary_key]

    @property
    def foreign_keys(self):
        out = []
        for c in self.c:
            out.extend(c.foreign_keys)
        return out

    def _compile(self, ctx):
        return ctx.dialect.quote(self.name)

    def _tables(self, acc):
        _add_unique(acc, self)

    def _all_tables(self):
        return [self]

    def _create_sql(self):
        d = _active[-1]
        q = d.quote
        parts = []
        pk = self.primary_key
        for c in self.c:
            s = "%s %s" % (q(c.name), d.column_ddl(c, self))
            if not c.nullable or c in pk:
                s += " NOT NULL"
            if d.autoincrement_suffix and d._is_autoincrement(c, self):
                s += d.autoincrement_suffix
            parts.append(s)
        if pk:
            parts.append("PRIMARY KEY (%s)" % ", ".join(q(c.name) for c in pk))
        for c in self.c:
            if c.unique:
                parts.append("UNIQUE (%s)" % q(c.name))
        for c in self.c:
            for fk in c.foreign_keys:
                tname, cname = fk._target()
                s = "FOREIGN KEY(%s) REFERENCES %s (%s)" % (q(c.name), q(tname), q(cname))
                if fk.ondelete:
                    s += " ON DELETE " + fk.ondelete
                parts.append(s)
        return "CREATE TABLE IF NOT EXISTS %s (\n\t%s\n)" % (q(self.name), ", \n\t".join(parts))

    def create(self, bind, checkfirst=True):
        with bind.begin() as conn:
            conn._run(self._create_sql(), ())

    def drop(self, bind, checkfirst=True):
        with bind.begin() as conn:
            conn._run("DROP TABLE IF EXISTS " + self.name, ())

    def insert(self):
        return insert(self)

    def select(self):
        return select(self)

    def update(self):
        return update(self)

    def delete(self):
        return delete(self)


class Join(FromClause):
    def __init__(self, left, right, onclause, isouter):
        self.left = left
        self.right = right
        self.isouter = isouter
        if onclause is None:
            onclause = _join_condition(left._all_tables(), right)
        self.onclause = onclause

    def _all_tables(self):
        return self.left._all_tables() + self.right._all_tables()

    def _tables(self, acc):
        for t in self._all_tables():
            _add_unique(acc, t)

    def _compile(self, ctx):
        kw = "LEFT OUTER JOIN" if self.isouter else "JOIN"
        return "%s %s %s ON %s" % (self.left._compile(ctx), kw, self.right._compile(ctx),
                                   self.onclause._compile(ctx))


def _join_condition(left_tables, right):
    for lt in left_tables:
        for c in right.c:
            for fk in c.foreign_keys:
                if fk.references(lt):
                    return lt.c[fk._target()[1]] == c
        for c in lt.c:
            for fk in c.foreign_keys:
                if fk.references(right):
                    return c == right.c[fk._target()[1]]
    raise exc.ArgumentError("Can't find any foreign key relationships between the tables")


def _from_obj(x):
    al = getattr(x, "_sa_aliased", None)
    if al is not None:
        return al.alias
    m = getattr(x, "__sa_mapper__", None)
    if m is not None:
        return m.local_from
    return x


class MetaData:
    def __init__(self, schema=None):
        self.tables = {}
        self._order = []

    @property
    def sorted_tables(self):
        done = []
        pending = list(self._order)
        while pending:
            progressed = False
            for t in list(pending):
                ok = True
                for fk in t.foreign_keys:
                    tn = fk._target()[0]
                    if tn != t.name and tn in self.tables and not any(d.name == tn for d in done):
                        ok = False
                if ok:
                    done.append(t)
                    pending.remove(t)
                    progressed = True
            if not progressed:
                done.extend(pending)
                break
        return done

    def create_all(self, bind, tables=None, checkfirst=True):
        with bind.begin() as conn:
            for t in self.sorted_tables:
                if tables is None or t in tables:
                    conn._run(t._create_sql(), ())

    def drop_all(self, bind, tables=None, checkfirst=True):
        with bind.begin() as conn:
            for t in reversed(self.sorted_tables):
                if tables is None or t in tables:
                    conn._run("DROP TABLE IF EXISTS " + t.name, ())


# ----------------------------------------------------------------------------- statements

class _Slot:
    """One entry of a select's columns clause: a column expression, or a mapped entity."""

    def __init__(self, elem=None, mapper=None, cols=None, from_=None, key=None):
        self.elem = elem
        self.mapper = mapper
        if mapper is not None:
            self.key = key if key is not None else mapper.class_.__name__
            self.cols = cols if cols is not None else mapper.columns
            self.from_ = from_ if from_ is not None else mapper.local_from
            self.width = len(self.cols)
        else:
            self.key = elem.key if elem.key is not None else elem.name
            self.width = 1


def _slots(ent):
    al = getattr(ent, "_sa_aliased", None)
    if al is not None:
        return [al._slot()]
    m = getattr(ent, "__sa_mapper__", None)
    if m is not None:
        return [_Slot(mapper=m)]
    if isinstance(ent, Table):
        return [_Slot(c) for c in ent.c]
    return [_Slot(_lit(ent))]


class Select(ClauseElement):
    def __init__(self, *entities):
        if len(entities) == 1 and isinstance(entities[0], (list, tuple)):
            entities = entities[0]
        self._slots = []
        for e in entities:
            self._slots.extend(_slots(e))
        self._where = []
        self._order = []
        self._group = []
        self._having = []
        self._joins = []
        self._from = []
        self._limit = None
        self._offset = None
        self._distinct = False
        self._options = []
        self._exec_opts = {}

    def _clone(self):
        s = Select()
        s._options = list(self._options)
        s._exec_opts = dict(self._exec_opts)
        s._slots = list(self._slots)
        s._where = list(self._where)
        s._order = list(self._order)
        s._group = list(self._group)
        s._having = list(self._having)
        s._joins = list(self._joins)
        s._from = list(self._from)
        s._limit = self._limit
        s._offset = self._offset
        s._distinct = self._distinct
        return s

    def where(self, *clauses):
        s = self._clone()
        s._where.extend(clauses)
        return s

    filter = where

    def filter_by(self, **kw):
        s = self._clone()
        t = self._first_table()
        for k in kw:
            s._where.append(t.c[k] == kw[k])
        return s

    def order_by(self, *clauses):
        s = self._clone()
        for c in clauses:
            if c is not None:
                s._order.append(c)
        return s

    def group_by(self, *clauses):
        s = self._clone()
        s._group.extend(clauses)
        return s

    def having(self, *clauses):
        s = self._clone()
        s._having.extend(clauses)
        return s

    def limit(self, n):
        s = self._clone()
        s._limit = n
        return s

    def offset(self, n):
        s = self._clone()
        s._offset = n
        return s

    def distinct(self):
        s = self._clone()
        s._distinct = True
        return s

    def select_from(self, *froms):
        s = self._clone()
        s._from.extend(_from_obj(f) for f in froms)
        return s

    def join(self, target, onclause=None, isouter=False, full=False):
        s = self._clone()
        info = getattr(target, "_join_info", None)
        if info is not None and onclause is None:
            target, onclause = info()
        s._joins.append((_from_obj(target), onclause, isouter))
        return s

    def outerjoin(self, target, onclause=None, full=False):
        return self.join(target, onclause, True)

    join_from = None

    def add_columns(self, *cols):
        s = self._clone()
        for c in cols:
            s._slots.extend(_slots(c))
        return s

    def _first_table(self):
        acc = []
        self._column_tables(acc)
        return acc[0]

    def options(self, *opts):
        s = self._clone()
        s._options.extend(opts)
        return s

    def execution_options(self, **kw):
        s = self._clone()
        s._exec_opts.update(kw)
        return s

    def _column_tables(self, acc):
        for sl in self._slots:
            if sl.mapper is not None:
                _add_unique(acc, sl.from_)
            else:
                sl.elem._tables(acc)

    def _all_where(self):
        out = list(self._where)
        for sl in self._slots:
            if sl.mapper is not None and sl.mapper.poly_filter is not None:
                f = sl.mapper.poly_filter(sl.cols)
                if f is not None:
                    out.append(f)
        return out

    def _froms(self, ctx=None):
        out = self._froms_all()
        if ctx is not None and ctx.correlate and ctx.outer:
            kept = [b for b in out if not any(b is o for o in ctx.outer)]
            if kept:
                return kept
        return out

    def _froms_all(self):
        if self._from:
            base = list(self._from)
        else:
            base = []
            self._column_tables(base)
            if not base:
                for w in self._all_where():
                    w._tables(base)
        joined = []
        for target, _, _ in self._joins:
            joined.append(target)
        covered = []
        for b in base:
            if isinstance(b, Join):
                for t in b._all_tables():
                    covered.append(t)
        out = []
        for b in base:
            if any(b is j for j in joined):
                continue
            if not isinstance(b, Join) and any(b is c for c in covered):
                continue
            out.append(b)
        return out

    def _compile(self, ctx):
        froms = self._froms(ctx)
        saved_outer = ctx.outer
        saved_corr = ctx.correlate
        mine = list(saved_outer)
        for f in froms:
            mine.extend(f._all_tables())
        for target, _, _ in self._joins:
            mine.extend(target._all_tables())
        ctx.outer = mine
        ctx.correlate = False
        ctx.columns_clause = True
        cols = []
        for sl in self._slots:
            if sl.mapper is not None:
                for c in sl.cols:
                    cols.append(c._compile(ctx))
            else:
                cols.append(sl.elem._compile(ctx))
        ctx.columns_clause = False
        sql = "SELECT " + ("DISTINCT " if self._distinct else "") + ", ".join(cols)
        sql = self._compile_rest(ctx, sql, froms)
        ctx.outer = saved_outer
        ctx.correlate = saved_corr
        return sql

    def _compile_rest(self, ctx, sql, froms):
        if froms or self._joins:
            if not froms:
                froms = [self._joins[0][0]]
            parts = [f._compile(ctx) for f in froms]
            seen = []
            for f in froms:
                seen.extend(f._all_tables())
            jsql = ""
            for target, onclause, isouter in self._joins:
                if onclause is None:
                    onclause = _join_condition(seen, target)
                jsql += " %s %s ON %s" % ("LEFT OUTER JOIN" if isouter else "JOIN", target._compile(ctx),
                                          onclause._compile(ctx))
                seen.append(target)
            parts[0] = parts[0] + jsql
            sql += " \nFROM " + ", ".join(parts)
        where = self._all_where()
        if where:
            sql += " \nWHERE " + " AND ".join(w._compile(ctx) for w in where)
        if self._group:
            sql += " GROUP BY " + ", ".join(g._compile(ctx) for g in self._group)
        if self._having:
            sql += " \nHAVING " + " AND ".join(h._compile(ctx) for h in self._having)
        if self._order:
            sql += " ORDER BY " + ", ".join(o._compile(ctx) for o in self._order)
        if self._limit is not None or self._offset is not None:
            sql += ctx.dialect.limit_clause(ctx, self._limit, self._offset)
        return sql

    def _execute_on(self, conn, params, session=None):
        ctx = _Ctx()
        sql = _compile_top(self, ctx)
        cur = conn._run(sql, ctx.params)
        res = CursorResult._from_cursor(cur, self._slots, session)
        res._yield_per = self._exec_opts.get("yield_per")
        return res

    def __str__(self):
        return _compile_top(self, _Ctx())

    def subquery(self, name=None):
        return Alias(self, name)

    alias = subquery

    def cte(self, name=None, recursive=False):
        if recursive:
            raise NotImplementedError("recursive CTEs are not supported")
        return CTE(self, name)

    def scalar_subquery(self):
        return ScalarSelect(self)

    as_scalar = scalar_subquery

    def label(self, name):
        return ScalarSelect(self).label(name)

    def exists(self):
        return Exists(self)

    def union(self, *others):
        return CompoundSelect("UNION", (self,) + others)

    def union_all(self, *others):
        return CompoundSelect("UNION ALL", (self,) + others)

    def _first(self):
        return self

    def _all_tables(self):
        return []


_anon = [0]


def _anon_name(prefix):
    _anon[0] += 1
    return "%s_%d" % (prefix, _anon[0])


def _compile_top(stmt, ctx):
    """Compiles a statement, putting the WITH clause of every CTE it reached ahead of it."""
    body_ctx = _Ctx()
    body = stmt._compile(body_ctx)
    if not body_ctx.ctes:
        ctx.params.extend(body_ctx.params)
        return body
    parts = []
    for c in body_ctx.ctes:
        inner = _Ctx()
        parts.append("%s AS \n(%s)" % (c.name, c.element._compile(inner)))
        ctx.params.extend(inner.params)
    ctx.params.extend(body_ctx.params)
    return "WITH " + ", \n".join(parts) + " \n" + body


class Alias(FromClause):
    """A named FROM element over a table (an alias) or a select (a subquery)."""

    def __init__(self, element, name=None):
        self.element = element
        self.name = name if name is not None else _anon_name(
            element.name if isinstance(element, Table) else "anon")
        self.c = self.columns = ColumnCollection()
        if isinstance(element, Table):
            src = list(element.c)
        else:
            src = []
            for sl in element._first()._slots:
                if sl.mapper is not None:
                    src.extend(sl.cols)
                else:
                    src.append(sl.elem)
        for col in src:
            nm = col.key if col.key is not None else col.name
            c = Column(nm, col.type, primary_key=getattr(col, "primary_key", False))
            c.table = self
            c._proxied = col
            self.c._add(c)

    def corresponding_column(self, col):
        for c in self.c:
            if getattr(c, "_proxied", None) is col:
                return c
        return None

    def _compile(self, ctx):
        if isinstance(self.element, Table):
            return "%s AS %s" % (self.element.name, self.name)
        inner = _Ctx()
        inner.params = ctx.params
        inner.ctes = ctx.ctes
        return "(%s) AS %s" % (self.element._compile(inner), self.name)

    def _tables(self, acc):
        _add_unique(acc, self)

    def _all_tables(self):
        return [self]

    @property
    def foreign_keys(self):
        return []

    def select(self):
        return select(self)


class CTE(Alias):
    def _compile(self, ctx):
        if not any(c is self for c in ctx.ctes):
            ctx.ctes.append(self)
        return self.name


class ScalarSelect(ColumnElement):
    def __init__(self, sel):
        self.sel = sel
        sl = sel._first()._slots[0]
        self.type = sl.elem.type if sl.elem is not None else NullType()

    def _compile(self, ctx):
        cc = ctx.columns_clause
        corr = ctx.correlate
        ctx.columns_clause = False
        ctx.correlate = True
        s = "(%s)" % self.sel._compile(ctx)
        ctx.columns_clause = cc
        ctx.correlate = corr
        return s

    def _first(self):
        return self.sel._first()


class _SubqueryList(ScalarSelect):
    def _compile(self, ctx):
        s = ScalarSelect._compile(self, ctx)
        return s[1:-1]


class Exists(ColumnElement):
    def __init__(self, sel):
        self.sel = sel
        self.type = Boolean()

    def where(self, *clauses):
        return Exists(self.sel.where(*clauses))

    def select_from(self, *froms):
        return Exists(self.sel.select_from(*froms))

    def _compile(self, ctx):
        cc = ctx.columns_clause
        corr = ctx.correlate
        ctx.columns_clause = False
        ctx.correlate = True
        s = "EXISTS (%s)" % self.sel._compile(ctx)
        ctx.columns_clause = cc
        ctx.correlate = corr
        return s

    def select(self):
        return select(self)


def exists(*args):
    if args:
        a = args[0]
        if isinstance(a, (Select, CompoundSelect)):
            return Exists(a)
        return Exists(Select(*args))
    return Exists(Select(TextClause("*")))


class CompoundSelect(ClauseElement):
    def __init__(self, keyword, selects):
        self.keyword = keyword
        self.selects = list(selects)
        self._order = []
        self._limit = None
        self._exec_opts = {}

    def _first(self):
        return self.selects[0]._first()

    @property
    def _slots(self):
        return self._first()._slots

    def order_by(self, *clauses):
        c = CompoundSelect(self.keyword, self.selects)
        c._order = self._order + [x for x in clauses if x is not None]
        c._limit = self._limit
        return c

    def limit(self, n):
        c = self.order_by()
        c._limit = n
        return c

    def _compile(self, ctx):
        sql = (" %s " % self.keyword).join(x._compile(ctx) for x in self.selects)
        if self._order:
            q = ctx.qualify
            ctx.qualify = False
            sql += " ORDER BY " + ", ".join(o._compile(ctx) for o in self._order)
            ctx.qualify = q
        if self._limit is not None:
            sql += " LIMIT ?"
            ctx.params.append(self._limit)
        return sql

    def __str__(self):
        return _compile_top(self, _Ctx())

    def _execute_on(self, conn, params, session=None):
        ctx = _Ctx()
        sql = _compile_top(self, ctx)
        return CursorResult._from_cursor(conn._run(sql, ctx.params), self._slots, session)

    def subquery(self, name=None):
        return Alias(self, name)

    def cte(self, name=None, recursive=False):
        return CTE(self, name)

    def scalar_subquery(self):
        return ScalarSelect(self)

    def _all_tables(self):
        return []


def union(*selects):
    return CompoundSelect("UNION", selects)


def union_all(*selects):
    return CompoundSelect("UNION ALL", selects)


def alias(element, name=None):
    return Alias(element, name)


class Case(ColumnElement):
    def __init__(self, whens, else_):
        self.whens = [(w[0], _lit(w[1])) for w in whens]
        self.else_ = None if else_ is None else _lit(else_)
        self.type = self.whens[0][1].type if self.whens else NullType()

    def _compile(self, ctx):
        cc = ctx.columns_clause
        ctx.columns_clause = False
        out = "CASE"
        for cond, val in self.whens:
            out += " WHEN %s THEN %s" % (cond._compile(ctx), val._compile(ctx))
        if self.else_ is not None:
            out += " ELSE " + self.else_._compile(ctx)
        ctx.columns_clause = cc
        return out + " END"

    def _tables(self, acc):
        for cond, val in self.whens:
            cond._tables(acc)
            val._tables(acc)


def case(*whens, **kw):
    if len(whens) == 1 and isinstance(whens[0], dict):
        raise NotImplementedError("case() with a value= mapping is not supported")
    return Case(whens, kw.get("else_"))


class Cast(ColumnElement):
    def __init__(self, expr, typ):
        self.expr = _lit(expr)
        self.type = _to_type(typ)

    def _compile(self, ctx):
        cc = ctx.columns_clause
        ctx.columns_clause = False
        out = "CAST(%s AS %s)" % (self.expr._compile(ctx), self.type.ddl())
        ctx.columns_clause = cc
        return out

    def _tables(self, acc):
        self.expr._tables(acc)


def cast(expr, type_):
    return Cast(expr, type_)


def select(*entities):
    return Select(*entities)


def _values_from(args, kw):
    d = {}
    if args:
        a = args[0]
        if isinstance(a, dict):
            d.update(a)
    d.update(kw)
    return d


def _key_of(table, k):
    if isinstance(k, Column):
        return k.key
    return k


class _DML(ClauseElement):
    def __init__(self, table):
        self.table = _from_obj(table)
        self._values = None
        self._where = []
        self._returning = []

    def _clone(self):
        s = type(self)(self.table)
        s._values = None if self._values is None else dict(self._values)
        s._where = list(self._where)
        s._returning = list(self._returning)
        return s

    def values(self, *args, **kw):
        s = self._clone()
        d = {} if s._values is None else s._values
        for k, v in _values_from(args, kw).items():
            d[_key_of(self.table, k)] = v
        s._values = d
        return s

    def where(self, *clauses):
        s = self._clone()
        s._where.extend(clauses)
        return s

    filter = where

    def returning(self, *cols):
        s = self._clone()
        for c in cols:
            if isinstance(c, Table):
                s._returning.extend(c.c)
            else:
                s._returning.append(c)
        return s

    def _returning_sql(self, ctx):
        if not self._returning:
            return ""
        q = ctx.qualify
        ctx.qualify = False
        s = " RETURNING " + ", ".join(c._compile(ctx) for c in self._returning)
        ctx.qualify = q
        return s

    def _where_sql(self, ctx):
        if not self._where:
            return ""
        return " WHERE " + " AND ".join(w._compile(ctx) for w in self._where)

    def _result(self, cur):
        slots = [_Slot(_lit(c)) for c in self._returning] if self._returning else None
        return CursorResult._from_cursor(cur, slots)


class Insert(_DML):
    def _row(self, params):
        row = {}
        if self._values:
            row.update(self._values)
        if params:
            row.update(params)
        for c in self.table.c:
            if c.key not in row and c.default is not None:
                row[c.key] = c._default_value()
        return row

    def _sql(self, row, ctx):
        cols = [c for c in self.table.c if c.key in row]
        if not cols:
            return "INSERT INTO %s DEFAULT VALUES" % ctx.dialect.quote(self.table.name) + self._returning_sql(ctx)
        vals = []
        for c in cols:
            vals.append(_lit(row[c.key], c.type)._compile(ctx))
        q = ctx.dialect.quote
        return "INSERT INTO %s (%s) VALUES (%s)" % (
            q(self.table.name), ", ".join(q(c.name) for c in cols), ", ".join(vals)) + self._returning_sql(ctx)

    def _execute_on(self, conn, params, session=None):
        if isinstance(params, list) and len(params) > 1:
            rows = [self._row(p) for p in params]
            ctx = _Ctx()
            sql = self._sql(rows[0], ctx)
            seq = []
            for r in rows:
                c2 = _Ctx()
                self._sql(r, c2)
                seq.append(c2.params)
            cur = conn._run(sql, seq, many=True)
            return self._result(cur)
        if isinstance(params, list):
            params = params[0] if params else None
        row = self._row(params)
        ctx = _Ctx()
        sql = self._sql(row, ctx)
        gen = [c for c in self.table.primary_key if row.get(c.key) is None]
        if gen and not self._returning and ctx.dialect.insert_returning_key:
            # No lastrowid on PostgreSQL: the generated key comes back through RETURNING.
            cur = conn._run(sql + " RETURNING " + ctx.dialect.quote(gen[0].name), ctx.params)
            got = cur.fetchone()
            cur.lastrowid = got[0] if got else None
            cur.description = None
        else:
            cur = conn._run(sql, ctx.params)
        res = self._result(cur)
        pk = []
        for c in self.table.primary_key:
            pk.append(row[c.key] if row.get(c.key) is not None else cur.lastrowid)
        res.inserted_primary_key = Row(pk, dict((c.key, i) for i, c in enumerate(self.table.primary_key)))
        return res


class Update(_DML):
    def _execute_on(self, conn, params, session=None):
        vals = dict(self._values or {})
        if isinstance(params, dict):
            vals.update(params)
        ctx = _Ctx()
        sets = []
        for c in self.table.c:
            if c.key in vals:
                v = vals[c.key]
                sets.append("%s=%s" % (c.name, _lit(v, c.type)._compile(ctx)))
        sql = "UPDATE %s SET %s" % (ctx.dialect.quote(self.table.name), ", ".join(sets)) + self._where_sql(ctx) + self._returning_sql(ctx)
        return self._result(conn._run(sql, ctx.params))


class Delete(_DML):
    def _execute_on(self, conn, params, session=None):
        ctx = _Ctx()
        sql = "DELETE FROM " + ctx.dialect.quote(self.table.name) + self._where_sql(ctx) + self._returning_sql(ctx)
        return self._result(conn._run(sql, ctx.params))


def insert(table):
    return Insert(table)


def update(table):
    return Update(table)


def delete(table):
    return Delete(table)


# ----------------------------------------------------------------------------- results

class Row:
    def __init__(self, values, keymap):
        self.__dict__["_values"] = tuple(values)
        self.__dict__["_keymap"] = keymap

    def __getattr__(self, name):
        km = self.__dict__.get("_keymap")
        if km is not None and name in km:
            return self.__dict__["_values"][km[name]]
        raise AttributeError("Could not locate column in row for column '%s'" % name)

    def __getitem__(self, i):
        return self._values[i]

    def __iter__(self):
        return iter(self._values)

    def __len__(self):
        return len(self._values)

    def __eq__(self, other):
        if isinstance(other, Row):
            return self._values == other._values
        if isinstance(other, tuple):
            return self._values == other
        return False

    def __ne__(self, other):
        return not self.__eq__(other)

    def __hash__(self):
        return hash(self._values)

    def __repr__(self):
        return repr(self._values)

    def _asdict(self):
        return dict((k, self._values[i]) for k, i in self._keymap.items())

    @property
    def _mapping(self):
        return RowMapping(self._asdict())

    @property
    def _fields(self):
        return tuple(self._keymap.keys())

    def _tuple(self):
        return self._values

    @property
    def t(self):
        return self._values

    def tuple(self):
        return self._values


class RowMapping(dict):
    pass


def _only(rows, allow_none):
    if not rows:
        if allow_none:
            return None
        raise exc.NoResultFound("No row was found when one was required")
    if len(rows) > 1:
        raise exc.MultipleResultsFound("Multiple rows were found when exactly one was required")
    return rows[0]


_END = object()


class _Rows:
    """Rows read on demand from an iterator: a cursor is only fetched as far as it is consumed."""

    _yield_per = None

    def __init__(self, rows):
        self._it = iter(rows)

    def _next(self):
        return next(self._it, _END)

    def _take(self):
        return list(self._it)

    def __iter__(self):
        while True:
            r = self._next()
            if r is _END:
                return
            yield r

    def all(self):
        return self._take()

    fetchall = all

    def fetchone(self):
        r = self._next()
        return None if r is _END else r

    def fetchmany(self, size=None):
        if size is None:
            size = self._yield_per or 1
        out = []
        while len(out) < size:
            r = self._next()
            if r is _END:
                break
            out.append(r)
        return out

    def partitions(self, size=None):
        if size is None:
            size = self._yield_per or 100
        while True:
            p = self.fetchmany(size)
            if not p:
                return
            yield p

    def yield_per(self, num):
        self._yield_per = num
        return self

    def _derive(self, cls, gen):
        r = cls(gen)
        r._yield_per = self._yield_per
        return r

    def first(self):
        r = self._take()
        return r[0] if r else None

    def one(self):
        return _only(self._take(), False)

    def one_or_none(self):
        return _only(self._take(), True)

    def unique(self):
        seen = []
        out = []
        for r in self._take():
            if not any(r is x or r == x for x in seen):
                seen.append(r)
                out.append(r)
        self._it = iter(out)
        return self


class ScalarResult(_Rows):
    pass


class MappingResult(_Rows):
    pass


class CursorResult(_Rows):
    def __init__(self, rows, keys, rowcount=-1, lastrowid=None):
        _Rows.__init__(self, rows)
        self._keys = keys
        self.rowcount = rowcount
        self.lastrowid = lastrowid
        self.inserted_primary_key = None

    @staticmethod
    def _from_cursor(cur, slots, session=None):
        desc = cur.description
        if desc is None:
            return CursorResult([], [], cur.rowcount, cur.lastrowid)
        if slots is None:
            keys = [d[0] for d in desc]
            km = dict((k, i) for i, k in enumerate(keys))
            return CursorResult((Row(r, km) for r in _cursor_rows(cur)), keys, cur.rowcount, cur.lastrowid)
        keys = [s.key for s in slots]
        km = {}
        for i, k in enumerate(keys):
            if k is not None and k not in km:
                km[k] = i
        return CursorResult(_load_rows(cur, slots, km, session), keys, cur.rowcount, cur.lastrowid)

    def keys(self):
        return list(self._keys)

    def scalars(self, index=0):
        return self._derive(ScalarResult, (r[index] for r in self))

    def mappings(self):
        return self._derive(MappingResult, (r._mapping for r in self))

    def scalar(self):
        r = self._take()
        return r[0][0] if r else None

    def scalar_one(self):
        return _only(self._take(), False)[0]

    def scalar_one_or_none(self):
        r = _only(self._take(), True)
        return None if r is None else r[0]

    def tuples(self):
        return self

    def close(self):
        pass


Result = CursorResult


def _cursor_rows(cur):
    while True:
        batch = cur.fetchmany(64)
        if not batch:
            return
        for r in batch:
            yield r


def _load_rows(cur, slots, km, session):
    for r in _cursor_rows(cur):
        vals = []
        pos = 0
        for sl in slots:
            if sl.mapper is not None:
                vals.append(sl.mapper._load_row(session, r[pos:pos + sl.width], sl.cols))
            else:
                vals.append(sl.elem.type.result(r[pos]))
            pos += sl.width
        yield Row(vals, km)


# ----------------------------------------------------------------------------- engine

class Connection:
    def __init__(self, engine):
        self.engine = engine
        self._dbapi = engine._raw_connection()
        self.closed = False

    def _run(self, sql, params, many=False):
        if self.engine.echo:
            log = _logging.getLogger("sqlalchemy.engine.Engine")
            log.info("%s", sql)
            log.info("%r", tuple(params) if isinstance(params, list) and not many else params)
        cur = self._dbapi.cursor()
        try:
            if many:
                cur.executemany(sql, params)
            else:
                cur.execute(sql, params)
        except _sqlite3.Error as e:
            raise exc._wrap(e, sql, params)
        return cur

    def execute(self, statement, parameters=None, execution_options=None):
        if isinstance(statement, str):
            raise exc.ArgumentError("Textual SQL expression should be explicitly declared as text()")
        _active.append(self.engine.dialect)
        try:
            return statement._execute_on(self, parameters)
        finally:
            _active.pop()

    def exec_driver_sql(self, sql, parameters=()):
        return CursorResult._from_cursor(self._run(sql, parameters), None)

    def scalar(self, statement, parameters=None):
        return self.execute(statement, parameters).scalar()

    def scalars(self, statement, parameters=None):
        return self.execute(statement, parameters).scalars()

    def commit(self):
        self._dbapi.commit()

    def rollback(self):
        self._dbapi.rollback()

    def begin(self):
        return _Tx(self, False)

    def in_transaction(self):
        return self._dbapi.in_transaction

    def close(self):
        if not self.closed:
            self._dbapi.rollback()
            self.closed = True

    def __enter__(self):
        return self

    def __exit__(self, t, v, tb):
        self.close()
        return False


class _Tx:
    def __init__(self, conn, close_after):
        self.conn = conn
        self.close_after = close_after

    def __enter__(self):
        return self.conn

    def __exit__(self, t, v, tb):
        if t is None:
            self.conn.commit()
        else:
            self.conn.rollback()
        if self.close_after:
            self.conn.close()
        return False

    def commit(self):
        self.conn.commit()

    def rollback(self):
        self.conn.rollback()


_RESERVED = frozenset((
    "all", "and", "as", "asc", "between", "both", "by", "case", "check", "column", "constraint",
    "create", "cross", "default", "delete", "desc", "distinct", "drop", "else", "end", "except",
    "false", "for", "foreign", "from", "grant", "group", "having", "in", "index", "inner", "insert",
    "intersect", "into", "is", "join", "key", "leading", "left", "like", "limit", "not", "null",
    "offset", "on", "or", "order", "outer", "primary", "references", "right", "select", "set",
    "table", "then", "to", "trailing", "true", "union", "unique", "update", "user", "using",
    "values", "when", "where", "with"))


def _plain_identifier(name):
    if not name or not (name[0] == "_" or "a" <= name[0] <= "z"):
        return False
    for ch in name:
        if not (ch == "_" or "a" <= ch <= "z" or "0" <= ch <= "9" or ch == "$"):
            return False
    return name not in _RESERVED


class _IdentifierPreparer:
    def __init__(self, q):
        self.initial_quote = q
        self.final_quote = q

    def quote(self, name):
        if name is None or _plain_identifier(name):
            return name
        q = self.initial_quote
        return q + name.replace(q, q + q) + q


class _Dialect:
    """What differs between engines: quoting, column DDL, LIMIT/OFFSET, booleans and
    how a generated key comes back. Statements always run with ``?`` placeholders (the
    drivers here bind them); ``paramstyle`` reports the one SQLAlchemy's DB-API uses."""

    name = "sqlite"
    driver = "pysqlite"
    paramstyle = "qmark"
    supports_native_boolean = False
    insert_returning = True
    insert_returning_key = False
    autoincrement_suffix = ""
    _quote_char = '"'

    def __init__(self, **kw):
        self.identifier_preparer = _IdentifierPreparer(self._quote_char)

    def quote(self, name):
        return self.identifier_preparer.quote(name)

    def _is_autoincrement(self, c, table):
        pk = table.primary_key
        if len(pk) != 1 or pk[0] is not c or c.foreign_keys:
            return False
        if getattr(c, "autoincrement", "auto") is False:
            return False
        return isinstance(c.type, Integer)

    def column_ddl(self, c, table):
        return c.type.ddl()

    def limit_clause(self, ctx, limit, offset):
        ctx.params.append(limit if limit is not None else -1)
        if offset is None:
            return " \nLIMIT ?"
        ctx.params.append(offset)
        return " \nLIMIT ? OFFSET ?"


class SQLiteDialect(_Dialect):
    pass


def _server_type(t, names):
    for cls, ddl in names:
        if isinstance(t, cls):
            return ddl(t) if callable(ddl) else ddl
    return t.ddl()


def _varchar(dialect):
    def ddl(t):
        n = getattr(t, "length", None)
        if n is None:
            if dialect == "postgresql":
                return "VARCHAR"
            raise exc.CompileError("VARCHAR requires a length on dialect %s" % dialect)
        return "VARCHAR(%d)" % n
    return ddl


class PGDialect(_Dialect):
    name = "postgresql"
    driver = "psycopg2"
    paramstyle = "pyformat"
    supports_native_boolean = True
    insert_returning_key = True

    def column_ddl(self, c, table):
        t = c.type
        if self._is_autoincrement(c, table):
            if isinstance(t, BigInteger):
                return "BIGSERIAL"
            if isinstance(t, SmallInteger):
                return "SMALLSERIAL"
            return "SERIAL"
        return _server_type(t, (
            (Text, "TEXT"), (String, _varchar("postgresql")), (BigInteger, "BIGINT"),
            (SmallInteger, "SMALLINT"), (Integer, "INTEGER"), (Boolean, "BOOLEAN"),
            (DateTime, "TIMESTAMP WITHOUT TIME ZONE"), (Date, "DATE"), (Numeric, "NUMERIC"),
            (Float, "FLOAT"), (LargeBinary, "BYTEA")))

    def limit_clause(self, ctx, limit, offset):
        sql = ""
        if limit is not None:
            sql += " \nLIMIT ?"
            ctx.params.append(limit)
        if offset is not None:
            sql += " OFFSET ?"
            ctx.params.append(offset)
        return sql


class MySQLDialect(_Dialect):
    name = "mysql"
    driver = "mysqldb"
    paramstyle = "format"
    supports_native_boolean = False
    insert_returning = False
    autoincrement_suffix = " AUTO_INCREMENT"
    _quote_char = "`"

    def column_ddl(self, c, table):
        return _server_type(c.type, (
            (Text, "TEXT"), (String, _varchar(self.name)), (BigInteger, "BIGINT"),
            (SmallInteger, "SMALLINT"), (Integer, "INTEGER"), (Boolean, "BOOL"),
            (DateTime, "DATETIME"), (Date, "DATE"), (Numeric, "NUMERIC"), (Float, "FLOAT"),
            (LargeBinary, "BLOB")))

    def limit_clause(self, ctx, limit, offset):
        if offset is None:
            ctx.params.append(limit)
            return " \nLIMIT ?"
        ctx.params.append(offset)
        ctx.params.append(limit if limit is not None else 18446744073709551615)
        return " \nLIMIT ?, ?"


class MariaDBDialect(MySQLDialect):
    name = "mariadb"
    insert_returning = True


_DIALECTS = {"sqlite": SQLiteDialect, "postgresql": PGDialect, "postgres": PGDialect,
             "mysql": MySQLDialect, "mariadb": MariaDBDialect}
_DRIVERS = {"sqlite": "pysqlite", "postgresql": "psycopg", "mysql": "mysqldb", "mariadb": "mysqldb"}


def _dialect_for(url):
    if url.get_backend_name() not in _DIALECTS:
        raise exc.NoSuchModuleError("Can't load plugin: sqlalchemy.dialects:" + url.drivername.replace("+", "."))
    d = _DIALECTS[url.get_backend_name()]()
    drv = url.get_driver_name()
    d.driver = drv
    if drv in ("psycopg", "psycopg2", "pymysql", "asyncpg") and d.name != "sqlite":
        d.paramstyle = "pyformat"
    return d


_active.append(SQLiteDialect())


def _unquote(s):
    out = []
    i = 0
    while i < len(s):
        if s[i] == "%" and i + 2 < len(s):
            try:
                out.append(chr(int(s[i + 1:i + 3], 16)))
                i += 3
                continue
            except ValueError:
                pass
        out.append(s[i])
        i += 1
    return "".join(out)


class URL:
    def __init__(self, drivername, username=None, password=None, host=None, port=None,
                 database=None, query=None):
        self.drivername = drivername
        self.username = username
        self.password = password
        self.host = host
        self.port = port
        self.database = database
        self.query = dict(query or {})

    @classmethod
    def create(cls, drivername, username=None, password=None, host=None, port=None,
               database=None, query=None):
        return cls(drivername, username, password, host, port, database, query)

    def get_backend_name(self):
        return self.drivername.split("+", 1)[0]

    def get_driver_name(self):
        if "+" in self.drivername:
            return self.drivername.split("+", 1)[1]
        return _DRIVERS.get(self.get_backend_name(), "")

    def get_dialect(self):
        return _DIALECTS[self.get_backend_name()]

    def set(self, **kw):
        a = dict(drivername=self.drivername, username=self.username, password=self.password,
                 host=self.host, port=self.port, database=self.database, query=self.query)
        a.update(kw)
        return URL(**a)

    def render_as_string(self, hide_password=True):
        s = self.drivername + "://"
        if self.username is not None:
            s += self.username
            if self.password is not None:
                s += ":" + ("***" if hide_password else self.password)
            s += "@"
        if self.host is not None:
            s += "[" + self.host + "]" if ":" in self.host else self.host
        if self.port is not None:
            s += ":" + str(self.port)
        if self.database is not None:
            s += "/" + self.database
        if self.query:
            s += "?" + "&".join("%s=%s" % (k, self.query[k]) for k in sorted(self.query))
        return s

    def _backend_url(self):
        # The backend's drivers take postgresql:// | mysql:// | mariadb:// and no driver suffix.
        u = self.set(drivername="postgresql" if self.get_backend_name() == "postgres"
                     else self.get_backend_name())
        return u.render_as_string(hide_password=False)

    def __str__(self):
        return self.render_as_string(hide_password=True)

    def __repr__(self):
        return self.render_as_string(hide_password=True)

    def __eq__(self, other):
        return isinstance(other, URL) and str(self) == str(other) and self.password == other.password

    def __hash__(self):
        return hash(str(self))


def make_url(s):
    if isinstance(s, URL):
        return s
    if "://" not in s:
        raise exc.ArgumentError("Could not parse SQLAlchemy URL from given URL string")
    name, rest = s.split("://", 1)
    backend = name.split("+", 1)[0]
    query = {}
    q = rest.find("?")
    if q >= 0:
        for part in rest[q + 1:].split("&"):
            if part:
                k, _, v = part.partition("=")
                query[k] = _unquote(v)
        rest = rest[:q]
    if backend == "sqlite":
        db = rest[1:] if rest.startswith("/") else rest
        return URL(name, database=db if db else None, query=query)
    user = password = host = port = None
    slash = rest.find("/")
    netloc, db = (rest, None) if slash < 0 else (rest[:slash], rest[slash + 1:] or None)
    at = netloc.rfind("@")
    if at >= 0:
        cred, netloc = netloc[:at], netloc[at + 1:]
        user, sep, pw = cred.partition(":")
        user = _unquote(user)
        password = _unquote(pw) if sep else None
    if netloc.startswith("["):
        end = netloc.find("]")
        host, after = netloc[1:end], netloc[end + 1:]
        if after.startswith(":"):
            port = int(after[1:])
    elif ":" in netloc:
        host, p = netloc.rsplit(":", 1)
        port = int(p)
    else:
        host = netloc or None
    return URL(name, user, password, host, port, db, query)


class Engine:
    def __init__(self, url, echo=False):
        self.url = url
        self.echo = echo
        self.dialect = _dialect_for(url)
        self._dbapi = None

    def _raw_connection(self):
        # One DB-API connection for the engine (SQLAlchemy's SingletonThreadPool for SQLite).
        if self._dbapi is None:
            if self.dialect.name == "sqlite":
                self._dbapi = _sqlite3.connect(self.url.database or ":memory:")
            else:
                from . import _cn1dbapi
                self._dbapi = _cn1dbapi.connect(self.url._backend_url())
                if self._dbapi.server_dialect == "mariadb" and self.dialect.name == "mysql":
                    # The server says which it is, whatever the URL scheme claimed.
                    self.dialect = MariaDBDialect()
        return self._dbapi

    def raw_connection(self):
        return self._raw_connection()

    def connect(self):
        return Connection(self)

    def begin(self):
        return _Tx(Connection(self), True)

    def dispose(self):
        if self._dbapi is not None:
            self._dbapi.close()
            self._dbapi = None


def create_engine(url, echo=False, **kwargs):
    if not isinstance(url, URL):
        url = make_url(url)
    if echo:
        # As SQLAlchemy does: INFO on the engine logger, and a stdout handler when nothing
        # handles that logger yet.
        log = _logging.getLogger("sqlalchemy.engine.Engine")
        log.setLevel(_logging.DEBUG if echo == "debug" else _logging.INFO)
        if not log.handlers:
            import sys as _sys
            h = _logging.StreamHandler(_sys.stdout)
            h.setFormatter(_logging.Formatter("%(asctime)s %(levelname)s %(name)s %(message)s"))
            log.addHandler(h)
    return Engine(url, echo)


def inspect(obj):
    return obj

"""SQLAlchemy 2.0 declarative ORM subset: DeclarativeBase, Mapped, mapped_column,
relationship (one-to-many / many-to-one, lazy "select" loading) and Session.

Mapping is done by a metaclass reading the class body's ``__annotations__``; nothing is
evaluated with ``exec``. Instance state lives in ``obj.__dict__``: a column attribute that
is absent from it is *expired* and is reloaded from the database on access, which is how
``expire_on_commit``, ``refresh`` and ``rollback`` work. Changes are found at flush time by
comparing the attribute values with a snapshot taken at load, so no attribute events.

Unsupported: many-to-many (``secondary``), inheritance mapping, eager loading options,
cascade configuration (save-update and backref population only; deleting a parent does
not touch its children), composite primary keys in ``Session.get``, hybrid properties.
"""

import datetime as _dt
import typing as _typing

from sqlalchemy import (Column, ForeignKey, Integer, String, Float, Boolean, DateTime, Date, LargeBinary,
                        MetaData, Table, TypeEngine, select, exc, _is_type, _to_type)

__all__ = ["DeclarativeBase", "Mapped", "mapped_column", "relationship", "Session", "sessionmaker",
           "declarative_base", "registry"]

_PY_TYPES = {int: Integer, str: String, float: Float, bool: Boolean, bytes: LargeBinary,
             _dt.datetime: DateTime, _dt.date: Date}
_PY_NAMES = {"int": Integer, "str": String, "float": Float, "bool": Boolean, "bytes": LargeBinary,
             "datetime": DateTime, "datetime.datetime": DateTime, "date": Date, "datetime.date": Date}


class _MappedAnnotation:
    def __init__(self, arg):
        self.arg = arg


class Mapped:
    def __class_getitem__(cls, item):
        return _MappedAnnotation(item)


WriteOnlyMapped = Mapped
DynamicMapped = Mapped


def _name_of(t):
    if isinstance(t, str):
        return t.strip().strip("'\"")
    fa = getattr(t, "__forward_arg__", None)
    if fa is not None:
        return fa
    return getattr(t, "__name__", None)


def _strip(s, prefix):
    for p in prefix:
        if s.startswith(p + "[") and s.endswith("]"):
            return s[len(p) + 1:-1].strip()
    return None


def _analyze(ann):
    """Returns (python type or class name, optional flag, is_list) for an annotation."""
    if isinstance(ann, _MappedAnnotation):
        ann = ann.arg
    elif isinstance(ann, str):
        inner = _strip(ann.strip(), ("Mapped", "orm.Mapped", "WriteOnlyMapped", "DynamicMapped"))
        if inner is None:
            return None, False, False
        ann = inner
    if isinstance(ann, str):
        s = ann.strip()
        inner = _strip(s, ("Optional", "typing.Optional"))
        if inner is not None:
            t, _, lst = _analyze_str(inner)
            return t, True, lst
        if s.endswith("| None"):
            t, _, lst = _analyze_str(s[:-6].strip())
            return t, True, lst
        return _analyze_str(s)
    origin = _typing.get_origin(ann)
    args = _typing.get_args(ann)
    if origin is _typing.Union or (args and type(None) in args and origin is not list):
        rest = [a for a in args if a is not type(None)]
        t, _, lst = _analyze(rest[0]) if rest else (None, False, False)
        return t, True, lst
    if origin in (list, set):
        return _name_of(args[0]) if args else None, False, True
    return ann, False, False


def _analyze_str(s):
    inner = _strip(s, ("List", "list", "typing.List", "Set", "set"))
    if inner is not None:
        return _name_of(inner), False, True
    return _name_of(s), False, False


def _sql_type(pytype):
    if pytype is None:
        return None
    if isinstance(pytype, str):
        t = _PY_NAMES.get(pytype)
    else:
        t = _PY_TYPES.get(pytype)
    return t() if t is not None else None


class _MappedColumnSpec:
    def __init__(self, args, kw):
        self.args = args
        self.kw = kw

    def _make(self, key, pytype, optional):
        args = list(self.args)
        name = key
        if args and isinstance(args[0], str):
            name = args.pop(0)
        if not any(_is_type(a) for a in args):
            t = _sql_type(pytype)
            if t is not None:
                args.insert(0, t)
        kw = dict(self.kw)
        if "nullable" not in kw:
            kw["nullable"] = optional and not kw.get("primary_key", False)
        col = Column(name, *args, **kw)
        col.key = key
        return col


def mapped_column(*args, **kw):
    for k in ("init", "repr", "kw_only", "insert_default", "sort_order", "deferred", "compare"):
        if k == "insert_default" and k in kw:
            kw["default"] = kw.pop(k)
        else:
            kw.pop(k, None)
    return _MappedColumnSpec(args, kw)


class _ColumnAttr:
    def __init__(self, key, column):
        self.key = key
        self.column = column

    def __get__(self, obj, owner=None):
        if obj is None:
            return self.column
        d = obj.__dict__
        if self.key in d:
            return d[self.key]
        st = d.get("_sa_state")
        if st is not None and st.session is not None and st.persistent:
            st.session._load_expired(obj)
            # obj.__dict__ may be a snapshot taken when read: read it again after the load.
            return obj.__dict__.get(self.key)
        return None

    def __set__(self, obj, value):
        obj.__dict__[self.key] = value


_ONETOMANY = "ONETOMANY"
_MANYTOONE = "MANYTOONE"


class InstrumentedList:
    def __init__(self, owner, prop):
        self._owner = owner
        self._prop = prop
        self._items = []

    def _raw_add(self, x):
        for i in self._items:
            if i is x:
                return
        self._items.append(x)

    def append(self, x):
        self._raw_add(x)
        self._prop._back_set(self._owner, x)

    def extend(self, xs):
        for x in xs:
            self.append(x)

    def insert(self, i, x):
        self._items.insert(i, x)
        self._prop._back_set(self._owner, x)

    def remove(self, x):
        for i in range(len(self._items)):
            if self._items[i] is x:
                del self._items[i]
                self._prop._back_set(None, x)
                return
        raise ValueError("list.remove(x): x not in list")

    def pop(self, i=-1):
        x = self._items.pop(i)
        self._prop._back_set(None, x)
        return x

    def clear(self):
        for x in list(self._items):
            self.remove(x)

    def __iter__(self):
        return iter(list(self._items))

    def __len__(self):
        return len(self._items)

    def __bool__(self):
        return len(self._items) > 0

    def __getitem__(self, i):
        return self._items[i]

    def __contains__(self, x):
        return any(i is x for i in self._items)

    def __eq__(self, other):
        if isinstance(other, InstrumentedList):
            return self._items == other._items
        return self._items == other

    def __repr__(self):
        return repr(self._items)


class RelationshipProperty:
    def __init__(self, argument=None, back_populates=None, uselist=None, lazy="select", order_by=None,
                 foreign_keys=None, cascade=None, backref=None, viewonly=False, secondary=None):
        if secondary is not None:
            raise NotImplementedError("many-to-many relationships (secondary=) are not supported")
        self.argument = argument
        self.back_populates = back_populates
        self.uselist = uselist
        self.order_by = order_by
        self.key = None
        self.parent = None
        self._target_name = None
        self._list_ann = None
        self._ready = False

    def _setup(self):
        if self._ready:
            return
        arg = self.argument
        if arg is None:
            arg = self._target_name
        if callable(arg) and not isinstance(arg, type):
            arg = arg()
        if isinstance(arg, str):
            arg = self.parent._sa_registry[arg]
        self.target = arg
        pt = self.parent.__table__
        tt = arg.__table__
        self.direction = None
        for c in pt.c:
            for fk in c.foreign_keys:
                if fk.references(tt):
                    self.direction = _MANYTOONE
                    self.local = c
                    self.remote = tt.c[fk._target()[1]]
        if self.direction is None:
            for c in tt.c:
                for fk in c.foreign_keys:
                    if fk.references(pt):
                        self.direction = _ONETOMANY
                        self.local = pt.c[fk._target()[1]]
                        self.remote = c
        if self.direction is None:
            raise exc.ArgumentError("Could not determine join condition for relationship %s.%s"
                                    % (self.parent.__name__, self.key))
        if self.uselist is None:
            self.uselist = self.direction == _ONETOMANY if self._list_ann is None else self._list_ann
        self._ready = True

    def _other(self):
        self._setup()
        if self.back_populates:
            return self.target.__sa_mapper__.relationships[self.back_populates]
        return None

    def _join_info(self):
        self._setup()
        return self.target.__table__, self.local == self.remote

    def __get__(self, obj, owner=None):
        if obj is None:
            return self
        d = obj.__dict__
        if self.key in d:
            return d[self.key]
        self._setup()
        st = d.get("_sa_state")
        if st is not None and st.session is not None and st.persistent:
            value = st.session._lazy_load(obj, self)
        elif self.uselist:
            value = InstrumentedList(obj, self)
        else:
            value = None
        d[self.key] = value
        return value

    def __set__(self, obj, value):
        self._setup()
        if self.uselist:
            lst = InstrumentedList(obj, self)
            obj.__dict__[self.key] = lst
            for x in value:
                lst.append(x)
            return
        old = obj.__dict__.get(self.key)
        obj.__dict__[self.key] = value
        other = self._other()
        if other is None:
            return
        if other.uselist:
            if old is not None and old is not value:
                ol = old.__dict__.get(other.key)
                if isinstance(ol, InstrumentedList):
                    ol._items = [i for i in ol._items if i is not obj]
            if value is not None:
                getattr(value, other.key)._raw_add(obj)
        elif value is not None:
            value.__dict__[other.key] = obj

    def _back_set(self, owner, child):
        """``child`` was added to (owner) or removed from (None) this one-to-many collection."""
        other = self._other()
        if other is not None:
            child.__dict__[other.key] = owner


def relationship(argument=None, **kw):
    return RelationshipProperty(argument, **kw)


class _State:
    def __init__(self):
        self.session = None
        self.persistent = False
        self.snapshot = {}


def _state(obj):
    d = obj.__dict__
    st = d.get("_sa_state")
    if st is None:
        st = _State()
        d["_sa_state"] = st
    return st


class Mapper:
    def __init__(self, cls, table, attrs, relationships):
        self.class_ = cls
        self.table = table
        self.attrs = attrs
        self.columns = [c for k, c in attrs]
        self.relationships = relationships
        self.pk = [(k, c) for k, c in attrs if c.primary_key]

    def _identity(self, obj):
        return tuple(obj.__dict__.get(k) for k, c in self.pk)

    def _populate(self, obj, values):
        d = obj.__dict__
        st = _state(obj)
        for i in range(len(self.attrs)):
            k, c = self.attrs[i]
            v = c.type.result(values[i])
            d[k] = v
            st.snapshot[k] = v

    def _load_row(self, session, values):
        key = []
        for i in range(len(self.attrs)):
            if self.attrs[i][1].primary_key:
                key.append(values[i])
        key = (self.class_, tuple(key))
        if session is not None:
            obj = session._identity.get(key)
            if obj is not None:
                if any(k not in obj.__dict__ for k, c in self.attrs):
                    self._populate(obj, values)
                return obj
        obj = self.class_.__new__(self.class_)
        st = _state(obj)
        self._populate(obj, values)
        if session is not None:
            st.session = session
            st.persistent = True
            session._identity[key] = obj
        return obj


class DeclarativeMeta(type):
    def __new__(mcs, name, bases, ns, **kw):
        cls = type.__new__(mcs, name, bases, ns)
        if ns.get("_sa_root"):
            return cls
        if not any(b.__dict__.get("_sa_registry") is not None for b in cls.__mro__[1:]) \
                or "_sa_registry" in ns:
            if "metadata" not in ns:
                cls.metadata = MetaData()
            cls._sa_registry = {}
            cls.registry = cls._sa_registry
            return cls
        if "__tablename__" not in ns and "__table__" not in ns:
            return cls
        _map_class(cls, ns)
        return cls


def _map_class(cls, ns):
    ann = ns.get("__annotations__", {})
    attrs = []
    rels = {}
    for key in ann:
        a = ann[key]
        v = ns.get(key)
        if isinstance(v, RelationshipProperty):
            pyt, optional, is_list = _analyze(a)
            v._target_name = pyt if isinstance(pyt, str) else _name_of(pyt)
            v._list_ann = is_list if pyt is not None else None
            v.key = key
            v.parent = cls
            rels[key] = v
            continue
        pyt, optional, is_list = _analyze(a)
        if isinstance(v, _MappedColumnSpec):
            col = v._make(key, pyt, optional)
        elif isinstance(v, Column):
            col = v
            if col.name is None:
                col.name = col.key = key
        elif v is None and (isinstance(a, _MappedAnnotation) or (isinstance(a, str) and "Mapped" in a)):
            t = _sql_type(pyt)
            if t is None:
                raise exc.ArgumentError("Could not locate SQLAlchemy type for %s.%s" % (cls.__name__, key))
            col = Column(key, t, nullable=optional)
        else:
            continue
        attrs.append((key, col))
    for key in ns:
        v = ns[key]
        if key not in ann:
            if isinstance(v, _MappedColumnSpec):
                attrs.append((key, v._make(key, None, False)))
            elif isinstance(v, Column):
                if v.name is None:
                    v.name = v.key = key
                attrs.append((key, v))
            elif isinstance(v, RelationshipProperty):
                v.key = key
                v.parent = cls
                rels[key] = v
    table = Table(cls.__tablename__, cls.metadata, *[c for k, c in attrs])
    cls.__table__ = table
    cls.__sa_mapper__ = Mapper(cls, table, attrs, rels)
    for k, c in attrs:
        setattr(cls, k, _ColumnAttr(k, c))
    cls._sa_registry[cls.__name__] = cls


class DeclarativeBase(metaclass=DeclarativeMeta):
    _sa_root = True

    def __init__(self, **kw):
        m = type(self).__sa_mapper__
        _state(self)
        for k in kw:
            if not any(k == a for a, c in m.attrs) and k not in m.relationships:
                raise TypeError("%r is an invalid keyword argument for %s" % (k, type(self).__name__))
            setattr(self, k, kw[k])

    def __repr__(self):
        m = type(self).__sa_mapper__
        return "<%s %s>" % (type(self).__name__, m._identity(self))


DeclarativeBaseNoMeta = DeclarativeBase


class MappedAsDataclass:
    pass


def declarative_base(metadata=None):
    ns = {"_sa_registry": {}}
    if metadata is not None:
        ns["metadata"] = metadata
    return DeclarativeMeta("Base", (DeclarativeBase,), ns)


class _SessionTx:
    def __init__(self, session):
        self.session = session

    def __enter__(self):
        return self.session

    def __exit__(self, t, v, tb):
        if t is None:
            self.session.commit()
        else:
            self.session.rollback()
        return False


class Session:
    def __init__(self, bind=None, autoflush=True, expire_on_commit=True, **kw):
        self.bind = bind
        self.autoflush = autoflush
        self.expire_on_commit = expire_on_commit
        self._conn = None
        self._identity = {}
        self._new = []
        self._deleted = []
        self._tx_inserted = []
        self._tx_deleted = []

    # -- plumbing
    def connection(self):
        if self._conn is None:
            self._conn = self.bind.connect()
        return self._conn

    def __enter__(self):
        return self

    def __exit__(self, t, v, tb):
        self.close()
        return False

    def begin(self):
        return _SessionTx(self)

    def close(self):
        if self._conn is not None:
            self._conn.close()
            self._conn = None
        self.expunge_all()

    def expunge_all(self):
        for obj in list(self._identity.values()) + self._new:
            _state(obj).session = None
        self._identity = {}
        self._new = []
        self._deleted = []

    def expunge(self, obj):
        st = _state(obj)
        st.session = None
        self._new = [o for o in self._new if o is not obj]
        for k in list(self._identity):
            if self._identity[k] is obj:
                del self._identity[k]

    # -- unit of work
    def add(self, obj, _warn=True):
        st = _state(obj)
        if st.persistent and st.session is self:
            return
        if any(o is obj for o in self._new):
            return
        st.session = self
        self._new.append(obj)
        self._cascade(obj)

    def add_all(self, objs):
        for o in objs:
            self.add(o)

    def _cascade(self, obj):
        m = type(obj).__sa_mapper__
        for key in m.relationships:
            v = obj.__dict__.get(key)
            if v is None:
                continue
            if isinstance(v, InstrumentedList):
                for x in v._items:
                    self._cascade_one(x)
            else:
                self._cascade_one(v)

    def _cascade_one(self, x):
        st = _state(x)
        if st.persistent and st.session is self:
            return
        self.add(x)

    def delete(self, obj):
        st = _state(obj)
        if not st.persistent:
            self._new = [o for o in self._new if o is not obj]
            return
        if not any(o is obj for o in self._deleted):
            self._deleted.append(obj)

    def __contains__(self, obj):
        st = _state(obj)
        return st.session is self

    def _sync(self, obj):
        m = type(obj).__sa_mapper__
        for key in m.relationships:
            rel = m.relationships[key]
            rel._setup()
            v = obj.__dict__.get(key)
            if v is None:
                continue
            if rel.direction == _MANYTOONE and not isinstance(v, InstrumentedList):
                obj.__dict__[_attr_key(m, rel.local)] = v.__dict__.get(_attr_key(rel.target.__sa_mapper__, rel.remote))
            elif rel.direction == _ONETOMANY and isinstance(v, InstrumentedList):
                pk = obj.__dict__.get(_attr_key(m, rel.local))
                tm = rel.target.__sa_mapper__
                for child in v._items:
                    child.__dict__[_attr_key(tm, rel.remote)] = pk

    def flush(self, objects=None):
        for obj in list(self._identity.values()):
            self._cascade(obj)
        for obj in list(self._new):
            self._cascade(obj)
        conn = self.connection()
        order = {}
        tables = None
        if self._new:
            tables = type(self._new[0]).metadata.sorted_tables
            for i in range(len(tables)):
                order[tables[i].name] = i
        new = sorted(self._new, key=lambda o: order.get(type(o).__table__.name, 0))
        self._new = []
        for obj in new:
            self._sync(obj)
            self._insert(conn, obj)
            self._sync(obj)
        for obj in list(self._identity.values()):
            if any(o is obj for o in self._deleted):
                continue
            self._sync(obj)
            self._update(conn, obj)
        dels = self._deleted
        self._deleted = []
        if dels:
            tables = type(dels[0]).metadata.sorted_tables
            for i in range(len(tables)):
                order[tables[i].name] = i
            dels = sorted(dels, key=lambda o: -order.get(type(o).__table__.name, 0))
        for obj in dels:
            m = type(obj).__sa_mapper__
            where = [c == obj.__dict__.get(k) for k, c in m.pk]
            conn.execute(m.table.delete().where(*where))
            key = (type(obj), m._identity(obj))
            if key in self._identity:
                del self._identity[key]
            _state(obj).persistent = False
            self._tx_deleted.append(obj)

    def _insert(self, conn, obj):
        m = type(obj).__sa_mapper__
        st = _state(obj)
        d = obj.__dict__
        values = {}
        for k, c in m.attrs:
            v = d.get(k)
            if v is None and c.default is not None:
                v = c._default_value()
                d[k] = v
            if v is not None or k in d:
                values[c.key] = v
        res = conn.execute(m.table.insert().values(**values))
        for k, c in m.pk:
            if d.get(k) is None:
                d[k] = res.lastrowid
        for k, c in m.attrs:
            if k not in d:
                d[k] = None
            st.snapshot[k] = d[k]
        st.persistent = True
        st.session = self
        self._identity[(type(obj), m._identity(obj))] = obj
        self._tx_inserted.append(obj)

    def _update(self, conn, obj):
        m = type(obj).__sa_mapper__
        st = _state(obj)
        d = obj.__dict__
        changes = {}
        for k, c in m.attrs:
            if k in d and (k not in st.snapshot or st.snapshot[k] != d[k]):
                changes[c.key] = d[k]
        if not changes:
            return
        where = [c == st.snapshot.get(k, d.get(k)) for k, c in m.pk]
        conn.execute(m.table.update().where(*where).values(**changes))
        for k in changes:
            st.snapshot[k] = d[k]

    def commit(self):
        self.flush()
        if self._conn is not None:
            self._conn.commit()
        self._tx_inserted = []
        self._tx_deleted = []
        if self.expire_on_commit:
            self.expire_all()

    def rollback(self):
        if self._conn is not None:
            self._conn.rollback()
        for obj in self._tx_inserted:
            m = type(obj).__sa_mapper__
            key = (type(obj), m._identity(obj))
            if self._identity.get(key) is obj:
                del self._identity[key]
            st = _state(obj)
            st.persistent = False
            st.session = None
            for k, c in m.pk:
                if c.autoincrement and isinstance(c.type, Integer):
                    obj.__dict__[k] = None
        for obj in self._tx_deleted:
            st = _state(obj)
            st.persistent = True
            self._identity[(type(obj), type(obj).__sa_mapper__._identity(obj))] = obj
        for obj in self._new:
            _state(obj).session = None
        self._new = []
        self._deleted = []
        self._tx_inserted = []
        self._tx_deleted = []
        self.expire_all()

    def expire_all(self):
        for obj in list(self._identity.values()):
            self.expire(obj)

    def expire(self, obj, attribute_names=None):
        m = type(obj).__sa_mapper__
        d = obj.__dict__
        pk = [k for k, c in m.pk]
        for k, c in m.attrs:
            if k not in pk and (attribute_names is None or k in attribute_names):
                if k in d:
                    del d[k]
        for k in m.relationships:
            if attribute_names is None or k in attribute_names:
                if k in d:
                    del d[k]

    def refresh(self, obj, attribute_names=None):
        self.expire(obj, attribute_names)
        self._load_expired(obj)

    def _load_expired(self, obj):
        m = type(obj).__sa_mapper__
        st = _state(obj)
        where = [c == st.snapshot.get(k, obj.__dict__.get(k)) for k, c in m.pk]
        stmt = select(*m.columns).where(*where)
        row = stmt._execute_on(self.connection(), None).first()
        if row is None:
            raise exc.InvalidRequestError("Instance has been deleted, or its row is otherwise not present.")
        d = obj.__dict__
        for i in range(len(m.attrs)):
            k = m.attrs[i][0]
            d[k] = row[i]
            st.snapshot[k] = row[i]

    def _lazy_load(self, obj, rel):
        m = type(obj).__sa_mapper__
        tm = rel.target.__sa_mapper__
        if rel.direction == _ONETOMANY:
            lst = InstrumentedList(obj, rel)
            pk = getattr(obj, _attr_key(m, rel.local))
            stmt = select(rel.target).where(rel.remote == pk)
            if rel.order_by is not None:
                stmt = stmt.order_by(rel.order_by)
            for x in stmt._execute_on(self.connection(), None, self).scalars():
                lst._items.append(x)
            if rel.uselist:
                return lst
            return lst._items[0] if lst._items else None
        fk = getattr(obj, _attr_key(m, rel.local))
        if fk is None:
            return None
        key = (rel.target, (fk,))
        if key in self._identity:
            return self._identity[key]
        stmt = select(rel.target).where(rel.remote == fk)
        return stmt._execute_on(self.connection(), None, self).scalars().first()

    # -- queries
    def _autoflush(self):
        if self.autoflush:
            self.flush()

    def execute(self, statement, params=None, **kw):
        self._autoflush()
        return statement._execute_on(self.connection(), params, self)

    def scalars(self, statement, params=None, **kw):
        return self.execute(statement, params).scalars()

    def scalar(self, statement, params=None, **kw):
        return self.execute(statement, params).scalar()

    def get(self, entity, ident, **kw):
        if isinstance(ident, dict):
            ident = tuple(ident.values())
        if not isinstance(ident, tuple):
            ident = (ident,)
        obj = self._identity.get((entity, ident))
        if obj is not None:
            return obj
        m = entity.__sa_mapper__
        where = [m.pk[i][1] == ident[i] for i in range(len(m.pk))]
        return select(entity).where(*where)._execute_on(self.connection(), None, self).scalars().first()

    def get_one(self, entity, ident, **kw):
        obj = self.get(entity, ident)
        if obj is None:
            raise exc.NoResultFound("No row was found when one was required")
        return obj

    def merge(self, obj):
        self.add(obj)
        return obj

    @property
    def new(self):
        return list(self._new)

    @property
    def deleted(self):
        return list(self._deleted)

    @property
    def dirty(self):
        out = []
        for obj in self._identity.values():
            st = _state(obj)
            m = type(obj).__sa_mapper__
            for k, c in m.attrs:
                if k in obj.__dict__ and k in st.snapshot and st.snapshot[k] != obj.__dict__[k]:
                    out.append(obj)
                    break
        return out


def _attr_key(mapper, col):
    for k, c in mapper.attrs:
        if c is col:
            return k
    return col.key


class sessionmaker:
    def __init__(self, bind=None, **kw):
        self.bind = bind
        self.kw = kw

    def __call__(self, **kw):
        args = dict(self.kw)
        args.update(kw)
        return Session(self.bind, **args)

    def begin(self):
        return _SessionTx(self())


def registry():
    return {}

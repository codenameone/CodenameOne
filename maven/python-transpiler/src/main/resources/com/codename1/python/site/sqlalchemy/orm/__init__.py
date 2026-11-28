"""SQLAlchemy 2.0 declarative ORM subset: DeclarativeBase, Mapped, mapped_column,
relationship (one-to-many, many-to-one, many-to-many through ``secondary``) and Session.

There is no metaclass and nothing is looked up by a computed name. The build
(``SqlAlchemyLowering``) rewrites every module-level mapped class: each column and
relationship becomes a descriptor naming its key and its slot number literally
(``_sa_col``/``_sa_rel``), the class gets literal tables (``__cn1_sa_cols__``,
``__cn1_sa_rels__``, ``__cn1_sa_parent__``, ``__cn1_sa_table__``, ``__cn1_sa_args__``,
``__cn1_sa_nslots__``), a keyword-only ``__init__`` storing each column's slot literally
(unless the user wrote one) and a keyword hook ``__cn1_sa_kw__``, ``back_populates`` pairs are
linked with ``_sa_pair`` and the class is mapped by ``M.__cn1_sa_map__()`` right after
its definition.

Instance state lives in a declared slot list ``obj._sa_v`` indexed by those slot
numbers: a slot holding ``_UNSET`` is *expired* and is reloaded from the database on
access, which is how ``expire_on_commit``, ``refresh`` and ``rollback`` work. Changes are
found at flush time by comparing the slots with a snapshot taken at load, so no
attribute events.

Also: cascades (save-update, delete, delete-orphan; deleting a parent nulls its children's
foreign keys unless ``passive_deletes``), ``selectinload``/``joinedload`` and ``lazy=`` eager
strategies (both run one extra ``IN`` query per relationship rather than a JOIN), composite
primary keys, single-table and joined-table inheritance with ``polymorphic_on``,
``aliased()`` and ``any()``/``has()`` on relationships.

Unsupported: chained loader options, concrete-table inheritance, joins along a many-to-many
relationship, hybrid properties, mapped classes not defined at module level.
"""

import datetime as _dt
import sqlalchemy as _sa

from sqlalchemy import (Column, ForeignKey, Integer, String, Float, Boolean, DateTime, Date, LargeBinary,
                        MetaData, Table, TypeEngine, select, exc, _is_type, _to_type)
from sqlalchemy import Select as _Select, Exists as _Exists, TextClause as _TextClause, Join as _Join, \
    CursorResult as _CursorResult, Alias as _Alias, _Slot

__all__ = ["DeclarativeBase", "Mapped", "mapped_column", "relationship", "Session", "sessionmaker",
           "declarative_base", "registry", "selectinload", "joinedload", "subqueryload", "lazyload",
           "aliased"]

_PY_NAMES = {"int": Integer, "str": String, "float": Float, "bool": Boolean, "bytes": LargeBinary,
             "datetime": DateTime, "datetime.datetime": DateTime, "date": Date, "datetime.date": Date}


class _Unset:
    def __repr__(self):
        return "<unset>"


# The value of a slot that holds nothing: never loaded, or expired.
_UNSET = _Unset()


class Mapped:
    def __class_getitem__(cls, item):
        return cls


WriteOnlyMapped = Mapped
DynamicMapped = Mapped


def _sql_type(name, tmap=None):
    if name is None:
        return None
    if tmap:
        # DeclarativeBase.type_annotation_map: {str: String(100)} is how a model
        # gives every Mapped[str] a length, which MySQL's VARCHAR demands.
        short = name.split(".")[-1]
        for k in tmap:
            if getattr(k, "__name__", None) == short:
                v = tmap[k]
                return v() if isinstance(v, type) else v
    t = _PY_NAMES.get(name)
    return t() if t is not None else None


class _MappedColumnSpec:
    def __init__(self, args, kw):
        self.args = args
        self.kw = kw

    def _make(self, key, pytype, optional, tmap=None):
        args = list(self.args)
        name = key
        if args and isinstance(args[0], str):
            name = args.pop(0)
        if not any(_is_type(a) for a in args):
            t = _sql_type(pytype, tmap)
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


def _peek(obj, i):
    """Slot ``i`` of ``obj`` without loading it: None when it holds nothing."""
    v = obj._sa_v[i]
    return None if v is _UNSET else v


def _get(obj, i):
    """Slot ``i`` of ``obj``, reloading the row first when it is expired."""
    v = obj._sa_v[i]
    if v is not _UNSET:
        return v
    st = obj._sa_state
    if st.session is not None and st.persistent:
        st.session._load_expired(obj)
        return _peek(obj, i)
    return None


class _ColumnAttr:
    """A mapped column: the class reads the Column, an instance reads slot ``idx``."""

    def __init__(self, idx, key, pytype, optional, spec):
        self.idx = idx
        self.key = key
        self.pytype = pytype
        self.optional = optional
        self.spec = spec
        self.column = None

    def _build(self, owner, tmap):
        spec = self.spec
        if isinstance(spec, _MappedColumnSpec):
            col = spec._make(self.key, self.pytype, self.optional, tmap)
        elif isinstance(spec, Column):
            col = spec
            if col.name is None:
                col.name = col.key = self.key
        else:
            t = _sql_type(self.pytype, tmap)
            if t is None:
                raise exc.ArgumentError("Could not locate SQLAlchemy type for %s.%s" % (owner.__name__, self.key))
            col = Column(self.key, t, nullable=self.optional)
        self.column = col
        return col

    def __get__(self, obj, owner=None):
        if obj is None:
            return self.column
        return _get(obj, self.idx)

    def __set__(self, obj, value):
        obj._sa_v[self.idx] = value


def _sa_col(idx, key, pytype, optional, spec):
    return _ColumnAttr(idx, key, pytype, optional, spec)


class _Thunk:
    """A value the build wrote as a deferred expression (``order_by="Book.id"``)."""

    def __init__(self, fn):
        self.fn = fn


_ONETOMANY = "ONETOMANY"
_MANYTOONE = "MANYTOONE"
_MANYTOMANY = "MANYTOMANY"


class InstrumentedList:
    def __init__(self, owner, prop):
        self._owner = owner
        self._prop = prop
        self._items = []
        # A many-to-many collection's members as the association table holds them.
        self._db = []

    def _raw_add(self, x):
        for i in self._items:
            if i is x:
                return
        self._items.append(x)

    def _raw_remove(self, x):
        for i in range(len(self._items)):
            if self._items[i] is x:
                del self._items[i]
                return True
        return False

    def append(self, x):
        self._raw_add(x)
        self._prop._back_set(self._owner, x, True)

    def extend(self, xs):
        for x in xs:
            self.append(x)

    def insert(self, i, x):
        self._items.insert(i, x)
        self._prop._back_set(self._owner, x, True)

    def remove(self, x):
        if not self._raw_remove(x):
            raise ValueError("list.remove(x): x not in list")
        self._removed(x)

    def _removed(self, x):
        self._prop._back_set(self._owner, x, False)
        if self._prop.delete_orphan:
            sess = self._owner._sa_state.session
            if sess is not None:
                sess._orphans.append((x, self._prop))

    def pop(self, i=-1):
        x = self._items.pop(i)
        self._removed(x)
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
                 foreign_keys=None, cascade=None, backref=None, viewonly=False, secondary=None,
                 passive_deletes=False, **kw):
        if backref is not None:
            raise exc.ArgumentError("relationship(backref=...) is not supported; declare both sides"
                                    " with back_populates")
        self.argument = argument
        self.back_populates = back_populates
        self.uselist = uselist
        self.order_by = order_by
        self.lazy = lazy
        self.secondary = secondary
        self.passive_deletes = passive_deletes
        parts = [x.strip() for x in (cascade if cascade is not None else "save-update, merge").split(",")]
        if "all" in parts:
            parts.extend(["save-update", "merge", "refresh-expire", "expunge", "delete"])
        self.cascade = parts
        self.cascade_delete = "delete" in parts
        self.delete_orphan = "delete-orphan" in parts
        self.key = None
        self.idx = -1
        self.parent = None
        self._target_fn = None
        self._list_ann = None
        self._back = None
        self._ready = False

    def _setup(self):
        if self._ready:
            return
        arg = self._target_fn()
        self.target = arg
        pt = self.parent.__table__
        tt = arg.__table__
        self.direction = None
        sec = self.secondary
        if isinstance(sec, str):
            sec = self.parent.metadata.tables[sec]
        elif isinstance(sec, _Thunk):
            sec = sec.fn()
        self.secondary = sec
        if sec is not None:
            self.sec_local = None
            self.sec_remote = None
            for c in sec.c:
                for fk in c.foreign_keys:
                    if self.sec_local is None and fk.references(pt):
                        self.sec_local = c
                        self.local = pt.c[fk._target()[1]]
                    elif fk.references(tt):
                        self.sec_remote = c
                        self.remote = tt.c[fk._target()[1]]
            if self.sec_local is not None and self.sec_remote is not None:
                self.direction = _MANYTOMANY
        else:
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
        if isinstance(self.order_by, _Thunk):
            self.order_by = self.order_by.fn()
        if self.uselist is None:
            self.uselist = self.direction != _MANYTOONE if self._list_ann is None else self._list_ann
        self._ready = True

    def _other(self):
        self._setup()
        b = self._back
        if b is not None:
            # Its uselist is decided by its own setup: read before it, a
            # collection side looked scalar (a fresh Author's books set to the
            # Book itself by `Book(author=a)`).
            b._setup()
        return b

    def _join_info(self):
        self._setup()
        if self.direction == _MANYTOMANY:
            raise NotImplementedError("joining along a many-to-many relationship is not supported")
        return self.target.__table__, self.local == self.remote

    def _exists(self, criterion):
        sel = _Select(_TextClause("1"))
        tf = self.target.__sa_mapper__.local_from
        if self.direction == _MANYTOMANY:
            sel = sel.select_from(self.secondary, tf).where(self.local == self.sec_local,
                                                           self.sec_remote == self.remote)
        else:
            sel = sel.select_from(tf).where(self.local == self.remote)
        if criterion is not None:
            sel = sel.where(criterion)
        return _Exists(sel)

    def any(self, criterion=None, **kw):
        self._setup()
        if self.direction == _MANYTOONE:
            raise exc.InvalidRequestError("'any()' not implemented for scalar attributes. Use has().")
        return self._exists(criterion)

    def has(self, criterion=None, **kw):
        self._setup()
        if self.direction != _MANYTOONE:
            raise exc.InvalidRequestError("'has()' not implemented for collections. Use any().")
        return self._exists(criterion)

    def __get__(self, obj, owner=None):
        if obj is None:
            return self
        v = obj._sa_v[self.idx]
        if v is not _UNSET:
            return v
        self._setup()
        st = obj._sa_state
        if st.session is not None and st.persistent:
            value = st.session._lazy_load(obj, self)
        elif self.uselist:
            value = InstrumentedList(obj, self)
        else:
            value = None
        obj._sa_v[self.idx] = value
        return value

    def __set__(self, obj, value):
        self._setup()
        if self.uselist:
            lst = InstrumentedList(obj, self)
            if self.direction == _MANYTOMANY:
                old = self.__get__(obj)
                lst._db = list(old._db)
            obj._sa_v[self.idx] = lst
            for x in value:
                lst.append(x)
            return
        old = _peek(obj, self.idx)
        obj._sa_v[self.idx] = value
        other = self._other()
        if other is None:
            return
        if other.uselist:
            if old is not None and old is not value:
                ol = old._sa_v[other.idx]
                if isinstance(ol, InstrumentedList):
                    ol._items = [i for i in ol._items if i is not obj]
            if value is not None:
                other.__get__(value)._raw_add(obj)
        elif value is not None:
            value._sa_v[other.idx] = obj

    def _back_set(self, owner, child, add):
        """``child`` was added to or removed from ``owner``'s collection: update the other side."""
        other = self._other()
        if other is None:
            return
        if self.direction == _MANYTOMANY:
            lst = child._sa_v[other.idx]
            if add:
                if not isinstance(lst, InstrumentedList):
                    lst = other.__get__(child)
                lst._raw_add(owner)
            elif isinstance(lst, InstrumentedList):
                lst._raw_remove(owner)
            return
        child._sa_v[other.idx] = owner if add else None


def relationship(argument=None, **kw):
    return RelationshipProperty(argument, **kw)


def _sa_rel(idx, key, target, is_list, rel):
    """The build's form of a relationship: ``target`` returns the class it names."""
    rel.idx = idx
    rel.key = key
    rel._target_fn = target
    rel._list_ann = is_list
    return rel


def _sa_pair(a, b):
    """Links two relationships that name each other in back_populates."""
    a._back = b
    b._back = a


class _LoaderOption:
    def __init__(self, rel, strategy):
        self.rel = rel
        self.strategy = strategy


def selectinload(attr, *args, **kw):
    return _LoaderOption(attr, "selectin")


def joinedload(attr, *args, **kw):
    return _LoaderOption(attr, "joined")


def subqueryload(attr, *args, **kw):
    return _LoaderOption(attr, "selectin")


def lazyload(attr, *args, **kw):
    return _LoaderOption(attr, "select")


class AliasedClass:
    def __init__(self, cls, name=None):
        m = cls.__sa_mapper__
        if len(m.tables) > 1:
            raise NotImplementedError("aliased() of a joined-inheritance class is not supported")
        self._m = m
        self.alias = _Alias(m.table, name)
        self._sa_aliased = self

    def _slot(self):
        return _Slot(mapper=self._m, cols=[self.alias.corresponding_column(c) for c in self._m.columns],
                     from_=self.alias, key=self.alias.name)

    def __getattr__(self, name):
        # The mapped attributes are data here: a list of (key, column) the
        # mapper holds, compared by value, never an attribute lookup.
        if name.startswith("_"):
            raise AttributeError(name)
        for k, c, i in self._m.attrs:
            if k == name:
                return self.alias.corresponding_column(c)
        raise AttributeError(name)


def aliased(element, alias=None, name=None, **kw):
    return AliasedClass(element, name)


class _State:
    def __init__(self):
        self.session = None
        self.persistent = False
        # Slot number -> the value the database last held.
        self.snapshot = {}


def _state(obj):
    return obj._sa_state


class Mapper:
    def __init__(self, cls, table, attrs, relationships, inherits=None, args=None, inherit_cols=None):
        args = args or {}
        self.class_ = cls
        self.table = table
        # (key, column, slot number) for every mapped column, the inherited ones first.
        self.attrs = attrs
        self.columns = [c for k, c, i in attrs]
        self.relationships = relationships
        self.inherits = inherits
        self.base = inherits.base if inherits is not None else self
        self.identity_class = self.base.class_
        # (subclass table column, parent column) pairs linking joined-inheritance tables.
        self.inherit_cols = (inherits.inherit_cols if inherits is not None else []) + (inherit_cols or [])
        if inherits is not None:
            self.pk = inherits.pk
            self.tables = list(inherits.tables)
        else:
            self.pk = [(k, c, i) for k, c, i in attrs if c.primary_key]
            self.tables = []
        if not any(t is table for t in self.tables):
            self.tables.append(table)
        if inherits is not None and table is not inherits.table:
            cond = None
            for sub, par in (inherit_cols or []):
                cond = sub == par
            self.local_from = _Join(inherits.local_from, table, cond, False)
        else:
            self.local_from = inherits.local_from if inherits is not None else table
        if inherits is None:
            self.poly_map = {}
            self.polymorphic_on = args.get("polymorphic_on")
        else:
            self.polymorphic_on = inherits.polymorphic_on
        self.polymorphic_identity = args.get("polymorphic_identity")
        if self.polymorphic_identity is not None:
            self.base.poly_map[self.polymorphic_identity] = self

    def poly_filter(self, cols):
        if self.inherits is None or self.polymorphic_on is None or self.table is not self.inherits.table:
            return None
        ids = [m.polymorphic_identity for m in self.base.poly_map.values() if issubclass(m.class_, self.class_)]
        return self.polymorphic_on.in_(ids)

    def _identity(self, obj):
        return tuple(_peek(obj, i) for k, c, i in self.pk)

    def _load_row(self, session, values, cols=None):
        if cols is None:
            cols = self.columns
        origs = [getattr(c, "_proxied", c) for c in cols]
        ident = []
        for k, pc, pi in self.pk:
            for i in range(len(origs)):
                if origs[i] is pc:
                    ident.append(values[i])
                    break
        m = self
        if self.polymorphic_on is not None:
            for i in range(len(origs)):
                if origs[i] is self.polymorphic_on:
                    sub = self.base.poly_map.get(values[i])
                    if sub is not None:
                        m = sub
                    break
        key = (self.identity_class, tuple(ident))
        obj = session._identity.get(key) if session is not None else None
        fresh = obj is None
        if fresh:
            obj = m.class_.__new__(m.class_)
            obj._sa_alloc()
        st = obj._sa_state
        sv = obj._sa_v
        for i in range(len(origs)):
            j = _attr_idx(m, origs[i])
            if fresh or sv[j] is _UNSET:
                v = origs[i].type.result(values[i])
                sv[j] = v
                st.snapshot[j] = v
        if fresh and session is not None:
            st.session = session
            st.persistent = True
            session._identity[key] = obj
        return obj


def _map_class(cls):
    """What the metaclass did, from the literal tables the build wrote into ``cls``."""
    pcls = cls.__cn1_sa_parent__
    parent = pcls.__sa_mapper__ if pcls is not None else None
    tmap = cls.type_annotation_map
    attrs = []
    for a in cls.__cn1_sa_cols__:
        attrs.append((a.key, a._build(cls, tmap), a.idx))
    rels = {}
    for r in cls.__cn1_sa_rels__:
        r.parent = cls
        rels[r.key] = r
    margs = cls.__cn1_sa_args__
    args = dict(margs) if margs is not None else {}
    tablename = cls.__cn1_sa_table__
    inherit_cols = []
    if parent is None:
        table = Table(tablename, cls.metadata, *[c for k, c, i in attrs])
        all_attrs = attrs
        all_rels = rels
    else:
        all_rels = dict(parent.relationships)
        all_rels.update(rels)
        if tablename is None:
            # Single-table inheritance: the subclass's columns join the parent's table.
            table = parent.table
            for k, c, i in attrs:
                c.table = table
                table.c._add(c)
            all_attrs = parent.attrs + attrs
        else:
            table = Table(tablename, cls.metadata, *[c for k, c, i in attrs])
            pkeys = [k for k, c, i in parent.attrs]
            for k, c, i in attrs:
                for pk_, pc, pi in parent.attrs:
                    if k == pk_:
                        inherit_cols.append((c, pc))
            all_attrs = parent.attrs + [(k, c, i) for k, c, i in attrs if k not in pkeys]
    on = args.get("polymorphic_on")
    if on is not None:
        if isinstance(on, str):
            on = [c for k, c, i in all_attrs if k == on][0]
        elif isinstance(on, _ColumnAttr):
            on = on.column
        elif isinstance(on, _MappedColumnSpec):
            on = [a.column for a in cls.__cn1_sa_cols__ if a.spec is on][0]
        args["polymorphic_on"] = on
    cls.__table__ = table
    cls.__sa_mapper__ = Mapper(cls, table, all_attrs, all_rels, parent, args, inherit_cols)


class DeclarativeBase:
    metadata = None
    type_annotation_map = None
    __table__ = None
    __sa_mapper__ = None
    # The tables SqlAlchemyLowering writes into every mapped class.
    __cn1_sa_cols__ = ()
    __cn1_sa_rels__ = ()
    __cn1_sa_parent__ = None
    __cn1_sa_table__ = None
    __cn1_sa_args__ = None
    __cn1_sa_nslots__ = 0

    def __init__(self, **kw):
        self._sa_new()
        self.__cn1_sa_kw__(kw)
        for k in kw:
            raise TypeError("%r is an invalid keyword argument for %s" % (k, type(self).__name__))

    def _sa_new(self):
        # The start of every constructor: the keyword one above, and the
        # keyword-only __init__ the build writes into each model.
        if type(self).__sa_mapper__ is None:
            raise exc.InvalidRequestError("class %s was not mapped at build time: a model must be defined at"
                                          " module level, deriving from a DeclarativeBase subclass of the same"
                                          " module" % type(self).__name__)
        self._sa_alloc()

    def _sa_alloc(self):
        self._sa_v = [_UNSET] * self.__cn1_sa_nslots__
        self._sa_state = _State()

    def __cn1_sa_kw__(self, kw):
        pass

    @classmethod
    def __cn1_sa_map__(cls):
        _map_class(cls)

    def __repr__(self):
        m = type(self).__sa_mapper__
        return "<%s %s>" % (type(self).__name__, m._identity(self))


DeclarativeBaseNoMeta = DeclarativeBase


class MappedAsDataclass:
    pass


def declarative_base(metadata=None):
    raise exc.InvalidRequestError("declarative_base() is compiled by the build: assign it to a name at"
                                  " module level (Base = declarative_base())")


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
        self._orphans = []

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
            obj._sa_state.session = None
        self._identity = {}
        self._new = []
        self._deleted = []

    def expunge(self, obj):
        st = obj._sa_state
        st.session = None
        self._new = [o for o in self._new if o is not obj]
        for k in list(self._identity):
            if self._identity[k] is obj:
                del self._identity[k]

    # -- unit of work
    def add(self, obj, _warn=True):
        st = obj._sa_state
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
            v = _peek(obj, m.relationships[key].idx)
            if v is None:
                continue
            if isinstance(v, InstrumentedList):
                for x in v._items:
                    self._cascade_one(x)
            else:
                self._cascade_one(v)

    def _cascade_one(self, x):
        st = x._sa_state
        if st.persistent and st.session is self:
            return
        self.add(x)

    def delete(self, obj):
        st = obj._sa_state
        if not st.persistent:
            self._new = [o for o in self._new if o is not obj]
            return
        if any(o is obj for o in self._deleted):
            return
        self._deleted.append(obj)
        m = type(obj).__sa_mapper__
        for key in m.relationships:
            rel = m.relationships[key]
            rel._setup()
            if rel.direction != _ONETOMANY:
                continue
            if obj._sa_v[rel.idx] is _UNSET and rel.passive_deletes:
                # The database's ON DELETE takes care of rows that were never loaded.
                continue
            v = rel.__get__(obj)
            if isinstance(v, InstrumentedList):
                children = list(v._items)
            else:
                children = [] if v is None else [v]
            other = rel._other()
            rk = _attr_idx(rel.target.__sa_mapper__, rel.remote)
            for c in children:
                if rel.cascade_delete:
                    self.delete(c)
                else:
                    c._sa_v[rk] = None
                    if other is not None:
                        c._sa_v[other.idx] = None

    def __contains__(self, obj):
        st = obj._sa_state
        return st.session is self

    def _sync(self, obj):
        m = type(obj).__sa_mapper__
        for key in m.relationships:
            rel = m.relationships[key]
            rel._setup()
            v = _peek(obj, rel.idx)
            if v is None:
                continue
            if rel.direction == _MANYTOONE and not isinstance(v, InstrumentedList):
                obj._sa_v[_attr_idx(m, rel.local)] = _peek(v, _attr_idx(rel.target.__sa_mapper__, rel.remote))
            elif rel.direction == _ONETOMANY and isinstance(v, InstrumentedList):
                pk = _peek(obj, _attr_idx(m, rel.local))
                tm = rel.target.__sa_mapper__
                for child in v._items:
                    child._sa_v[_attr_idx(tm, rel.remote)] = pk

    def _is_deleted(self, obj):
        return any(o is obj for o in self._deleted)

    def flush(self, objects=None):
        orphans = self._orphans
        self._orphans = []
        for x, prop in orphans:
            other = prop._other()
            if other is not None and _peek(x, other.idx) is not None:
                continue
            self.delete(x)
        for obj in list(self._identity.values()):
            if not self._is_deleted(obj):
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
        new = sorted(self._new, key=lambda o: order.get(type(o).__sa_mapper__.tables[0].name, 0))
        self._new = []
        for obj in new:
            self._sync(obj)
            self._insert(conn, obj)
            self._sync(obj)
        live = []
        for obj in list(self._identity.values()):
            if self._is_deleted(obj):
                continue
            live.append(obj)
            self._sync(obj)
            self._update(conn, obj)
        done = []
        for obj in live:
            self._sync_m2m(conn, obj, done)
        dels = self._deleted
        self._deleted = []
        if dels:
            tables = type(dels[0]).metadata.sorted_tables
            for i in range(len(tables)):
                order[tables[i].name] = i
            dels = sorted(dels, key=lambda o: -order.get(type(o).__sa_mapper__.tables[0].name, 0))
        for obj in dels:
            m = type(obj).__sa_mapper__
            for key in m.relationships:
                rel = m.relationships[key]
                rel._setup()
                if rel.direction == _MANYTOMANY:
                    conn.execute(rel.secondary.delete().where(
                        rel.sec_local == _peek(obj, _attr_idx(m, rel.local))))
            for t in reversed(m.tables):
                conn.execute(t.delete().where(*self._pk_where(m, t, obj)))
            key = (m.identity_class, m._identity(obj))
            if key in self._identity:
                del self._identity[key]
            obj._sa_state.persistent = False
            self._tx_deleted.append(obj)

    def _sync_m2m(self, conn, obj, done):
        m = type(obj).__sa_mapper__
        for key in m.relationships:
            rel = m.relationships[key]
            rel._setup()
            if rel.direction != _MANYTOMANY:
                continue
            lst = obj._sa_v[rel.idx]
            if not isinstance(lst, InstrumentedList):
                continue
            mine = _peek(obj, _attr_idx(m, rel.local))
            for x in lst._items:
                if not any(x is y for y in lst._db):
                    self._assoc(conn, rel, mine, x, True, done)
            for y in lst._db:
                if not any(x is y for x in lst._items):
                    self._assoc(conn, rel, mine, y, False, done)
            lst._db = list(lst._items)

    def _assoc(self, conn, rel, mine, other, add, done):
        theirs = _peek(other, _attr_idx(rel.target.__sa_mapper__, rel.remote))
        pair = sorted([(rel.sec_local.name, mine), (rel.sec_remote.name, theirs)])
        sig = (rel.secondary.name, add, pair[0], pair[1])
        if sig in done:
            return
        done.append(sig)
        if add:
            conn.execute(rel.secondary.insert().values({rel.sec_local.key: mine, rel.sec_remote.key: theirs}))
        else:
            conn.execute(rel.secondary.delete().where(rel.sec_local == mine, rel.sec_remote == theirs))

    def _pk_where(self, m, t, obj):
        st = obj._sa_state
        if t is m.tables[0]:
            return [c == st.snapshot.get(i, _peek(obj, i)) for k, c, i in m.pk]
        out = []
        for sub, par in m.inherit_cols:
            if sub.table is t:
                i = _attr_idx(m, par)
                out.append(sub == st.snapshot.get(i, _peek(obj, i)))
        return out

    def _insert(self, conn, obj):
        m = type(obj).__sa_mapper__
        st = obj._sa_state
        sv = obj._sa_v
        if m.polymorphic_on is not None and m.polymorphic_identity is not None:
            dk = _attr_idx(m, m.polymorphic_on)
            if _peek(obj, dk) is None:
                sv[dk] = m.polymorphic_identity
        for t in m.tables:
            values = {}
            for k, c, i in m.attrs:
                if c.table is not t:
                    continue
                present = sv[i] is not _UNSET
                v = sv[i] if present else None
                if v is None and c.default is not None:
                    v = c._default_value()
                    sv[i] = v
                    present = True
                if present:
                    values[c.key] = v
            for sub, par in m.inherit_cols:
                if sub.table is t:
                    values[sub.key] = _peek(obj, _attr_idx(m, par))
            res = conn.execute(t.insert().values(values))
            if t is m.tables[0]:
                for k, c, i in m.pk:
                    if _peek(obj, i) is None:
                        sv[i] = res.lastrowid
        for k, c, i in m.attrs:
            if sv[i] is _UNSET:
                sv[i] = None
            st.snapshot[i] = sv[i]
        st.persistent = True
        st.session = self
        self._identity[(m.identity_class, m._identity(obj))] = obj
        self._tx_inserted.append(obj)

    def _update(self, conn, obj):
        m = type(obj).__sa_mapper__
        st = obj._sa_state
        sv = obj._sa_v
        changed = []
        for k, c, i in m.attrs:
            if sv[i] is not _UNSET and (i not in st.snapshot or st.snapshot[i] != sv[i]):
                changed.append((c, i))
        if not changed:
            return
        for t in m.tables:
            ch = {}
            for c, i in changed:
                if c.table is t:
                    ch[c.key] = sv[i]
            if ch:
                conn.execute(t.update().where(*self._pk_where(m, t, obj)).values(ch))
        for c, i in changed:
            st.snapshot[i] = sv[i]

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
            key = (m.identity_class, m._identity(obj))
            if self._identity.get(key) is obj:
                del self._identity[key]
            st = obj._sa_state
            st.persistent = False
            st.session = None
            for k, c, i in m.pk:
                if c.autoincrement and isinstance(c.type, Integer):
                    obj._sa_v[i] = None
        for obj in self._tx_deleted:
            st = obj._sa_state
            st.persistent = True
            m = type(obj).__sa_mapper__
            self._identity[(m.identity_class, m._identity(obj))] = obj
        for obj in self._new:
            obj._sa_state.session = None
        self._new = []
        self._deleted = []
        self._orphans = []
        self._tx_inserted = []
        self._tx_deleted = []
        self.expire_all()

    def expire_all(self):
        for obj in list(self._identity.values()):
            self.expire(obj)

    def expire(self, obj, attribute_names=None):
        m = type(obj).__sa_mapper__
        sv = obj._sa_v
        pk = [k for k, c, i in m.pk]
        for k, c, i in m.attrs:
            if k not in pk and (attribute_names is None or k in attribute_names):
                sv[i] = _UNSET
        for k in m.relationships:
            if attribute_names is None or k in attribute_names:
                sv[m.relationships[k].idx] = _UNSET

    def refresh(self, obj, attribute_names=None):
        self.expire(obj, attribute_names)
        self._load_expired(obj)

    def _load_expired(self, obj):
        m = type(obj).__sa_mapper__
        st = obj._sa_state
        where = [c == st.snapshot.get(i, _peek(obj, i)) for k, c, i in m.pk]
        stmt = select(*m.columns).select_from(m.local_from).where(*where)
        row = stmt._execute_on(self.connection(), None).first()
        if row is None:
            raise exc.InvalidRequestError("Instance has been deleted, or its row is otherwise not present.")
        sv = obj._sa_v
        for j in range(len(m.attrs)):
            i = m.attrs[j][2]
            sv[i] = row[j]
            st.snapshot[i] = row[j]

    def _lazy_load(self, obj, rel):
        m = type(obj).__sa_mapper__
        tm = rel.target.__sa_mapper__
        if rel.direction == _MANYTOMANY:
            lst = InstrumentedList(obj, rel)
            pk = _get(obj, _attr_idx(m, rel.local))
            stmt = select(rel.target).select_from(tm.local_from, rel.secondary).where(
                rel.sec_local == pk, rel.remote == rel.sec_remote)
            if rel.order_by is not None:
                stmt = stmt.order_by(rel.order_by)
            for x in stmt._execute_on(self.connection(), None, self).scalars():
                lst._items.append(x)
            lst._db = list(lst._items)
            return lst
        if rel.direction == _ONETOMANY:
            lst = InstrumentedList(obj, rel)
            pk = _get(obj, _attr_idx(m, rel.local))
            stmt = select(rel.target).where(rel.remote == pk)
            if rel.order_by is not None:
                stmt = stmt.order_by(rel.order_by)
            for x in stmt._execute_on(self.connection(), None, self).scalars():
                lst._items.append(x)
            if rel.uselist:
                return lst
            return lst._items[0] if lst._items else None
        fk = _get(obj, _attr_idx(m, rel.local))
        if fk is None:
            return None
        key = (tm.identity_class, (fk,))
        if key in self._identity:
            return self._identity[key]
        stmt = select(rel.target).where(rel.remote == fk)
        return stmt._execute_on(self.connection(), None, self).scalars().first()

    # -- eager loading
    def _eager_rels(self, stmt):
        out = []
        if not isinstance(stmt, _Select):
            return out
        for i in range(len(stmt._slots)):
            sl = stmt._slots[i]
            if sl.mapper is None:
                continue
            for key in sl.mapper.relationships:
                rel = sl.mapper.relationships[key]
                strategy = rel.lazy
                for o in stmt._options:
                    if o.rel is rel:
                        strategy = o.strategy
                if strategy in ("selectin", "joined", "subquery", "immediate"):
                    out.append((i, rel))
        return out

    def _run(self, statement, params):
        conn = self.connection()
        # Compile for the bind's dialect, as Connection.execute does: without it a
        # Session statement compiled as SQLite (CAST AS VARCHAR, || on MySQL).
        _sa._active.append(conn.engine.dialect)
        try:
            res = statement._execute_on(conn, params, self)
        finally:
            _sa._active.pop()
        eager = self._eager_rels(statement)
        if not eager:
            return res
        rows = res.all()
        for i, rel in eager:
            self._eager_load([r[i] for r in rows], rel)
        out = _CursorResult(rows, res._keys, res.rowcount, res.lastrowid)
        out._yield_per = res._yield_per
        return out

    def _eager_load(self, objs, rel):
        """Loads ``rel`` for every object at once, with one ``IN`` query."""
        rel._setup()
        todo = []
        for o in objs:
            if o is not None and o._sa_v[rel.idx] is _UNSET and not any(o is x for x in todo):
                todo.append(o)
        if not todo:
            return
        tm = rel.target.__sa_mapper__
        if rel.direction == _MANYTOMANY:
            for o in todo:
                o._sa_v[rel.idx] = self._lazy_load(o, rel)
            return
        rk = _attr_idx(tm, rel.remote)
        keys = [_peek(o, _attr_idx(type(o).__sa_mapper__, rel.local)) for o in todo]
        wanted = []
        for k in keys:
            if k is not None and k not in wanted:
                wanted.append(k)
        groups = {}
        if wanted:
            stmt = select(rel.target).where(rel.remote.in_(wanted))
            if rel.direction == _ONETOMANY and rel.order_by is not None:
                stmt = stmt.order_by(rel.order_by)
            for x in stmt._execute_on(self.connection(), None, self).scalars():
                k = _peek(x, rk)
                if k not in groups:
                    groups[k] = []
                groups[k].append(x)
        for i in range(len(todo)):
            o = todo[i]
            found = groups.get(keys[i], [])
            if rel.direction == _ONETOMANY:
                lst = InstrumentedList(o, rel)
                lst._items = list(found)
                o._sa_v[rel.idx] = lst if rel.uselist else (found[0] if found else None)
            else:
                o._sa_v[rel.idx] = found[0] if found else None

    # -- queries
    def _autoflush(self):
        if self.autoflush:
            self.flush()

    def execute(self, statement, params=None, **kw):
        self._autoflush()
        return self._run(statement, params)

    def scalars(self, statement, params=None, **kw):
        return self.execute(statement, params).scalars()

    def scalar(self, statement, params=None, **kw):
        return self.execute(statement, params).scalar()

    def get(self, entity, ident, **kw):
        m = entity.__sa_mapper__
        if isinstance(ident, dict):
            ident = tuple(ident[k] for k, c, i in m.pk)
        if not isinstance(ident, tuple):
            ident = (ident,)
        obj = self._identity.get((m.identity_class, ident))
        if obj is not None and isinstance(obj, entity):
            return obj
        where = [m.pk[i][1] == ident[i] for i in range(len(m.pk))]
        return self._run(select(entity).where(*where), None).scalars().first()

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
            st = obj._sa_state
            m = type(obj).__sa_mapper__
            for k, c, i in m.attrs:
                if obj._sa_v[i] is not _UNSET and i in st.snapshot and st.snapshot[i] != obj._sa_v[i]:
                    out.append(obj)
                    break
        return out


def _attr_idx(mapper, col):
    """The slot of ``col`` in ``mapper``'s instances; a joined-inheritance subclass's own
    copy of an inherited key (its primary key) shares the inherited slot."""
    for k, c, i in mapper.attrs:
        if c is col:
            return i
    for k, c, i in mapper.attrs:
        if k == col.key:
            return i
    raise exc.InvalidRequestError("Column %s is not mapped by %s" % (col.key, mapper.class_.__name__))


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
    raise exc.InvalidRequestError("registry() is not supported; derive the base from DeclarativeBase")

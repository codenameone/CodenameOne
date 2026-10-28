import sys
import logging
from typing import List, Optional
from sqlalchemy import create_engine, ForeignKey, String, Integer, select, func, exists, union, \
    union_all, case, cast, and_, or_, literal
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, relationship, Session, aliased


class Base(DeclarativeBase):
    pass


class User(Base):
    __tablename__ = "users"
    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(30))
    age: Mapped[int]
    boss_id: Mapped[Optional[int]] = mapped_column(ForeignKey("users.id"))
    addresses: Mapped[List["Address"]] = relationship(back_populates="user")


class Address(Base):
    __tablename__ = "addresses"
    id: Mapped[int] = mapped_column(primary_key=True)
    email: Mapped[str]
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"))
    user: Mapped["User"] = relationship(back_populates="addresses")


class Capture(logging.Handler):
    def __init__(self):
        logging.Handler.__init__(self)
        self.records = []

    def emit(self, record):
        self.records.append(record)


cap = Capture()
log = logging.getLogger("sqlalchemy.engine.Engine")
log.addHandler(cap)
log.propagate = False

engine = create_engine(sys.argv[1] if len(sys.argv) > 1 else "sqlite://", echo=True)
engine.echo = False
Base.metadata.drop_all(engine)  # a server database keeps tables between runs
# Every query is ordered by id: a server collation sorts "bob" and "Cid" differently from SQLite.
engine.echo = True
Base.metadata.create_all(engine)
s = Session(engine)
ann = User(name="Ann", age=30, addresses=[Address(email="ann@a.com"), Address(email="ann@b.org")])
s.add(ann)
s.flush()
s.add_all([User(name="bob", age=25, boss_id=ann.id, addresses=[Address(email="bob@a.com")]),
           User(name="Cid", age=41, boss_id=ann.id), User(name="dee", age=19)])
s.commit()
print(len(cap.records) > 0, cap.records[0].name, any("SELECT" in r.getMessage() for r in cap.records))
log.removeHandler(cap)
engine.echo = False
n = len(cap.records)

# scalar subquery (correlated)
cnt = select(func.count(Address.id)).where(Address.user_id == User.id).scalar_subquery()
print(s.execute(select(User.name, cnt.label("n")).order_by(User.id)).all())

# subquery in FROM
sub = select(Address.user_id, func.count(Address.id).label("c")).group_by(Address.user_id).subquery()
print(s.execute(select(User.name, sub.c.c).join(sub, User.id == sub.c.user_id).order_by(User.id)).all())

# exists
print(s.scalars(select(User.name).where(exists().where(Address.user_id == User.id)).order_by(User.id)).all())
print(s.scalars(select(User.name).where(~select(Address.id).where(Address.user_id == User.id).exists())
                .order_by(User.id)).all())

# in_ with subquery, between, ilike
print(s.scalars(select(User.name).where(User.id.in_(select(Address.user_id).where(Address.email.like("%a.com"))))
                .order_by(User.id)).all())
print(s.scalars(select(User.name).where(User.age.between(20, 35)).order_by(User.id)).all())
print(s.scalars(select(User.name).where(User.name.ilike("c%")).order_by(User.id)).all())

# cte
old = select(User.id, User.name).where(User.age > 24).cte("old")
print(s.execute(select(old.c.name).order_by(old.c.id)).scalars().all())

# union / union_all
u = union(select(User.name).where(User.age < 20), select(User.name).where(User.age > 40))
print(sorted(s.execute(u).scalars().all()))
ua = union_all(select(User.age).where(User.age < 26), select(User.age).where(User.age < 26))
print(sorted(s.execute(ua).scalars().all()))
print(sorted(s.execute(select(User.name).where(User.age < 20).union(select(User.name).where(User.age == 30))).scalars()))

# aliased self join
boss = aliased(User)
print(s.execute(select(User.name, boss.name).join(boss, User.boss_id == boss.id).order_by(User.id)).all())
print(s.scalars(select(boss).where(boss.age > 40)).one().name)

# case, cast, functions
grp = case((User.age < 20, "teen"), (User.age < 35, "adult"), else_="senior")
print(s.execute(select(User.name, grp).order_by(User.id)).all())
print(s.execute(select(cast(User.age, String) + "y").order_by(User.id)).scalars().all())
print(s.execute(select(cast("12", Integer) + 1)).scalar())
print(s.execute(select(func.lower(User.name), func.upper(User.name), func.length(User.name))
                .order_by(User.id)).all())
print(s.execute(select(func.coalesce(User.boss_id, -1)).order_by(User.id)).scalars().all())

# relationship any / has
print(s.scalars(select(User.name).where(User.addresses.any(Address.email.like("%.org"))).order_by(User.id)).all())
print(s.scalars(select(User.name).where(User.addresses.any()).order_by(User.id)).all())
print(s.scalars(select(Address.email).where(Address.user.has(User.age > 26)).order_by(Address.id)).all())

# streaming
res = s.execute(select(User.name).order_by(User.id).execution_options(yield_per=2))
print([[r[0] for r in p] for p in res.partitions()])
res = s.execute(select(User.age).order_by(User.id))
print([list(p) for p in res.scalars().partitions(3)])
res = s.scalars(select(User).order_by(User.id).execution_options(yield_per=1))
print(next(iter(res)).name, [u.name for u in res])
print(len(cap.records) == n)
s.close()

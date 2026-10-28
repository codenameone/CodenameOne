from typing import List, Optional
from sqlalchemy import create_engine, ForeignKey, String, select, func
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, relationship, Session


class Base(DeclarativeBase):
    pass


class User(Base):
    __tablename__ = "user_account"
    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(30))
    fullname: Mapped[Optional[str]]
    age: Mapped[int] = mapped_column(default=18)
    addresses: Mapped[List["Address"]] = relationship(back_populates="user")


class Address(Base):
    __tablename__ = "address"
    id: Mapped[int] = mapped_column(primary_key=True)
    email: Mapped[str] = mapped_column(nullable=False)
    user_id: Mapped[int] = mapped_column(ForeignKey("user_account.id"))
    user: Mapped["User"] = relationship(back_populates="addresses")


engine = create_engine("sqlite:///:memory:")
Base.metadata.create_all(engine)
print(sorted(Base.metadata.tables), User.__table__.name, [c.name for c in Address.__table__.columns])

with Session(engine) as session:
    spongebob = User(name="spongebob", fullname="Spongebob Squarepants",
                     addresses=[Address(email="spongebob@sqlalchemy.org")])
    sandy = User(name="sandy", fullname="Sandy Cheeks")
    sandy.addresses.append(Address(email="sandy@sqlalchemy.org"))
    sandy.addresses.append(Address(email="sandy@squirrelpower.org"))
    patrick = User(name="patrick", age=40)
    print(spongebob.id, sandy.addresses[0].user is sandy)
    session.add_all([spongebob, sandy])
    session.add(patrick)
    session.flush()
    print(spongebob.id, sandy.id, patrick.id, patrick.age, patrick.fullname)
    print(sorted(a.id for a in sandy.addresses), sandy.addresses[1].user_id)
    session.commit()

with Session(engine) as session:
    stmt = select(User).where(User.name.in_(["spongebob", "sandy"])).order_by(User.id)
    for user in session.scalars(stmt):
        print(user.id, user.name, user.fullname, user.age)
    u = session.execute(select(User).where(User.name == "sandy")).scalar_one()
    print(u is session.get(User, 2), [a.email for a in u.addresses])
    a = session.scalars(select(Address).where(Address.email.like("%squirrel%"))).one()
    print(a.user.name, a.user is u)
    print(session.get(User, 99))
    print(session.scalar(select(func.count()).select_from(User)))
    rows = session.execute(select(User.name, Address.email).join(User.addresses).order_by(Address.id)).all()
    print([tuple(r) for r in rows])
    rows = session.execute(select(User.name).join(Address, User.id == Address.user_id)
                           .where(Address.email.like("sandy%")).distinct()).scalars().all()
    print(rows)
    print(session.scalars(select(User.name).order_by(User.age.desc(), User.name).limit(2)).all())

    patrick = session.get(User, 3)
    patrick.fullname = "Patrick Star"
    patrick.addresses.append(Address(email="pat@star.org"))
    session.commit()
    print(patrick.fullname, [x.email for x in patrick.addresses])

    sandy = session.get(User, 2)
    session.delete(sandy.addresses[0])
    session.flush()
    session.refresh(sandy)
    print([x.email for x in sandy.addresses])
    session.rollback()
    print(sorted(x.email for x in session.get(User, 2).addresses))

    tmp = User(name="tmp")
    session.add(tmp)
    session.flush()
    print(tmp.id is not None, session.scalar(select(func.count(User.id))))
    session.rollback()
    print(session.scalar(select(func.count(User.id))))

    session.delete(session.scalars(select(Address).where(Address.user_id == 1)).one())
    session.flush()
    session.delete(session.get(User, 1))
    session.commit()
    print(session.scalars(select(User.name).order_by(User.id)).all())
    print(session.scalars(select(Address.email).order_by(Address.id)).all())

with Session(engine) as s2:
    u = s2.scalars(select(User).where(User.age > 20)).first()
    print(u.name, u.age, len(u.addresses), u.addresses[0].user.name)
    print(s2.scalars(select(User).where(User.fullname.is_(None))).all())

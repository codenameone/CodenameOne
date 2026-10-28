import sys
from typing import List, Optional
from sqlalchemy import create_engine, ForeignKey, String, Table, Column, Integer, select, func
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, relationship, Session, \
    selectinload, joinedload


class Base(DeclarativeBase):
    type_annotation_map = {str: String(200)}
    pass


post_tags = Table("post_tags", Base.metadata,
                  Column("post_id", ForeignKey("post.id"), primary_key=True),
                  Column("tag_id", ForeignKey("tag.id"), primary_key=True))


class Post(Base):
    __tablename__ = "post"
    id: Mapped[int] = mapped_column(primary_key=True)
    title: Mapped[str] = mapped_column(String(50))
    tags: Mapped[List["Tag"]] = relationship(secondary=post_tags, back_populates="posts")


class Tag(Base):
    __tablename__ = "tag"
    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(20))
    posts: Mapped[List["Post"]] = relationship(secondary=post_tags, back_populates="tags")


class Author(Base):
    __tablename__ = "author"
    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str]
    books: Mapped[List["Book"]] = relationship(back_populates="author", cascade="all, delete-orphan",
                                               order_by="Book.id")
    notes: Mapped[List["Note"]] = relationship(back_populates="author")
    memos: Mapped[List["Memo"]] = relationship(passive_deletes=True, lazy="selectin")


class Book(Base):
    __tablename__ = "book"
    id: Mapped[int] = mapped_column(primary_key=True)
    title: Mapped[str]
    author_id: Mapped[int] = mapped_column(ForeignKey("author.id"))
    author: Mapped["Author"] = relationship(back_populates="books")


class Note(Base):
    __tablename__ = "note"
    id: Mapped[int] = mapped_column(primary_key=True)
    text: Mapped[str]
    author_id: Mapped[Optional[int]] = mapped_column(ForeignKey("author.id"))
    author: Mapped[Optional["Author"]] = relationship(back_populates="notes", lazy="joined")


class Memo(Base):
    __tablename__ = "memo"
    id: Mapped[int] = mapped_column(primary_key=True)
    author_id: Mapped[Optional[int]] = mapped_column(ForeignKey("author.id", ondelete="CASCADE"))


class Enrollment(Base):
    __tablename__ = "enrollment"
    student: Mapped[str] = mapped_column(String(20), primary_key=True)
    course: Mapped[str] = mapped_column(String(20), primary_key=True)
    grade: Mapped[int]


class Employee(Base):
    __tablename__ = "employee"
    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str]
    type: Mapped[str]
    __mapper_args__ = {"polymorphic_on": "type", "polymorphic_identity": "employee"}


class Manager(Employee):
    manager_data: Mapped[Optional[str]]
    __mapper_args__ = {"polymorphic_identity": "manager"}


class Engineer(Employee):
    language: Mapped[Optional[str]]
    __mapper_args__ = {"polymorphic_identity": "engineer"}


class Vehicle(Base):
    __tablename__ = "vehicle"
    id: Mapped[int] = mapped_column(primary_key=True)
    kind: Mapped[str]
    wheels: Mapped[int]
    __mapper_args__ = {"polymorphic_on": "kind", "polymorphic_identity": "vehicle"}


class Truck(Vehicle):
    __tablename__ = "truck"
    id: Mapped[int] = mapped_column(ForeignKey("vehicle.id"), primary_key=True)
    payload: Mapped[int]
    __mapper_args__ = {"polymorphic_identity": "truck"}


engine = create_engine(sys.argv[1] if len(sys.argv) > 1 else "sqlite://")
Base.metadata.drop_all(engine)  # a server database keeps tables between runs
Base.metadata.create_all(engine)

# ---- many-to-many
with Session(engine) as s:
    py, db, web = Tag(name="py"), Tag(name="db"), Tag(name="web")
    p1 = Post(title="one", tags=[py, db])
    p2 = Post(title="two", tags=[py])
    s.add_all([p1, p2, web])
    s.commit()
    # A lazy load has no ORDER BY, so the order of py.posts is the database's to pick.
    print(sorted(t.name for t in p1.tags), sorted(p.title for p in py.posts))
    p2.tags.append(web)
    p1.tags.remove(db)
    s.commit()
    print(s.scalar(select(func.count()).select_from(post_tags)))
    print(sorted((p.title, sorted(t.name for t in p.tags)) for p in s.scalars(select(Post))))
    print(sorted(t.name for t in web.posts[0].tags), len(db.posts))
    s.delete(p2)
    s.commit()
    print(s.scalar(select(func.count()).select_from(post_tags)))

# ---- cascades
with Session(engine) as s:
    a = Author(name="ann", books=[Book(title="b1"), Book(title="b2"), Book(title="b3")],
               notes=[Note(text="n1"), Note(text="n2")])
    s.add(a)
    s.commit()
    print(s.scalar(select(func.count(Book.id))), s.scalar(select(func.count(Note.id))))
    a.books.remove(a.books[0])
    s.commit()
    print([b.title for b in s.scalars(select(Book).order_by(Book.id))])
    s.delete(a)
    s.commit()
    print(s.scalar(select(func.count(Book.id))), [(n.text, n.author_id) for n in s.scalars(select(Note).order_by(Note.id))])

with Session(engine) as s:
    a = Author(name="bob")
    s.add(a)
    s.flush()
    s.add_all([Memo(author_id=a.id), Memo(author_id=a.id)])
    s.commit()
    aid = a.id
with Session(engine) as s:
    a = s.get(Author, aid)
    print(len(a.memos))
    s.expire(a, ["memos"])
    s.delete(a)
    s.commit()
    # passive_deletes: the ORM leaves the memos to the database's ON DELETE CASCADE and never
    # nulls them. SQLite enforces no foreign keys by default and keeps them; a server deletes
    # them. Either way none is left pointing at NULL or at another author.
    left = s.scalars(select(Memo.author_id)).all()
    print(len(left) in (0, 2), all(x == aid for x in left))

# ---- eager loading
with Session(engine) as s:
    a = Author(name="cy", books=[Book(title="x"), Book(title="y")], notes=[Note(text="z")])
    s.add(a)
    s.commit()
with Session(engine) as s:
    authors = s.scalars(select(Author).where(Author.name == "cy").options(selectinload(Author.books))).all()
    s.expunge_all()
    print([b.title for b in authors[0].books])
with Session(engine) as s:
    authors = s.scalars(select(Author).where(Author.name == "cy").options(joinedload(Author.notes))).unique().all()
    s.expunge_all()
    print([n.text for n in authors[0].notes])
with Session(engine) as s:
    notes = s.scalars(select(Note).where(Note.text == "z")).all()
    s.expunge_all()
    print(notes[0].author.name)

# ---- composite primary keys
with Session(engine) as s:
    s.add_all([Enrollment(student="al", course="math", grade=90),
               Enrollment(student="al", course="art", grade=70),
               Enrollment(student="bo", course="math", grade=80)])
    s.commit()
    e = s.get(Enrollment, ("al", "art"))
    print(e.grade, s.get(Enrollment, {"course": "math", "student": "bo"}).grade, s.get(Enrollment, ("zz", "art")))
    e.grade = 75
    s.commit()
    print(s.scalars(select(Enrollment.grade).order_by(Enrollment.grade)).all())

# ---- single-table inheritance
with Session(engine) as s:
    s.add_all([Manager(name="m1", manager_data="md"), Engineer(name="e1", language="py"),
               Engineer(name="e2", language="java"), Employee(name="x")])
    s.commit()
    print([(type(e).__name__, e.name, e.type) for e in s.scalars(select(Employee).order_by(Employee.id))])
    print([e.language for e in s.scalars(select(Engineer).order_by(Engineer.id))])
    print(s.scalars(select(Manager)).one().manager_data, [c.name for c in Employee.__table__.columns])

# ---- joined-table inheritance
with Session(engine) as s:
    s.add_all([Truck(wheels=6, payload=10), Vehicle(wheels=2)])
    s.commit()
    print([(type(v).__name__, v.wheels, v.kind) for v in s.scalars(select(Vehicle).order_by(Vehicle.id))])
    t = s.scalars(select(Truck)).one()
    print(t.payload, t.wheels, t.id)
    t.payload = 12
    t.wheels = 8
    s.commit()
    print(s.get(Truck, t.id).payload, s.get(Vehicle, t.id).wheels)
    s.delete(t)
    s.commit()
    print(s.scalar(select(func.count(Vehicle.id))), s.scalar(select(func.count(Truck.payload))))

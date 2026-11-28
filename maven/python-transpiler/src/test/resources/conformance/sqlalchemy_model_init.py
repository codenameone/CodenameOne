# The constructor of a mapped class: keyword-only, columns, relationships,
# an inherited user __init__, unknown keywords and positional arguments.
from typing import List, Optional
from sqlalchemy import create_engine, ForeignKey, String, select
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, relationship, Session


class Base(DeclarativeBase):
    pass


class Author(Base):
    __tablename__ = "author"
    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(50))
    nick: Mapped[Optional[str]] = mapped_column(default="anon")
    books: Mapped[List["Book"]] = relationship(back_populates="author")


class Book(Base):
    __tablename__ = "book"
    id: Mapped[int] = mapped_column(primary_key=True)
    title: Mapped[str]
    author_id: Mapped[Optional[int]] = mapped_column(ForeignKey("author.id"))
    author: Mapped[Optional[Author]] = relationship(back_populates="books")


class Note(Base):
    __tablename__ = "note"
    id: Mapped[int] = mapped_column(primary_key=True)
    text: Mapped[str]

    def __init__(self, text, **kw):
        super().__init__(text=text.upper(), **kw)


engine = create_engine("sqlite://")
Base.metadata.create_all(engine)
a = Author(name="Ann")
b = Book(title="T1", author=a)
c = Book(author=a, title="T2")
n = Note("hello")
print(a.name, [x.title for x in a.books], b.author is a, c.author.name, n.text)
for bad in (lambda: Author(nme="x"), lambda: Author("x"), lambda: Note("x", bogus=1)):
    try:
        bad()
        print("no error")
    except TypeError:
        print("TypeError")
with Session(engine) as s:
    s.add_all([a, n, Author(name="Bob", nick="bb")])
    s.commit()
    for x in s.scalars(select(Author).order_by(Author.id)):
        print(x.id, x.name, x.nick, len(x.books))
    print(s.scalars(select(Note)).one().text)

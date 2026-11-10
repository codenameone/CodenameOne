import os
from sqlalchemy import ForeignKey, String, create_engine, select
from sqlalchemy.orm import (DeclarativeBase, Mapped, Session, mapped_column,
                            relationship, selectinload)


class Base(DeclarativeBase):
    pass


class Author(Base):
    __tablename__ = "authors"
    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(80))
    books: Mapped[list["Book"]] = relationship(
        back_populates="author", cascade="all, delete-orphan")


class Book(Base):
    __tablename__ = "books"
    id: Mapped[int] = mapped_column(primary_key=True)
    title: Mapped[str] = mapped_column(String(200))
    author_id: Mapped[int] = mapped_column(ForeignKey("authors.id"))
    author: Mapped[Author] = relationship(back_populates="books")


engine = create_engine(os.environ.get("DATABASE_URL", "sqlite://"))
Base.metadata.create_all(engine)

with Session(engine) as session:
    session.add(Author(name="Ursula", books=[Book(title="The Dispossessed")]))
    session.commit()
    query = select(Author).options(selectinload(Author.books)).order_by(Author.id)
    for author in session.scalars(query):
        print(author.name, [b.title for b in author.books])

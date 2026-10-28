import sys
import datetime
from sqlalchemy import (create_engine, text, MetaData, Table, Column, Integer, String, Text, Float,
                        Boolean, DateTime, Date, ForeignKey, select, insert, update, delete, func,
                        and_, or_, not_)
from sqlalchemy import exc

engine = create_engine(sys.argv[1] if len(sys.argv) > 1 else "sqlite://")
# Raw DDL is the one place the SQL is the engine's own: an auto-numbered key is spelled per engine.
AUTO_ID = {"postgresql": "SERIAL PRIMARY KEY", "mysql": "INTEGER PRIMARY KEY AUTO_INCREMENT",
           "mariadb": "INTEGER PRIMARY KEY AUTO_INCREMENT"}.get(engine.dialect.name, "INTEGER PRIMARY KEY")
with engine.connect() as conn:
    conn.execute(text("DROP TABLE IF EXISTS t"))
    conn.execute(text("CREATE TABLE t (id " + AUTO_ID + ", name TEXT, n INTEGER)"))
    conn.execute(text("INSERT INTO t (name, n) VALUES (:name, :n)"), [{"name": "a", "n": 1}, {"name": "b", "n": 2}])
    conn.execute(text("INSERT INTO t (name, n) VALUES (:name, :n)"), {"name": "c", "n": 3})
    conn.commit()
    r = conn.execute(text("SELECT id, name, n FROM t WHERE n >= :lo ORDER BY id"), {"lo": 2})
    for row in r:
        print(tuple(row), row.name, row[2], row.n)
    print(conn.execute(text("SELECT count(*) FROM t")).scalar())
    print(conn.execute(text("SELECT name FROM t ORDER BY name")).scalars().all())
    print([dict(m) for m in conn.execute(text("SELECT id, name FROM t ORDER BY id")).mappings()])
    print(tuple(conn.execute(text("SELECT name, n FROM t ORDER BY n DESC")).first()))
    print(conn.execute(text("SELECT name FROM t WHERE n = 9")).one_or_none())
    print(conn.execute(text("SELECT name FROM t WHERE n = 1")).one().name)
    try:
        conn.execute(text("SELECT name FROM t")).one()
    except exc.MultipleResultsFound:
        print("MultipleResultsFound")
    try:
        conn.execute(text("SELECT name FROM t WHERE n = 9")).one()
    except exc.NoResultFound:
        print("NoResultFound")
    conn.execute(text("DELETE FROM t WHERE n = 3"))
    conn.rollback()
    print(conn.execute(text("SELECT count(*) FROM t")).scalar_one())

metadata = MetaData()
users = Table("users", metadata,
              Column("id", Integer, primary_key=True),
              Column("name", String(50), nullable=False),
              Column("bio", Text),
              Column("score", Float, default=1.5),
              Column("active", Boolean, default=True),
              Column("created", DateTime),
              Column("born", Date))
posts = Table("posts", metadata,
              Column("id", Integer, primary_key=True),
              Column("user_id", Integer, ForeignKey("users.id")),
              Column("title", String(100)),
              Column("likes", Integer, default=0))
metadata.drop_all(engine)  # a server database keeps tables between runs
metadata.create_all(engine)
print(sorted(metadata.tables), [c.name for c in users.columns], users.c.name.name)

with engine.begin() as conn:
    r = conn.execute(insert(users).values(name="ann", bio="hi", created=datetime.datetime(2024, 1, 2, 3, 4, 5),
                                          born=datetime.date(1990, 5, 6)))
    print("pk", r.inserted_primary_key[0])
    conn.execute(insert(users), [{"name": "bob", "score": 3.0, "active": False}, {"name": "cid", "score": 2.5, "active": True}])
    conn.execute(insert(posts), [{"user_id": 1, "title": "p1", "likes": 5}, {"user_id": 1, "title": "p2", "likes": 7},
                                 {"user_id": 2, "title": "p3", "likes": 1}])

with engine.connect() as conn:
    row = conn.execute(select(users).where(users.c.id == 1)).one()
    print(row.name, row.bio, row.score, row.active, row.created, row.born,
          type(row.created).__name__, type(row.born).__name__)
    print(conn.execute(select(users.c.name, users.c.active).order_by(users.c.id)).all()
          == [("ann", True), ("bob", False), ("cid", True)])
    q = select(users.c.name).where(and_(users.c.score > 1.0, users.c.active == True)).order_by(users.c.name.desc())
    print(conn.execute(q).scalars().all())
    q = select(users.c.name).where(or_(users.c.name == "bob", users.c.name.like("c%"))).order_by(users.c.name)
    print(conn.execute(q).scalars().all())
    print(conn.execute(select(users.c.name).where(not_(users.c.name.in_(["ann", "bob"])))).scalars().all())
    print(conn.execute(select(users.c.name).where(users.c.bio.is_(None)).order_by(users.c.id)).scalars().all())
    print(conn.execute(select(users.c.name).where(users.c.name != "ann").order_by(users.c.id)
                       .limit(1).offset(1)).scalars().all())
    print(conn.execute(select(users.c.name).where(users.c.score < 2.0, users.c.id >= 1)).scalars().all())
    j = select(users.c.name, posts.c.title).join(posts, users.c.id == posts.c.user_id).order_by(posts.c.title)
    print([tuple(r) for r in conn.execute(j)])
    j = (select(users.c.name, posts.c.title).select_from(users.join(posts))
         .where(posts.c.likes > 4).order_by(posts.c.id))
    print([tuple(r) for r in conn.execute(j)])
    print(conn.execute(select(func.count()).select_from(posts)).scalar())
    print(conn.execute(select(func.count(posts.c.id), func.sum(posts.c.likes), func.max(posts.c.likes),
                              func.min(posts.c.likes))).one() == (3, 13, 7, 1))
    g = (select(posts.c.user_id, func.count(posts.c.id).label("cnt"))
         .group_by(posts.c.user_id).order_by(posts.c.user_id))
    print([(r.user_id, r.cnt) for r in conn.execute(g)])
    conn.execute(update(users).where(users.c.name == "bob").values(score=users.c.score + 1))
    print(conn.execute(select(users.c.score).where(users.c.name == "bob")).scalar())
    r = conn.execute(delete(posts).where(posts.c.likes < 2))
    print("deleted", r.rowcount)
    r = conn.execute(update(posts).values(likes=0))
    print("updated", r.rowcount)
    conn.rollback()
    print(conn.execute(select(func.count()).select_from(posts)).scalar())
    print(conn.execute(select(posts.c.title).order_by(posts.c.likes.desc())).first()[0])
    r = conn.execute(insert(posts).values(title="r", likes=4).returning(posts.c.id, posts.c.likes))
    print(tuple(r.one()))
    try:
        conn.execute(insert(users).values(id=1, name="dup"))
    except exc.IntegrityError:
        print("IntegrityError")
    conn.rollback()

metadata.drop_all(engine)
with engine.connect() as conn:
    print(conn.execute(text("SELECT count(*) FROM sqlite_master WHERE type='table'")).scalar())

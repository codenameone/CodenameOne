from sqlalchemy import (create_engine, make_url, MetaData, Table, Column, Integer, String,
                        Boolean, select, insert, true, exc)
from sqlalchemy.dialects import postgresql, mysql, sqlite

for s in ("postgresql://scott:tiger@db.example:5433/app?sslmode=require",
          "postgresql+psycopg://u@localhost/app",
          "mysql://root:pw@127.0.0.1/shop",
          "mysql+pymysql://root:p%40ss@[::1]:3307/shop?charset=utf8mb4",
          "mariadb://u:p@host/db",
          "sqlite:///file.db",
          "sqlite://"):
    u = make_url(s)
    print(str(u), u.drivername, u.get_backend_name(), u.get_driver_name())
    print("  ", u.username, u.password, u.host, u.port, u.database, dict(u.query))

print(make_url("mysql://a:b@h/d").render_as_string(hide_password=False))
print(make_url("postgresql://a@h/d").set(database="other"))

try:
    make_url("oracle2://x@y/z")
except exc.ArgumentError as e:
    print("ArgumentError", type(e).__name__)

for d in (postgresql.dialect(), mysql.dialect(), sqlite.dialect()):
    print(d.name, d.supports_native_boolean)

e = create_engine("sqlite://")
print(e.dialect.name, e.dialect.driver, e.dialect.paramstyle)

md = MetaData()
t = Table("t", md, Column("id", Integer, primary_key=True), Column("name", String(20)),
          Column("ok", Boolean))

# Behaviour on SQLite is unchanged.
eng = create_engine("sqlite://")
md.create_all(eng)
with eng.begin() as conn:
    r = conn.execute(insert(t).values(name="a", ok=True))
    print(r.inserted_primary_key)
    conn.execute(insert(t).values(name="b", ok=False))
    print(conn.execute(select(t.c.name, t.c.ok).where(t.c.ok == true())).all())
    print(conn.execute(select(t.c.name).order_by(t.c.id).offset(1)).all())

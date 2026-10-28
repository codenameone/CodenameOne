from sqlalchemy import Column, Integer, String, MetaData, Table, cast, select
from sqlalchemy.dialects import mysql, postgresql, sqlite

md = MetaData()
t = Table("t", md, Column("id", Integer, primary_key=True), Column("age", Integer))
stmt = select(cast(t.c.age, String) + "y")
for d in (mysql.dialect(), postgresql.dialect(), sqlite.dialect()):
    sql = str(stmt.compile(dialect=d))
    print(d.name, "concat(" in sql, "AS CHAR)" in sql, "AS VARCHAR)" in sql, "||" in sql)

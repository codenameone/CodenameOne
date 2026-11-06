import os
import shelve

path = "db.shelf"


class Item:
    def __init__(self, n):
        self.n = n

    def __repr__(self):
        return "Item(%d)" % self.n


with shelve.open(path) as db:
    db["a"] = 1
    db["list"] = [1, 2, {"k": (3, 4)}]
    db["item"] = Item(7)
    print(sorted(db.keys()), len(db), "a" in db, "z" in db)
    print(db["list"], db["item"], db.get("z", "dflt"))
    db["list"].append(5)
    print(db["list"])
    print(db.setdefault("s", "x"), db.pop("s"), db.pop("s", None))

print(os.path.exists(path))
db = shelve.open(path)
print(sorted(db), db["a"], db["list"], db["item"])
del db["a"]
db.update({"b": 2}, c=3)
print(sorted(db.items()))
db.close()
try:
    db["b"]
except ValueError as e:
    print("closed:", e)

db = shelve.open(path, "r")
print(sorted(db.keys()))
try:
    db["x"] = 1
except OSError:
    print("read only")
db.close()

with shelve.open(path, writeback=True) as db:
    db["wb"] = [1]
    db["wb"].append(2)
    db["wb"].append(3)
with shelve.open(path, "r") as db:
    print(db["wb"])

with shelve.open(path, "n") as db:
    print(len(db))

# os and os.path over the application's files.
import os
import os.path
from os.path import join, splitext

print(os.sep, os.name, os.curdir, os.pardir, os.linesep == "\n")
print(join("a", "b", "c.txt"), join("a/", "b"), join("a", "/abs", "x"), join("", "x"))
print(os.path.split("/a/b/c"), os.path.split("c"), os.path.split("/"), os.path.split("a//b"))
print(splitext("x/y.tar.gz"), splitext(".bashrc"), splitext("a.b/c"), splitext("f."))
print(os.path.basename("/a/b/"), os.path.dirname("/a/b/c"), os.path.dirname("c"))
print(os.path.normpath("a/./b/../c//d/"), os.path.normpath("../x"), os.path.normpath("/../x"),
      os.path.normpath(""), os.path.normpath("//a"))
print(os.path.isabs("/x"), os.path.isabs("x"))
print(os.path.commonprefix(["/usr/lib", "/usr/local"]), os.path.commonpath(["/usr/lib", "/usr/local"]))
print(os.path.relpath("a/b/c", "a/d"), os.path.relpath("a", "a"))
print(os.path.splitroot("/a/b"), os.path.splitdrive("x/y"))
start = os.getcwd()
print(os.path.abspath("q") == join(start, "q"))
os.mkdir("d1")
print(os.path.exists("d1"), os.path.isdir("d1"), os.path.isfile("d1"))
os.makedirs("d1/x/y")
os.makedirs("d1/x/y", exist_ok=True)
with open("d1/f.txt", "w") as f:
    f.write("12345")
print(sorted(os.listdir("d1")))
print(os.path.getsize("d1/f.txt"), os.stat("d1/f.txt").st_size, os.path.isfile("d1/f.txt"))
print(sorted(e.name for e in os.scandir("d1")), [e.is_dir() for e in sorted(os.scandir("d1"), key=lambda e: e.name)])
for root, dirs, files in os.walk("d1"):
    print(root, sorted(dirs), sorted(files))
os.chdir("d1")
print(os.getcwd() == join(start, "d1"), sorted(os.listdir()))
print(open("f.txt").read())
os.rename("f.txt", "g.txt")
print(sorted(os.listdir(".")))
os.replace("g.txt", "x/h.txt")
print(os.listdir("x"))
os.chdir("..")
os.remove("d1/x/h.txt")
os.rmdir("d1/x/y")
try:
    os.rmdir("d1")
except OSError as e:
    print(isinstance(e, OSError), e.errno in (39, 66))
os.removedirs("d1/x")
print(os.path.exists("d1"))
for f, arg in [(os.remove, "nope"), (os.listdir, "nope"), (os.stat, "nope"), (os.rmdir, "nope"), (os.chdir, "nope")]:
    try:
        f(arg)
    except OSError as e:
        print(type(e).__name__, e)
os.mkdir("z")
try:
    os.mkdir("z")
except FileExistsError as e:
    print(e)
try:
    os.mkdir("p/q")
except FileNotFoundError as e:
    print(e)
try:
    os.remove("z")
except OSError as e:
    print(isinstance(e, OSError))
class P:
    def __fspath__(self):
        return "z"
print(os.fspath(P()), os.path.isdir(P()), os.fspath("s"), os.fsencode("a"))
try:
    os.fspath(3)
except TypeError as e:
    print(e)
print(os.getenv("CN1_NOT_SET_XYZ"), os.getenv("CN1_NOT_SET_XYZ", "dflt"))
print(os.path.exists("missing"), os.path.getsize.__name__)
print(len(os.urandom(5)))

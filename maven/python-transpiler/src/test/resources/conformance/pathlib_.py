# pathlib, fnmatch and glob.
from pathlib import Path, PurePath, PurePosixPath
import fnmatch
import glob

p = PurePosixPath("/usr/lib/python3.tar.gz")
print(p, repr(p), p.name, p.suffix, p.suffixes, p.stem, p.parent, p.root, p.anchor, p.parts)
print([str(x) for x in p.parents])
print(p.with_name("x.py"), p.with_suffix(".bz2"), p.with_stem("y"), p.with_suffix(""))
print(PurePath("a", "b/c", "d"), PurePath("a") / "b" / "c", "x" / PurePath("y"), PurePath(), PurePath("a//b/./c/"))
print(PurePath("/a/b/c").relative_to("/a"), PurePath("/a/b").is_relative_to("/a"), PurePath("/a/b").is_relative_to("/c"))
print(PurePath("a/b").relative_to("a/c", walk_up=True))
try:
    PurePath("/a/b").relative_to("/c")
except ValueError as e:
    print(e)
print(PurePath("a/b.py").match("*.py"), PurePath("/a/b/c.py").match("b/*.py"), PurePath("a/b.py").match("/*.py"))
print(PurePath("a/b/c.py").full_match("a/**/*.py"), PurePath("a.py").full_match("**/*.py"))
print(PurePath("a") == PurePath("a/"), PurePath("a") < PurePath("b"), hash(PurePath("a")) == hash(PurePath("a")))
print(sorted([PurePath("b"), PurePath("a/z"), PurePath("a")]))
print(PurePath(".bashrc").suffix, PurePath("x.").suffix, PurePath("/").name, PurePath("/").parent)
print(Path("/a").as_uri(), PurePath("/a").is_absolute(), PurePath("a").is_absolute())
try:
    PurePath("a").with_suffix("py")
except ValueError as e:
    print(e)
print(type(Path("x")).__name__, type(PurePath("x")).__name__, isinstance(Path("x"), PurePath))

base = Path("work")
base.mkdir()
(base / "sub" / "deep").mkdir(parents=True)
(base / "a.txt").write_text("alpha")
(base / "b.py").write_bytes(b"print(1)")
(base / "sub" / "c.py").write_text("c")
(base / "sub" / "deep" / "d.py").write_text("d")
(base / ".hidden.py").touch()
print((base / "a.txt").read_text(), (base / "b.py").read_bytes(), (base / "a.txt").stat().st_size)
print(sorted(str(x) for x in base.iterdir()))
print(sorted(str(x) for x in base.glob("*.py")))
print(sorted(str(x) for x in base.rglob("*.py")))
print(sorted(str(x) for x in base.glob("*/*.py")))
print(base.exists(), base.is_dir(), (base / "a.txt").is_file(), (base / "zz").exists())
print(Path("work/sub/../a.txt").resolve() == Path.cwd() / "work" / "a.txt", Path("w").absolute() == Path.cwd() / "w")
t = (base / "a.txt").rename(base / "z.txt")
print(t, t.exists(), (base / "a.txt").exists())
(base / "z.txt").unlink()
(base / "z.txt").unlink(missing_ok=True)
try:
    (base / "z.txt").unlink()
except FileNotFoundError as e:
    print(e)
base.mkdir(exist_ok=True)
try:
    base.mkdir()
except FileExistsError as e:
    print(e)
with (base / "w.txt").open("w") as f:
    f.write("hi")
print((base / "w.txt").read_text())
for root, dirs, files in sorted(base.walk()):
    print(root, sorted(dirs), sorted(files))

print(fnmatch.fnmatch("foo.py", "*.py"), fnmatch.fnmatch("foo.py", "f?o.*"), fnmatch.fnmatch("foo", "[a-f]*"),
      fnmatch.fnmatch("zoo", "[!z]*"), fnmatch.fnmatchcase("A.PY", "*.py"))
print(fnmatch.filter(["a.py", "b.txt", "c.py"], "*.py"), fnmatch.translate("*.py"), fnmatch.translate("[!a-c]?"))
print(sorted(glob.glob("work/*.py")), sorted(glob.glob("work/**/*.py", recursive=True)))
print(sorted(glob.glob("*.py", root_dir="work")), sorted(glob.glob("work/s*/")), glob.has_magic("a*"), glob.escape("a*b?"))

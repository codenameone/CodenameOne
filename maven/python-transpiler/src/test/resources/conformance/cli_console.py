# cn1console off a terminal, as a tool run under a pipe or CI sees it. CPython
# has no cn1console, so cli_console.out is written by hand: styles dropped,
# progress and spinners reduced to their final lines, prompts answered from
# their defaults at the end of input.
import cn1console as c

print(c.is_terminal(0), c.is_terminal(1), c.color_enabled())
print(repr(c.style("plain", "bold", "red", "on #102030")))
c.echo("echo", 1, 2, style="green")
print(c.width("abc"), c.width("日本"))
c.print_table([["api", 12], ["worker", None]], headers=["Name", "MB"], align={1: "right"}, border="ascii")
print(c.table([["a", "b"]], border="none"), end="")
print(c.tree("app", ["README", ("src", ["Main.java", ("util", ["Io.java"])]), "pom.xml"]), end="")
print(c.tree("app", ["README", ("src", ["Main.java", ("util", ["Io.java"])]), "pom.xml"]) != "")

total = 0
for n in c.progress(range(5), label="sum"):
    total += n
print("total", total)
with c.progress(total=3, label="copy") as bar:
    bar.advance(2)
    bar.update()
try:
    with c.progress(total=4, label="broken") as bar:
        bar.advance()
        raise ValueError("disk")
except ValueError as e:
    print("caught", e)
with c.spinner("loading") as s:
    s.text = "loaded"
s2 = c.spinner("fetch").start()
s2.warn("slow")
try:
    with c.spinner("upload"):
        raise RuntimeError("net")
except RuntimeError as e:
    print("caught", e)

print(c.confirm("Deploy?", default=True))
print(c.confirm("Delete?"))
print(c.text("Name", default="anon"))
print(c.select("Db", ["pg", "mysql", "sqlite"], default="mysql"))
print(c.select("Db", ["pg", "mysql"], default=0))
print(c.checkbox("Features", ["auth", "mail", "jobs"], default=["mail"]))
try:
    c.style("x", "sparkly")
except Exception as e:
    print(type(e).__name__)

# A break out of a tracked loop is not a failure; a bar with no iterable
# cannot be iterated; defaults by negative index and a single checkbox one.
with c.progress(range(5), label="brk") as bar:
    for n in bar:
        if n == 2:
            break
try:
    iter(c.Progress(total=2)).__next__()
except TypeError:
    print("TypeError")
print(c.select("Last", ["a", "b", "c"], default=-1))
print(c.checkbox("One", ["a", "b"], default="b"))
print(c.select("Num", [10, 20], default=20))

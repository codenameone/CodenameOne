# logging: levels, hierarchy, handlers, formatters, filters and adapters.
import logging
from io import StringIO

buf = StringIO()
h = logging.StreamHandler(buf)
h.setFormatter(logging.Formatter("%(levelname)s|%(name)s|%(message)s"))
app = logging.getLogger("app")
app.addHandler(h)
app.setLevel(logging.DEBUG)
db = logging.getLogger("app.db")
print(db.parent is app, app.parent is logging.root, logging.getLogger("app") is app, db.getEffectiveLevel())
app.debug("debug %d", 1)
db.info("query %s took %.1fms", "select", 2.25)
db.warning("dict %(a)s", {"a": "args"})
app.log(25, "custom")
logging.addLevelName(25, "NOTICE")
app.log(25, "named")
print(logging.getLevelName(25), logging.getLevelName("NOTICE"), logging.getLevelName(7), logging.getLevelName("ERROR"))
print(buf.getvalue(), end="")

buf2 = StringIO()
h2 = logging.StreamHandler(buf2)
h2.setFormatter(logging.Formatter("{levelname:<8}{name}:{message}", style="{"))
h2.setLevel(logging.WARNING)
app.addHandler(h2)
app.info("only first")
app.warning("both")
h3 = logging.StreamHandler(buf2)
h3.setFormatter(logging.Formatter("$levelname $message [${user}]", style="$"))
app.addHandler(h3)
app.critical("templ", extra={"user": "ada"})
app.removeHandler(h3)


class OnlyOdd(logging.Filter):
    def filter(self, record):
        return record.args[0] % 2 == 1


h2.addFilter(OnlyOdd())
for i in range(4):
    app.error("n=%d", i)
app.addFilter(lambda r: "secret" not in r.getMessage())
app.error("secret %d", 1)
print(buf2.getvalue(), end="")

f = logging.Filter("a.b")
for name in ["a", "a.b", "a.b.c", "a.bc"]:
    print(name, bool(f.filter(logging.makeLogRecord({"name": name}))), end="; ")
print()

buf3 = StringIO()
h4 = logging.StreamHandler(buf3)
h4.setFormatter(logging.Formatter("%(message)s"))
lg = logging.getLogger("exc")
lg.addHandler(h4)
lg.propagate = False
try:
    1 / 0
except ZeroDivisionError:
    lg.exception("failed")
print(buf3.getvalue().splitlines()[0], buf3.getvalue().splitlines()[-1])

ad = logging.LoggerAdapter(lg, {"ctx": "req1"})
h4.setFormatter(logging.Formatter("%(ctx)s %(message)s"))


class Prefix(logging.LoggerAdapter):
    def process(self, msg, kwargs):
        msg, kwargs = super().process(msg, kwargs)
        return "[%s] %s" % (self.extra["ctx"], msg), kwargs


buf3.seek(0)
buf3.truncate()
ad.warning("adapted")
Prefix(lg, {"ctx": "p"}).warning("pref")
print(repr(buf3.getvalue()))
print(repr(app), repr(logging.root), repr(ad), app.isEnabledFor(5), app.hasHandlers())
logging.disable(logging.ERROR)
print(app.isEnabledFor(logging.ERROR), app.isEnabledFor(logging.CRITICAL))
logging.disable(logging.NOTSET)
try:
    app.setLevel("LOUD")
except ValueError as e:
    print(e)
try:
    app.log("x", "y")
except TypeError as e:
    print(e)
rec = logging.makeLogRecord({"msg": "hi %s", "args": ("there",), "levelno": 20})
print(rec.getMessage(), logging.Formatter().format(rec))
try:
    logging.Formatter("no fields", style="{")
except ValueError as e:
    print(e)
out = StringIO()
logging.basicConfig(stream=out, level=logging.INFO, format="%(levelname)s:%(message)s")
logging.info("root info")
logging.debug("root debug")
logging.getLogger("other").warning("propagated")
print(repr(out.getvalue()), logging.root.level)
print(sorted(logging.getLevelNamesMapping().items())[:3], db.getChild("x").name, sorted(c.name for c in app.getChildren()))

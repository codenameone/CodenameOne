import logging
from io import StringIO

out = StringIO()
logging.basicConfig(stream=out, level=logging.INFO, format="%(levelname)s:%(message)s")
h = logging.root.handlers[0]
print(h.stream is out, h.level, logging.root.isEnabledFor(20), logging.root.disabled)
r = logging.LogRecord("n", 20, "p", 1, "hi", None, None)
print(repr(h.format(r)))
h.emit(r)
print(repr(out.getvalue()))
h.handle(r)
print(repr(out.getvalue()))
logging.root.handle(r)
print(repr(out.getvalue()))
logging.info("root info")
print(repr(out.getvalue()))

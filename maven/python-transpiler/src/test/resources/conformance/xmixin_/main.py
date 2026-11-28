from base import Parser
from detect import Detects


class TagParser(Parser, Detects):
    def __init__(self, data):
        Parser.__init__(self, data)
        self._init_detector()
        self.upper = []

    def handle_tag(self, name):
        self._seen(name)
        self.upper.append(name.upper())
        Parser.handle_tag(self, name)


p = TagParser("<a><b>x</b><thtml>")
print(p.feed())
print(p.upper, p._first, p.count, Detects.count, TagParser.count)
print(isinstance(p, Parser), isinstance(p, Detects), [k.__name__ for k in TagParser.__mro__])
print(TagParser.looks_like("<?xml x"), p.looks_like("<a>"), p.looks_like("<xhtml>"))
q = TagParser("")
print(q.feed(), q._first, q.count)
q.extra = 5
print(q.extra, sorted(k for k in vars(p) if not k.startswith("__")))

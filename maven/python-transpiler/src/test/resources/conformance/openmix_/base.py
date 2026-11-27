class Parser:
    def __init__(self):
        self.raw = ""
        self.count = 0
        self.last = None
        self.owner = None

    def feed(self, data):
        self.raw = self.raw + data
        self.scan()

    def scan(self):
        i = 0
        n = len(self.raw)
        while i < n and self.raw[i] != ">":
            i += 1
        self.count = self.count + i
        self.last = self.tag()
        self.owner = self
        keep = [self]
        return keep[0]

    def tag(self):
        return self.raw[:3]

    def label(self):
        return "parser"


def peers(a, b):
    a.last = b.last
    a.count = a.count + b.count
    return a.label() + "/" + b.label()

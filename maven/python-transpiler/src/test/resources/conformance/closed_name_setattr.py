# setattr(obj, prop[0], v) where prop comes from a local table of literal
# tuples: each store is one of the table's literal names.


class Parser:
    def parse(self, items):
        properties = {
            "a": ("alpha", int),
            "b": ("beta", str),
            "c": ("gamma", float),
        }
        for t in items:
            if t in properties:
                prop = properties[t]
                obj = prop[1](1)
                setattr(self, prop[0], obj)
        return self


class One:
    def fill(self, keys):
        table = {"k": ("only", 7)}
        for k in keys:
            if k not in table:
                continue
            row = table[k]
            setattr(self, row[0], row[1])
        return self


p = Parser().parse(["c", "a", "b", "x"])
print(p.alpha, p.beta, p.gamma)
print(One().fill(["z", "k"]).only)

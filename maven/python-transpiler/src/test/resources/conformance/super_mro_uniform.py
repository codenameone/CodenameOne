# super() in a class some subclass mixes another base into: the next
# class in the MRO is the same for every subclass (resolved statically),
# and patching a class between them after definition is still seen.
class Base:
    def put(self, v):
        return ["Base", v]

class Typed(Base):
    def put(self, v):
        return ["Typed"] + super().put(v)

class Conv(Typed):
    def put(self, v):
        return ["Conv"] + super().put(v * 2)

class Str(Typed):
    def put(self, v):
        return ["Str"] + super().put(v + 1)

class Text(Str, Conv):
    pass

class Text2(Text):
    def put(self, v):
        return ["Text2"] + super().put(v)

for o in (Typed(), Conv(), Str(), Text(), Text2()):
    print(type(o).__name__, o.put(3))
print([c.__name__ for c in Text2.__mro__])

def patched(self, v):
    return ["patched", v]
Base.put = patched
print(Text2().put(1), Conv().put(1))

class Spy:
    def __init__(self):
        self.x = 1
    def __getattribute__(self, name):
        print("get", name)
        return object.__getattribute__(self, name)
    def __len__(self):
        return 3

s = Spy()
print(s.x)
d = s.__dict__
print(sorted(d))
print(getattr(s, '__dict__') == {'x': 1})
print(s.__class__.__name__)
print(len(s))
print(s.__len__())

class Fake:
    def __getattribute__(self, name):
        if name == '__doc__':
            return 'faked doc'
        if name == '__secret__':
            return 42
        return object.__getattribute__(self, name)
f = Fake()
print(f.__doc__)
print(f.__secret__)
print(getattr(f, '__secret__'))
print(hasattr(f, '__secret__'))

class C:
    def __init__(self):
        self.a = 1
c = C()
d = c.__dict__
c.b = 2
print(d)
d['x'] = 10
print(c.x)
vars(c).update(y=20, z=30)
print(c.y, c.z)
c.a = 99
print(d['a'], len(d), 'b' in d, sorted(d.keys()))
del c.b
print(d)
del d['x']
print(hasattr(c, 'x'), d)
print(c.__dict__ == d)
d.clear()
print(vars(c), hasattr(c, 'a'))
c.q = 5
print(list(d.items()))

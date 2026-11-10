class P:
    def __init__(self):
        self.a = 1

r = P().__reduce_ex__(2)
print(r[0].__name__, r[1][0] is P, r[2])
print(P().__getstate__())

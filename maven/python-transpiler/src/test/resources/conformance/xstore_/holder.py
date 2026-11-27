class Holder:
    def __init__(self, **kw):
        self.count = 0
        self.name = "h"
        self.extra = kw

    def bump(self):
        self.count = self.count + 1
        return self.count

    def describe(self):
        return self.name + ":" + str(self.count)


class Other:
    def __init__(self):
        self.count = 1.5

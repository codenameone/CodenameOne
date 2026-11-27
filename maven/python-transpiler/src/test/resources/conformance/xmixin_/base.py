class Parser:
    def __init__(self, data):
        self.data = data
        self.pos = 0
        self.tags = []

    def feed(self):
        while self.pos < len(self.data):
            c = self.data[self.pos]
            if c == "<":
                end = self.data.index(">", self.pos)
                self.handle_tag(self.data[self.pos + 1:end])
                self.pos = end + 1
            else:
                self.pos += 1
        return self.tags

    def handle_tag(self, name):
        self.tags.append(name)

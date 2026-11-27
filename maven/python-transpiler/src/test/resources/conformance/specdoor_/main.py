import json


class Buf:
    def __init__(self):
        self.data = ""
        self.n = 0

    def feed(self, s):
        self.data = self.data + s
        self.n += len(s)
        return self.data.upper()

    def scale(self, k):
        return k * 3


def unknown(text):
    # json gives values the pass knows nothing about
    return json.loads(text)


def main():
    b = Buf()
    print(b.feed("ab"))
    print(b.feed(unknown('"cd"')))
    try:
        b.feed(unknown("[1, 2]"))
    except TypeError as e:
        print("TypeError", b.data, b.n)
    print(b.scale(2), b.scale(unknown('"x"')), b.scale(unknown("[1]")))
    holder = [b, Buf()]
    for h in holder:
        print(h.feed(unknown('"z"')), h.n)


main()

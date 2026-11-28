# Parameters a typed function rebinds to another type, not first thing.


class Parser:
    def __init__(self, tag: str):
        self.tag = tag


def parse(text: str, parser=None, encoding=None):
    n = len(text)
    if parser is None:
        parser = Parser("default")
    if encoding is None:
        encoding = "utf-8"
    return parser.tag + ":" + encoding + ":" + str(n)


def only_default(text: str, parser=None):
    n = len(text)
    if parser is None:
        parser = Parser("d" + str(n))
    return parser.tag


def main():
    print(only_default("xy"), only_default("xyz"))
    print(parse("abc"))
    print(parse("abcd", Parser("mine"), "latin-1"))


main()

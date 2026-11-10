# Typed bytes: literals, encode/decode, indexing, slicing, str repetition.


def run(n):
    s = "héllo — " * 3
    b = s.encode("utf-8")
    t = b.decode("utf-8")
    print(t == s, len(t), len(b))
    print(b[0], b[-1], b[n], b[len(b) - 1])
    head = b[:n + 3]
    print(head, head.decode("latin-1"), len(head.decode("latin-1")))
    print(b"abc"[1], b"abc"[1:], b"xyz".decode())
    print(s.encode("ascii", "replace")[:8], b"\xff\xfe".decode("utf-8", "replace"))
    print(3 * "ab", "ab" * 0, "ab" * True)
    try:
        b[100]
    except IndexError as e:
        print("IndexError", e)
    try:
        b"\xff".decode("utf-8")
    except UnicodeDecodeError as e:
        print("UnicodeDecodeError", e.start)


run(2)

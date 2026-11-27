# Loops whose source the typed body cannot see through.


def stepped(n: int, step):
    out = []
    i = -1
    for i in range(0, n, step):
        out.append(i * 2)
    return out, i


def from_bytes(src):
    total = 0
    for b in src:
        total = total + b
    for b in bytearray(b"\x01\x02"):
        total += b
    return total, b


print(stepped(10, 3), stepped(5, -1) if False else stepped(3, 1))
print(from_bytes(b"\x10\x20"), from_bytes([1, 2]))

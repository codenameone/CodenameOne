# Developer guide, Python chapter. Transpiled and compiled with the demos
# so the guide's code cannot drift from what builds.

# tag::python-interop-python-005[]
M32 = 0xFFFFFFFF

def i32(v: int) -> int:
    return ((v + 0x80000000) & M32) - 0x80000000

def int_arithmetic() -> int:
    a: int = 0x12345678
    b: int = i32(0x9E3779B9)
    checksum: int = 0
    for _ in range(40000000):
        a = i32((a * 1103515245 + 12345) ^ ((b & M32) >> 3))
        b = i32((b + a) * 5 - (a << 7))
        checksum += (a ^ b) & 0xFFFF
    return checksum + a + b
# end::python-interop-python-005[]

# Python port of com.bench.CommonWorkloads (vm/benchmarks/common), the
# workloads every Codename One port and the Dart port already run.
#
# Like the Dart port it reproduces Java's semantics rather than idiomatic
# Python, so the checksums are the contract: each workload returns exactly
# the value the Java method returns.
#
#  * Java `int` is 32-bit and `long` 64-bit, and both wrap. Python's int does
#    not, so every expression that can overflow is folded back with a mask.
#  * Java `>>>` shifts the unsigned bit pattern: mask first, then shift.
#  * Java `%` on doubles is C's fmod, not Python's floor modulo.
#  * Java's String.hashCode is reimplemented.
#
# The code is plain, type-annotated Python and runs unchanged on CPython,
# PyPy, GraalPy and Codename One.
import math
import struct

M32 = 0xFFFFFFFF
M64 = 0xFFFFFFFFFFFFFFFF


def i32(v: int) -> int:
    return ((v + 0x80000000) & M32) - 0x80000000


def i64(v: int) -> int:
    return ((v + 0x8000000000000000) & M64) - 0x8000000000000000


# ---- 1. integer arithmetic: dependent ALU chain ----
def int_arithmetic() -> int:
    a: int = 0x12345678
    b: int = i32(0x9E3779B9)
    checksum: int = 0
    for _ in range(40000000):
        a = i32((a * 1103515245 + 12345) ^ ((b & M32) >> 3))
        b = i32((b + a) * 5 - (a << 7))
        checksum += (a ^ b) & 0xFFFF
    return i64(checksum + a + b)


# ---- 2. long (64-bit) arithmetic: dependent chain ----
def long_arithmetic() -> int:
    a: int = 0x0123456789ABCDEF
    b: int = -0x123456789
    checksum: int = 0
    for _ in range(30000000):
        a = i64((a * 6364136223846793005 + 1442695040888963407) ^ ((b & M64) >> 7))
        b = i64((b ^ (a << 13)) + ((a & M64) >> 11))
        checksum += (a + b) & 0xFF
    return i64(checksum + a + b)


# ---- 3. floating point + transcendental ----
def math_transcendental() -> int:
    acc: float = 1.0
    x: float = 0.5
    for i in range(8000000):
        x = x + 0.000001 * (i & 1023)
        acc += math.sqrt(x) + math.sin(x) * math.cos(x) - math.sqrt(math.fmod(acc, 1000.0) + 1.0)
        if acc > 1e12 or acc < -1e12:
            acc = math.fmod(acc, 1000.0)
    return struct.unpack(">q", struct.pack(">d", acc))[0]


# ---- 4. sequential array fill + reduce ----
# The arrays are allocated on first use and kept, as Java's static ones are
# when the class is first touched, so each workload's memory is its own.
seq_arr = []


def array_sequential() -> int:
    arr = seq_arr
    if not arr:
        arr.extend([0] * 8000000)
    n: int = len(arr)
    checksum: int = 0
    seed: int = i32(0x9E3779B9)
    for i in range(n):
        seed = i32(seed * 1103515245 + 12345)
        arr[i] = seed
    for p in range(4):
        s: int = 0
        for i in range(n):
            s += arr[i]
        checksum ^= s + p
    return checksum


# ---- 5. random-access gather ----
rand_arr = []


def array_random() -> int:
    arr = rand_arr
    if not arr:
        arr.extend([0] * 4000000)
    n: int = len(arr)
    for i in range(n):
        arr[i] = i32(i * 2654435761) if ((i * 2654435761) >> 8) > 0 else i
    checksum: int = 0
    idx: int = 12345
    for _ in range(20000000):
        v: int = arr[(idx & 0x7FFFFFFF) % n]
        checksum += v
        idx = i32(v ^ (idx * 31 + 7))
    return checksum


# ---- 6. object allocation + GC churn ----
class Node:
    __slots__ = ("v", "next")

    def __init__(self, v: int, nxt):
        self.v = v
        self.next = nxt


def object_allocation() -> int:
    checksum: int = 0
    head = None
    for i in range(8000000):
        head = Node(i, head)
        if (i & 511) == 0:
            p = head
            steps: int = 0
            while p is not None and steps < 48:
                checksum += p.v
                p = p.next
                steps += 1
            head = None
    return checksum


# ---- non-escaping value object ----
class Vec:
    __slots__ = ("x", "y")

    def __init__(self, x: int, y: int):
        self.x = x
        self.y = y

    def get_x(self) -> int:
        return self.x

    def get_y(self) -> int:
        return self.y


def value_escape() -> int:
    total: int = 0
    for i in range(8000000):
        v = Vec(i, i * 2)
        total = (total + v.get_x() + v.get_y()) & 0x3FFFFFFF
    return total


# ---- 7. hash map churn ----
def hash_map_churn() -> int:
    m = {}
    checksum: int = 0
    window: int = 50000
    for i in range(3000000):
        key: int = i & 0x3FFFF
        prev = m.get(key)
        m[key] = i if prev is None else i32(prev + i)
        if prev is not None:
            checksum += prev
        if len(m) > window:
            m.clear()
    return checksum + len(m)


# ---- 8. string building + hashing ----
def java_hash(s: str) -> int:
    h: int = 0
    for c in s:
        h = (h * 31 + ord(c)) & M32
    return h - 0x100000000 if h > 0x7FFFFFFF else h


sb_ring = [""] * 256


def string_building() -> int:
    ring = sb_ring
    checksum: int = 0
    for i in range(400000):
        ring[i & 255] = "item-" + str(i) + "-" + str(i32(i * 31) ^ 0x55AA) + ":" + (
            "even" if (i & 1) == 0 else "odd")
        if (i & 255) == 255:
            for j in range(256):
                s = ring[j]
                checksum += java_hash(s) + len(s)
    return checksum


# ---- 9. recursion ----
def fib(n: int) -> int:
    if n < 2:
        return n
    return fib(n - 1) + fib(n - 2)


def recursion() -> int:
    checksum: int = 0
    for i in range(3):
        checksum += fib(35 + (i & 1))
    return checksum


# ---- 10. quicksort ----
sort_arr = []


def quicksort(a, lo: int, hi: int) -> None:
    while lo < hi:
        pivot: int = a[(lo + hi) >> 1]
        i: int = lo
        j: int = hi
        while i <= j:
            while a[i] < pivot:
                i += 1
            while a[j] > pivot:
                j -= 1
            if i <= j:
                a[i], a[j] = a[j], a[i]
                i += 1
                j -= 1
        if j - lo < hi - i:
            quicksort(a, lo, j)
            lo = i
        else:
            quicksort(a, i, hi)
            hi = j


def quicksort_bench() -> int:
    a = sort_arr
    if not a:
        a.extend([0] * 1500000)
    n: int = len(a)
    seed: int = i32(0xCAFEBABE)
    for i in range(n):
        seed = i32(seed * 1103515245 + 12345)
        a[i] = seed
    quicksort(a, 0, n - 1)
    checksum: int = 0
    for i in range(0, n, 997):
        checksum += a[i] * (i + 1)
    for i in range(1, n):
        if a[i - 1] > a[i]:
            checksum ^= 0xDEADBEEF
    return checksum


WORKLOADS = {
    "intArithmetic": int_arithmetic,
    "longArithmetic": long_arithmetic,
    "mathTranscendental": math_transcendental,
    "arraySequential": array_sequential,
    "arrayRandom": array_random,
    "objectAllocation": object_allocation,
    "valueEscape": value_escape,
    "hashMapChurn": hash_map_churn,
    "stringBuilding": string_building,
    "recursion": recursion,
    "quicksort": quicksort_bench,
}

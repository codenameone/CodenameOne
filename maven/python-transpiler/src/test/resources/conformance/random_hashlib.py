import hashlib
import random

for name in ["md5", "sha1", "sha224", "sha256", "sha384", "sha512"]:
    for msg in [b"", b"abc", b"a" * 200, bytes(range(256)) * 3]:
        print(name, len(msg), hashlib.new(name, msg).hexdigest())
h = hashlib.sha256()
h.update(b"hello ")
c = h.copy()
h.update(b"world")
print(h.hexdigest() == hashlib.sha256(b"hello world").hexdigest(), c.hexdigest(), h.name, h.digest_size,
      h.block_size)
print(hashlib.pbkdf2_hmac("sha256", b"password", b"salt", 2).hex())
print(hashlib.md5(b"x").digest())
try:
    hashlib.md5("text")
except TypeError as e:
    print("TypeError", e)

random.seed(42)
print([random.random() for _ in range(3)])
print([random.randint(1, 100) for _ in range(10)])
print(random.randrange(0, 1000, 7), random.randrange(10), random.getrandbits(100), random.getrandbits(5))
xs = list(range(20))
random.shuffle(xs)
print(xs)
print(random.choice("abcdef"), random.sample(range(100), 8), random.sample(range(10000), 5))
print(random.choices(["a", "b", "c"], k=6), random.choices(["a", "b", "c"], weights=[1, 0, 5], k=6))
print(random.uniform(1, 2), random.gauss(), random.gauss(), random.normalvariate(5, 2))
print(random.expovariate(2.0), random.triangular(0, 10, 3))
print(random.gammavariate(2.5, 1.0), random.betavariate(2, 3), random.paretovariate(3), random.weibullvariate(1, 2))
print(random.vonmisesvariate(1.0, 4.0), random.lognormvariate(0, 1), random.binomialvariate(20, 0.3),
      random.binomialvariate(1000, 0.4))
print(random.randbytes(6))
for s in [0, 1, -5, 2 ** 70 + 3, "hello", b"bytes", 3.5]:
    random.seed(s)
    print(repr(s), random.random(), random.randint(0, 10 ** 12))
r = random.Random(7)
st = r.getstate()
a = [r.random() for _ in range(3)]
r.setstate(st)
print(a == [r.random() for _ in range(3)], random.Random(7).random() == random.Random(7).random())
print(random.sample(["x", "y", "z"], 3, counts=None), random.Random(3).sample("aabbc", k=2))
print(random.Random(1).sample(["red", "blue"], counts=[4, 2], k=5))
for call in [lambda: random.choice([]), lambda: random.randrange(0), lambda: random.randrange(5, 5),
             lambda: random.randrange(0, 10, 0), lambda: random.sample(range(3), 4),
             lambda: random.sample({1, 2}, 1)]:
    try:
        call()
    except (ValueError, IndexError, TypeError) as e:
        print(type(e).__name__, e)

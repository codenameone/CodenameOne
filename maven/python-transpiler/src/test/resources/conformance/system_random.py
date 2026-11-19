# The platform's entropy behind os.urandom, secrets, uuid4 and SystemRandom:
# nothing here can print a value, only what every value must satisfy.
import os
import random
import secrets
import uuid

print(len(os.urandom(0)), len(os.urandom(1)), len(os.urandom(300)), type(os.urandom(4)).__name__)
print(os.urandom(16) != os.urandom(16))
try:
    os.urandom(-1)
except ValueError:
    print("ValueError")
r = random.SystemRandom()
print(all(0.0 <= r.random() < 1.0 for _ in range(200)))
print(r.getrandbits(0), all(r.getrandbits(k) < (1 << k) for k in (1, 7, 8, 9, 31, 32, 33, 64, 65, 200)))
print(max(r.getrandbits(70).bit_length() for _ in range(64)) > 60)
print(len(r.randbytes(5)), all(3 <= r.randint(3, 9) <= 9 for _ in range(100)))
print(sorted(r.sample(range(10), 10)) == list(range(10)), r.choice("a"))
r.seed(1)
a = r.getrandbits(64)
r.seed(1)
print(a != r.getrandbits(64))
for name in ("getstate", "setstate"):
    try:
        getattr(r, name)(*([None] if name == "setstate" else []))
    except NotImplementedError:
        print("NotImplementedError")
print(len(secrets.token_bytes(12)), len(secrets.token_hex(12)), secrets.randbelow(1), secrets.randbits(0))
u = uuid.uuid4()
print(u.version, u != uuid.uuid4())

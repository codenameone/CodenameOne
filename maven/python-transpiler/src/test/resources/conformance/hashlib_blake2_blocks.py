import hashlib

# Lengths on both sides of the 128 byte block of BLAKE2b and the 64 byte one
# of BLAKE2s, whole and fed in pieces.
data = bytes((i * 31 + 7) % 256 for i in range(700))
for n in (0, 1, 63, 64, 65, 127, 128, 129, 255, 256, 257, 700):
    print(n, hashlib.blake2b(data[:n]).hexdigest())
    print(n, hashlib.blake2s(data[:n]).hexdigest())

h = hashlib.blake2b(digest_size=16, key=b"a key", salt=b"salt", person=b"me")
for start in range(0, 700, 37):
    h.update(data[start:start + 37])
print(h.hexdigest())

h = hashlib.blake2b(digest_size=20)
h.update(data[:128])
c = h.copy()
h.update(data[128:300])
c.update(b"other")
print(h.hexdigest(), c.hexdigest())
print(hashlib.blake2b(b"x" * 100000, digest_size=32).hexdigest())

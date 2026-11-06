# uuid: parsing, fields, name-based versions, random/time versions, and errors.
import uuid
import sys
import string

u = uuid.UUID("{12345678-1234-5678-1234-567812345678}")
print(u, repr(u), u.hex, u.int, u.urn, u.version, u.variant)
print(u.fields, u.time_low, u.time_mid, u.clock_seq, u.node, u.bytes, u.bytes_le)
print(uuid.UUID(bytes=u.bytes) == u, uuid.UUID(bytes_le=u.bytes_le) == u, uuid.UUID(fields=u.fields) == u,
      uuid.UUID(int=u.int) == u, uuid.UUID("urn:uuid:12345678123456781234567812345678") == u)
print(uuid.uuid3(uuid.NAMESPACE_DNS, "python.org"), uuid.uuid5(uuid.NAMESPACE_DNS, "python.org"))
print(uuid.uuid3(uuid.NAMESPACE_URL, b"http://x"), uuid.uuid5(uuid.NAMESPACE_OID, "1.2.3"))
print(uuid.uuid8(1, 2, 3), uuid.NIL, uuid.MAX, uuid.NIL.version, uuid.MAX.variant)
for f in (uuid.uuid1, uuid.uuid4, uuid.uuid6, uuid.uuid7, uuid.uuid8):
    a, b = f(), f()
    print(f.__name__, a.version, a.variant, a != b, len(str(a)), int(a) == a.int)
s7 = [uuid.uuid7() for _ in range(50)]
print(s7 == sorted(s7), uuid.uuid1(node=0xabc, clock_seq=7).node, uuid.uuid1(clock_seq=7).clock_seq)
print(uuid.UUID(int=5, version=4), sorted([uuid.MAX, uuid.NIL, u])[1] == u, hash(u) == hash(uuid.UUID(str(u))))
print({u: 1}[uuid.UUID(u.hex)], u < uuid.MAX, uuid.uuid1().node == uuid.getnode())
for kw in [{"hex": "zz"}, {"bytes": b"ab"}, {"int": -1}, {"fields": (1, 2, 3)}, {"int": 1, "version": 9}, {}]:
    try:
        uuid.UUID(**kw)
    except (ValueError, TypeError) as e:
        print(type(e).__name__, e)
try:
    u.int = 3
except TypeError as e:
    print(e)
try:
    raise KeyError("k")
except KeyError:
    print(sys.exception(), sys.exc_info()[0].__name__)
print(sys.exception(), sys.exc_info())
fm = string.Formatter()
print(fm.format("{0}-{name!r:>6}-{x[1]}-{y.real}", 1, name="n", x=[7, 8], y=3), list(fm.parse("a{b:c}{{d}}")))

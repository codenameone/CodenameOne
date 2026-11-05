# base64 and binascii.
import base64
import binascii

data = [b"", b"f", b"fo", b"foo", b"foob", b"fooba", b"foobar", bytes(range(256))[::7], b"\xff\xfe\xfd"]
for d in data:
    e = base64.b64encode(d)
    print(e, base64.b64decode(e) == d, base64.urlsafe_b64encode(d), base64.b32encode(d), base64.b32hexencode(d),
          base64.b16encode(d), base64.b85encode(d), base64.a85encode(d))
    assert base64.b32decode(base64.b32encode(d)) == d
    assert base64.b32hexdecode(base64.b32hexencode(d)) == d
    assert base64.b16decode(base64.b16encode(d)) == d
    assert base64.b85decode(base64.b85encode(d)) == d
    assert base64.a85decode(base64.a85encode(d)) == d
    assert base64.urlsafe_b64decode(base64.urlsafe_b64encode(d)) == d
print(base64.b64encode(b"\xfb\xff", altchars=b"-_"), base64.b64decode("+/8=", altchars="-_"), base64.b64decode("Zm9v\nYmFy"))
print(base64.b64decode(b"Zm9vYg"[:4]), base64.b85encode(b"abc", pad=True), base64.a85encode(b"    ", foldspaces=True),
      base64.a85encode(b"\0\0\0\0hello", adobe=True), base64.a85decode(b"<~87cURD]i,\"Ebo80~>", adobe=True))
print(base64.z85encode(b"\x86\x4f\xd2\x6f\xb5\x59\xf7\x5b"), base64.z85decode(b"HelloWorld"))
print(base64.encodebytes(b"x" * 100), base64.decodebytes(b"eHh4\n"), base64.b32decode("mzxw6===", casefold=True))
for bad in ["Zm9vY", "Zm9", "a===="]:
    try:
        base64.b64decode(bad)
    except binascii.Error as e:
        print("Error:", e)
try:
    base64.b64decode("Zm9v!", validate=True)
except binascii.Error as e:
    print(e)
try:
    base64.b64encode("text")
except TypeError as e:
    print(e)
try:
    base64.b16decode("zz")
except binascii.Error as e:
    print(e)
print(binascii.hexlify(b"\x01\xab"), binascii.hexlify(b"\x01\xab\xcd", "-"), binascii.unhexlify("01ab"), binascii.b2a_base64(b"hi"))
print(binascii.crc32(b"hello world"), binascii.crc32(b"world", binascii.crc32(b"hello ")), binascii.crc_hqx(b"hello", 0))
try:
    binascii.unhexlify("abc")
except binascii.Error as e:
    print(e)
print(issubclass(binascii.Error, ValueError))

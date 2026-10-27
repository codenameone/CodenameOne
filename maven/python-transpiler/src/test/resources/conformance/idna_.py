import idna

for s in ["example.com", "Example.COM", "münchen.de", "bücher.example",
          "日本語.jp", "ex--ample.com", "-ab.com", "ab-.com", "a..b", "",
          "a" * 64 + ".com", "Bücher.de", "ΑΒΓ.gr", "ab。cd",
          "xn--mnchen-3ya.de", "xn--zzz.com", "a b.com", ".com", "a.", "ß.de",
          "faß.de", "例え.テスト", "a_b.com", "ab.12", "1.2.3.4",
          "буква.рф", "é.fr", "café.example.org."]:
    try:
        print(ascii(s)[:40], idna.encode(s))
    except idna.IDNAError as e:
        print(ascii(s)[:40], type(e).__name__, e)

for s in ["xn--mnchen-3ya.de", "xn--wgv71a119e.jp", "XN--MNCHEN-3YA.de", "xn--.com", "abc.def", "xn--zzz.com", "ab..cd", "xn--bcher-kva.example",
          "xn--r8jz45g.xn--zckzah", "xn--e1afmkfd.xn--p1ai", "Example.COM", "xn--caf-dma.example.org."]:
    try:
        print(s, ascii(idna.decode(s)))
    except idna.IDNAError as e:
        print(s, type(e).__name__, e)

print(idna.alabel("münchen"), ascii(idna.ulabel("xn--mnchen-3ya")), idna.ulabel("abc"))
print(idna.encode("münchen.de", uts46=True), idna.encode("MÜNCHEN.de", uts46=True))
print(issubclass(idna.IDNAError, UnicodeError), issubclass(idna.InvalidCodepoint, idna.IDNAError))
print(idna.encode(b"abc.com"), ascii(idna.decode(b"xn--bcher-kva.de")))
for host in ["中文.com", "مثال.إختبار", "हिन्दी.com"]:
    try:
        e = idna.encode(host)
        print(e, idna.decode(e) == host)
    except idna.IDNAError as ex:
        print("err", type(ex).__name__)

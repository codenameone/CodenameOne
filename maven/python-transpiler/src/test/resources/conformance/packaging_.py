from packaging.version import Version, parse, InvalidVersion
from packaging.specifiers import SpecifierSet, Specifier, InvalidSpecifier
from packaging.utils import canonicalize_name

for s in ["1.0", "1.0.0", "v1.2.3", "1!2.0", "1.0a1", "1.0alpha1", "1.0-beta.2", "1.0c3", "1.0rc", "1.0pre4",
          "1.0.post1", "1.0-1", "1.0.rev3", "1.0.dev4", "1.0.dev", "1.0a1.post2.dev3", "1.0+local.1",
          "1.0+Ubuntu-1_2", "2.0.0.0", "  3.1  ", "1.0.POST1", "1.0_a_1", "1.0.0rc1+abc.5"]:
    v = Version(s)
    print(repr(s), str(v), v.epoch, v.release, v.pre, v.post, v.dev, v.local, v.public, v.base_version,
          v.is_prerelease, v.is_postrelease, v.is_devrelease, v.major, v.minor, v.micro)

for s in ["", "abc", "1.0.", "1..0", "1.0+", "1.0+a..b", "1.0 beta", "1.0.0.x", "!1", "1.0-"]:
    try:
        Version(s)
        print("valid", repr(s))
    except InvalidVersion as e:
        print("InvalidVersion", e)

order = ["1.0.dev1", "1.0a1.dev1", "1.0a1", "1.0a2", "1.0b1", "1.0rc1", "1.0", "1.0+abc", "1.0+abc.1",
         "1.0+5", "1.0.post1.dev1", "1.0.post1", "1.0.1", "1.1", "2.0", "1!0.5"]
vs = [Version(x) for x in order]
print([str(v) for v in sorted(reversed(vs))])
print(all(vs[i] < vs[i + 1] for i in range(len(vs) - 1)))
print(Version("1.0") == Version("1.0.0"), Version("1.0") == Version("1.0.0.0.0"), hash(Version("1.0")) == hash(Version("1.0.0")))
print(Version("1.0") != Version("1.0.1"), Version("2.0") > Version("1.9.9"), Version("1.0") >= Version("1.0"), Version("1.0a1") <= Version("1.0"))
print(parse("1.2.3"), repr(parse("1.2.3")), max(Version("1.2"), Version("1.10")))
print(Version("1.0") == "1.0", Version("1.0") < Version("1.0.1"))
print(canonicalize_name("Foo_Bar.baz--Qux"))

cases = [(">=1.0", ["0.9", "1.0", "1.5", "2.0a1"]),
         (">1.0", ["1.0", "1.0.post1", "1.0.1", "1.0+local"]),
         ("<2.0", ["1.9", "2.0", "2.0a1", "1.0a1"]),
         ("<=2.0", ["2.0", "2.0.0", "2.0.1", "2.0+x"]),
         ("==1.0", ["1.0", "1.0.0", "1.0+local", "1.0.1"]),
         ("==1.0+local", ["1.0", "1.0+local", "1.0+other"]),
         ("==1.*", ["1.0", "1.9.9", "2.0", "1.0a1", "1"]),
         ("==1.2.*", ["1.2", "1.2.5", "1.3"]),
         ("!=1.0", ["1.0", "1.0.0", "1.1"]),
         ("!=1.*", ["1.5", "2.0"]),
         ("~=2.2", ["2.1", "2.2", "2.9", "3.0"]),
         ("~=1.4.5", ["1.4.4", "1.4.5", "1.4.9", "1.5.0"]),
         ("===1.0", ["1.0", "1.0.0"]),
         (">=1.0a1", ["1.0a1", "1.0a2", "0.9"]),
         ("<1.0", ["1.0a1", "0.9"]),
         ]
for spec, items in cases:
    sp = Specifier(spec)
    print(str(sp), repr(sp), sp.operator, sp.version, sp.prereleases, [(i, sp.contains(i)) for i in items])

s = SpecifierSet(">=1.0, <2.0, !=1.5")
print(s, repr(s), len(s), sorted([str(x) for x in s]))
print([v for v in ["0.9", "1.0", "1.5", "1.6", "2.0", "1.7a1"] if v in s])
print(list(s.filter(["0.9", "1.0", "1.5", "1.6", "2.0", "1.7a1"])))
print(list(s.filter(["1.7a1"])), list(s.filter(["1.7a1", "1.8"])), list(s.filter(["1.7a1"], prereleases=True)))
print(s.contains("1.7a1", prereleases=True), s.prereleases, SpecifierSet("").prereleases)
print(SpecifierSet("") == SpecifierSet(""), SpecifierSet(">=1,<2") == SpecifierSet("<2, >=1"))
print(SpecifierSet(">=1") & SpecifierSet("<2"), SpecifierSet(">=1.0a1").prereleases)
print(SpecifierSet("").contains("1.0"), SpecifierSet("").contains("1.0a1"), SpecifierSet("").contains("junk"))
print(Version("1.5") in SpecifierSet(">=1.0"))
print(SpecifierSet(">=1.0", prereleases=True).contains("1.1a1"))

for bad in ["1.0", ">=", "~=1", "==1.*.x", ">=1.0.*", "abc", "=>1.0", "==1.0.dev1.*"]:
    try:
        Specifier(bad)
        print("valid", repr(bad))
    except InvalidSpecifier as e:
        print("InvalidSpecifier", e)
try:
    SpecifierSet(">=1.0,foo")
except InvalidSpecifier as e:
    print("InvalidSpecifier", e)

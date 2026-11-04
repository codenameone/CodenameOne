typedef IntFn = int Function(int);
typedef Opt = int Function(int, [int]);

int twice(int x) => x * 2;
int addOpt(int a, [int b = 1]) => a + b;
int named(int a, {int b = 3}) => a + b;
T pick<T>(T v) => v;
untypedTop(x) => x;

class C {
  static int plain(int x) => x + 1;
  static int opt(int a, [int b = 5]) => a + b;
  static int nam(int a, {int b = 7}) => a + b;
  static T gen<T>(T v) => v;
  static noRet(x) => x;
}

void keep(IntFn f, Set<Object> into) => into.add(f);
int Function(int) asInline() => twice;

void main() {
  print('opt == ${C.opt == C.opt} identical ${identical(C.opt, C.opt)}');
  print('nam == ${C.nam == C.nam} identical ${identical(C.nam, C.nam)}');
  print('gen == ${C.gen == C.gen} identical ${identical(C.gen, C.gen)}');
  print('noRet == ${C.noRet == C.noRet} identical ${identical(C.noRet, C.noRet)}');
  print('plain == ${C.plain == C.plain} identical ${identical(C.plain, C.plain)}');
  print('top addOpt ${addOpt == addOpt} ${identical(addOpt, addOpt)}');
  print('top named ${named == named} ${identical(named, named)}');
  print('top pick ${identical(pick, pick)}');
  print('top untyped ${untypedTop == untypedTop} ${identical(untypedTop, untypedTop)}');
  print('opt != plain ${C.opt == C.plain}');

  // Views of one function: as a typedef, as Function, as Object.
  IntFn a = twice;
  Function b = twice;
  Object o = twice;
  print('views eq ${a == b} ${b == o} ${identical(a, o)}');
  print('views hash ${a.hashCode == b.hashCode} ${b.hashCode == o.hashCode}');
  final set = <Object>{a, b, o, twice};
  print('set size ${set.length}');
  final map = <Object, String>{};
  map[a] = 'a';
  map[b] = 'b';
  map[o] = 'o';
  print('map ${map.length} ${map[twice]}');

  Opt oa = addOpt;
  Function ob = addOpt;
  print('opt views ${oa == ob} ${oa.hashCode == ob.hashCode} ${<Object>{oa, ob, addOpt}.length}');
  Function sa = C.opt;
  Object sb = C.opt;
  print('static opt views ${sa == sb} ${sa.hashCode == sb.hashCode} ${<Function>{sa, C.opt}.length}');
  print('static calls ${C.opt(1)} ${C.nam(1, b: 2)} ${C.gen<String>("g")} ${C.noRet(4)} ${oa(2)}');

  // Signed views (a typedef that is tested) next to plain ones.
  final IntFn signed = twice;
  print('is ${signed is IntFn} ${(twice as Object) is IntFn}');
  final keys = <Object>{};
  keep(twice, keys);
  keys.add(asInline());
  keys.add(signed);
  final Object plainObj = twice;
  keys.add(plainObj);
  print('mixed set ${keys.length} ${keys.contains(twice)}');
  final m = <Function, int>{signed: 1};
  m[asInline()] = 2;
  m[plainObj as Function] = 3;
  print('mixed map ${m.length} ${m[twice]}');
  print('mixed hash ${signed.hashCode == asInline().hashCode} ${plainObj.hashCode == signed.hashCode}');
  print('mixed eq ${signed == asInline()} ${identical(plainObj, signed)}');

}

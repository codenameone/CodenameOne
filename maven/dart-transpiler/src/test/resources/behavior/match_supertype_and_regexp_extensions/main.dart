// Shapes the pure-Dart packages lean on: Match as a supertype, a private
// extension on RegExpMatch, Match.pattern.

extension on RegExpMatch {
  String firstWord() => this[1] ?? '';
}

int spanEnd(Match m) => m.end;

String describe(Match m) =>
    '${m.start}-${m.end} ${m[0]} groups=${m.groupCount} in "${m.input}"';

const bool kFlag = bool.fromEnvironment('cn1.pure.flag');
const int kSize = int.fromEnvironment('cn1.pure.size', defaultValue: 7);

int prefixLength(Pattern p, String s) => p.matchAsPrefix(s)?.end ?? -1;

void main() {
  print('$kFlag $kSize');
  print(prefixLength('ab', 'abc'));
  print(prefixLength(RegExp(r'\d+'), '123x'));
  print(prefixLength('zz', 'abc'));
  print(Uri.encodeFull('http://h/a b?q=1&r=\u00e9#f'));
  print(Uri.decodeFull('a%20b%2Fc'));
  print(Uri.splitQueryString('a=1&b=x%20y&c'));
  print(Uri.base.scheme);

  final re = RegExp(r'(\w+)\s(\d+)');
  final m = re.firstMatch('item alpha 42 rest')!;
  print(describe(m));
  print(m.firstWord());
  print(spanEnd(m));
  print(m.groups([1, 2]));
  print(identical(m.pattern, re));
  Match? lit = 'banana'.allMatches('xbananay').first;
  print('${lit.start} ${lit.pattern}');
  for (final Match g in RegExp(r'a').allMatches('aXa')) {
    print(g.start);
  }
}

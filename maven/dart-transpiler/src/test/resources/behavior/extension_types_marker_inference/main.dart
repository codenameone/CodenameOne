// The extension-type marker must survive inferred and re-derived types: each line
// calls a member only the extension type declares.

extension type Id(int value) {
  String describe() => 'id#$value';
}

T pick<T>(T a, T b) => a;
Future<Id> later(int v) async => Id(v);

void t(String n, String Function() f) {
  try {
    print('$n ${f()}');
  } catch (e) {
    print('$n FAIL');
  }
}

void main() async {
  var ids = [Id(3), Id(4)];
  List<Id> typed = [Id(5)];
  Id? maybe;
  var flag = ids.length > 1;
  t('pick', () => pick(Id(6), Id(7)).describe());
  Id q = Id(6);
  var pv = pick(Id(6), Id(7));
  t('pickvar', () => pv.describe());
  t('pickq', () => pick(q, q).describe());
  t('pickexplicit', () => pick<Id>(q, q).describe());
  t('first', () => ids.first.describe());
  t('typedfirst', () => typed.first.describe());
  t('index', () => ids[0].describe());
  t('last', () => typed.last.describe());
  t('where', () => typed.where((e) => true).first.describe());
  for (final x in ids) {
    t('for', () => x.describe());
  }
  for (var x in typed) {
    t('fortyped', () => x.describe());
  }
  t('qq', () => (maybe ?? Id(9)).describe());
  t('cond', () => (flag ? ids[0] : ids[1]).describe());
  final aw = await later(12);
  t('awaitvar', () => aw.describe());
  t('map', () => ids.map((e) => e.describe()).join(','));
  var m = <String, Id>{'a': Id(1)};
  t('mapidx', () => m['a']!.describe());
  t('values', () => m.values.first.describe());
}

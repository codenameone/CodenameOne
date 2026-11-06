import 'dart:async';

class User {
  final int id;
  final String name;
  User(this.id, this.name);
  String toString() => 'User($id, $name)';
}

class Api {
  final Map<int, User> _users = {1: User(1, 'ann'), 2: User(2, 'bob')};

  Future<List<Map<String, dynamic>>> fetch() async => [{'id': 1}, {'id': 2}];
  Future<Map<String, Object?>> one() async { return {'a': 1}; }
  Future<List<num>> nums() async => [1, 2];
  Future<Map<String, int>> counts() async => {'x': 1, 'y': 2};
  Future<User?> find(int id) async => _users[id];
  Future<User?> findBlock(int id) async {
    if (id < 0) return null;
    return _users[id];
  }
  Future<List<User>> all() async => _users.values.toList();
  Future<Iterable<num>> mixed() async => [1, 2.5, -3].where((n) => n > 0);
  Future<Iterable<int>> ids() async => [3, 4];
  Future<void> save(User u) async { _users[u.id] = u; }
  Future<void> touch() async => print('touched');
  FutureOr<int> cached(bool hit) async => hit ? 1 : 2;
  Future<int> viaFutureOr() async { FutureOr<int> v = 3; return v; }
  Future<String?> maybe(bool b) async => b ? 'yes' : null;
  Future<List<Map<String, dynamic>>> nested() async {
    var rows = await fetch();
    return rows.map((r) => {...r, 'seen': true}).toList();
  }
}

void main() async {
  var a = Api();
  print((await a.fetch()).length);
  print(await a.one());
  print(await a.nums());
  print(a.nums() is Future<List<int>>);
  print(a.nums() is Future<List<num>>);
  print(await a.counts());
  print(await a.find(1));
  print(await a.find(9));
  print(await a.findBlock(-1));
  print(await a.all());
  print(await a.mixed());
  await a.save(User(3, 'cy'));
  print(await a.find(3));
  await a.touch();
  print(await a.cached(true));
  print(await a.viaFutureOr());
  print(await a.maybe(true));
  print(await a.maybe(false));
  print(await a.nested());
  print(a.fetch() is Future<List<Map<String, dynamic>>>);
  print(await a.ids());
}

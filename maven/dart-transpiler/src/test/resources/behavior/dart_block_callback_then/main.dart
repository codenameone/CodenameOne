class User {
  String name = 'a';
}

Future<User> load() async => User();

Future<void> main() async {
  await load().then((u) {
    print(u.name);
  });
  final n = await Future.value(2).then((v) {
    if (v > 1) {
      return;
    }
    print('small');
  });
  print(n);
  await Future<String>.error('bad').catchError((e) {
    print('caught $e');
    return 'x';
  });
}

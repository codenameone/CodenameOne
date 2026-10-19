import 'dart:convert';

class User {
  final String name;
  final int age;
  final List<String> tags;
  User(this.name, this.age, this.tags);

  factory User.fromJson(Map<String, dynamic> json) => User(
        json['name'] as String,
        json['age'] as int,
        (json['tags'] as List<dynamic>).map((e) => e as String).toList(),
      );

  Map<String, dynamic> toJson() => {'name': name, 'age': age, 'tags': tags};
}

void main() {
  final raw = '{"name":"Ada","age":36,"tags":["math","code"],"score":9.5,"ok":true,"none":null}';
  final map = jsonDecode(raw) as Map<String, dynamic>;
  final u = User.fromJson(map);
  print('${u.name} ${u.age} ${u.tags.length}');
  print(map['score']);
  print(map['ok']);
  print(map['none']);
  print(jsonEncode(u));
  print(jsonEncode([1, 2.5, 'a"b', null, {'k': false}]));
  print(const JsonEncoder.withIndent('  ').convert({'a': [1]}));
  final bytes = utf8.encode('hé');
  print(bytes.length);
  print(utf8.decode(bytes));
  final b64 = base64Encode(utf8.encode('hello'));
  print(b64);
  print(utf8.decode(base64Decode(b64)));
  print(LineSplitter.split('a\nb\r\nc').length);
  try {
    jsonDecode('{bad');
  } on FormatException catch (e) {
    print('format error');
  }
}

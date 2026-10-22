// shared_preferences and path_provider, headless: values keep their types, a
// missing or differently typed key reads null, and the app directories exist and
// can hold files.
import 'dart:io';

import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';

Future<void> main() async {
  SharedPreferences.setMockInitialValues({'seen': true});
  final prefs = await SharedPreferences.getInstance();
  print('seen ${prefs.getBool('seen')}');
  await prefs.setString('name', 'Biscuit');
  await prefs.setInt('count', 3);
  await prefs.setDouble('ratio', 0.5);
  await prefs.setStringList('favorites', ['1', '2']);
  final again = await SharedPreferences.getInstance();
  print('${again.getString('name')} ${again.getInt('count')} ${again.getDouble('ratio')} '
      '${again.getStringList('favorites')}');
  print('wrong type ${again.getInt('name')} missing ${again.getString('nope')}');
  print('keys ${(again.getKeys().toList()..sort()).join(',')}');
  final count = (again.getInt('count') ?? 0) + 1;
  await again.setInt('count', count);
  print('count $count ${again.containsKey('count')}');
  await again.remove('name');
  print('removed ${again.getString('name')} ${again.containsKey('name')}');

  final docs = await getApplicationDocumentsDirectory();
  final file = File('${docs.path}/notes.txt');
  await file.writeAsString('hello');
  print('docs file ${await file.readAsString()}');
  final support = await getApplicationSupportDirectory();
  print('support exists ${await support.exists()}');
  final tmp = await getTemporaryDirectory();
  print('tmp exists ${tmp.existsSync()}');
}

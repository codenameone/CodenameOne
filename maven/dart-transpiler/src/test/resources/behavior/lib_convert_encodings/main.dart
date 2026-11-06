import 'dart:async';
import 'dart:convert';

Future<void> main() async {
  final enc = Encoding.getByName('UTF-8');
  print('byName ${enc?.name} ${Encoding.getByName('latin1')?.name} ${Encoding.getByName('nope')}');
  print('escape ${htmlEscape.convert('<a href="x">Tom & Jerry\'s</a>')}');
  print('escape2 ${const HtmlEscape().convert('plain')}');
}

// Value behaviour behind matrices, tab bars, spans, autocomplete and button styles.
//
// A Matrix4's storage is the matrix itself, so writing an element through it moves
// the matrix. A text-only TabBar is the tab height plus the indicator weight tall.
// Spans answer what they were built with. Autocomplete shows what its options
// builder answers, and merging a ButtonStyle with null answers the same style.
import 'package:flutter/material.dart';

Iterable<String> filter(TextEditingValue v) {
  const all = ['apple', 'banana', 'grape', 'pineapple'];
  if (v.text.isEmpty) return const <String>[];
  return all.where((o) => o.contains(v.text.toLowerCase()));
}

void main() {
  final m = Matrix4.identity();
  final s = m.storage;
  s[12] = 5;
  s[13] = 7;
  print('translation: ${m.storage[12]} ${m.storage[13]}');
  final t = Matrix4.translationValues(1, 2, 3);
  print('translation storage: ${t.storage[12]} ${t.storage[13]} ${t.storage[14]}');

  final bar = TabBar(tabs: const [Tab(text: 'one'), Tab(text: 'two')]);
  print('tab bar height: ${bar.preferredSize.height}');

  const span = TextSpan(text: 'root', children: [TextSpan(text: 'a'), TextSpan(text: 'b')]);
  print('span text: ${span.text}');
  print('span children: ${span.children!.length}');
  print('span style null: ${span.style == null}');

  print('pine: ${filter(const TextEditingValue(text: 'pine')).toList()}');
  print('ap: ${filter(const TextEditingValue(text: 'AP')).toList()}');
  print('empty: ${filter(const TextEditingValue(text: '')).toList()}');

  final style = ElevatedButton.styleFrom(backgroundColor: Colors.red);
  print('merge null same: ${identical(style.merge(null), style)}');
  print('merged is style: ${style.merge(TextButton.styleFrom(foregroundColor: Colors.blue)) is ButtonStyle}');
}

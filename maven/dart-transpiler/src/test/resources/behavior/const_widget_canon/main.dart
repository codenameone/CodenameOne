import 'package:flutter/material.dart';

class Badge extends StatelessWidget {
  final String label;
  const Badge(this.label, {super.key});
  @override
  Widget build(BuildContext context) => Text(label);
}

class Counter extends StatefulWidget {
  const Counter({super.key});
  @override
  State<Counter> createState() => _CounterState();
}

class _CounterState extends State<Counter> {
  @override
  Widget build(BuildContext context) => const Badge('n');
}

void main() {
  print(identical(const Badge('a'), const Badge('a')));
  print(identical(const Badge('a'), const Badge('b')));
  print(identical(const Badge('a', key: ValueKey(1)), const Badge('a', key: ValueKey(1))));
  print(identical(const Badge('a', key: ValueKey(1)), const Badge('a', key: ValueKey(2))));
  print(identical(const Badge('a', key: ValueKey(1)), const Badge('a')));
  print(identical(const Counter(), const Counter()));
  print(identical(Badge('a'), Badge('a')));
}

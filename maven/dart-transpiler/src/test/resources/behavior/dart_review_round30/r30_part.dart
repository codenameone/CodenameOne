part of 'main.dart';

class PartThing {
  final String n;
  PartThing(this.n);
  String describe() => 'part:' + n + _secret;
}

int partFunction(int x) => x * _factor;

import 'main.dart';
class Sub extends P { Sub() : super('s'); String? get _p => null; }
class Impl implements P { dynamic noSuchMethod(Invocation i) => null; }

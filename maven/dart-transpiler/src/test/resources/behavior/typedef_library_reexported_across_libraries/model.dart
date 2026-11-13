import 'types.dart';
class Pet { final String name; Pet(this.name); Json toJson() => {'name': name}; static Pet from(Json j) => Pet(j['name'] as String); }

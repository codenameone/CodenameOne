import 'package:flutter/material.dart';
class MyColor extends Color { MyColor() : super(0xFF112233); get value => 7; }
class MyColor2 extends Color { MyColor2() : super(5); int get value => super.value + 1; }
class Base { get value => 1; }
class Shady extends MaterialColorHolder {}
class MaterialColorHolder { get shade => Colors.red; }
void main() {
  Color c = MyColor(); print(c.value + 1); print(MyColor().value);
  Color c2 = MyColor2(); print(c2.value); print(c2.value == MyColor2().value);
  print(identical(c.value, c.value));
  print(Shady().shade == Colors.red);
}

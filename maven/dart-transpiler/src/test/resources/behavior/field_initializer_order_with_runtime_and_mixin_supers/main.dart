import 'dart:collection';

int counter = 0;
int next(String tag) {
  counter++;
  print('init $tag $counter');
  return counter;
}

String msg(String tag) {
  print('arg $tag');
  return tag;
}

// Program chain rooted in a runtime (dart:collection) class.
class Seq extends IterableBase<int> {
  int first0 = next('seq.first');
  final int limit;
  Seq(int n) : limit = next('seq.limit') + n, super() {
    print('Seq body sees ${describe()}');
  }
  String describe() => 'Seq first0=$first0 limit=$limit';
  @override
  Iterator<int> get iterator => <int>[first0, limit].iterator;
}

class DeepSeq extends Seq {
  int depth = next('deep.depth');
  final int arg;
  DeepSeq() : arg = next('deep.list'), super(next('deep.superArg')) {
    print('DeepSeq body depth=$depth');
  }
  @override
  String describe() => 'DeepSeq depth=$depth arg=$arg first0=$first0 limit=$limit';
}

// Mixins with fields over a program base.
mixin Tagged {
  int tag = next('mixin.tag');
  String describeTag() => 'tag=$tag';
}

mixin Counted {
  int count = next('mixin.count');
}

class Root {
  int rootField = next('root.field');
  Root(int seed) {
    print('Root body seed=$seed sees ${describe()}');
  }
  String describe() => 'Root';
}

class Mixed extends Root with Tagged, Counted {
  int own = next('mixed.own');
  final int fromList;
  Mixed() : fromList = next('mixed.list'), super(next('mixed.superArg')) {
    print('Mixed body own=$own ${describeTag()} count=$count');
  }
  @override
  String describe() => 'Mixed own=$own fromList=$fromList tag=$tag count=$count';
}

class MixedChild extends Mixed {
  int child = next('child.field');
  MixedChild() : super() {
    print('MixedChild body child=$child');
  }
}

// Factory-only class and factories delegating to generative/redirecting ctors.
class Shape extends Root {
  int sides = next('shape.sides');
  Shape._(int s) : super(s) {
    print('Shape._ body sides=$sides');
  }
  Shape.redirect() : this._(next('shape.redirectArg'));
  factory Shape(bool viaRedirect) {
    print('Shape factory');
    return viaRedirect ? Shape.redirect() : Shape._(next('shape.directArg'));
  }
  @override
  String describe() => 'Shape sides=$sides';
}

class Square extends Shape {
  int edge = next('square.edge');
  Square() : super._(4) {
    print('Square body edge=$edge');
  }
}

abstract class Maker {
  factory Maker() = _MadeImpl;
  String made();
}

class _MadeImpl extends Root implements Maker {
  int value = next('made.value');
  _MadeImpl() : super(0);
  @override
  String made() => 'made value=$value';
  @override
  String describe() => 'MadeImpl value=$value';
}

void main() {
  print(Seq(10).toList());
  var d = DeepSeq();
  print('${d.describe()} ${d.toList()}');
  var m = Mixed();
  print(m.describe());
  var mc = MixedChild();
  print(mc.describe());
  print(Shape(true).describe());
  print(Shape(false).describe());
  print(Square().describe());
  print(Maker().made());
}

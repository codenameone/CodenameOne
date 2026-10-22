void main() {
  var a = [1, 2, 3];
  var i = 0;
  a[i]++;
  print(a[i]--);
  print(++a[1]);
  var m = <String, int>{'k': 5};
  m['k']--;
  print(m['k']);
  print(a);
  dyn();
}

var calls = 0;
dynamic box = {'x': 1};
dynamic getBox() { calls++; return box; }

void dyn() {
  dynamic d = [10, 20, 30];
  var i = 0;
  d[i++] += 1;
  print('$d $i');
  print(d[1]++);
  print(d);
  getBox()['x'] += 1;
  print('${box['x']} $calls');
}

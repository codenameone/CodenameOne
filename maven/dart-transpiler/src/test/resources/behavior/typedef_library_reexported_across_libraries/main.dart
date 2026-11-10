import 'repo.dart';
import 'model.dart';
void main() {
  final r = Repo();
  r.add(Pet('rex')); r.add(Pet('tom'));
  r.each((Json j) => print(Pet.from(j).name));
  Ids x = r.ids;
  print(x);
  Handler h = (j) => print(j.keys.toList());
  h(r.rows.first);
}

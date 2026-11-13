import 'types.dart';
import 'model.dart';
export 'types.dart';
class Repo { final List<Json> rows = []; Ids ids = []; void add(Pet p) { rows.add(p.toJson()); ids.add(ids.length); } void each(Handler h) { for (final r in rows) h(r); } }

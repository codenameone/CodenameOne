// dart:developer: log is its own function, not dart:math's, whichever way the
// library is imported; the timeline runs what it times.
import 'dart:developer' as developer;
import 'dart:developer' show Timeline, TimelineTask;
import 'dart:math';

void main() {
  developer.log('hello', name: 'app');
  developer.log('bad', name: 'app', level: 1000, error: StateError('x'));
  print(log(1.0).round());
  final r = Timeline.timeSync('t', () => 41 + 1);
  print(r);
  Timeline.startSync('frame', arguments: {'n': 1});
  Timeline.finishSync();
  final task = TimelineTask()..start('load');
  task.finish();
  print(developer.inspect('seen'));
  print(developer.debugger(when: false));
}

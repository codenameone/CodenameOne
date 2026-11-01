class Timer {
  static const String periodicName = 'periodic';
  final int ticks;
  Timer(this.ticks);
  factory Timer.zero() => Timer(0);
}

class Config {
  final String name;
  Config(this.name);
}

String describeSettings() => 'settings ${Config('s').name}';

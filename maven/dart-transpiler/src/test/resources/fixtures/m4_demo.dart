import 'package:flutter/material.dart';

void main() {
  runApp(const M4App());
}

class M4App extends StatefulWidget {
  const M4App({super.key});

  @override
  State<M4App> createState() => _M4AppState();
}

class _M4AppState extends State<M4App> {
  bool _dark = false;

  void _toggle(bool v) {
    setState(() {
      _dark = v;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'M4 Demo',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      darkTheme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: Colors.deepPurple,
          brightness: Brightness.dark,
        ),
        useMaterial3: true,
      ),
      themeMode: _dark ? ThemeMode.dark : ThemeMode.light,
      home: ThemePage(_dark, _toggle),
    );
  }
}

class ThemePage extends StatelessWidget {
  final bool dark;
  final BoolCallback onModeChanged;

  const ThemePage(this.dark, this.onModeChanged, {super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('M4 Theming')),
      body: Padding(
        padding: EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            Row(
              children: <Widget>[
                Switch(value: dark, onChanged: onModeChanged),
                const Text('Dark mode'),
              ],
            ),
            SizedBox(height: 16.0),
            RichText(
              text: TextSpan(
                text: 'Flutter ',
                style: TextStyle(fontSize: 20.0),
                children: <TextSpan>[
                  TextSpan(
                    text: 'rich text',
                    style: TextStyle(fontWeight: FontWeight.bold, color: Colors.deepPurple),
                  ),
                  TextSpan(text: ' running on '),
                  TextSpan(
                    text: 'Codename One',
                    style: TextStyle(fontWeight: FontWeight.bold),
                  ),
                  TextSpan(text: ' with per-span styles that wrap across lines.'),
                ],
              ),
            ),
            SizedBox(height: 16.0),
            Card(
              child: Padding(
                padding: EdgeInsets.all(12.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    const Text('MediaQuery'),
                    Text('size: ${MediaQuery.of(context).size.width} x ${MediaQuery.of(context).size.height}'),
                    Text('dpr: ${MediaQuery.of(context).devicePixelRatio}'),
                  ],
                ),
              ),
            ),
            SizedBox(height: 16.0),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: <Widget>[
                ElevatedButton(onPressed: () {}, child: const Text('Elevated')),
                OutlinedButton(onPressed: () {}, child: const Text('Outlined')),
                TextButton(onPressed: () {}, child: const Text('Text')),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

import 'package:flutter/material.dart';

void main() {
  runApp(const M3App());
}

class M3App extends StatelessWidget {
  const M3App({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'M3 Demo',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const FormPage(),
    );
  }
}

class FormPage extends StatefulWidget {
  const FormPage({super.key});

  @override
  State<FormPage> createState() => _FormPageState();
}

class _FormPageState extends State<FormPage> {
  final TextEditingController _name = TextEditingController(text: 'World');
  bool _subscribe = true;
  bool _dark = false;
  double _volume = 0.4;
  String _status = 'idle';

  Future<void> _save() async {
    setState(() {
      _status = 'saving...';
    });
    await Future.delayed(Duration(milliseconds: 900));
    setState(() {
      _status = 'saved';
    });
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('Saved ${_name.text}')),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('M3 Inputs')),
      body: Padding(
        padding: EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            TextField(
              controller: _name,
              decoration: InputDecoration(labelText: 'Name', hintText: 'Enter a name'),
            ),
            SizedBox(height: 12.0),
            Row(
              children: <Widget>[
                Checkbox(
                  value: _subscribe,
                  onChanged: (v) {
                    setState(() {
                      _subscribe = v;
                    });
                  },
                ),
                const Text('Subscribe'),
                SizedBox(width: 24.0),
                Switch(
                  value: _dark,
                  onChanged: (v) {
                    setState(() {
                      _dark = v;
                    });
                  },
                ),
                const Text('Dark'),
              ],
            ),
            Slider(
              value: _volume,
              min: 0.0,
              max: 1.0,
              onChanged: (v) {
                setState(() {
                  _volume = v;
                });
              },
            ),
            Text('Volume: $_volume  status: $_status'),
            SizedBox(height: 12.0),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: <Widget>[
                ElevatedButton(
                  onPressed: _save,
                  child: const Text('Save'),
                ),
                OutlinedButton(
                  onPressed: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(builder: (context) => DetailPage(_name.text)),
                    );
                  },
                  child: const Text('Details'),
                ),
                TextButton(
                  onPressed: () {
                    showDialog(
                      context: context,
                      builder: (context) {
                        return AlertDialog(
                          title: const Text('About'),
                          content: const Text('Flutter running on Codename One.'),
                          actions: <Widget>[
                            TextButton(
                              onPressed: () {
                                Navigator.pop(context);
                              },
                              child: const Text('OK'),
                            ),
                          ],
                        );
                      },
                    );
                  },
                  child: const Text('About'),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class DetailPage extends StatelessWidget {
  final String name;
  const DetailPage(this.name, {super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Details')),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Text('Hello $name'),
            SizedBox(height: 16.0),
            ElevatedButton(
              onPressed: () {
                Navigator.pop(context);
              },
              child: const Text('Back'),
            ),
          ],
        ),
      ),
    );
  }
}

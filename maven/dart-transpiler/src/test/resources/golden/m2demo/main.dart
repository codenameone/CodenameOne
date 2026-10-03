import 'package:flutter/material.dart';

void main() {
  runApp(const DemoApp());
}

class DemoApp extends StatelessWidget {
  const DemoApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'M2 Demo',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
        useMaterial3: true,
      ),
      home: const DemoPage(),
    );
  }
}

class DemoPage extends StatefulWidget {
  const DemoPage({super.key});

  @override
  State<DemoPage> createState() => _DemoPageState();
}

class _DemoPageState extends State<DemoPage> {
  final List<String> _items = ['Alpha', 'Beta', 'Gamma'];
  int _taps = 0;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('M2 Widgets')),
      body: Column(
        children: <Widget>[
          Padding(
            padding: EdgeInsets.all(8.0),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: <Widget>[
                ElevatedButton(
                  onPressed: _addItem,
                  child: const Text('Add'),
                ),
                OutlinedButton(
                  onPressed: _clear,
                  child: const Text('Clear'),
                ),
                TextButton(
                  onPressed: () {
                    setState(() {
                      _taps++;
                    });
                  },
                  child: Text('Taps: $_taps'),
                ),
              ],
            ),
          ),
          const Divider(),
          Expanded(
            child: ListView.builder(
              itemCount: _items.length,
              itemBuilder: (context, index) {
                return Card(
                  child: Padding(
                    padding: EdgeInsets.all(12.0),
                    child: Row(
                      children: <Widget>[
                        const Icon(Icons.favorite),
                        SizedBox(width: 8.0),
                        Expanded(child: Text(_items[index])),
                        Text('#$index'),
                      ],
                    ),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  void _addItem() {
    setState(() {
      _items.add('Item ${_items.length + 1}');
    });
  }

  void _clear() {
    setState(() {
      _items.clear();
      _taps = 0;
    });
  }
}

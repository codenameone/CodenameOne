import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';

class SignInScreen extends StatefulWidget {
  const SignInScreen({super.key});

  @override
  State<SignInScreen> createState() => _SignInScreenState();
}

class _SignInScreenState extends State<SignInScreen> {
  final _email = TextEditingController();
  final _password = TextEditingController();
  String? _error;
  bool _busy = false;

  Future<void> _submit({required bool create}) async {
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      if (create) {
        await FirebaseAuth.instance.createUserWithEmailAndPassword(
            email: _email.text.trim(), password: _password.text);
      } else {
        await FirebaseAuth.instance.signInWithEmailAndPassword(
            email: _email.text.trim(), password: _password.text);
      }
    } on FirebaseAuthException catch (e) {
      setState(() => _error = _message(e.code));
    } finally {
      if (mounted) {
        setState(() => _busy = false);
      }
    }
  }

  String _message(String code) {
    switch (code) {
      case 'user-not-found':
      case 'wrong-password':
      case 'invalid-credential':
        return 'Email or password is wrong';
      case 'email-already-in-use':
        return 'That email already has an account';
      case 'weak-password':
        return 'Choose a longer password';
      case 'network-request-failed':
        return 'You appear to be offline';
      default:
        return 'Sign-in failed ($code)';
    }
  }

  @override
  void dispose() {
    _email.dispose();
    _password.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Field Notes')),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            TextField(
              controller: _email,
              keyboardType: TextInputType.emailAddress,
              decoration: const InputDecoration(labelText: 'Email'),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _password,
              obscureText: true,
              decoration: const InputDecoration(labelText: 'Password'),
            ),
            const SizedBox(height: 24),
            if (_error != null)
              Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: Text(_error!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
              ),
            FilledButton(
              onPressed: _busy ? null : () => _submit(create: false),
              child: const Text('Sign in'),
            ),
            TextButton(
              onPressed: _busy ? null : () => _submit(create: true),
              child: const Text('Create account'),
            ),
            TextButton(
              onPressed: _busy ? null : () => FirebaseAuth.instance.signInAnonymously(),
              child: const Text('Continue without an account'),
            ),
          ],
        ),
      ),
    );
  }
}

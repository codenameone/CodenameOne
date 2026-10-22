// go_router, headless: locations match nested patterns and capture their
// parameters, named routes build their locations, and redirects apply in order.
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

bool signedIn = false;

final router = GoRouter(
  initialLocation: '/',
  redirect: (context, state) =>
      !signedIn && state.matchedLocation.startsWith('/account') ? '/login' : null,
  routes: [
    GoRoute(path: '/', name: 'home', builder: (context, state) => const Text('home'), routes: [
      GoRoute(path: 'pets/:id', name: 'pet', builder: (context, state) => Text('pet ${state.pathParameters['id']}')),
    ]),
    GoRoute(path: '/login', builder: (context, state) => const Text('login')),
    GoRoute(path: '/account', redirect: (context, state) => '/account/profile', routes: [
      GoRoute(path: 'profile', builder: (context, state) => const Text('profile')),
    ]),
  ],
);

void show(String label) {
  final s = router.state!;
  print('$label ${s.matchedLocation} ${s.name} ${s.pathParameters} ${s.uri.queryParameters} ${s.extra}');
}

Future<void> main() async {
  router.push('/pets/42?sort=age', extra: 'biscuit');
  show('push');
  print(router.canPop());
  router.pop();
  print(router.canPop());
  print(router.namedLocation('pet', pathParameters: {'id': 'a b'}, queryParameters: {'q': '1'}));
  router.pushNamed('pet', pathParameters: {'id': '7'});
  show('named');
  router.go('/account');
  show('guarded');
  signedIn = true;
  router.go('/account');
  show('redirected');
  router.go('/nowhere');
  print('error ${router.state!.error}');
  router.go('/');
  print(router.canPop());
}

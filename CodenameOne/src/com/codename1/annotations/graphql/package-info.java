/// Annotations for declaring type safe GraphQL clients.
///
/// A client interface is marked with `GraphQLClient` and its methods are mapped
/// to GraphQL operations using `Query`, `Mutation` and `Subscription`, while
/// `Var` binds method parameters to GraphQL variables. These annotations are
/// consumed by the Codename One GraphQL client generator to produce the network
/// implementation.
package com.codename1.annotations.graphql;

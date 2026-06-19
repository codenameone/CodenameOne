/// Annotations for declaring type safe REST clients.
///
/// An interface is marked with `RestClient` and its methods describe HTTP
/// requests using the verb annotations `GET`, `POST`, `PUT`, `PATCH` and
/// `DELETE`. Request data is bound through `Path`, `Query`, `Header`, `Cookie`
/// and `Body`. These annotations are processed to generate the concrete REST
/// client implementation.
package com.codename1.annotations.rest;

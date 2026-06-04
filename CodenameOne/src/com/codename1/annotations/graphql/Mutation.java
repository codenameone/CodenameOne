/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.annotations.graphql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Declares a [GraphQLClient] method as a GraphQL **mutation**.
/// Identical in shape to [Query] -- the [#value()] is the operation
/// document and [Var]-annotated parameters supply its `$variables` --
/// but sent as a mutation. The method ends with an
/// `OnComplete<GraphQLResponse<T>>` callback.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Mutation {
    /// The GraphQL operation document, e.g.
    /// `mutation AddReview($ep: Episode!, $review: ReviewInput!) { createReview(episode: $ep, review: $review) { stars } }`.
    String value();

    /// The operation name to send in the request's `operationName`
    /// field. Optional; required only when [#value()] declares more
    /// than one operation.
    String operationName() default "";
}

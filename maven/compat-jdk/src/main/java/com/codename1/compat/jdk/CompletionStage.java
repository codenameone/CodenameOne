/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.compat.jdk;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/// `java.util.concurrent.CompletionStage`: one step of a computation that
/// others can be attached to. [CompletableFuture] is the implementation.
public interface CompletionStage<T> {

    <U> CompletionStage<U> thenApply(Function<? super T, ? extends U> fn);

    <U> CompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn);

    <U> CompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor);

    CompletionStage<Void> thenAccept(Consumer<? super T> action);

    CompletionStage<Void> thenAcceptAsync(Consumer<? super T> action);

    CompletionStage<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor);

    CompletionStage<Void> thenRun(Runnable action);

    CompletionStage<Void> thenRunAsync(Runnable action);

    CompletionStage<Void> thenRunAsync(Runnable action, Executor executor);

    <U, V> CompletionStage<V> thenCombine(CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn);

    <U, V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn);

    <U, V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn, Executor executor);

    <U> CompletionStage<Void> thenAcceptBoth(CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action);

    <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action);

    <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action, Executor executor);

    CompletionStage<Void> runAfterBoth(CompletionStage<?> other, Runnable action);

    CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action);

    CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor);

    <U> CompletionStage<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn);

    <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn);

    <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn,
            Executor executor);

    CompletionStage<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action);

    CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action);

    CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action,
            Executor executor);

    CompletionStage<Void> runAfterEither(CompletionStage<?> other, Runnable action);

    CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action);

    CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor);

    <U> CompletionStage<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn);

    <U> CompletionStage<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn);

    <U> CompletionStage<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn,
            Executor executor);

    <U> CompletionStage<U> handle(BiFunction<? super T, Throwable, ? extends U> fn);

    <U> CompletionStage<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn);

    <U> CompletionStage<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn, Executor executor);

    CompletionStage<T> whenComplete(BiConsumer<? super T, ? super Throwable> action);

    CompletionStage<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action);

    CompletionStage<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor);

    CompletionStage<T> exceptionally(Function<Throwable, ? extends T> fn);

    CompletableFuture<T> toCompletableFuture();
}

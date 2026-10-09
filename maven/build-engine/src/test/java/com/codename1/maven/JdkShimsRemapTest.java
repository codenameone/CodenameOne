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
package com.codename1.maven;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/// The JDK stand-ins, end to end: source written against the JDK is compiled,
/// run, relocated by the remap step and run again, and has to answer the same.
public class JdkShimsRemapTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private RemapDifferential java8() throws Exception {
        return new RemapDifferential(tmp.newFolder(), 8);
    }

    @Test
    public void textIsEncodedAndDecodedByCharset() throws Exception {
        java8().same(""
                + "byte[] utf = \"caf\\u00e9 \\u20ac\".getBytes(StandardCharsets.UTF_8);\n"
                + "out.add(utf);\n"
                + "out.add(new String(utf, StandardCharsets.UTF_8));\n"
                + "out.add(new String(utf, 0, 3, StandardCharsets.US_ASCII));\n"
                + "out.add(new String(utf, 1, 4, StandardCharsets.ISO_8859_1).length());\n"
                + "out.add(StandardCharsets.UTF_8.name());\n"
                + "out.add(StandardCharsets.ISO_8859_1.toString());\n"
                + "out.add(Charset.forName(\"utf-8\").name());\n"
                + "out.add(Charset.forName(\"UTF8\") == StandardCharsets.UTF_8);\n"
                + "out.add(Charset.forName(\"latin1\").name());\n"
                + "out.add(Charset.forName(\"ascii\").name());\n"
                + "out.add(Charset.isSupported(\"UTF-8\"));\n"
                + "out.add(Charset.defaultCharset() != null);\n"
                + "Reader r = new InputStreamReader(new ByteArrayInputStream(utf), StandardCharsets.UTF_8);\n"
                + "StringBuilder sb = new StringBuilder();\n"
                + "for (int c = r.read(); c >= 0; c = r.read()) { sb.append((char) c); }\n"
                + "out.add(sb.toString());\n"
                + "ByteArrayOutputStream bytes = new ByteArrayOutputStream();\n"
                + "Writer w = new OutputStreamWriter(bytes, StandardCharsets.UTF_8);\n"
                + "w.write(\"na\\u00efve\");\n"
                + "w.close();\n"
                + "out.add(bytes.toByteArray());\n"
                + "out.add(bytes.toString(\"UTF-8\"));\n", 14, "Charset.forName");
    }

    @Test
    public void regularExpressionsAreTheJdks() throws Exception {
        RemapDifferential d = new RemapDifferential(tmp.newFolder(), 8);
        d.same(""
                + "Pattern p = Pattern.compile(\"(?<key>[a-z]+)=(\\\\d+)\", Pattern.CASE_INSENSITIVE);\n"
                + "Matcher m = p.matcher(\"Alpha=1, beta=22; GAMMA=333\");\n"
                + "while (m.find()) {\n"
                + "    out.add(m.group(\"key\") + \":\" + m.group(2) + \"@\" + m.start() + \"-\" + m.end());\n"
                + "}\n"
                + "out.add(m.replaceAll(\"$2=${key}\"));\n"
                + "out.add(p.split(\"x a=1 y b=2 z\"));\n"
                + "out.add(\"a1b22c\".split(\"\\\\d+\"));\n"
                + "out.add(\"a,b,,\".split(\",\"));\n"
                + "out.add(\"a,b,,\".split(\",\", -1));\n"
                + "out.add(\"2024-02-29\".matches(\"\\\\d{4}-\\\\d{2}-\\\\d{2}\"));\n"
                + "out.add(\"  trim  me \".replaceAll(\"^\\\\s+|\\\\s+$\", \"\"));\n"
                + "out.add(\"camelCaseName\".replaceAll(\"(?<=[a-z])(?=[A-Z])\", \"_\"));\n"
                + "out.add(\"a.b.c\".replaceFirst(\"\\\\.\", \"/\"));\n"
                + "out.add(Pattern.matches(\"[\\\\w.]+@[\\\\w.]+\", \"me@example.com\"));\n"
                + "out.add(Pattern.quote(\"a.b\"));\n"
                + "out.add(Matcher.quoteReplacement(\"$1\"));\n"
                + "try {\n"
                + "    Pattern.compile(\"(unclosed\");\n"
                + "} catch (PatternSyntaxException e) {\n"
                + "    out.add(e.getIndex() + \"|\" + e.getPattern() + \"|\" + e.getDescription());\n"
                + "}\n"
                + "MatchResult r = Pattern.compile(\"b+\").matcher(\"abbbc\");\n"
                + "out.add(((Matcher) r).find() ? r.group() + r.start() + r.end() + r.groupCount() : \"none\");\n"
                + "out.add(Pattern.compile(\"\\\\s*,\\\\s*\").splitAsStream(\"a , b,c\").collect(Collectors.toList()));\n"
                + "out.add(Pattern.compile(\"^\\\\d+$\").asPredicate().test(\"123\"));\n"
                + "StringBuffer sb = new StringBuffer();\n"
                + "Matcher d2 = Pattern.compile(\"\\\\d\").matcher(\"a1b2\");\n"
                + "while (d2.find()) {\n"
                + "    d2.appendReplacement(sb, \"<\" + d2.group() + \">\");\n"
                + "}\n"
                + "out.add(d2.appendTail(sb).toString());\n", 20, "java/util/regex/");
    }

    @Test
    public void futuresLatchesAndScheduledWorkCompleteAsInTheJdk() throws Exception {
        RemapDifferential d = new RemapDifferential(tmp.newFolder(), 8);
        d.same(""
                + "CompletableFuture<Integer> one = CompletableFuture.supplyAsync(() -> 1);\n"
                + "out.add(one.get());\n"
                + "out.add(one.thenApply(x -> x + 1).thenApply(x -> x * 10).join());\n"
                + "out.add(CompletableFuture.completedFuture(\"a\").thenCombine(CompletableFuture.supplyAsync(() -> \"b\"), (a, b) -> a + b).get());\n"
                + "out.add(CompletableFuture.supplyAsync(() -> 2).thenCompose(x -> CompletableFuture.supplyAsync(() -> x * 3)).get(5, TimeUnit.SECONDS));\n"
                + "List<String> seen = new ArrayList<>();\n"
                + "CompletableFuture<Void> accepted = CompletableFuture.completedFuture(\"v\").thenAccept(seen::add).thenRun(() -> seen.add(\"ran\"));\n"
                + "accepted.join();\n"
                + "out.add(seen);\n"
                + "CompletableFuture<String> failed = CompletableFuture.supplyAsync(() -> { throw new IllegalStateException(\"boom\"); });\n"
                + "try {\n"
                + "    failed.get();\n"
                + "} catch (ExecutionException e) {\n"
                + "    out.add(\"get: \" + e.getCause().getClass().getSimpleName() + \" \" + e.getCause().getMessage());\n"
                + "}\n"
                + "try {\n"
                + "    failed.join();\n"
                + "} catch (CompletionException e) {\n"
                + "    out.add(\"join: \" + e.getCause().getClass().getSimpleName());\n"
                + "}\n"
                + "out.add(failed.isCompletedExceptionally() + \" \" + failed.isDone() + \" \" + failed.isCancelled());\n"
                + "out.add(failed.exceptionally(t -> \"recovered \" + t.getClass().getSimpleName() + \"/\" + t.getCause().getMessage()).join());\n"
                + "out.add(failed.handle((v, t) -> v == null ? \"handled \" + (t != null) : v).join());\n"
                + "CompletableFuture<String> dependent = failed.thenApply(s -> s + \"!\");\n"
                + "try {\n"
                + "    dependent.join();\n"
                + "} catch (CompletionException e) {\n"
                + "    out.add(\"dependent: \" + e.getCause().getMessage());\n"
                + "}\n"
                + "out.add(dependent.exceptionally(t -> t.getClass().getSimpleName()).join());\n"
                + "List<String> log = new ArrayList<>();\n"
                + "out.add(CompletableFuture.completedFuture(5).whenComplete((v, t) -> log.add(v + \"/\" + t)).join());\n"
                + "try {\n"
                + "    failed.whenComplete((v, t) -> log.add(v + \"/\" + (t != null))).join();\n"
                + "} catch (CompletionException e) {\n"
                + "    log.add(\"still failed\");\n"
                + "}\n"
                + "out.add(log);\n"
                + "CompletableFuture<String> manual = new CompletableFuture<>();\n"
                + "out.add(manual.isDone() + \" \" + manual.getNow(\"absent\"));\n"
                + "CompletableFuture<Integer> length = manual.thenApply(String::length);\n"
                + "out.add(manual.complete(\"done\") + \" \" + manual.complete(\"again\") + \" \" + manual.get() + \" \" + length.get());\n"
                + "CompletableFuture<String> raw = new CompletableFuture<>();\n"
                + "raw.completeExceptionally(new java.io.IOException(\"io\"));\n"
                + "try {\n"
                + "    raw.join();\n"
                + "} catch (CompletionException e) {\n"
                + "    out.add(\"raw join: \" + e.getCause().getClass().getSimpleName());\n"
                + "}\n"
                + "out.add(raw.exceptionally(t -> t.getClass().getSimpleName()).join());\n"
                + "try {\n"
                + "    raw.get();\n"
                + "} catch (ExecutionException e) {\n"
                + "    out.add(\"raw get: \" + e.getCause().getMessage());\n"
                + "}\n"
                + "CompletableFuture<String> cancelled = new CompletableFuture<>();\n"
                + "out.add(cancelled.cancel(true) + \" \" + cancelled.isCancelled() + \" \" + cancelled.isCompletedExceptionally());\n"
                + "try {\n"
                + "    cancelled.get();\n"
                + "} catch (CancellationException e) {\n"
                + "    out.add(\"cancelled get\");\n"
                + "}\n"
                + "try {\n"
                + "    cancelled.join();\n"
                + "} catch (CancellationException e) {\n"
                + "    out.add(\"cancelled join\");\n"
                + "}\n"
                + "try {\n"
                + "    cancelled.thenApply(s -> s).join();\n"
                + "} catch (CompletionException e) {\n"
                + "    out.add(\"after cancel: \" + e.getCause().getClass().getSimpleName());\n"
                + "}\n"
                + "CompletableFuture<Integer> a = CompletableFuture.supplyAsync(() -> 10);\n"
                + "CompletableFuture<Integer> b = CompletableFuture.supplyAsync(() -> 20);\n"
                + "CompletableFuture<Integer> c = new CompletableFuture<>();\n"
                + "CompletableFuture<Void> all = CompletableFuture.allOf(a, b, c);\n"
                + "out.add(all.isDone());\n"
                + "c.complete(30);\n"
                + "all.get(5, TimeUnit.SECONDS);\n"
                + "out.add(a.join() + b.join() + c.join());\n"
                + "out.add(CompletableFuture.allOf().isDone());\n"
                + "out.add(CompletableFuture.anyOf(new CompletableFuture<String>(), CompletableFuture.completedFuture(\"first\")).join());\n"
                + "try {\n"
                + "    CompletableFuture.allOf(a, raw).join();\n"
                + "} catch (CompletionException e) {\n"
                + "    out.add(\"allOf failed: \" + e.getCause().getMessage());\n"
                + "}\n"
                + "out.add(new CompletableFuture<String>().applyToEither(CompletableFuture.completedFuture(\"right\"), s -> s + \"!\").join());\n"
                + "List<String> both = new ArrayList<>();\n"
                + "a.thenAcceptBoth(b, (x, y) -> both.add(\"\" + (x + y))).join();\n"
                + "a.runAfterBoth(b, () -> both.add(\"both\")).join();\n"
                + "a.acceptEither(new CompletableFuture<Integer>(), x -> both.add(\"either \" + x)).join();\n"
                + "a.runAfterEither(new CompletableFuture<Object>(), () -> both.add(\"ran either\")).join();\n"
                + "out.add(both);\n"
                + "CompletableFuture<String> never = new CompletableFuture<>();\n"
                + "try {\n"
                + "    never.get(50, TimeUnit.MILLISECONDS);\n"
                + "} catch (TimeoutException e) {\n"
                + "    out.add(\"timed out\");\n"
                + "}\n"
                + "Executor direct = Runnable::run;\n"
                + "out.add(CompletableFuture.supplyAsync(() -> \"on executor\", direct).thenApplyAsync(String::toUpperCase, direct).join());\n"
                + "out.add(CompletableFuture.runAsync(() -> seen.add(\"async\")).thenApply(v -> seen.size()).get());\n"
                + "CompletionStage<String> stage = CompletableFuture.completedFuture(\"stage\");\n"
                + "out.add(stage.thenApply(s -> s + \"d\").toCompletableFuture().join());\n"
                + "CountDownLatch latch = new CountDownLatch(3);\n"
                + "ExecutorService pool = Executors.newFixedThreadPool(2);\n"
                + "AtomicInteger sum = new AtomicInteger();\n"
                + "for (int i = 1; i <= 3; i++) {\n"
                + "    final int n = i;\n"
                + "    pool.execute(() -> { sum.addAndGet(n); latch.countDown(); });\n"
                + "}\n"
                + "out.add(latch.await(5, TimeUnit.SECONDS) + \" \" + sum.get() + \" \" + latch.getCount());\n"
                + "out.add(new CountDownLatch(1).await(20, TimeUnit.MILLISECONDS));\n"
                + "Future<String> submitted = pool.submit(() -> \"submitted\");\n"
                + "out.add(submitted.get());\n"
                + "pool.shutdown();\n"
                + "out.add(pool.awaitTermination(5, TimeUnit.SECONDS));\n"
                + "ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();\n"
                + "ScheduledFuture<String> later = timer.schedule(() -> \"later\", 30, TimeUnit.MILLISECONDS);\n"
                + "out.add(later.get(5, TimeUnit.SECONDS) + \" \" + later.isDone());\n"
                + "AtomicInteger ticks = new AtomicInteger();\n"
                + "CountDownLatch three = new CountDownLatch(3);\n"
                + "ScheduledFuture<?> repeating = timer.scheduleAtFixedRate(() -> { ticks.incrementAndGet(); three.countDown(); }, 0, 10, TimeUnit.MILLISECONDS);\n"
                + "out.add(three.await(5, TimeUnit.SECONDS));\n"
                + "out.add(repeating.cancel(false) + \" \" + repeating.isCancelled() + \" \" + (ticks.get() >= 3));\n"
                + "CountDownLatch two = new CountDownLatch(2);\n"
                + "ScheduledFuture<?> delayed = timer.scheduleWithFixedDelay(two::countDown, 5, 5, TimeUnit.MILLISECONDS);\n"
                + "out.add(two.await(5, TimeUnit.SECONDS));\n"
                + "delayed.cancel(false);\n"
                + "ScheduledFuture<?> dropped = timer.schedule(() -> { }, 1, TimeUnit.HOURS);\n"
                + "out.add(dropped.cancel(false) + \" \" + dropped.isDone());\n"
                + "timer.shutdownNow();\n"
                + "out.add(timer.isShutdown());\n"
                + "ExecutorService named = Executors.newSingleThreadExecutor(r -> new Thread(r, \"named\"));\n"
                + "out.add(named.submit(() -> 7).get());\n"
                + "named.shutdown();\n"
                + "out.add(Executors.newCachedThreadPool(Executors.defaultThreadFactory()).submit(() -> 8).get());\n", 45, "java/util/concurrent/C", "java/util/concurrent/S", "java/util/concurrent/T");
    }
}

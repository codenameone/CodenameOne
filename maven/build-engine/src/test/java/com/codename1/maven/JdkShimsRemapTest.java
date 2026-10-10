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

    /// On a JDK that has `findFirst` and `stream`, which is also the one
    /// whose iterations share what either created. The providers come from
    /// a services file with the comments, blank lines and repeats the format
    /// allows; the stand-in reads it itself here, where no build generated
    /// a registry (`ServiceProvidersTest` covers that one).
    @Test
    public void serviceProvidersAreLoadedInFileOrderAndLazily() throws Exception {
        RemapDifferential d = new RemapDifferential(tmp.newFolder(), 17);
        org.junit.Assume.assumeTrue("needs a JDK 17 (JAVA17_HOME)", d.available());
        d.source("Shape", "public interface Shape { String name(); }\n")
                .source("Lonely", "public interface Lonely { }\n")
                .source("Made", "public class Made { public static final List<String> LOG = new ArrayList<>(); }\n")
                .source("Circle", "public class Circle implements Shape {\n"
                        + "    public Circle() { Made.LOG.add(\"circle\"); }\n"
                        + "    public String name() { return \"circle\"; }\n}\n")
                .source("Square", "public class Square implements Shape {\n"
                        + "    public Square() { Made.LOG.add(\"square\"); }\n"
                        + "    public String name() { return \"square\"; }\n}\n")
                .resource("META-INF/services/q.Shape",
                        "# shapes\n\n  q.Square   # first\nq.Circle\n\t\nq.Square\n");
        d.same(""
                + "ServiceLoader<Shape> l = ServiceLoader.load(Shape.class);\n"
                + "out.add(l.toString());\n"
                + "out.add(new ArrayList<>(Made.LOG));\n"
                + "Iterator<Shape> it = l.iterator();\n"
                + "out.add(it.hasNext() + \" \" + Made.LOG);\n"
                + "out.add(it.next().name() + \" \" + Made.LOG);\n"
                + "Iterator<Shape> other = l.iterator();\n"
                + "out.add(other.next().name() + \" \" + Made.LOG);\n"
                + "out.add(other.next().name() + \" \" + Made.LOG);\n"
                + "out.add(it.next().name() + \" \" + Made.LOG);\n"
                + "out.add(it.hasNext() + \" \" + other.hasNext());\n"
                + "try { it.next(); out.add(\"no\"); } catch (NoSuchElementException e) { out.add(\"ended\"); }\n"
                + "List<String> names = new ArrayList<>();\n"
                + "l.forEach(s -> names.add(s.name()));\n"
                + "out.add(names + \" \" + Made.LOG);\n"
                + "out.add(l.findFirst().get().name());\n"
                + "out.add(l.findFirst().get() == l.iterator().next());\n"
                + "out.add(ServiceLoader.load(Lonely.class).findFirst().isPresent());\n"
                + "out.add(ServiceLoader.load(Lonely.class).iterator().hasNext());\n"
                + "out.add(l.stream().map(p -> p.type().getName()).collect(Collectors.toList()) + \" \" + Made.LOG);\n"
                + "out.add(l.stream().map(p -> p.get().name()).collect(Collectors.toList()));\n"
                + "l.reload();\n"
                + "Made.LOG.clear();\n"
                + "out.add(l.iterator().next().name() + \" \" + Made.LOG);\n"
                + "out.add(ServiceLoader.load(Shape.class, Shape.class.getClassLoader()).iterator().next().name());\n"
                + "out.add(ServiceLoader.loadInstalled(Shape.class).iterator().hasNext());\n"
                + "try { ServiceLoader.load(null); out.add(\"no\"); } catch (NullPointerException e) { out.add(\"npe\"); }\n",
                18, "java/util/ServiceLoader");
    }

    @Test
    public void aClassIsInTheUnnamedModule() throws Exception {
        RemapDifferential d = new RemapDifferential(tmp.newFolder(), 17);
        org.junit.Assume.assumeTrue("needs a JDK 17 (JAVA17_HOME)", d.available());
        d.source("Here", "public class Here { }\n");
        d.same(""
                + "Module m = Here.class.getModule();\n"
                + "out.add(m.isNamed());\n"
                + "out.add(m.getName());\n"
                + "out.add(m.getLayer() == null);\n"
                + "out.add(m == D.class.getModule());\n"
                + "ModuleLayer none = m.getLayer();\n"
                + "out.add(none);\n"
                + "try { ServiceLoader.load(none, Here.class); out.add(\"no\"); }"
                + " catch (NullPointerException e) { out.add(\"npe\"); }\n",
                6, "java/lang/Module", "java/lang/ModuleLayer");
    }

    @Test
    public void aCopyOnWriteSetIteratesOverWhatItHeldWhenAsked() throws Exception {
        java8().same(""
                + "CopyOnWriteArraySet<String> set = new CopyOnWriteArraySet<>(Arrays.asList(\"b\", \"a\", \"b\"));\n"
                + "out.add(set.toString() + set.size());\n"
                + "out.add(set.add(\"a\") + \" \" + set.add(\"c\"));\n"
                + "List<String> seen = new ArrayList<>();\n"
                + "for (String s : set) { seen.add(s); set.add(s + s); set.remove(\"c\"); }\n"
                + "out.add(seen);\n"
                + "out.add(set);\n"
                + "Iterator<String> it = set.iterator();\n"
                + "it.next();\n"
                + "try { it.remove(); out.add(\"no\"); } catch (UnsupportedOperationException e) { out.add(\"fixed\"); }\n"
                + "out.add(set.contains(\"aa\") + \" \" + set.containsAll(Arrays.asList(\"a\", \"bb\")));\n"
                + "out.add(set.addAll(Arrays.asList(\"a\", \"z\", \"z\")) + \" \" + set);\n"
                + "out.add(set.removeAll(Arrays.asList(\"z\", \"q\")) + \" \" + set);\n"
                + "out.add(set.retainAll(Arrays.asList(\"a\", \"b\")) + \" \" + set);\n"
                + "out.add(set.removeIf(s -> s.equals(\"a\")) + \" \" + set);\n"
                + "out.add(set.equals(new HashSet<>(Arrays.asList(\"b\"))) + \" \" + (set.hashCode() == \"b\".hashCode()));\n"
                + "out.add(set.toArray());\n"
                + "out.add(set.toArray(new String[0]));\n"
                + "set.clear();\n"
                + "out.add(set.isEmpty());\n"
                + "out.add(new CopyOnWriteArraySet<Integer>().iterator().hasNext());\n",
                13, "java/util/concurrent/CopyOnWriteArraySet");
    }

    /// The four OSGi types a library asks about before taking its class
    /// path route are answered "not in a framework", and nothing else of
    /// OSGi is mapped.
    @Test
    public void aLibraryThatAsksIsToldItIsNotInAnOsgiFramework() {
        ClassRelocator relocator = new ClassRelocator(CompatLayers.SWING);
        org.junit.Assert.assertEquals("com/codename1/compat/jdk/osgi/FrameworkUtil", relocator.map("org/osgi/framework/FrameworkUtil"));
        org.junit.Assert.assertEquals("com/codename1/compat/jdk/osgi/Bundle", relocator.map("org/osgi/framework/Bundle"));
        org.junit.Assert.assertEquals("com/codename1/compat/jdk/osgi/BundleContext", relocator.map("org/osgi/framework/BundleContext"));
        org.junit.Assert.assertEquals("com/codename1/compat/jdk/osgi/ServiceReference",
                relocator.map("org/osgi/framework/ServiceReference"));
        org.junit.Assert.assertEquals("org/osgi/framework/BundleActivator", relocator.map("org/osgi/framework/BundleActivator"));
        org.junit.Assert.assertNull(com.codename1.compat.jdk.osgi.FrameworkUtil.getBundle(String.class));
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

    @Test
    public void datesTimesAndDurationsAreTheJdks() throws Exception {
        RemapDifferential d = new RemapDifferential(tmp.newFolder(), 17);
        org.junit.Assume.assumeTrue("needs a JDK 17", d.available());
        d.same(""
                + "ZoneId utc = ZoneId.of(\"UTC\");\n"
                + "LocalDate d = LocalDate.of(2024, 2, 29);\n"
                + "out.add(d.getDayOfWeek() + \" \" + d.getMonth() + \" \" + d.getDayOfYear() + \" \" + d.lengthOfYear());\n"
                + "out.add(d.getDayOfWeek().getValue() + \" \" + d.getDayOfWeek().plus(3) + \" \" + DayOfWeek.of(7) + \" \" + DayOfWeek.valueOf(\"FRIDAY\").minus(9));\n"
                + "out.add(d.getMonth().getValue() + \" \" + d.getMonth().plus(11) + \" \" + Month.of(12).maxLength() + \" \" + Month.FEBRUARY.length(false));\n"
                + "out.add(Arrays.asList(DayOfWeek.values()));\n"
                + "out.add(d.atStartOfDay());\n"
                + "out.add(d.withDayOfMonth(1) + \" \" + d.withMonth(4) + \" \" + d.withYear(2023) + \" \" + d.withDayOfYear(366));\n"
                + "out.add(d.plus(1, ChronoUnit.MONTHS) + \" \" + d.minus(1, ChronoUnit.YEARS) + \" \" + d.plus(3, ChronoUnit.WEEKS) + \" \" + d.plus(2, ChronoUnit.DECADES));\n"
                + "out.add(d.plus(Period.of(1, 1, 1)) + \" \" + d.minus(Period.ofMonths(1)));\n"
                + "out.add(d.datesUntil(LocalDate.of(2024, 3, 3)).map(LocalDate::toString).collect(Collectors.joining(\",\")));\n"
                + "out.add(d.datesUntil(d).count());\n"
                + "try {\n"
                + "    d.datesUntil(d.minusDays(1));\n"
                + "    out.add(\"no failure\");\n"
                + "} catch (IllegalArgumentException e) {\n"
                + "    out.add(\"backwards\");\n"
                + "}\n"
                + "LocalDate e = LocalDate.of(2026, 10, 9);\n"
                + "for (ChronoUnit u : ChronoUnit.values()) {\n"
                + "    try {\n"
                + "        out.add(u + \" \" + u.between(d, e) + \" \" + d.until(e, u) + \" \" + u.isDateBased() + \" \" + u.isTimeBased() + \" \" + u.getDuration());\n"
                + "    } catch (DateTimeException x) {\n"
                + "        out.add(u + \" unsupported for dates\");\n"
                + "    }\n"
                + "}\n"
                + "out.add(ChronoUnit.DAYS.between(e, d) + \" \" + ChronoUnit.valueOf(\"HOURS\") + \" \" + ChronoUnit.HOURS.name());\n"
                + "out.add(LocalDate.of(2024, Month.MARCH, 31).plusMonths(1) + \" \" + LocalDate.ofInstant(Instant.ofEpochSecond(86400L * 366), utc));\n"
                + "LocalDateTime t = LocalDateTime.of(2024, 3, 31, 23, 59, 58, 123456789);\n"
                + "out.add(t.getDayOfWeek() + \" \" + t.getMonth() + \" \" + t.getDayOfYear());\n"
                + "out.add(t.minusDays(31) + \" \" + t.minusHours(25) + \" \" + t.minusMinutes(61) + \" \" + t.minusSeconds(59) + \" \" + t.plusNanos(999999999999L));\n"
                + "out.add(t.plusWeeks(1) + \" \" + t.minusWeeks(1) + \" \" + t.plusMonths(11) + \" \" + t.minusMonths(1) + \" \" + t.plusYears(4) + \" \" + t.minusYears(1));\n"
                + "out.add(t.withHour(1) + \" \" + t.withMinute(2) + \" \" + t.withSecond(3) + \" \" + t.withNano(4));\n"
                + "out.add(t.truncatedTo(ChronoUnit.HOURS) + \" \" + t.truncatedTo(ChronoUnit.DAYS) + \" \" + t.truncatedTo(ChronoUnit.MILLIS));\n"
                + "out.add(t.plus(90, ChronoUnit.MINUTES) + \" \" + t.minus(2, ChronoUnit.HALF_DAYS) + \" \" + t.plus(Duration.ofHours(49)) + \" \" + t.minus(Period.ofDays(31)));\n"
                + "LocalDateTime later = LocalDateTime.of(2026, 10, 9, 8, 0);\n"
                + "out.add(t.isBefore(later) + \" \" + t.isAfter(later) + \" \" + t.isEqual(t) + \" \" + t.toEpochSecond(ZoneOffset.UTC));\n"
                + "out.add(ChronoUnit.HOURS.between(t, later) + \" \" + ChronoUnit.MONTHS.between(t, later) + \" \" + t.until(later, ChronoUnit.MILLIS) + \" \" + ChronoUnit.MINUTES.between(later, t));\n"
                + "Instant i = Instant.ofEpochSecond(1700000000L, 5);\n"
                + "ZonedDateTime z = i.atZone(utc);\n"
                + "out.add(z.toLocalDate() + \" \" + z.toLocalTime() + \" \" + z.toEpochSecond() + \" \" + z.toLocalDateTime());\n"
                + "out.add(z.getYear() + \" \" + z.getMonthValue() + \" \" + z.getMonth() + \" \" + z.getDayOfMonth() + \" \" + z.getDayOfYear() + \" \" + z.getDayOfWeek() + \" \" + z.getHour() + \" \" + z.getMinute() + \" \" + z.getSecond() + \" \" + z.getNano());\n"
                + "out.add(z.plusDays(1).toInstant() + \" \" + z.minusWeeks(1).toInstant() + \" \" + z.plusMonths(3).toInstant() + \" \" + z.minusYears(1).toInstant() + \" \" + z.plusHours(30).toInstant() + \" \" + z.minusMinutes(1).toInstant() + \" \" + z.plusSeconds(1).toInstant());\n"
                + "out.add(z.isBefore(z.plusSeconds(1)) + \" \" + z.isAfter(z.plusSeconds(1)) + \" \" + z.isEqual(z.withZoneSameInstant(utc)) + \" \" + ChronoUnit.DAYS.between(z, z.plusHours(49)));\n"
                + "out.add(t.atZone(utc).toInstant());\n"
                + "out.add(i.isBefore(i.plusNanos(1)) + \" \" + i.isAfter(i.minusNanos(1)) + \" \" + i.plus(Duration.ofMillis(1500)) + \" \" + i.minus(Duration.ofDays(1)) + \" \" + i.plus(3, ChronoUnit.HOURS) + \" \" + i.minus(250, ChronoUnit.MILLIS));\n"
                + "out.add(ChronoUnit.SECONDS.between(i, i.plusMillis(2500)) + \" \" + i.until(i.minusSeconds(90), ChronoUnit.MINUTES) + \" \" + Duration.between(i, i.plusMillis(2500)) + \" \" + Duration.between(t, later));\n"
                + "Duration du = Duration.ofSeconds(90061, 123456789);\n"
                + "out.add(du.toDays() + \" \" + du.toHours() + \" \" + du.toMinutes() + \" \" + du.toSeconds() + \" \" + du.toNanos() + \" \" + du.toDaysPart() + \" \" + du.toHoursPart() + \" \" + du.toMinutesPart() + \" \" + du.toSecondsPart() + \" \" + du.toMillisPart() + \" \" + du.toNanosPart());\n"
                + "out.add(du.isZero() + \" \" + du.isNegative() + \" \" + Duration.ZERO.isZero() + \" \" + du.negated() + \" \" + du.negated().abs() + \" \" + du.negated().isNegative());\n"
                + "out.add(du.plusDays(1) + \" \" + du.plusHours(1) + \" \" + du.plusMinutes(1) + \" \" + du.plusSeconds(1) + \" \" + du.plusMillis(1) + \" \" + du.plusNanos(1));\n"
                + "out.add(du.minusDays(1) + \" \" + du.minusHours(1) + \" \" + du.minusMinutes(1) + \" \" + du.minusSeconds(1) + \" \" + du.minusMillis(1) + \" \" + du.minusNanos(1));\n"
                + "out.add(du.multipliedBy(3) + \" \" + du.dividedBy(7) + \" \" + Duration.ofNanos(-1) + \" \" + Duration.of(90, ChronoUnit.MINUTES) + \" \" + Duration.of(5, ChronoUnit.MICROS));\n"
                + "Date date = new Date(1700000000123L);\n"
                + "out.add(date.toInstant() + \" \" + Date.from(i).getTime() + \" \" + date.after(new Date(0)) + \" \" + date.before(new Date(0)));\n"
                + "out.add((date.getYear() + 1900) + \" \" + date.getMonth() + \" \" + (date.getDate() > 0) + \" \" + (date.getDay() >= 0) + \" \" + (date.getHours() < 24) + \" \" + date.getMinutes() + \" \" + date.getSeconds());\n", 50, "java/time/DayOfWeek", "java/time/Month", "java/time/temporal/ChronoUnit");
    }

    @Test
    public void codingDigestsAndStreamMembersAreTheJdks() throws Exception {
        RemapDifferential d = new RemapDifferential(tmp.newFolder(), 17);
        org.junit.Assume.assumeTrue("needs a JDK 17", d.available());
        d.same(""
                + "byte[] data = \"Hello, \\u0142\\u00f3d\\u017a! ~?>\".getBytes(StandardCharsets.UTF_8);\n"
                + "String b64 = Base64.getEncoder().encodeToString(data);\n"
                + "out.add(b64 + \" \" + Base64.getUrlEncoder().withoutPadding().encodeToString(data) + \" \" + Base64.getMimeEncoder().encodeToString(new byte[100]).length());\n"
                + "out.add(new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8) + \" \" + Base64.getUrlDecoder().decode(\"_-8\").length + \" \" + Base64.getMimeDecoder().decode(\"QU\\r\\nJD\").length);\n"
                + "Base64.Encoder encoder = Base64.getEncoder();\n"
                + "Base64.Decoder decoder = Base64.getDecoder();\n"
                + "out.add(Arrays.toString(decoder.decode(encoder.encode(new byte[] {1, 2, 3, 4}))));\n"
                + "try {\n"
                + "    decoder.decode(\"a*b\");\n"
                + "    out.add(\"decoded\");\n"
                + "} catch (IllegalArgumentException e) {\n"
                + "    out.add(\"illegal base64\");\n"
                + "}\n"
                + "out.add(URLEncoder.encode(\"a b&c=\\u00e9/\\ud83d\\ude00\", StandardCharsets.UTF_8) + \" \" + URLEncoder.encode(\"x y\", \"UTF-8\"));\n"
                + "out.add(URLDecoder.decode(\"a+b%26c%3D%C3%A9%2F%F0%9F%98%80\", StandardCharsets.UTF_8) + \" \" + URLDecoder.decode(\"x+y%21\", \"UTF-8\"));\n"
                + "try {\n"
                + "    URLDecoder.decode(\"%zz\", StandardCharsets.UTF_8);\n"
                + "    out.add(\"decoded\");\n"
                + "} catch (IllegalArgumentException e) {\n"
                + "    out.add(\"illegal escape\");\n"
                + "}\n"
                + "HexFormat hex = HexFormat.of();\n"
                + "out.add(hex.formatHex(data) + \" \" + hex.withUpperCase().formatHex(data, 0, 3) + \" \" + HexFormat.ofDelimiter(\":\").formatHex(new byte[] {0, 127, -128, -1}));\n"
                + "out.add(Arrays.toString(hex.parseHex(\"00ff7f80\")) + \" \" + Arrays.toString(HexFormat.ofDelimiter(\", \").withPrefix(\"#\").parseHex(\"#0a, #FF\")));\n"
                + "out.add(hex.toHexDigits((byte) 10) + \" \" + hex.toHexDigits('a') + \" \" + hex.toHexDigits((short) -2) + \" \" + hex.toHexDigits(255) + \" \" + hex.toHexDigits(-1L) + \" \" + hex.toHexDigits(0xabcdefL, 5));\n"
                + "out.add(HexFormat.isHexDigit('g') + \" \" + HexFormat.fromHexDigit('F') + \" \" + HexFormat.fromHexDigits(\"7fffffff\") + \" \" + HexFormat.fromHexDigitsToLong(\"ffffffffffffffff\") + \" \" + hex.toLowHexDigit(0x1f) + hex.toHighHexDigit(0x1f));\n"
                + "out.add(hex.withDelimiter(\"-\").withSuffix(\"h\") + \" \" + hex.equals(HexFormat.of()) + \" \" + hex.isUpperCase() + hex.delimiter() + hex.prefix() + hex.suffix());\n"
                + "for (String bad : new String[] {\"0\", \"0g\", \"00:11\", \"zz\"}) {\n"
                + "    try {\n"
                + "        out.add(Arrays.toString(hex.parseHex(bad)));\n"
                + "    } catch (IllegalArgumentException e) {\n"
                + "        out.add(\"bad hex \" + bad);\n"
                + "    }\n"
                + "}\n"
                + "for (String name : new String[] {\"MD5\", \"SHA-1\", \"SHA-256\", \"SHA-512\", \"WHIRLPOOL\"}) {\n"
                + "    try {\n"
                + "        MessageDigest md = MessageDigest.getInstance(name);\n"
                + "        md.update(data, 0, 5);\n"
                + "        md.update(data, 5, data.length - 5);\n"
                + "        out.add(md.getAlgorithm() + \" \" + md.getDigestLength() + \" \" + hex.formatHex(md.digest()) + \" \" + hex.formatHex(md.digest(new byte[0])));\n"
                + "    } catch (NoSuchAlgorithmException e) {\n"
                + "        out.add(\"no \" + name);\n"
                + "    } catch (GeneralSecurityException e) {\n"
                + "        out.add(\"other\");\n"
                + "    }\n"
                + "}\n"
                + "out.add(MessageDigest.isEqual(new byte[] {1, 2}, new byte[] {1, 2}) + \" \" + MessageDigest.isEqual(new byte[] {1, 2}, new byte[] {1, 3}));\n"
                // Constructed and typed only: the bytes come from the port, and the
                // headless implementation these tests run on has no source of them.
                + "SecureRandom random = new SecureRandom();\n"
                + "out.add((random instanceof Random) + \" \" + (random.getAlgorithm() != null));\n"
                + "InputStream in = new ByteArrayInputStream(data);\n"
                + "out.add(in.readNBytes(5).length + \" \" + in.readNBytes(new byte[8], 2, 3) + \" \" + in.readAllBytes().length + \" \" + in.readAllBytes().length);\n"
                + "ByteArrayOutputStream copy = new ByteArrayOutputStream();\n"
                + "ByteArrayInputStream source = new ByteArrayInputStream(data);\n"
                + "out.add(source.transferTo(copy) + \" \" + copy.toString(StandardCharsets.UTF_8));\n"
                + "StringWriter written = new StringWriter();\n"
                + "out.add(new StringReader(\"copied text\").transferTo(written) + \" \" + written);\n"
                + "BufferedReader reader = new BufferedReader(new StringReader(\"one\\ntwo\\r\\n\\nfour\"));\n"
                + "out.add(reader.lines().map(String::toUpperCase).collect(Collectors.toList()));\n"
                + "StringBuilder sb = new StringBuilder();\n"
                + "out.add(sb.isEmpty() + \" \" + sb.appendCodePoint(0x1F600).appendCodePoint('x').length() + \" \" + sb.isEmpty() + \" \" + new StringBuffer().appendCodePoint(65) + \" \" + new StringBuffer().isEmpty());\n"
                + "out.add(Arrays.toString(\"a\\ud83d\\ude00b\".codePoints().toArray()) + \" \" + sb.codePoints().count() + \" \" + Character.toString(0x1F600).length() + \" \" + Character.toString(66));\n"
                + "CharSequence seq = \"\";\n"
                + "out.add(seq.isEmpty() + \" \" + \"x\".isEmpty());\n", 30, "java/util/Base64", "java/util/HexFormat", "java/security/", "java/net/URL");
    }

    @Test
    public void aListHasAFirstAndALastElement() throws Exception {
        RemapDifferential d = new RemapDifferential(tmp.newFolder(), 21);
        org.junit.Assume.assumeTrue("needs a JDK 21", d.available());
        d.same(""
                + "List<String> list = new ArrayList<>(List.of(\"a\", \"b\", \"c\"));\n"
                + "out.add(list.getFirst() + list.getLast());\n"
                + "List<String> back = list.reversed();\n"
                + "out.add(back);\n"
                + "back.add(\"z\");\n"
                + "out.add(list + \" \" + back);\n"
                + "out.add(list.removeFirst() + list.removeLast());\n"
                + "list.addFirst(\"0\");\n"
                + "list.addLast(\"9\");\n"
                + "out.add(list + \" \" + back + \" \" + back.getFirst());\n"
                + "ArrayList<Integer> numbers = new ArrayList<>(List.of(1, 2, 3));\n"
                + "out.add(numbers.getFirst() + numbers.getLast() + numbers.reversed().get(0));\n"
                + "try {\n"
                + "    new ArrayList<String>().getFirst();\n"
                + "    out.add(\"no failure\");\n"
                + "} catch (NoSuchElementException e) {\n"
                + "    out.add(\"empty\");\n"
                + "}\n"
                + "LinkedList<String> linked = new LinkedList<>(list);\n"
                + "out.add(linked.getFirst() + linked.getLast());\n", 8);
    }

    @Test
    public void aRecordsEqualsHashCodeAndToStringAreTheJdks() throws Exception {
        RemapDifferential d = new RemapDifferential(tmp.newFolder(), 17);
        org.junit.Assume.assumeTrue("needs a JDK 17", d.available());
        d.source("Point", ""
                + "public record Point(int x, int y) { }\n");
        d.source("All", ""
                + "public record All(boolean flag, byte b, char c, short s, int i, long l, float f, double d, String text, int[] array, Point point, List<Integer> list) { }\n");
        d.source("Empty", ""
                + "public record Empty() { }\n");
        d.source("Outer", ""
                + "public class Outer {\n"
                + "    public record Inner(String name, long value) { }\n"
                + "}\n");
        d.source("Pair", ""
                + "public record Pair<A, B>(A first, B second) {\n"
                + "    public Pair<B, A> swapped() { return new Pair<>(second, first); }\n"
                + "}\n");
        d.source("Custom", ""
                + "public record Custom(String name) {\n"
                + "    public Custom {\n"
                + "        if (name == null) { throw new IllegalArgumentException(\"name\"); }\n"
                + "    }\n"
                + "    @Override public String toString() { return \"custom \" + name; }\n"
                + "    @Override public boolean equals(Object o) { return o instanceof Custom; }\n"
                + "    @Override public int hashCode() { return 7; }\n"
                + "}\n");
        d.same(""
                + "Point p = new Point(3, -4);\n"
                + "out.add(p + \" \" + p.hashCode() + \" \" + p.equals(new Point(3, -4)) + \" \" + p.equals(new Point(3, 4)) + \" \" + p.equals(null) + \" \" + p.equals(\"x\") + \" \" + p.x() + p.y());\n"
                + "All a = new All(true, (byte) -7, 'q', (short) 300, 123456789, 9876543210123L, 1.5f, -2.25, \"text\", new int[] {1}, p, null);\n"
                + "All b = new All(true, (byte) -7, 'q', (short) 300, 123456789, 9876543210123L, 1.5f, -2.25, \"text\", a.array(), new Point(3, -4), null);\n"
                + "out.add(a.equals(b) + \" \" + (a.hashCode() == b.hashCode()) + \" \" + a.equals(a));\n"
                + "String shown = a.toString();\n"
                + "out.add(shown.substring(0, shown.indexOf(\"array=\")) + shown.substring(shown.indexOf(\", point=\")));\n"
                + "All none = new All(false, (byte) 0, 'a', (short) 0, 0, 0L, 0f, 0.0, null, null, null, null);\n"
                + "out.add(none.hashCode() + \" \" + new All(true, (byte) 0, 'a', (short) 0, 0, 0L, 0f, 0.0, null, null, null, null).hashCode());\n"
                + "out.add(new All(false, (byte) 1, 'b', (short) 2, 3, 4L, 0f, 0.0, \"s\", null, p, List.of(1, 2)).hashCode());\n"
                + "Object[] variants = {\n"
                + "    new All(false, (byte) 1, 'a', (short) 0, 0, 0L, 0f, 0.0, null, null, null, null),\n"
                + "    new All(false, (byte) 0, 'b', (short) 0, 0, 0L, 0f, 0.0, null, null, null, null),\n"
                + "    new All(false, (byte) 0, 'a', (short) 1, 0, 0L, 0f, 0.0, null, null, null, null),\n"
                + "    new All(false, (byte) 0, 'a', (short) 0, 1, 0L, 0f, 0.0, null, null, null, null),\n"
                + "    new All(false, (byte) 0, 'a', (short) 0, 0, 1L << 40, 0f, 0.0, null, null, null, null),\n"
                + "    new All(false, (byte) 0, 'a', (short) 0, 0, 0L, -0f, 0.0, null, null, null, null),\n"
                + "    new All(false, (byte) 0, 'a', (short) 0, 0, 0L, 0f, -0.0, null, null, null, null),\n"
                + "    new All(false, (byte) 0, 'a', (short) 0, 0, 0L, 0f, 0.0, \"\", null, null, null),\n"
                + "    new All(false, (byte) 0, 'a', (short) 0, 0, 0L, 0f, 0.0, null, new int[0], null, null),\n"
                + "    new All(false, (byte) 0, 'a', (short) 0, 0, 0L, 0f, 0.0, null, null, p, null),\n"
                + "    new All(false, (byte) 0, 'a', (short) 0, 0, 0L, 0f, 0.0, null, null, null, List.of()),\n"
                + "};\n"
                + "StringBuilder differs = new StringBuilder();\n"
                + "for (Object v : variants) {\n"
                + "    differs.append(none.equals(v) ? 'e' : 'd').append(v.equals(none) ? 'e' : 'd').append(v.equals(v) ? 'e' : 'd');\n"
                + "}\n"
                + "out.add(differs);\n"
                + "for (int i = 0; i < 8; i++) {\n"
                + "    out.add(variants[i].hashCode() + \" \" + variants[i]);\n"
                + "}\n"
                + "All nan = new All(false, (byte) 0, 'a', (short) 0, 0, 0L, Float.NaN, Double.NaN, null, null, null, null);\n"
                + "All nan2 = new All(false, (byte) 0, 'a', (short) 0, 0, 0L, Float.NaN, Double.NaN, null, null, null, null);\n"
                + "out.add(nan.equals(nan2) + \" \" + nan.hashCode() + \" \" + (nan.hashCode() == nan2.hashCode()));\n"
                + "out.add(new Empty() + \" \" + new Empty().hashCode() + \" \" + new Empty().equals(new Empty()) + \" \" + new Empty().equals(p));\n"
                + "out.add(new Outer.Inner(\"in\", 2L) + \" \" + new Outer.Inner(\"in\", 2L).equals(new Outer.Inner(\"in\", 2L)) + \" \" + new Outer.Inner(\"in\", 2L).hashCode());\n"
                + "record Local(String name, List<Point> points) { }\n"
                + "Local local = new Local(\"l\", List.of(p, new Point(0, 0)));\n"
                + "out.add(local + \" \" + local.equals(new Local(\"l\", List.of(new Point(3, -4), new Point(0, 0)))) + \" \" + local.hashCode());\n"
                + "Pair<String, Integer> pair = new Pair<>(\"k\", 5);\n"
                + "out.add(pair + \" \" + pair.swapped() + \" \" + pair.equals(pair.swapped().swapped()) + \" \" + pair.hashCode());\n"
                + "Map<Point, String> byPoint = new HashMap<>();\n"
                + "byPoint.put(new Point(1, 2), \"one-two\");\n"
                + "out.add(byPoint.get(new Point(1, 2)) + \" \" + byPoint.get(new Point(2, 1)) + \" \" + new HashSet<>(List.of(new Point(1, 1), new Point(1, 1 + 0), new Point(2, 2))).size());\n"
                + "out.add(new Custom(\"x\").toString() + \" \" + new Custom(\"x\").equals(new Custom(\"y\")) + \" \" + new Custom(\"x\").hashCode());\n"
                + "out.add((Object) p instanceof Record);\n", 20, "java/lang/runtime/ObjectMethods");
    }

    @Test
    public void aSwitchOverPatternsChoosesTheJdksCase() throws Exception {
        RemapDifferential d = new RemapDifferential(tmp.newFolder(), 21);
        org.junit.Assume.assumeTrue("needs a JDK 21", d.available());
        d.source("Shape", ""
                + "public sealed interface Shape permits Circle, Square, Named { }\n");
        d.source("Circle", ""
                + "public record Circle(int radius) implements Shape { }\n");
        d.source("Square", ""
                + "public record Square(int side) implements Shape { }\n");
        d.source("Named", ""
                + "public record Named(String name, Shape shape) implements Shape { }\n");
        d.source("Level", ""
                + "public sealed interface Level permits Tone, Fixed { }\n");
        d.source("Fixed", ""
                + "public record Fixed(int value) implements Level { }\n");
        d.source("Tone", ""
                + "public enum Tone implements Level { LOW, MID, HIGH }\n");
        d.source("Shapes", ""
                + "public class Shapes {\n"
                + "    public static String describe(Object o) {\n"
                + "        return switch (o) {\n"
                + "            case null -> \"null\";\n"
                + "            case String s when s.isEmpty() -> \"empty string\";\n"
                + "            case String s -> \"string \" + s;\n"
                + "            case Integer i when i > 6 -> \"big \" + i;\n"
                + "            case Integer i -> \"int \" + i;\n"
                + "            case Character c -> \"char \" + c;\n"
                + "            case Long l -> \"long \" + l;\n"
                + "            case int[] array -> \"ints \" + array.length;\n"
                + "            case Circle c -> \"circle \" + c.radius();\n"
                + "            case Named(String name, Circle(int radius)) -> \"named circle \" + name + radius;\n"
                + "            case Shape s -> \"shape \" + s;\n"
                + "            case Tone t when t == Tone.HIGH -> \"high\";\n"
                + "            case Tone t -> \"tone \" + t;\n"
                + "            case List<?> list -> \"list \" + list.size();\n"
                + "            case CharSequence cs -> \"chars \" + cs.length();\n"
                + "            default -> \"other \" + o;\n"
                + "        };\n"
                + "    }\n"
                + "    public static int area(Shape s) {\n"
                + "        return switch (s) {\n"
                + "            case Circle c -> 3 * c.radius() * c.radius();\n"
                + "            case Square q -> q.side() * q.side();\n"
                + "            case Named n -> area(n.shape());\n"
                + "        };\n"
                + "    }\n"
                + "    public static String guarded(Shape s) {\n"
                + "        if (s instanceof Named(String name, Named(String inner, Shape shape))) {\n"
                + "            return name + \"/\" + inner + \"/\" + shape;\n"
                + "        }\n"
                + "        if (s instanceof Named(var name, var shape) && shape instanceof Square(int side)) {\n"
                + "            return name + \" square \" + side;\n"
                + "        }\n"
                + "        return \"plain\";\n"
                + "    }\n"
                + "    public static String tone(Tone t) {\n"
                + "        return switch (t) {\n"
                + "            case LOW -> \"low\";\n"
                + "            case Tone x when x.ordinal() == 1 -> \"middle\";\n"
                + "            case HIGH -> \"high\";\n"
                + "            case Tone x -> \"rest \" + x;\n"
                + "        };\n"
                + "    }\n"
                + "    public static int level(Tone t) {\n"
                + "        switch (t) {\n"
                + "            case LOW: return 1;\n"
                + "            case MID: return 2;\n"
                + "            default: return 3;\n"
                + "        }\n"
                + "    }\n"
                + "    public static String sealedLevel(Level l) {\n"
                + "        return switch (l) {\n"
                + "            case Tone.LOW -> \"LOW\";\n"
                + "            case Tone.MID -> \"MID\";\n"
                + "            case Tone t -> \"tone \" + t;\n"
                + "            case Fixed f -> \"fixed \" + f.value();\n"
                + "        };\n"
                + "    }\n"
                + "    public static String boxed(Integer i) {\n"
                + "        return switch (i) {\n"
                + "            case 0 -> \"zero\";\n"
                + "            case 1 -> \"one\";\n"
                + "            case Integer n when n > 50 -> \"large\";\n"
                + "            case Integer n -> \"n\" + n;\n"
                + "        };\n"
                + "    }\n"
                + "    public static String text(String s) {\n"
                + "        return switch (s) {\n"
                + "            case \"a\" -> \"A\";\n"
                + "            case String t when t.length() == 2 -> \"two\";\n"
                + "            case \"ccc\" -> \"C\";\n"
                + "            case String t -> \"any \" + t;\n"
                + "        };\n"
                + "    }\n"
                + "    public static String nullable(String s) {\n"
                + "        return switch (s) {\n"
                + "            case null -> \"nothing\";\n"
                + "            case String t -> \"something\";\n"
                + "        };\n"
                + "    }\n"
                + "}\n");
        d.same(""
                + "Object[] values = {null, \"text\", \"\", 5, 7, -1, 'c', 3L, 2.5, new int[] {1, 2}, new Circle(2), new Square(3), new Named(\"n\", new Circle(1)), Tone.LOW, Tone.HIGH, List.of(1), new StringBuilder(\"sb\")};\n"
                + "for (Object v : values) {\n"
                + "    out.add(Shapes.describe(v));\n"
                + "}\n"
                + "for (Shape s : new Shape[] {new Circle(1), new Square(2), new Named(\"a\", new Square(1)), new Named(\"b\", new Named(\"c\", new Circle(5)))}) {\n"
                + "    out.add(Shapes.area(s) + \" \" + Shapes.guarded(s));\n"
                + "}\n"
                + "for (Tone t : Tone.values()) {\n"
                + "    out.add(Shapes.tone(t) + \" \" + Shapes.level(t));\n"
                + "}\n"
                + "for (Level l : new Level[] {Tone.LOW, Tone.MID, Tone.HIGH, new Fixed(4)}) {\n"
                + "    out.add(Shapes.sealedLevel(l));\n"
                + "}\n"
                + "for (Integer i : new Integer[] {0, 1, 2, 100, -5}) {\n"
                + "    out.add(Shapes.boxed(i));\n"
                + "}\n"
                + "for (String s : new String[] {\"a\", \"bb\", \"ccc\", \"none\"}) {\n"
                + "    out.add(Shapes.text(s));\n"
                + "}\n"
                + "try {\n"
                + "    Shapes.tone(null);\n"
                + "    out.add(\"no failure\");\n"
                + "} catch (NullPointerException e) {\n"
                + "    out.add(\"null selector\");\n"
                + "}\n"
                + "out.add(Shapes.nullable(null) + \" \" + Shapes.nullable(\"s\"));\n", 35, "java/lang/runtime/SwitchBootstraps", "java/lang/runtime/ObjectMethods", "java/lang/MatchException");
    }
}

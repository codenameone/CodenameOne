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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Flow;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/// Classes issue #6153 found missing from the class library: Semaphore,
/// ConcurrentLinkedQueue, Flow, YearMonth, TemporalAdjusters, BigDecimal,
/// RoundingMode and InetAddress, each against the JDK's answers.
public final class JdkGapChecks {
    private JdkGapChecks() {
    }

    private static final String[] DECIMALS = {"0", "0.00", "1", "-1", "1.5", "-2.5", "2.5", "0.125", "123.4500",
        "-0.001", "1E+3", "1.23E-10", "99999999999999999999.99", "-7", "3", "0.1", "1000"};

    public static void run() {
        Bulk.check("JdkGap.semaphoreCountsPermits", () -> {
            Semaphore s = new Semaphore(2, true);
            Bulk.p("fair", s.isFair());
            Bulk.p("a1", s.tryAcquire());
            Bulk.p("a2", s.tryAcquire());
            Bulk.p("a3", s.tryAcquire());
            Bulk.p("timed", s.tryAcquire(20, TimeUnit.MILLISECONDS));
            s.release(3);
            Bulk.p("avail", s.availablePermits());
            s.acquire(2);
            Bulk.p("afterAcquire", s.availablePermits());
            Bulk.p("timedOk", s.tryAcquire(1, 20, TimeUnit.MILLISECONDS));
            s.release(4);
            Bulk.p("drained", s.drainPermits());
            Bulk.pThrown("negative", IllegalArgumentException.class, () -> s.release(-1));
            final Semaphore gate = new Semaphore(0);
            Thread t = new Thread(() -> gate.release());
            t.start();
            gate.acquire();
            t.join();
            Bulk.p("crossThread", gate.availablePermits());
        });
        Bulk.check("JdkGap.concurrentLinkedQueueIsFifo", () -> {
            ConcurrentLinkedQueue<String> q = new ConcurrentLinkedQueue<String>(Arrays.asList("a", "b"));
            q.offer("c");
            q.add("d");
            Bulk.p("size", q.size());
            Bulk.p("peek", q.peek());
            Bulk.p("poll", q.poll());
            Bulk.p("contains", q.contains("c"));
            Bulk.p("remove", q.remove("c"));
            Iterator<String> it = q.iterator();
            it.next();
            it.remove();
            q.offer("e");
            List<String> rest = new ArrayList<String>();
            for (String s : q) {
                rest.add(s);
            }
            Bulk.p("rest", rest.toString());
            Bulk.pThrown("nullOffer", NullPointerException.class, () -> q.offer(null));
            q.clear();
            Bulk.p("emptyPoll", q.poll());
            Bulk.p("isEmpty", q.isEmpty());
        });
        Bulk.check("JdkGap.flowWiresASubscription", () -> {
            Bulk.p("buffer", Flow.defaultBufferSize());
            final List<String> seen = new ArrayList<String>();
            Flow.Publisher<String> pub = sub -> {
                sub.onSubscribe(new Flow.Subscription() {
                    @Override
                    public void request(long n) {
                        seen.add("request" + n);
                    }

                    @Override
                    public void cancel() {
                        seen.add("cancel");
                    }
                });
                sub.onNext("x");
                sub.onComplete();
            };
            pub.subscribe(new Flow.Subscriber<String>() {
                @Override
                public void onSubscribe(Flow.Subscription s) {
                    s.request(1);
                }

                @Override
                public void onNext(String item) {
                    seen.add("next" + item);
                }

                @Override
                public void onError(Throwable t) {
                    seen.add("error");
                }

                @Override
                public void onComplete() {
                    seen.add("complete");
                }
            });
            Bulk.p("seen", seen.toString());
        });
        Bulk.check("JdkGap.yearMonthMatchesTheJdk", () -> {
            int[][] yms = {{2024, 2}, {2023, 2}, {1900, 2}, {2000, 12}, {-5, 1}, {12345, 7}, {1, 1}};
            for (int[] ym : yms) {
                YearMonth m = YearMonth.of(ym[0], ym[1]);
                String k = m.toString();
                Bulk.p(k + ".len", m.lengthOfMonth());
                Bulk.p(k + ".leap", m.isLeapYear());
                Bulk.p(k + ".plus13", m.plusMonths(13).toString());
                Bulk.p(k + ".minus14", m.minusMonths(14).toString());
                Bulk.p(k + ".plusYears", m.plusYears(3).toString());
                Bulk.p(k + ".atDay1", m.atDay(1).toString());
                Bulk.p(k + ".end", m.atEndOfMonth().toString());
                Bulk.p(k + ".month", m.getMonth().name());
                final YearMonth fm = m;
                Bulk.p(k + ".parse", safe(() -> YearMonth.parse(fm.toString()).equals(fm)));
                int c = m.compareTo(YearMonth.of(2000, 1));
                Bulk.p(k + ".vs2000", (c < 0 ? -1 : (c > 0 ? 1 : 0)) + ":" + m.isAfter(YearMonth.of(2000, 1)));
            }
            Bulk.p("before", YearMonth.of(2024, 1).isBefore(YearMonth.of(2024, 2)));
            Bulk.p("hash", YearMonth.of(2024, 1).hashCode() == YearMonth.of(2024, 1).hashCode());
            Bulk.p("parse", YearMonth.parse("2024-03").toString());
            Bulk.p("parseSigned", safe(() -> YearMonth.parse("+12345-07")));
            Bulk.p("parseNegative", safe(() -> YearMonth.parse("-0005-01")));
            Bulk.pThrown("badMonth", DateTimeException.class, () -> YearMonth.of(2024, 13));
            Bulk.pThrown("badParse", DateTimeException.class, () -> YearMonth.parse("2024-3"));
            Bulk.pThrown("badDay", DateTimeException.class, () -> YearMonth.of(2023, 2).atDay(29));
        });
        Bulk.check("JdkGap.temporalAdjustersMatchTheJdk", () -> {
            LocalDate[] dates = {LocalDate.of(2024, 2, 10), LocalDate.of(2024, 2, 29), LocalDate.of(2023, 12, 31),
                LocalDate.of(2024, 1, 1), LocalDate.of(2026, 11, 1)};
            for (LocalDate d : dates) {
                String k = d.toString();
                Bulk.p(k + ".first", d.with(TemporalAdjusters.firstDayOfMonth()).toString());
                Bulk.p(k + ".last", d.with(TemporalAdjusters.lastDayOfMonth()).toString());
                Bulk.p(k + ".nextMonth", d.with(TemporalAdjusters.firstDayOfNextMonth()).toString());
                Bulk.p(k + ".firstYear", d.with(TemporalAdjusters.firstDayOfYear()).toString());
                Bulk.p(k + ".lastYear", d.with(TemporalAdjusters.lastDayOfYear()).toString());
                Bulk.p(k + ".nextYear", d.with(TemporalAdjusters.firstDayOfNextYear()).toString());
                for (DayOfWeek w : DayOfWeek.values()) {
                    Bulk.p(k + "." + w + ".next", d.with(TemporalAdjusters.next(w)).toString());
                    Bulk.p(k + "." + w + ".nextOrSame", d.with(TemporalAdjusters.nextOrSame(w)).toString());
                    Bulk.p(k + "." + w + ".prev", d.with(TemporalAdjusters.previous(w)).toString());
                    Bulk.p(k + "." + w + ".prevOrSame", d.with(TemporalAdjusters.previousOrSame(w)).toString());
                }
            }
            Bulk.p("withDate", LocalDate.of(2024, 1, 1).with(LocalDate.of(1999, 5, 6)).toString());
        });
        Bulk.check("JdkGap.bigDecimalArithmeticMatchesTheJdk", () -> {
            for (String a : DECIMALS) {
                final BigDecimal x = new BigDecimal(a);
                Bulk.p(a + ".str", x.toString());
                Bulk.p(a + ".plain", x.toPlainString());
                Bulk.p(a + ".scale", x.scale());
                Bulk.p(a + ".signum", x.signum());
                Bulk.p(a + ".hash", x.hashCode());
                Bulk.p(a + ".strip", x.stripTrailingZeros().toString());
                Bulk.p(a + ".neg", x.negate().toString());
                Bulk.p(a + ".abs", x.abs().toString());
                Bulk.p(a + ".long", x.longValue());
                Bulk.p(a + ".int", x.intValue());
                Bulk.p(a + ".double", x.doubleValue());
                for (final RoundingMode m : RoundingMode.values()) {
                    Bulk.p(a + ".set1." + m.name(), safe(() -> x.setScale(1, m)));
                    Bulk.p(a + ".set0." + m.name(), safe(() -> x.setScale(0, m)));
                }
                for (String b : DECIMALS) {
                    final BigDecimal y = new BigDecimal(b);
                    String k = a + "," + b;
                    Bulk.p(k + ".add", x.add(y).toString());
                    Bulk.p(k + ".sub", x.subtract(y).toString());
                    Bulk.p(k + ".mul", x.multiply(y).toString());
                    Bulk.p(k + ".cmp", x.compareTo(y));
                    Bulk.p(k + ".eq", x.equals(y));
                    Bulk.p(k + ".div", safe(() -> x.divide(y)));
                    Bulk.p(k + ".div4he", safe(() -> x.divide(y, 4, RoundingMode.HALF_EVEN)));
                    Bulk.p(k + ".div2up", safe(() -> x.divide(y, 2, RoundingMode.UP)));
                    Bulk.p(k + ".divFloor", safe(() -> x.divide(y, RoundingMode.FLOOR)));
                }
            }
            Bulk.p("valueOfLong", BigDecimal.valueOf(Long.MIN_VALUE).toString());
            Bulk.p("valueOfScaled", BigDecimal.valueOf(12345, 3).toString());
            Bulk.p("valueOfDouble", BigDecimal.valueOf(0.1).toString());
            Bulk.p("valueOfDoubleBig", BigDecimal.valueOf(1e20).toString());
            Bulk.p("exactDouble", new BigDecimal(0.1).toString());
            Bulk.p("exactDoubleNeg", new BigDecimal(-2.5e-3).toString());
            Bulk.p("exactDoubleBig", new BigDecimal(1e20).toString());
            Bulk.p("constants", BigDecimal.ZERO + "," + BigDecimal.ONE + "," + BigDecimal.TEN);
            Bulk.p("bigLong", new BigDecimal("123456789012345678901234567890").longValue());
            Bulk.p("pow", new BigDecimal("1.1").pow(5).toString());
            Bulk.p("remainder", new BigDecimal("10.5").remainder(new BigDecimal("3")).toString());
            Bulk.pThrown("badString", NumberFormatException.class, () -> new BigDecimal("1.2.3"));
            Bulk.pThrown("nonTerminating", ArithmeticException.class, () -> BigDecimal.ONE.divide(new BigDecimal("3")));
            Bulk.p("roundingValueOf", RoundingMode.valueOf(BigDecimal.ROUND_HALF_EVEN).name());
        });
        Bulk.check("JdkGap.inetAddressLiteralsMatchTheJdk", () -> {
            String[] hosts = {"127.0.0.1", "10.1.2.3", "172.16.0.1", "172.32.0.1", "192.168.1.1", "169.254.1.1",
                "0.0.0.0", "8.8.8.8", "224.0.0.1", "::1", "::", "fe80::1", "fec0::1", "2001:db8::ff00:42:8329",
                "[2001:db8::1]", "::ffff:1.2.3.4", "1:2:3:4:5:6:7:8", "ff02::1"};
            for (String h : hosts) {
                InetAddress a = InetAddress.getByName(h);
                Bulk.p(h + ".v4", a instanceof Inet4Address);
                Bulk.p(h + ".v6", a instanceof Inet6Address);
                Bulk.p(h + ".addr", a.getHostAddress());
                Bulk.p(h + ".str", a.toString());
                Bulk.p(h + ".bytes", Arrays.toString(a.getAddress()));
                Bulk.p(h + ".loop", a.isLoopbackAddress());
                Bulk.p(h + ".site", a.isSiteLocalAddress());
                Bulk.p(h + ".any", a.isAnyLocalAddress());
                Bulk.p(h + ".link", a.isLinkLocalAddress());
                Bulk.p(h + ".multi", a.isMulticastAddress());
                Bulk.p(h + ".all", InetAddress.getAllByName(h).length);
                InetAddress same = InetAddress.getByAddress(a.getAddress());
                Bulk.p(h + ".eq", a.equals(same) && a.hashCode() == same.hashCode());
                if (a instanceof Inet4Address) {
                    Bulk.p(h + ".hash", a.hashCode());
                }
            }
            InetAddress named = InetAddress.getByAddress("db", new byte[] {10, 0, 0, 9});
            Bulk.p("named", named.toString());
            Bulk.p("namedHost", named.getHostName());
            Bulk.pThrown("badLength", UnknownHostException.class, () -> InetAddress.getByAddress(new byte[3]));
            Bulk.pThrown("badV6", UnknownHostException.class, () -> InetAddress.getByName("1::2::3"));
        });
        Bulk.cn1Only("JdkGap.inetAddressDoesNotResolveNames", () -> {
            Bulk.expectEquals("UnknownHostException",
                    Bulk.thrown(UnknownHostException.class, () -> InetAddress.getByName("example.com")),
                    "a host name is not resolved");
            Bulk.expect(InetAddress.getByName("localhost").isLoopbackAddress(), "localhost is the loopback");
        });
    }

    /// What `c` answers as text, or the simple name of what it threw.
    private static String safe(Op c) {
        try {
            return String.valueOf(c.call());
        } catch (Exception e) {
            return e.getClass().getSimpleName();
        }
    }

    private interface Op {
        Object call() throws Exception;
    }
}

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
package com.codename1.spreadsheet;

import com.codename1.util.MathUtil;

/// The financial functions: loans and annuities, cash flows and depreciation.
final class FinancialFunctions {
    static final String SCALAR_NAMES = "CUMIPMT CUMPRINC DB DDB DOLLARDE DOLLARFR EFFECT FV IPMT ISPMT NOMINAL NPER"
            + " PDURATION PMT PPMT PV RATE RRI SLN SYD VDB";

    static final String RANGE_NAMES = "FVSCHEDULE IRR MIRR NPV XIRR XNPV";

    private FinancialFunctions() {
    }

    private static Double d(double v) {
        return Double.valueOf(v);
    }

    private static double pow(double x, double y) {
        return Evaluator.power(x, y);
    }

    static double pmt(double rate, double nper, double pv, double fv, double type) {
        if (nper == 0) {
            throw EvalError.NUM;
        }
        if (rate == 0) {
            return -(pv + fv) / nper;
        }
        double f = pow(1 + rate, nper);
        return -(rate * (pv * f + fv)) / ((1 + rate * type) * (f - 1));
    }

    static double fv(double rate, double nper, double pmt, double pv, double type) {
        if (rate == 0) {
            return -(pv + pmt * nper);
        }
        double f = pow(1 + rate, nper);
        return -(pv * f + pmt * (1 + rate * type) * (f - 1) / rate);
    }

    private static double ipmt(double rate, double per, double nper, double pv, double fv, double type) {
        if (per < 1 || per > nper) {
            throw EvalError.NUM;
        }
        double payment = pmt(rate, nper, pv, fv, type);
        if (per == 1) {
            return type != 0 ? 0 : -pv * rate;
        }
        // the balance after the payments before this one
        double balance = type != 0 ? fv(rate, per - 2, payment, pv, 1) - payment : fv(rate, per - 1, payment, pv, 0);
        return balance * rate;
    }

    static Object call(String name, Evaluator ev, Object[] a) {
        switch (Functions.index(SWITCH_1, name)) {
            case 0: // PMT
                return d(pmt(Evaluator.num(a, 0), Evaluator.num(a, 1), Evaluator.num(a, 2), Evaluator.num(a, 3, 0),
                        Evaluator.num(a, 4, 0) != 0 ? 1 : 0));
            case 1: // FV
                return d(fv(Evaluator.num(a, 0), Evaluator.num(a, 1), Evaluator.num(a, 2), Evaluator.num(a, 3, 0),
                        Evaluator.num(a, 4, 0) != 0 ? 1 : 0));
            case 2: { // PV
                double rate = Evaluator.num(a, 0);
                double nper = Evaluator.num(a, 1);
                double payment = Evaluator.num(a, 2);
                double future = Evaluator.num(a, 3, 0);
                double type = Evaluator.num(a, 4, 0) != 0 ? 1 : 0;
                if (rate == 0) {
                    return d(-(future + payment * nper));
                }
                double f = pow(1 + rate, nper);
                return d(-(future + payment * (1 + rate * type) * (f - 1) / rate) / f);
            }
            case 3: { // NPER
                double rate = Evaluator.num(a, 0);
                double payment = Evaluator.num(a, 1);
                double pv = Evaluator.num(a, 2);
                double future = Evaluator.num(a, 3, 0);
                double type = Evaluator.num(a, 4, 0) != 0 ? 1 : 0;
                if (rate == 0) {
                    if (payment == 0) {
                        throw EvalError.NUM;
                    }
                    return d(-(pv + future) / payment);
                }
                double k = payment * (1 + rate * type) / rate;
                double ratio = (k - future) / (k + pv);
                if (ratio <= 0) {
                    throw EvalError.NUM;
                }
                return d(MathUtil.log(ratio) / MathUtil.log(1 + rate));
            }
            case 4: { // RATE
                double nper = Evaluator.num(a, 0);
                double payment = Evaluator.num(a, 1);
                double pv = Evaluator.num(a, 2);
                double future = Evaluator.num(a, 3, 0);
                double type = Evaluator.num(a, 4, 0) != 0 ? 1 : 0;
                double rate = Evaluator.num(a, 5, 0.1);
                // no interest at all is an answer the search below cannot land on exactly
                if (Math.abs(fv(0, nper, payment, pv, type) - future) <= 1e-12 * (Math.abs(pv) + Math.abs(payment * nper) + Math.abs(future))) {
                    return d(0);
                }
                for (int i = 0; i < 100; i++) {
                    double y = fv(rate, nper, payment, pv, type) - future;
                    double h = Math.max(1e-7, Math.abs(rate) * 1e-7);
                    double slope = (fv(rate + h, nper, payment, pv, type) - future - y) / h;
                    if (slope == 0 || Double.isNaN(slope) || Double.isNaN(y)) {
                        break;
                    }
                    double next = rate - y / slope;
                    if (Math.abs(next - rate) < 1e-11) {
                        return d(next);
                    }
                    rate = next;
                }
                throw EvalError.NUM;
            }
            case 5: // IPMT
                return d(ipmt(Evaluator.num(a, 0), Evaluator.num(a, 1), Evaluator.num(a, 2), Evaluator.num(a, 3),
                        Evaluator.num(a, 4, 0), Evaluator.num(a, 5, 0) != 0 ? 1 : 0));
            case 6: { // PPMT
                double rate = Evaluator.num(a, 0);
                double nper = Evaluator.num(a, 2);
                double pv = Evaluator.num(a, 3);
                double future = Evaluator.num(a, 4, 0);
                double type = Evaluator.num(a, 5, 0) != 0 ? 1 : 0;
                return d(pmt(rate, nper, pv, future, type) - ipmt(rate, Evaluator.num(a, 1), nper, pv, future, type));
            }
            case 7: // CUMIPMT
            case 8: { // CUMPRINC
                double rate = Evaluator.num(a, 0);
                double nper = Evaluator.num(a, 1);
                double pv = Evaluator.num(a, 2);
                int start = Evaluator.integer(a, 3);
                int end = Evaluator.integer(a, 4);
                double type = Evaluator.num(a, 5);
                if (rate <= 0 || nper <= 0 || pv <= 0 || start < 1 || end < start || end > nper
                        || type != 0 && type != 1) {
                    throw EvalError.NUM;
                }
                double payment = pmt(rate, nper, pv, 0, type);
                double total = 0;
                for (int per = start; per <= end; per++) {
                    double interest = ipmt(rate, per, nper, pv, 0, type);
                    total += "CUMIPMT".equals(name) ? interest : payment - interest;
                }
                return d(total);
            }
            case 9: { // ISPMT
                double nper = Evaluator.num(a, 2);
                if (nper == 0) {
                    throw EvalError.DIV0;
                }
                return d(-Evaluator.num(a, 3) * Evaluator.num(a, 0) * (1 - Evaluator.num(a, 1) / nper));
            }
            case 10: { // SLN
                double life = Evaluator.num(a, 2);
                if (life == 0) {
                    throw EvalError.DIV0;
                }
                return d((Evaluator.num(a, 0) - Evaluator.num(a, 1)) / life);
            }
            case 11: { // SYD
                double life = Evaluator.num(a, 2);
                double per = Evaluator.num(a, 3);
                if (life <= 0 || per < 1 || per > life) {
                    throw EvalError.NUM;
                }
                return d((Evaluator.num(a, 0) - Evaluator.num(a, 1)) * (life - per + 1) * 2 / (life * (life + 1)));
            }
            case 12: { // DB
                double cost = Evaluator.num(a, 0);
                double salvage = Evaluator.num(a, 1);
                int life = Evaluator.integer(a, 2);
                int period = Evaluator.integer(a, 3);
                int month = Evaluator.integer(a, 4, 12);
                if (cost < 0 || salvage < 0 || life <= 0 || period < 1 || month < 1 || month > 12
                        || period > life + (month < 12 ? 1 : 0)) {
                    throw EvalError.NUM;
                }
                if (cost == 0) {
                    return d(0);
                }
                double rate = Values.round(1 - pow(salvage / cost, 1.0 / life), 3);
                double total = cost * rate * month / 12;
                double current = total;
                for (int p = 2; p <= period; p++) {
                    if (p == life + 1) {
                        current = (cost - total) * rate * (12 - month) / 12;
                    } else {
                        current = (cost - total) * rate;
                    }
                    total += current;
                }
                return d(current);
            }
            case 13: { // DDB
                double cost = Evaluator.num(a, 0);
                double salvage = Evaluator.num(a, 1);
                double life = Evaluator.num(a, 2);
                double period = Evaluator.num(a, 3);
                double factor = Evaluator.num(a, 4, 2);
                if (cost < 0 || salvage < 0 || life <= 0 || period < 1 || period > life || factor <= 0) {
                    throw EvalError.NUM;
                }
                double book = cost;
                double current = 0;
                for (int p = 1; p <= (int) period; p++) {
                    current = Math.min(book * factor / life, Math.max(0, book - salvage));
                    book -= current;
                }
                return d(current);
            }
            case 14: // VDB
                return d(vdb(a));
            case 15: { // EFFECT
                double nominal = Evaluator.num(a, 0);
                double periods = Math.floor(Evaluator.num(a, 1));
                if (nominal <= 0 || periods < 1) {
                    throw EvalError.NUM;
                }
                return d(pow(1 + nominal / periods, periods) - 1);
            }
            case 16: { // NOMINAL
                double effective = Evaluator.num(a, 0);
                double periods = Math.floor(Evaluator.num(a, 1));
                if (effective <= 0 || periods < 1) {
                    throw EvalError.NUM;
                }
                return d((pow(1 + effective, 1 / periods) - 1) * periods);
            }
            case 17: { // RRI
                double nper = Evaluator.num(a, 0);
                double pv = Evaluator.num(a, 1);
                if (nper <= 0 || pv == 0) {
                    throw EvalError.NUM;
                }
                return d(pow(Evaluator.num(a, 2) / pv, 1 / nper) - 1);
            }
            case 18: { // PDURATION
                double rate = Evaluator.num(a, 0);
                double pv = Evaluator.num(a, 1);
                double future = Evaluator.num(a, 2);
                if (rate <= 0 || pv <= 0 || future <= 0) {
                    throw EvalError.NUM;
                }
                return d((MathUtil.log(future) - MathUtil.log(pv)) / MathUtil.log(1 + rate));
            }
            case 19: // DOLLARDE
            case 20: { // DOLLARFR
                double x = Evaluator.num(a, 0);
                double fraction = Math.floor(Evaluator.num(a, 1));
                if (fraction < 0) {
                    throw EvalError.NUM;
                }
                if (fraction == 0) {
                    throw EvalError.DIV0;
                }
                double whole = x < 0 ? Math.ceil(x) : Math.floor(x);
                double scale = Values.pow10((int) Math.ceil(MathUtil.log10(fraction)));
                if ("DOLLARDE".equals(name)) {
                    return d(whole + (x - whole) * scale / fraction);
                }
                return d(whole + (x - whole) * fraction / scale);
            }
            case 21: { // NPV
                double rate = ev.scalarNum(a, 0);
                DoubleList flows = ev.numbers(a, 1);
                double total = 0;
                double f = 1;
                for (int i = 0; i < flows.size; i++) {
                    f *= 1 + rate;
                    total += flows.data[i] / f;
                }
                return d(total);
            }
            case 22: { // FVSCHEDULE
                DoubleList rates = new DoubleList();
                ev.numbers(Evaluator.arg(a, 1), rates, false);
                double v = ev.scalarNum(a, 0);
                for (int i = 0; i < rates.size; i++) {
                    v *= 1 + rates.data[i];
                }
                return d(v);
            }
            case 23: { // IRR
                DoubleList flows = new DoubleList();
                ev.numbers(Evaluator.arg(a, 0), flows, false);
                double[] times = new double[flows.size];
                for (int i = 0; i < times.length; i++) {
                    times[i] = i;
                }
                return d(solveRate(flows, times, ev.scalarNum(a, 1, 0.1)));
            }
            case 24: { // MIRR
                DoubleList flows = new DoubleList();
                ev.numbers(Evaluator.arg(a, 0), flows, false);
                double finance = ev.scalarNum(a, 1);
                double reinvest = ev.scalarNum(a, 2);
                int n = flows.size;
                double positive = 0;
                double negative = 0;
                for (int i = 0; i < n; i++) {
                    double v = flows.data[i];
                    if (v > 0) {
                        positive += v * pow(1 + reinvest, n - 1 - i);
                    } else {
                        negative += v / pow(1 + finance, i);
                    }
                }
                if (positive == 0 || negative == 0 || n < 2) {
                    throw EvalError.DIV0;
                }
                return d(pow(-positive / negative, 1.0 / (n - 1)) - 1);
            }
            case 25: // XNPV
            case 26: { // XIRR
                boolean npv = "XNPV".equals(name);
                DoubleList flows = new DoubleList();
                DoubleList dates = new DoubleList();
                ev.numbers(Evaluator.arg(a, npv ? 1 : 0), flows, false);
                ev.numbers(Evaluator.arg(a, npv ? 2 : 1), dates, false);
                if (flows.size != dates.size || flows.size == 0) {
                    throw EvalError.NUM;
                }
                double[] times = new double[dates.size];
                for (int i = 0; i < times.length; i++) {
                    times[i] = (Math.floor(dates.data[i]) - Math.floor(dates.data[0])) / 365;
                    if (times[i] < 0) {
                        throw EvalError.NUM;
                    }
                }
                if (npv) {
                    double rate = ev.scalarNum(a, 0);
                    if (rate <= -1) {
                        throw EvalError.NUM;
                    }
                    return d(present(flows, times, rate));
                }
                return d(solveRate(flows, times, ev.scalarNum(a, 2, 0.1)));
            }
            default:
                return CellError.NAME;
        }
    }

    private static double present(DoubleList flows, double[] times, double rate) {
        double total = 0;
        for (int i = 0; i < flows.size; i++) {
            total += flows.data[i] / MathUtil.pow(1 + rate, times[i]);
        }
        return total;
    }

    /// The rate at which cash flows are worth nothing today: Newton's method from the guess,
    /// then bisection if that wanders off.
    private static double solveRate(DoubleList flows, double[] times, double guess) {
        boolean positive = false;
        boolean negative = false;
        for (int i = 0; i < flows.size; i++) {
            positive |= flows.data[i] > 0;
            negative |= flows.data[i] < 0;
        }
        if (!positive || !negative) {
            throw EvalError.NUM;
        }
        double rate = guess;
        for (int i = 0; i < 60; i++) {
            if (rate <= -1) {
                break;
            }
            double y = present(flows, times, rate);
            double slope = 0;
            for (int k = 0; k < flows.size; k++) {
                slope -= times[k] * flows.data[k] / MathUtil.pow(1 + rate, times[k] + 1);
            }
            if (slope == 0 || Double.isNaN(y) || Double.isNaN(slope)) {
                break;
            }
            double next = rate - y / slope;
            if (Math.abs(next - rate) < 1e-12) {
                return next;
            }
            rate = next;
        }
        double lo = -0.999999;
        double hi = 1;
        double flo = present(flows, times, lo);
        double fhi = present(flows, times, hi);
        for (int i = 0; i < 60 && flo * fhi > 0; i++) {
            hi *= 2;
            fhi = present(flows, times, hi);
        }
        if (flo * fhi > 0) {
            throw EvalError.NUM;
        }
        for (int i = 0; i < 200; i++) {
            double mid = (lo + hi) / 2;
            double fm = present(flows, times, mid);
            if (flo * fm <= 0) {
                hi = mid;
            } else {
                lo = mid;
                flo = fm;
            }
        }
        return (lo + hi) / 2;
    }

    private static double vdb(Object[] a) {
        double cost = Evaluator.num(a, 0);
        double salvage = Evaluator.num(a, 1);
        double life = Evaluator.num(a, 2);
        double start = Evaluator.num(a, 3);
        double end = Evaluator.num(a, 4);
        double factor = Evaluator.num(a, 5, 2);
        boolean noSwitch = Evaluator.bool(a, 6, false);
        if (cost < 0 || salvage < 0 || life <= 0 || start < 0 || end < start || end > life || factor <= 0) {
            throw EvalError.NUM;
        }
        double total = 0;
        double book = cost;
        boolean straight = false;
        double line = 0;
        int last = (int) Math.ceil(end);
        for (int p = 1; p <= last; p++) {
            double dep;
            if (straight) {
                dep = line;
            } else {
                dep = Math.min(book * factor / life, Math.max(0, book - salvage));
                double remaining = life - p + 1;
                double sl = remaining <= 0 ? 0 : (book - salvage) / remaining;
                if (!noSwitch && sl > dep) {
                    straight = true;
                    line = sl;
                    dep = sl;
                }
            }
            dep = Math.min(dep, Math.max(0, book - salvage));
            // the part of this period that lies between start and end
            double from = Math.max(start, p - 1);
            double to = Math.min(end, p);
            if (to > from) {
                total += dep * (to - from);
            }
            book -= dep;
        }
        return total;
    }

    private static final java.util.HashMap<String, Integer> SWITCH_1 = Functions.switchTable(
            "PMT", "FV", "PV", "NPER", "RATE", "IPMT", "PPMT", "CUMIPMT", "CUMPRINC", "ISPMT",
            "SLN", "SYD", "DB", "DDB", "VDB", "EFFECT", "NOMINAL", "RRI", "PDURATION", "DOLLARDE",
            "DOLLARFR", "NPV", "FVSCHEDULE", "IRR", "MIRR", "XNPV", "XIRR");
}

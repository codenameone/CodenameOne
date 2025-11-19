package com.codename1.util;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Display;

import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class UtilStringsAndCallbacksTest extends UITestBase {

    @FormTest
    void wrapperAndStringUtilitiesRoundTrip() {
        Wrapper<String> wrapped = new Wrapper<String>("start");
        assertEquals("start", wrapped.get());
        wrapped.set("updated");
        assertEquals("updated", wrapped.get());

        String source = "a,b,,c";
        assertEquals("a|b||c", StringUtil.replaceAll(source, ",", "|"));
        assertEquals("a|b,,c", StringUtil.replaceFirst(source, ",", "|"));

        List<String> tokens = StringUtil.tokenize("  spaced  text  ", ' ');
        assertTrue(tokens.contains("spaced"));
        assertTrue(tokens.contains("text"));

        List<String> csv = StringUtil.tokenize("one,two,three", ',');
        assertEquals(3, csv.size());

        CStringBuilder builder = new CStringBuilder().append(true).append(',').append(5).append(',').append(4L).append(',').append('Z').insert(0, "[");
        builder.append(']');
        assertTrue(builder.toString().startsWith("[true,5,4,Z]"));
        assertEquals('t', builder.charAt(1));
    }

    @FormTest
    void callbackDispatcherUsesSerialQueue() throws InterruptedException {
        final AtomicReference<String> successValue = new AtomicReference<String>(null);
        final AtomicReference<Throwable> errorValue = new AtomicReference<Throwable>(null);
        final AtomicBoolean ranOnSerialQueue = new AtomicBoolean(false);

        Runnable successInvoker = new Runnable() {
            public void run() {
                CallbackDispatcher.dispatchSuccess(new SuccessCallback<String>() {
                    public void onSucess(String value) {
                        ranOnSerialQueue.set(true);
                        successValue.set(value + "!" + Display.getInstance().isEdt());
                    }
                }, "ok");
            }
        };
        Thread successThread = new Thread(successInvoker);
        successThread.start();
        successThread.join(200);

        int attempts = 0;
        while (successValue.get() == null && attempts++ < 5) {
            flushSerialCalls();
            Thread.sleep(10L);
        }
        assertEquals("ok!true", successValue.get());
        assertTrue(ranOnSerialQueue.get());

        Runnable errorInvoker = new Runnable() {
            public void run() {
                CallbackDispatcher.dispatchError(new FailureCallback<Object>() {
                    public void onError(Object source, Throwable err, int errorCode, String errorMessage) {
                        errorValue.set(err);
                    }
                }, new IllegalStateException("boom"));
            }
        };
        Thread errorThread = new Thread(errorInvoker);
        errorThread.start();
        errorThread.join(200);
        attempts = 0;
        while (errorValue.get() == null && attempts++ < 5) {
            flushSerialCalls();
            Thread.sleep(10L);
        }
        assertEquals("boom", errorValue.get().getMessage());
    }

    @FormTest
    void dateUtilComparesAndOffsets() {
        Date base = new Date(0);
        Date later = new Date(DateUtil.HOUR + DateUtil.MINUTE);
        assertEquals(base, DateUtil.min(base, later));
        assertEquals(later, DateUtil.max(base, later));
        assertEquals(-1, DateUtil.compare(base, later));
        assertEquals(1, DateUtil.compare(later, base));
        assertEquals(1, DateUtil.compare(base, null));
        assertEquals(0, DateUtil.compare(null, null));

        DateUtil util = new DateUtil(TimeZone.getTimeZone("GMT+2"));
        int offset = util.getOffset(later.getTime());
        assertTrue(offset >= 2 * DateUtil.HOUR - DateUtil.MINUTE);

        assertTrue(DateUtil.compareByDateField(DateUtil.YEAR).compare(new Date(0), new Date(DateUtil.YEAR * 2)) < 0);
    }

    @FormTest
    void mathUtilProvidesStableResults() {
        assertEquals(Math.exp(1.5), MathUtil.exp(1.5), 1e-9);
        assertEquals(Math.log(10.0), MathUtil.log(10.0), 1e-9);
        assertEquals(Math.log10(1000.0), MathUtil.log10(1000.0), 1e-9);
        assertEquals(Math.pow(2.0, 5.0), MathUtil.pow(2.0, 5.0), 1e-9);

        assertEquals(Math.asin(0.25), MathUtil.asin(0.25), 1e-9);
        assertEquals(Math.acos(0.25), MathUtil.acos(0.25), 1e-9);
        assertEquals(Math.atan(0.5), MathUtil.atan(0.5), 1e-9);
        assertEquals(Math.atan2(1.0, 2.0), MathUtil.atan2(1.0, 2.0), 1e-9);

        assertEquals(Double.doubleToLongBits(Math.scalb(2.0, 3)), Double.doubleToLongBits(MathUtil.scalb(2.0, 3)));
        assertEquals(Double.doubleToLongBits(Math.scalb(2.0, -3)), Double.doubleToLongBits(MathUtil.scalb(2.0, -3)));
    }
}

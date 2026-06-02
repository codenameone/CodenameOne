package com.codename1.background;

import com.codename1.util.Callback;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/// Tests for the WorkRequest builder and its constraints.
class WorkRequestTest {

    public static class SampleWorker implements BackgroundWorker {
        public void performWork(String workId, Map<String, String> inputData, long deadline, Callback<Boolean> onComplete) {
            onComplete.onSucess(Boolean.TRUE);
        }
    }

    @Test
    void builderCapturesConstraintsAndData() {
        WorkRequest r = WorkRequest.builder("sync", SampleWorker.class)
                .setRequiresNetwork(true)
                .setRequiresCharging(true)
                .setRequiresBatteryNotLow(true)
                .setPeriodic(6 * 60 * 60 * 1000L)
                .setInitialDelay(5000L)
                .putInputData("account", "primary")
                .build();
        assertEquals("sync", r.getId());
        assertEquals(SampleWorker.class.getName(), r.getWorkerClass());
        assertTrue(r.isRequiresNetwork());
        assertTrue(r.isRequiresCharging());
        assertTrue(r.isRequiresBatteryNotLow());
        assertFalse(r.isRequiresIdle());
        assertTrue(r.isPeriodic());
        assertEquals(6 * 60 * 60 * 1000L, r.getMinIntervalMillis());
        assertEquals(5000L, r.getInitialDelayMillis());
        assertEquals("primary", r.getInputData().get("account"));
    }

    @Test
    void oneShotByDefault() {
        WorkRequest r = WorkRequest.builder("once", SampleWorker.class).build();
        assertFalse(r.isPeriodic());
        assertEquals(0L, r.getMinIntervalMillis());
        assertTrue(r.getInputData().isEmpty());
    }

    @Test
    void inputDataCopyIsDefensive() {
        WorkRequest r = WorkRequest.builder("x", SampleWorker.class).putInputData("k", "v").build();
        Map<String, String> copy = r.getInputData();
        copy.put("k", "mutated");
        assertEquals("v", r.getInputData().get("k"));
    }
}

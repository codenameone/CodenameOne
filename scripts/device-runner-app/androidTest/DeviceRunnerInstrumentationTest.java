package com.codenameone.examples.hellocodenameone;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DeviceRunnerInstrumentationTest {
    @Test
    public void launchMainActivityAndWaitForDeviceRunner() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, com.codename1.impl.android.CodenameOneActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);

        Thread.sleep(8000);
    }
}

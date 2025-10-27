package com.codename1.sms.intercept;

import com.codename1.system.NativeInterface;

// tag::nativeSmsInterceptor[]
public interface NativeSMSInterceptor extends NativeInterface {
    void bindSMSListener();
    void unbindSMSListener();
}
// end::nativeSmsInterceptor[]

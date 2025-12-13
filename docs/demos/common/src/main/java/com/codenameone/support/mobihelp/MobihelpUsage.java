package com.codenameone.support.mobihelp;

// tag::mobihelpUsage[]
public class MobihelpUsage {
    public void configure() {
        MobihelpConfig config = new MobihelpConfig();
        config.setAppSecret("xxxxxxx");
        config.setAppId("freshdeskdemo-2-xxxxxx");
        config.setDomain("codenameonetest1.freshdesk.com");
        MobihelpInitMethods.initIOS(config);

        config = new MobihelpConfig();
        config.setAppSecret("yyyyyyyy");
        config.setAppId("freshdeskdemo-1-yyyyyyyy");
        config.setDomain("https://codenameonetest1.freshdesk.com");
        MobihelpInitMethods.initAndroid(config);
    }
}
// end::mobihelpUsage[]

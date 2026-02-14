---
title: 'TIP: Intercept Incoming SMS on Android'
slug: tip-intercept-incoming-sms-on-android
url: /blog/tip-intercept-incoming-sms-on-android/
original_url: https://www.codenameone.com/blog/tip-intercept-incoming-sms-on-android.html
aliases:
- /blog/tip-intercept-incoming-sms-on-android.html
date: '2017-09-19'
author: Shai Almog
---

![Header Image](/blog/tip-intercept-incoming-sms-on-android/tip.jpg)

Last week I talked about [using SMS to activate your application](/blog/tip-send-device-activation-sms-via-twilio.html) which is a pretty powerful way to verify a user account. I left a couple of things out though. One of those things is the ability to grab the incoming SMS automatically. This is only possible on Android but it’s pretty cool for the users as it saves on the pain of typing the activation text.

### Broadcast Receiver

In order to grab an incoming SMS we need a broadcast receiver which is a standalone Android class that receives a specific event type. This is often confusing to developers who sometimes derive the impl class from broadcast receiver…​ That’s a mistake…​

The trick is you can just place any native Android class into the `native/android` directory. It will get compiled with the rest of the native code and “just works”. So I placed this class under `native/android/com/codename1/sms/intercept`:
    
    
    package com.codename1.sms.intercept;
    
    import android.content.*;
    import android.os.Bundle;
    import android.telephony.*;
    import com.codename1.io.Log;
    
    public class SMSListener extends BroadcastReceiver {
    
        @Override
        public void onReceive(Context cntxt, Intent intent) {
            // based on code from https://stackoverflow.com/questions/39526138/broadcast-receiver-for-receive-sms-is-not-working-when-declared-in-manifeststat
            if(intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
                Bundle bundle = intent.getExtras();
                SmsMessage[] msgs = null;
                if (bundle != null){
                    try{
                        Object[] pdus = (Object[]) bundle.get("pdus");
                        msgs = new SmsMessage[pdus.length];
                        for(int i=0; i<msgs.length; i++){
                            msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
                            String msgBody = msgs[i].getMessageBody();
                            SMSCallback.smsReceived(msgBody);
                        }
                    } catch(Exception e) {
                        Log.e(e);
                        SMSCallback.smsReceiveError(e);
                    }
                }
            }
        }
    }

The code above is pretty standard native Android code, it’s just a callback in which most of the logic is similar to the native Android code mentioned in this [stackoverflow question](https://stackoverflow.com/questions/39526138/broadcast-receiver-for-receive-sms-is-not-working-when-declared-in-manifeststat).

But there is still more we need to do. In order to implement this natively we need to register the permission and the receiver in the `manifest.xml` file as explained in that question. This is how their native manifest looked:
    
    
    <?xml version="1.0" encoding="utf-8"?>
    <manifest xmlns_android="http://schemas.android.com/apk/res/android"
        package="com.bulsy.smstalk1">
        <uses-permission android_name="android.permission.RECEIVE_SMS" />
        <uses-permission android_name="android.permission.READ_SMS" />
        <uses-permission android_name="android.permission.SEND_SMS"/>
        <uses-permission android_name="android.permission.READ_CONTACTS" />
    
        <application
            android_allowBackup="true"
            android_icon="@mipmap/ic_launcher"
            android_label="@string/app_name"
            android_supportsRtl="true"
            android_theme="@style/AppTheme">
            <activity android_name=".MainActivity">
                <intent-filter>
                    <action android_name="android.intent.action.MAIN" />
                    <category android_name="android.intent.category.LAUNCHER" />
                </intent-filter>
            </activity>
            <receiver android_name="com.bulsy.smstalk1.SmsListener"
                   android_enabled="true"
                   android_permission="android.permission.BROADCAST_SMS"
                   android_exported="true">
                <intent-filter android_priority="2147483647">//this doesnt work
                    <category android_name="android.intent.category.DEFAULT" />
                    <action android_name="android.provider.Telephony.SMS_RECEIVED" />
                </intent-filter>
            </receiver>
        </application>
    </manifest>

We only need the broadcast permission XML and the permission XML. Both are doable via the build hints. The former is pretty easy:
    
    
    android.xpermissions=<uses-permission android_name="android.permission.RECEIVE_SMS" />

The latter isn’t much harder, notice I took multiple lines and made them into a single line for convenience:
    
    
    android.xapplication=<receiver android_name="com.codename1.sms.intercept.SMSListener"  android_enabled="true" android_permission="android.permission.BROADCAST_SMS"  android_exported="true">                    <intent-filter android_priority="2147483647"><category android_name="android.intent.category.DEFAULT" />        <action android_name="android.provider.Telephony.SMS_RECEIVED" />                 </intent-filter>             </receiver>

Here it is formatted nicely:
    
    
    <receiver android_name="com.codename1.sms.intercept.SMSListener"
                  android_enabled="true"
                  android_permission="android.permission.BROADCAST_SMS"
                  android_exported="true">
                       <intent-filter android_priority="2147483647">
                              <category android_name="android.intent.category.DEFAULT" />
                              <action android_name="android.provider.Telephony.SMS_RECEIVED" />
                       </intent-filter>
    </receiver>

### Listening & Permissions

You will notice that these don’t include the actual binding or permission prompts you would expect for something like this. To do this we need a native interface.

The native sample in stack overflow bound the listener in the activity but here we want the app code to decide when we should bind the listening:
    
    
    public interface NativeSMSInterceptor extends NativeInterface {
        public void bindSMSListener();
        public void unbindSMSListener();
    }

That’s easy!

Notice that `isSupported()` returns false for all other OS’s so we won’t need to ask whether this is “Android” we can just use `isSupported()`.

The implementation is pretty easy too:
    
    
    package com.codename1.sms.intercept;
    
    import android.Manifest;
    import android.content.IntentFilter;
    import com.codename1.impl.android.AndroidNativeUtil;
    
    public class NativeSMSInterceptorImpl {
        private SMSListener smsListener;
        public void bindSMSListener() {
            if(AndroidNativeUtil.checkForPermission(Manifest.permission.RECEIVE_SMS, "We can automatically enter the SMS code for you")) { __**(1)**
                smsListener = new SMSListener();
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.provider.Telephony.SMS_RECEIVED");
                AndroidNativeUtil.getActivity().registerReceiver(smsListener, filter); __**(2)**
            }
        }
    
        public void unbindSMSListener() {
            AndroidNativeUtil.getActivity().unregisterReceiver(smsListener);
        }
    
        public boolean isSupported() {
            return true;
        }
    }

__**1** | This will trigger the permission prompt on Android 6 and newer. Even though the permission is declared in XML this isn’t enough for 6+. Notice that even when you run on Android 6 you still need to declare permissions in XML!  
---|---  
__**2** | Here we actually bind the listener, this allows us to grab one SMS and not listen in on every SMS coming thru  
  
### Callbacks

Up until now the code wasn’t very usable so lets abstract it a bit. But first we need to implement the callback class to which SMS’s and errors are sent from the code above:
    
    
    package com.codename1.sms.intercept; __**(1)**
    
    import com.codename1.util.FailureCallback;
    import com.codename1.util.SuccessCallback;
    import static com.codename1.ui.CN.*;
    
    /**
     * This is an internal class, it's package protect to hide that
     */
    class SMSCallback {
        static SuccessCallback<String> onSuccess;
        static FailureCallback onFail;
    
        public static void smsReceived(String sms) {
            if(onSuccess != null) {
                SuccessCallback<String> s = onSuccess;
                onSuccess = null;
                onFail = null;
                SMSInterceptor.unbindListener();
                callSerially(() -> s.onSucess(sms)); __**(2)**
            }
        }
    
        public static void smsReceiveError(Exception err) {
            if(onFail != null) {
                FailureCallback f = onFail;
                onFail = null;
                SMSInterceptor.unbindListener();
                onSuccess = null;
                callSerially(() -> f.onError(null, err, 1, err.toString()));
            } else {
                if(onSuccess != null) {
                    SMSInterceptor.unbindListener();
                    onSuccess = null;
                }
            }
        }
    }

__**1** | Notice that the package is the same as the native code and the other classes. This allows the callback class to be package protected so it isn’t exposed via the API (the class doesn’t have the public modifier)  
---|---  
__**2** | We wrap the callback in call serially to match the Codename One convention of using the EDT by default. The call will probably arrive on the Android native thread so it makes sense to normalize it and not expose the Android native thread to the user code  
  
### A simple API

The final piece of the puzzle is a simple API that can wrap the whole thing up and also hide the fact that this is Android specific. We’ll get into the full API in the last installment but for now this is the user level API that hides the native interface. Using a class like this is generally good practice as it allows us flexibility with the actual underlying native interface.
    
    
    package com.codename1.sms.intercept;
    
    import com.codename1.system.NativeLookup;
    import com.codename1.util.FailureCallback;
    import com.codename1.util.SuccessCallback;
    
    /**
     * This is a high level abstraction of the native classes and callbacks rolled into one.
     */
    public class SMSInterceptor {
        private static NativeSMSInterceptor nativeImpl;
    
        private static NativeSMSInterceptor get() {
            if(nativeImpl == null) {
                nativeImpl = NativeLookup.create(NativeSMSInterceptor.class);
                if(!nativeImpl.isSupported()) {
                    nativeImpl = null;
                }
            }
            return nativeImpl;
        }
    
        public static boolean isSupported() {
            return get() != null;
        }
    
        public static void grabNextSMS(SuccessCallback<String> onSuccess) {
            SMSCallback.onSuccess = onSuccess;
            get().bindSMSListener();
        }
    
        static void unbindListener() {
            get().unbindSMSListener();
        }
    }

### Next Time

Next time I will wrap this all up with the user experience and package everything into an easy to use cn1lib.

Some of the things I touched here might be a bit “hairy” in terms of native interface usage so if something isn’t clear just ask in the comments.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Diamond** — September 20, 2017 at 1:43 pm ([permalink](https://www.codenameone.com/blog/tip-intercept-incoming-sms-on-android.html#comment-24152))

> Diamond says:
>
> I’ve wrapped up the code into a cn1lib here: [https://github.com/diamondd…](<https://github.com/diamonddevgroup/SMSInterceptor>)
>



### **Diamond** — September 21, 2017 at 7:11 am ([permalink](https://www.codenameone.com/blog/tip-intercept-incoming-sms-on-android.html#comment-23738))

> Diamond says:
>
> I have no experience with c# code, but could this be implemented for Windows phones too? I found a lead here: [https://msdn.microsoft.com/…](<https://msdn.microsoft.com/en-us/library/bb932385.aspx>)
>



### **Shai Almog** — September 21, 2017 at 8:05 am ([permalink](https://www.codenameone.com/blog/tip-intercept-incoming-sms-on-android.html#comment-21527))

> Shai Almog says:
>
> I doubt that will work. It’s a pretty old document. You need to look for things that are relevant for UWP.
>



### **Diamond** — September 21, 2017 at 8:28 am ([permalink](https://www.codenameone.com/blog/tip-intercept-incoming-sms-on-android.html#comment-23747))

> Diamond says:
>
> This looks promising [https://github.com/Microsof…](<https://github.com/Microsoft/Windows-universal-samples/tree/master/Samples/SmsSendAndReceive>)
>



### **Diamond** — September 21, 2017 at 8:43 am ([permalink](https://www.codenameone.com/blog/tip-intercept-incoming-sms-on-android.html#comment-23628))

> Diamond says:
>
> The full sms API can be found here [https://docs.microsoft.com/…](<https://docs.microsoft.com/en-us/uwp/api/Windows.Devices.Sms>)
>



### **George Kent** — September 21, 2017 at 9:00 am ([permalink](https://www.codenameone.com/blog/tip-intercept-incoming-sms-on-android.html#comment-23655))

> George Kent says:
>
> How stable and accurate is your UWP port. Is it ready for production on all your features mentioned in your dev guide?
>



### **Shai Almog** — September 21, 2017 at 10:25 am ([permalink](https://www.codenameone.com/blog/tip-intercept-incoming-sms-on-android.html#comment-24136))

> Shai Almog says:
>
> That could work but I wouldn’t spend time on that. Notice it says that the sample will only work on unlocked devices and “will not pass WACK”. I’m not sure about this but I recall that if you use specific Windows features your app can’t be sold in China
>



### **Shai Almog** — September 21, 2017 at 10:30 am ([permalink](https://www.codenameone.com/blog/tip-intercept-incoming-sms-on-android.html#comment-23671))

> Shai Almog says:
>
> It’s used in production. All is a tall order. It’s newer than the Android and iOS ports and not even remotely as popular so it doesn’t see nearly as much testing.
>



### **Rashedul Hasan** — August 11, 2018 at 9:29 pm ([permalink](https://www.codenameone.com/blog/tip-intercept-incoming-sms-on-android.html#comment-24024))

> Rashedul Hasan says:
>
> Hi Shai, thanks for the article but how do i put the java class “SMSListener” inside “native/android” directory? a bit confused!
>



### **Shai Almog** — August 12, 2018 at 4:37 am ([permalink](https://www.codenameone.com/blog/tip-intercept-incoming-sms-on-android.html#comment-24051))

> Shai Almog says:
>
> Hi,  
> see this video on native interfaces. It should explain it clearly [https://www.codenameone.com…]([https://www.codenameone.com/how-do-i—access-native-device-functionality-invoke-native-interfaces.html](https://www.codenameone.com/how-do-i---access-native-device-functionality-invoke-native-interfaces.html))
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}

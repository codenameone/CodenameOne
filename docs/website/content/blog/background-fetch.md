---
title: Background Fetch
slug: background-fetch
url: /blog/background-fetch/
original_url: https://www.codenameone.com/blog/background-fetch.html
aliases:
- /blog/background-fetch.html
date: '2016-06-20'
author: Steve Hannah
---

![Header Image](/blog/background-fetch/background-fetch.jpg)

Background fetch allows an app to periodically “fetch” information from the network while the app is in  
the background. This is scheduled by the native platform, where apps that support background  
fetch will be started up (in the background), and their `performBackgroundFetch` method will be invoked.

__ |  Since the app will be launched directly to the background, you cannot assume that the  
`start()` method was invoked prior to the `performBackgroundFetch` call   
---|---  
  
### Implementing Background Fetch

Apps that wish to implement background fetch must implement the  
[BackgroundFetch](/javadoc/com/codename1/background/BackgroundFetch/)  
interface in their main class.

__ |  The main class is the one mentioned in the preferences not the state machine or some other class!   
---|---  
  
On iOS, you also need to include `fetch` in the list of background modes specifically include `fetch` in the  
`ios.background_modes` build hint e.g.:
    
    
    ios.background_modes=fetch

Or for more than one mode:
    
    
    ios.background_modes=fetch,music

In addition to implementing the `BackgroundFetch` interface, apps must explicitly set the background fetch  
interval by invoking `Display.getInstance().setPreferredBackgroundFetchInterval(interval)` at some point, usually  
in the `start()` or `init()` method.

### Platform Support

Currently background fetch is supported on iOS, Android, and in the Simulator (simulated using timers when the  
app is paused). You should use the `Display.getInstance().isBackgroundFetchSupported()` call to check if the  
current platform supports it.

### Sample

The following code demonstrates simple usage of the API:
    
    
    /**
     * A simple demo showing the use of the Background Fetch API.  This demo will load
     * data from the Slashdot RSS feed while it is in the background.
     *
     * To test it out, put the app into the background (or select Pause App in the simulator)
     * and wait 10 seconds.  Then open the app again. You should see that the data is loaded.
     */
    public class BackgroundFetchTest implements BackgroundFetch {
    
        private Form current;
        private Resources theme;
        List<Map> records;
    
        // Container to hold the list of records.
        Container recordsContainer;
    
        public void init(Object context) {
            theme = UIManager.initFirstTheme("/theme");
    
            // Enable Toolbar on all Forms by default
            Toolbar.setGlobalToolbar(true);
    
            // Pro only feature, uncomment if you have a pro subscription
            // Log.bindCrashProtection(true);
        }
    
        public void start() {
            if(current != null){
                // Make sure we update the records as we are coming in from the
                // background.
                updateRecords();
                current.show();
                return;
            }
            Display d = Display.getInstance();
    
            Form hi = new Form("Background Fetch Demo");
            hi.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
    
            Label supported = new Label();
            if (d.isBackgroundFetchSupported()){
                // This call is necessary to initialize background fetch
                d.setPreferredBackgroundFetchInterval(10);
    
                supported.setText("Background Fetch IS Supported");
            } else {
                supported.setText("Background Fetch is NOT Supported");
            }
    
            hi.addComponent(new Label("Records:"));
            recordsContainer = new Container(new BoxLayout(BoxLayout.Y_AXIS));
            //recordsContainer.setScrollableY(true);
            hi.addComponent(recordsContainer);
    
            hi.addComponent(supported);
            updateRecords();
            hi.show();
        }
    
        /**
         * Update the UI with the records that are currently loaded.
         */
        private void updateRecords() {
            recordsContainer.removeAll();
            if (records != null) {
                for (Map m : records) {
                    recordsContainer.addComponent(new SpanLabel((String)m.get("title")));
                }
            } else {
                recordsContainer.addComponent(new SpanLabel("Put the app in the background, wait 10 seconds, then open it again.  The app should background fetch some data from the Slashdot RSS feed and show it here."));
            }
            if (Display.getInstance().getCurrent() != null) {
                Display.getInstance().getCurrent().revalidate();
            }
        }
    
        public void stop() {
            current = Display.getInstance().getCurrent();
            if(current instanceof Dialog) {
                ((Dialog)current).dispose();
                current = Display.getInstance().getCurrent();
            }
        }
    
        public void destroy() {
        }
    
        /**
         * This method will be called in the background by the platform.  It will
         * load the RSS feed.  Note:  This only runs when the app is in the background.
         * @param deadline
         * @param onComplete
         */
        @Override
        public void performBackgroundFetch(long deadline, Callback<Boolean> onComplete) {
            RSSService rss = new RSSService("http://rss.slashdot.org/Slashdot/slashdotMain");
            NetworkManager.getInstance().addToQueueAndWait(rss);
            records = rss.getResults();
            System.out.println(records);
            onComplete.onSucess(Boolean.TRUE);
    
        }
    }
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Nick Koirala** — June 22, 2016 at 5:13 am ([permalink](/blog/background-fetch/#comment-21454))

> Nick Koirala says:
>
> Just tried this out on Android.
>
> It looks good, and I was pleased to see that I can schedule a local notification from the backgroundFetch, so if there is a reason for the user to open the app based on the background fetch it is possible.
>
> I’ve found that you can’t poll faster than 60 seconds, not a big problem, an update every minute is still plenty, but might be worth noting as the example code uses 10 seconds, but that’s not what result I’m getting on my phone. If you set it lower then it’ll just call the method every 60 seconds.
>



### **Lukman Javalove Idealist Jaji** — June 23, 2016 at 9:00 am ([permalink](/blog/background-fetch/#comment-22912))

> Lukman Javalove Idealist Jaji says:
>
> Will it be a good programming practice to use this feature to connect to a remote DB every x minutes? Or is there a more effective way to achieve this? Sometimes when the back button is pressed on the MainForm, the app ought to be minmized but when I reopen, it looks like the app starts all over again. Does this feature prevent that?
>



### **Shai Almog** — June 24, 2016 at 6:24 am ([permalink](/blog/background-fetch/#comment-22748))

> Shai Almog says:
>
> Probably not, it will grind your battery down to nothing. The right thing to do is to use push to trigger an update.
>



### **Lukman Javalove Idealist Jaji** — June 26, 2016 at 6:36 am ([permalink](/blog/background-fetch/#comment-22813))

> Lukman Javalove Idealist Jaji says:
>
> Thanks Shai .. does this apply also to on device background checks … say if a file exists in storage or on the filesystem
>



### **Shai Almog** — June 27, 2016 at 3:04 am ([permalink](/blog/background-fetch/#comment-22724))

> Shai Almog says:
>
> I have no idea. I’m guessing it should work.
>



### **Scott Turner** — May 11, 2017 at 2:21 pm ([permalink](/blog/background-fetch/#comment-23401))

> Scott Turner says:
>
> I noticed an issue with the BackgroundFetch functionality. It’s not mentioned in this blog post, but for ios, you have to set ios.locationusagedescription in the build hints, otherwise it won’t hit the performBackgroundFetch callback on apple devices. It took me several hours of poking around to figure this out, so it’s definitely worth amending the post.
>



### **Shai Almog** — May 12, 2017 at 12:25 pm ([permalink](/blog/background-fetch/#comment-23533))

> Shai Almog says:
>
> I’m not familiar enough with that piece of code so I asked about it. I understand that there should be no dependency on location usage description so if this happens with a simple hello world that might be a bug. One thing I did understand is that iOS is ridiculously sensitive about background behavior. So if you use things like location etc. this might fail by crashing with no messages or any indication of what went wrong.
>



### **Scott Turner** — May 12, 2017 at 12:48 pm ([permalink](/blog/background-fetch/#comment-23333))

> Scott Turner says:
>
> Thanks, Shai. After playing around with BackgroundFetch I realized it wasn’t really right for my use case anyway. It’s far too unpredictable on ios. I need it to hit reliably at least once every 30 seconds and it seems like it doesn’t allow that sort of flexibility. Oh well! Thanks for the follow up.
>



### **Ch Hjelm** — May 20, 2019 at 8:28 pm ([permalink](/blog/background-fetch/#comment-24093))

> Ch Hjelm says:
>
> I understand that only `init()` will be executed before `performBackgroundFetch` is run in the background. If I have a lot of things being executed on a normal application startup (and which are not necessary for the `performBackgroundFetch` to execute), I guess that initialization code should then rather go into `start()` to avoid slowing the `performBackgroundFetch` down (with the risk that it takes too long and get killed). In which case, `start()` should test on whether current is null like in your example, and only execute all the initialization code if `current` actually is null. Would you agree this is the best approach?
>



### **Shai Almog** — May 21, 2019 at 4:26 am ([permalink](/blog/background-fetch/#comment-23991))

> Shai Almog says:
>
> This is standard Codename One code that has nothing to do with suspend resume. I suggest checking out the second chapter of the uber book available for free here: [https://uber.cn1.co/](<https://uber.cn1.co/>) look for the part about application lifecycle.
>
> init is only invoked when the app is launched from destroyed mode (cold start). Start is invoked on resume from suspended mode. This lets us detect a case of resume which might be applicable.
>



### **Ch Hjelm** — May 21, 2019 at 6:42 am ([permalink](/blog/background-fetch/#comment-24058))

> Ch Hjelm says:
>
> Thanks. I already understand the life cycle (and I have your book :-)) and my question IS specific to background fetch. To rephrase: today I have a lot of heavy initialization in init() which is only needed if a *user* cold-starts the app but *not* necessary if the app is cold-started just to run the `performBackgroundFetch` . If all that initialization code is run on every background fetch it may slow down the fetch and the app may get killed, or at least it will consume unnecessary battery. So, I just wanted to double check if it is a good/workable approach to move the heavy initialization into `start()` using the pattern shown below?  
> `start() {  
> if (current!=null) {  
> current.show();  
> return;  
> } else {  
> //heavy initialization normally placed in init(), only needed for user but not necessary for background fetch  
> }  
> //normal start code…  
> }`  
> Hope I managed to make it clear. I basically want to double check with your expertise because any issues here could be difficult to catch in the Simulator or in device testing.
>



### **Shai Almog** — May 22, 2019 at 9:33 am ([permalink](/blog/background-fetch/#comment-23998))

> Shai Almog says:
>
> You shouldn’t write any code there. It could slow down restore and you should minimize code in init as they can trigger ANR’s (app not responding). In a case of ANR your app could be killed instantly. You can start a thread in the init code and do initialization logic there after a small delay to let the UI grab some CPU. There is no reason to prefer start over init() for this sort of logic though.
>



### **Arthur Major** — July 23, 2021 at 4:42 pm ([permalink](/blog/background-fetch/#comment-24470))

> Arthur Major says:
>
> I tested this code and works good in Android 7 and below, but Android 8+ just run once, is there something else I have to do?
>



### **Steve Hannah** — July 23, 2021 at 5:04 pm ([permalink](/blog/background-fetch/#comment-24471))

> Steve Hannah says:
>
> Background execution has gotten a lot harder with newer versions of Android. It is difficult to predict when the platform will run your background tasks, and they may be blocked for any reason on both Android and iOS. If you can check the device log it might give you a clue as to what its “complaint” is.
>



### **Arthur Major** — July 23, 2021 at 11:57 pm ([permalink](/blog/background-fetch/#comment-24472))

> Arthur Major says:
>
> 2021-07-23 17:25:42.650 8479-8479/? D/ViewRootImpl@984f7f7[SigueloNotifAppStub]: setView = DecorView@bbbdd82[SigueloNotifAppStub] TM=true MM=false  
> 2021-07-23 17:25:42.651 8479-8479/? D/ViewRootImpl@984f7f7[SigueloNotifAppStub]: dispatchAttachedToWindow  
> 2021-07-23 17:25:42.677 8479-8479/? D/ViewRootImpl@984f7f7[SigueloNotifAppStub]: Relayout returned: old=[0,0][0,0] new=[0,0][1080,1920] result=0x7 surface={valid=true 535584559104} changed=true  
> 2021-07-23 17:25:42.721 8479-8479/? D/ViewRootImpl@984f7f7[SigueloNotifAppStub]: MSG_RESIZED_REPORT: frame=Rect(0, 0 – 1080, 1920) ci=Rect(0, 72 – 0, 0) vi=Rect(0, 72 – 0, 0) or=1  
> 2021-07-23 17:25:42.721 8479-8479/? D/ViewRootImpl@984f7f7[SigueloNotifAppStub]: MSG_WINDOW_FOCUS_CHANGED 1  
> 2021-07-23 17:25:48.534 8479-8479/? D/ViewRootImpl@984f7f7[SigueloNotifAppStub]: MSG_WINDOW_FOCUS_CHANGED 0  
> 2021-07-23 17:25:48.603 8479-8479/? D/ViewRootImpl@984f7f7[SigueloNotifAppStub]: Relayout returned: old=[0,0][1080,1920] new=[0,0][1080,1920] result=0x5 surface={valid=false 0} changed=true  
> 2021-07-23 17:25:48.650 8479-8479/? D/ViewRootImpl@984f7f7[SigueloNotifAppStub]: Relayout returned: old=[0,0][1080,1920] new=[0,0][1080,1920] result=0x1 surface={valid=false 0} changed=false  
> 2021-07-23 17:25:53.304 8668-8668/? D/ViewRootImpl@de60cd[SigueloNotifAppStub]: setView = DecorView@bbbdd82[SigueloNotifAppStub] TM=true MM=false  
> 2021-07-23 17:25:53.313 8668-8668/? D/ViewRootImpl@de60cd[SigueloNotifAppStub]: dispatchAttachedToWindow  
> 2021-07-23 17:25:53.346 8668-8668/? D/ViewRootImpl@de60cd[SigueloNotifAppStub]: Relayout returned: old=[0,0][0,0] new=[0,0][1080,1920] result=0x7 surface={valid=true 535584444416} changed=true  
> 2021-07-23 17:25:53.385 8668-8668/? D/ViewRootImpl@de60cd[SigueloNotifAppStub]: MSG_RESIZED_REPORT: frame=Rect(0, 0 – 1080, 1920) ci=Rect(0, 72 – 0, 0) vi=Rect(0, 72 – 0, 0) or=1  
> 2021-07-23 17:25:53.385 8668-8668/? D/ViewRootImpl@de60cd[SigueloNotifAppStub]: MSG_WINDOW_FOCUS_CHANGED 1  
> 2021-07-23 17:25:58.730 8668-8668/? D/ViewRootImpl@de60cd[SigueloNotifAppStub]: MSG_WINDOW_FOCUS_CHANGED 0  
> 2021-07-23 17:25:58.775 8668-8668/? D/ViewRootImpl@de60cd[SigueloNotifAppStub]: Relayout returned: old=[0,0][1080,1920] new=[0,0][1080,1920] result=0x5 surface={valid=false 0} changed=true  
> 2021-07-23 17:25:58.884 8668-8668/? D/ViewRootImpl@de60cd[SigueloNotifAppStub]: Relayout returned: old=[0,0][1080,1920] new=[0,0][1080,1920] result=0x1 surface={valid=false 0} changed=false  
> 2021-07-23 17:27:39.347 8668-8668/? D/ViewRootImpl@de60cd[SigueloNotifAppStub]: Relayout returned: old=[0,0][1080,1920] new=[0,0][1080,1920] result=0x1 surface={valid=false 0} changed=false  
> 2021-07-23 17:27:39.394 8668-8668/? D/ViewRootImpl@de60cd[SigueloNotifAppStub]: Relayout returned: old=[0,0][1080,1920] new=[0,0][1080,1920] result=0x7 surface={valid=true 535346876416} changed=true  
> 2021-07-23 17:27:39.409 8668-8668/? D/ViewRootImpl@de60cd[SigueloNotifAppStub]: MSG_WINDOW_FOCUS_CHANGED 1  
> 2021-07-23 17:27:43.525 8668-8668/? D/ViewRootImpl@de60cd[SigueloNotifAppStub]: MSG_WINDOW_FOCUS_CHANGED 0  
> 2021-07-23 17:27:43.596 8668-8668/? D/ViewRootImpl@de60cd[SigueloNotifAppStub]: Relayout returned: old=[0,0][1080,1920] new=[0,0][1080,1920] result=0x5 surface={valid=false 0} changed=true  
> 2021-07-23 17:27:43.677 8668-8668/? D/ViewRootImpl@de60cd[SigueloNotifAppStub]: Relayout returned: old=[0,0][1080,1920] new=[0,0][1080,1920] result=0x1 surface={valid=false 0} changed=false
>
> That’s all the log I got when it runs.
>



### **Steve Hannah** — July 28, 2021 at 12:54 pm ([permalink](/blog/background-fetch/#comment-24473))

> Steve Hannah says:
>
> I’ve done some digging on this, and believe I have found the issue. (described here <https://developer.android.com/about/versions/oreo/background.html#services>).
>
> “Prior to Android 8.0, the usual way to create a foreground service was to create a background service, then promote that service to the foreground. With Android 8.0, there is a complication; the system doesn’t allow a background app to create a background service. For this reason, Android 8.0 introduces the new method startForegroundService() to start a new service in the foreground. After the system has created the service, the app has five seconds to call the service’s startForeground() method to show the new service’s user-visible notification. If the app does not call startForeground() within the time limit, the system stops the service and declares the app to be ANR.”
>
> This doesn’t offer a clear solution at your level, unfortunately, other than attempting to keep execution of background fetches under 5 seconds — it isn’t clear whether that is a solution or not.
>
> Working around these growing background execution restrictions is tricky. We may end up deprecating some of these background execution APIs and shifting to implementing this type of thing in cn1libs.
>



### **Arthur Major** — July 28, 2021 at 4:44 pm ([permalink](/blog/background-fetch/#comment-24474))

> Arthur Major says:
>
> Thanks for your response, so i think in this case is better continue with a native solution.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}

---
title: Integrating 3rd Party Native SDKs Part I
slug: integrating-3rd-party-native-sdks-part-1
url: /blog/integrating-3rd-party-native-sdks-part-1/
original_url: https://www.codenameone.com/blog/integrating-3rd-party-native-sdks-part-1.html
aliases:
- /blog/integrating-3rd-party-native-sdks-part-1.html
date: '2015-09-28'
author: Steve Hannah
---

![Header Image](/blog/integrating-3rd-party-native-sdks-part-1/native-sdks-header.jpg)

This past Thursday, we held our fourth webinar, and the topic was how to incorporate 3rd party native libraries into a Codename One app. I used the recently released [FreshDesk cn1lib](http://shannah.github.io/cn1-freshdesk/) as a case study for this webinar. As the topic is a little involved, I decided to break it up into two webinars. In [part one](https://codenameone.adobeconnect.com/p8tau90yr1d/), we focused on the public API and architecture involved in developing a wrapper for a native SDK, and walked through the native implementation for Android.

[View a Recording of the Part I webinar](https://youtu.be/1bNi9IVlQ_g)

In [part 2](https://www.eventbrite.co.uk/e/enhancing-your-app-with-3rd-party-native-libraries-part-2-tickets-18826645002), scheduled for Thursday October 8th, we’ll show how to implement the iOS side of the library.

[Sign up for Enhancing your App with 3rd Party Native Libraries Part 2](https://www.eventbrite.co.uk/e/enhancing-your-app-with-3rd-party-native-libraries-part-2-tickets-18826645002)

## The Companion Tutorial

For those of you who prefer written tutorials, I will be publishing a parallel 3-part series on this topic in the blog. The first part of the tutorial is contained in the remainder of this blog post.

The following is a description of the procedure that was used to create the [Codename One FreshDesk library](http://shannah.github.io/cn1-freshdesk/). This process can be easily adapted to wrap any native SDK on Android and iOS.

## Step 1 : Review the FreshDesk SDKs

Before we begin, we’ll need to review the Android and iOS SDKs.

  1. **FreshDesk Android SDK** : [Integration Guide](http://developer.freshdesk.com/mobihelp/android/integration_guide/) | [API Docs](http://developer.freshdesk.com/mobihelp/android/api/reference/com/freshdesk/mobihelp/package-summary.html)

  2. **FreshDesk iOS SDK** : [Integration Guide](http://developer.freshdesk.com/mobihelp/ios/integration_guide/) | [API Docs](http://developer.freshdesk.com/mobihelp/ios/api/)

In reviewing the SDKs, I’m looking to answer two questions:

  1. What should my Codename One FreshDesk API look like?

  2. What will be involved in integrating the native SDK in my app or lib?

## Step 2: Designing the Codename One Public API

When designing the Codename One API, I often begin by looking at the [Javadocs](http://developer.freshdesk.com/mobihelp/android/api/reference/com/freshdesk/mobihelp/package-summary.html) for the native Android SDK. If the class hierarchy doesn’t look too elaborate, I may decide model my Codename One public API fairly closely on the Android API. On the other hand, if I only need a small part of the SDK’s functionality, I may choose to create my abstractions around just the functionality that I need.

In the case of the FreshDesk SDK, it looks like most of the functionality is handled by one central class `Mobihelp`, with a few other POJO classes for passing data to and from the service. This is a good candidate for a comprehensive Codename One API.

Before proceeding, we also need to look at the iOS API to see if there are any features that aren’t included. While naming conventions in the iOS API are a little different than those in the Android API, it looks like they are functionally the same.

Therefore, I choose to create a class hierarchy and API that closely mirrors the Android SDK.

## Step 3: The Architecture and Internal APIs

A Codename One library that wraps a native SDK, will generally consist of the following:

  1. **Public Java API** , consisting of pure Java classes that are intended to be used by the outside world.

  2. **Native Interface(s)**. The Native Interface(s) act as a conduit for the public Java API to communicate to the native SDK. Parameters in native interface methods are limited to primitive types, arrays of primitive types, and Strings, as are return values.

  3. **Native code**. Each platform must include an implementation of the Native Interface(s). These implementations are written in the native language of the platform (e.g. Java for Android, and Objective-C for iOS).

  4. **Native dependencies**. Any 3rd party libraries required for the native code to work, need to be included for each platform. On android, this may mean bundling .jar files, .aar files, or .andlib files. On iOS, this may mean bundling .h files, .a files, and .bundle files.

  5. **Build hints**. Some libraries will require you to add some extra build hints to your project. E.g. On Android you may need to add permissions to the manifest, or define services in the `<Application>` section of the manifest. On iOS, this may mean specifying additional core frameworks for inclusion, or adding build flags for compilation.

The following diagram shows the dependencies in a native library:

![fc8a77d2 61e0 11e5 9ecf bf381d4ac966](/blog/integrating-3rd-party-native-sdks-part-1/fc8a77d2-61e0-11e5-9ecf-bf381d4ac966.png)

In the specific case of our FreshDesk API, the public API and classes will look like:

![5fe88406 61e4 11e5 951e e09bd28a93c9](/blog/integrating-3rd-party-native-sdks-part-1/5fe88406-61e4-11e5-951e-e09bd28a93c9.png)

### Things to Notice

  1. The public API consists of the main class ([`Mobihelp`](https://github.com/shannah/cn1-freshdesk/blob/master/cn1-freshdesk-demo/src/com/codename1/freshdesk/Mobihelp.java)), and a few supporting classes ([`FeedbackRequest`](https://github.com/shannah/cn1-freshdesk/blob/master/cn1-freshdesk-demo/src/com/codename1/freshdesk/FeedbackRequest.java), [`FeedbackType`](https://github.com/shannah/cn1-freshdesk/blob/master/cn1-freshdesk-demo/src/com/codename1/freshdesk/FeedbackType.java), [`MobihelpConfig`](https://github.com/shannah/cn1-freshdesk/blob/master/cn1-freshdesk-demo/src/com/codename1/freshdesk/MobihelpConfig.java), [`MobihelpCallbackStatus`](https://github.com/shannah/cn1-freshdesk/blob/master/cn1-freshdesk-demo/src/com/codename1/freshdesk/MobihelpCallbackStatus.java)), which were copied almost directly from the Android SDK.

  2. The only way for the public API to communicate with the native SDK is via the [`MobihelpNative`](https://github.com/shannah/cn1-freshdesk/blob/master/cn1-freshdesk-demo/src/com/codename1/freshdesk/MobihelpNative.java) interface.

  3. We introduced the [`MobihelpNativeCallback`](https://github.com/shannah/cn1-freshdesk/blob/master/cn1-freshdesk-demo/src/com/codename1/freshdesk/MobihelpNativeCallback.java) class to facilitate native code calling back into the public API. This was necessary for a few methods that used asynchronous callbacks.

## Step 4: Implement the Public API and Native Interface

We have already looked at the final product of the public API in the previous step, but let’s back up and walk through the process step-by-step.

I wanted to model my API closely around the Android API, and the central class that includes all of the functionality of the SDK is the [com.freshdesk.mobihelp.Mobihelp class](http://developer.freshdesk.com/mobihelp/android/api/reference/com/freshdesk/mobihelp/Mobihelp.html), so we begin there.

We’ll start by creating our own package (`com.codename1.freshdesk`) and our own `Mobihelp` class inside it.

### Adapting Method Signatures

#### The `Context` parameter

In a first glance at the [com.freshdesk.mobihelp.Mobihelp API](http://developer.freshdesk.com/mobihelp/android/api/reference/com/freshdesk/mobihelp/Mobihelp.html) we see that many of the methods take a parameter of type [`android.content.Context`](http://developer.android.com/reference/android/content/Context.html). This class is part of the core Android SDK, and will not be accessible to any pure Codename One APIs. Therefore, our public API cannot include any such references. Luckily, we’ll be able to access a suitable context in the native layer, so we’ll just omit this parameter from our public API, and inject them in our native implementation.

Hence, the method signature `public static final void setUserFullName (Context context, String name)` will simply become `public static final void setUserFullName (String name)` in our public API.

#### Non-Primitive Parameters

Although our public API isn’t constrained by the same rules as our Native Interfaces with respect to parameter and return types, we need to be cognizant of the fact that parameters we pass to our public API will ultimately be funnelled through our native interface. Therefore, we should pay attention to any parameters or return types that can’t be passed directly to a native interface, and start forming a strategy for them. E.g. consider the following method signature from the Android `Mobihelp` class:
    
    
    public static final void showSolutions (Context activityContext, ArrayList<String> tags)

We’ve already decided to just omit the `Context` parameter in our API, so that’s a non-issue. But what about the `ArrayList<String>` tags parameter? Passing this to our public API is no problem, but when we implement the public API, how will we pass this `ArrayList` to our native interface, since native interfaces don’t allow us to arrays of strings as parameters?

I generally use one of three strategies in such cases:

  1. Encode the parameter as either a single String (e.g. using JSON or some other easily parseable format) or a byte[] array (in some known format that can easily be parsed in native code).

  2. Store the parameter on the Codename One side and pass some ID or token that can be used on the native side to retrieve the value.

  3. If the data structure can be expressed as a finite number of primitive values, then simply design the native interface method to take the individual values as parameters instead of a single object. E.g. If there is a `User` class with properties `name` and `phoneNumber`, the native interface can just have `name` and `phoneNumber parameters rather than a single `user` parameter.

In this case, because an array of strings is such a simple data structure, I decided to use a variation on strategy number 1: Merge the array into a single string with a delimiter.

In any case, we don’t have to come up with the specifics right now, as we are still on the public API, but it will pay dividends later if we think this through ahead of time.

#### Callbacks

It is quite often the case that native code needs to call back into Codename One code when an event occurs. This may be connected directly to an API method call (e.g. as the result of an asynchronous method invocation), or due to something initiated by the operating system or the native SDK on its own (e.g. a push notification, a location event, etc..).

Native code will have access to both the Codename One API and any native APIs in your app, but on some platforms, accessing the Codename One API may be a little tricky. E.g. on iOS you’ll be calling from Objective-C back into Java which requires knowledge of Codename One’s java-to-objective C conversion process. In general, I have found that the easiest way to facilitate callbacks is to provide abstractions that involve static java methods (in Codename One space) that accept and return primitive types.

In the case of our `Mobihelp` class, the following method hints at the need to have a “callback plan”:
    
    
    public static final void getUnreadCountAsync (Context context, UnreadUpdatesCallback callback)

The interface definition for `UnreadUpdatesCallback` is:
    
    
    public interface UnreadUpdatesCallback {
        //This method is called once the unread updates count is available.
        void onResult(MobihelpCallbackStatus status, Integer count);
    
    }

I.e. If we were to implement this method (which I plan to do), we need to have a way for the native code to call the `callback.onResult()` method of the passed parameter.

So we have two issues that will need to be solved here:

  1. How to pass the `callback` object through the native interface.

  2. How to **call** the `callback.onResult()` method from native code at the right time.

For the first issue, we’ll use strategy #2 that we mentioned previously: (Store the parameter on the Codename One side and pass some ID or token that can be used on the native side to retrieve the value).

For the second issue, we’ll create a static method that can take the token generated to solve the first issue, and call the stored `callback` object’s `onResult()` method. We abstract both sides of this process using the [`MobihelpNativeCallback` class](https://github.com/shannah/cn1-freshdesk/blob/master/cn1-freshdesk-demo/src/com/codename1/freshdesk/MobihelpNativeCallback.java).
    
    
    public class MobihelpNativeCallback {
        private static int nextId = 0;
        private static Map<Integer,UnreadUpdatesCallback> callbacks = new HashMap<Integer,UnreadUpdatesCallback>();
    
        static int registerUnreadUpdatesCallback(UnreadUpdatesCallback callback) {
            callbacks.put(nextId, callback);
            return nextId++;
        }
    
        public static void fireUnreadUpdatesCallback(int callbackId, final int status, final int count) {
            final UnreadUpdatesCallback cb = callbacks.get(callbackId);
            if (cb != null) {
                callbacks.remove(callbackId);
                Display.getInstance().callSerially(new Runnable() {
    
                    public void run() {
                        MobihelpCallbackStatus status2 = MobihelpCallbackStatus.values()[status];
                        cb.onResult(status2, count);
                    }
    
                });
            }
        }
    
    }

**Things to notice here:**

  1. This class uses a static `Map<Integer,UnreadUpdatesCallback>` member to keep track of all callbacks, mapping a unique integer ID to each callback.

  2. The `registerUnreadUpdatesCallback()` method takes an `UnreadUpdatesCallback` object, places it in the `callbacks` map, and returns the integer **token** that can be used to fire the callback later. This method would be called by the public API inside the `getUnreadCountAsync()` method implementation to convert the `callback` into an integer, which can then be passed to the native API.

  3. The `fireUnreadUpdatesCallback()` method would be called later from native code. Its first parameter is the token for the callback to call.

  4. We wrap the `onResult()` call inside a `Display.callSerially()` invocation to ensure that the callback is called on the EDT. This is a general convention that is used throughout Codename One, and you’d be well-advised to follow it. **Event handlers** should be run on the EDT unless there is a good reason not to – and in that case your documentation and naming conventions should make this clear to avoid accidentally stepping into multithreading hell!

### Initialization

Most Native SDKs include some sort of initialization method where you pass your developer and application credentials to the API. When I filled in FreshDesk’s web-based form to create a new application, it generated an application ID, an app “secret”, and a “domain”. The SDK requires me to pass all three of these values to its `init()` method via the `MobihelpConfig` class.

Note, however, that FreshDesk (and most other service provides that have native SDKs) requires me to create different Apps for each platform. This means that my App ID and App secret will be different on iOS than they will be on Android.

Therefore our public API needs to enable us to provide multiple credentials in the same app, and our API needs to know to use the correct credentials depending on the device that the app is running on.

There are many solutions to this problem, but the one I chose was to provide two different `init()` methods:
    
    
    public final static void initIOS(MobihelpConfig config)

and
    
    
    public final static void initAndroid(MobihelpConfig config)

Then I can set up the API with code like:
    
    
    MobihelpConfig config = new MobihelpConfig();
    config.setAppSecret("xxxxxxx");
    config.setAppId("freshdeskdemo-2-xxxxxx");
    config.setDomain("codenameonetest1.freshdesk.com");
    Mobihelp.initIOS(config);
    
    config = new MobihelpConfig();
    config.setAppSecret("yyyyyyyy");
    config.setAppId("freshdeskdemo-1-yyyyyyyy");
    config.setDomain("https://codenameonetest1.freshdesk.com");
    Mobihelp.initAndroid(config);

### The Resulting Public API
    
    
    public class Mobihelp {
    
        private static char[] separators = new char[]{',','|','/','@','#','%','!','^','&','*','=','+','*','<'};
        private static MobihelpNative peer;
    
        public static boolean isSupported() {
            ....
        }
    
        public static void setPeer(MobihelpNative peer) {
            ....
        }
    
        //Attach the given custom data (key-value pair) to the conversations/tickets.
        public final static void	addCustomData(String key, String value) {
            ...
        }
        //Attach the given custom data (key-value pair) to the conversations/tickets with the ability to flag sensitive data.
        public final static void	addCustomData(String key, String value, boolean isSensitive) {
            ...
        }
        //Clear all breadcrumb data.
        public final static void	clearBreadCrumbs() {
            ...
        }
        //Clear all custom data.
        public final static void	clearCustomData() {
            ...
        }
        //Clears User information.
        public final static void	clearUserData() {
            ...
        }
        //Retrieve the number of unread items across all the conversations for the user synchronously i.e.
        public final static int	getUnreadCount() {
            ...
        }
    
        //Retrieve the number of unread items across all the conversations for the user asynchronously, count is delivered to the supplied UnreadUpdatesCallback instance Note : This may return 0 or stale value when there is no network connectivity etc
        public final static void	getUnreadCountAsync(UnreadUpdatesCallback callback) {
            ...
        }
        //Initialize the Mobihelp support section with necessary app configuration.
        public final static void	initAndroid(MobihelpConfig config) {
            ...
        }
    
        public final static void initIOS(MobihelpConfig config) {
            ...
        }
    
    
        //Attaches the given text as a breadcrumb to the conversations/tickets.
        public final static void	leaveBreadCrumb(String crumbText) {
            ...
        }
        //Set the email of the user to be reported on the Freshdesk Portal
        public final static void	setUserEmail(String email) {
            ...
        }
    
        //Set the name of the user to be reported on the Freshdesk Portal.
        public final static void	setUserFullName(String name) {
            ...
        }
    
        //Display the App Rating dialog with option to Rate, Leave feedback etc
        public static void	showAppRateDialog() {
            ...
        }
        //Directly launch Conversation list screen from anywhere within the application
        public final static void	showConversations() {
            ...
        }
        //Directly launch Feedback Screen from anywhere within the application.
        public final static void	showFeedback(FeedbackRequest feedbackRequest) {
            ...
        }
        //Directly launch Feedback Screen from anywhere within the application.
        public final static void	showFeedback() {
            ...
        }
        //Displays the Support landing page (Solution Article List Activity) where only solutions tagged with the given tags are displayed.
        public final static void	showSolutions(ArrayList<String> tags) {
            ...
        }
    
        private static String findUnusedSeparator(ArrayList<String> tags) {
            ...
    
        }
    
        //Displays the Support landing page (Solution Article List Activity) from where users can do the following
        //View solutions,
        //Search solutions,
        public final static void	showSolutions() {
            ...
        }
        //Displays the Integrated Support landing page where only solutions tagged with the given tags are displayed.
        public final static void	showSupport(ArrayList<String> tags) {
            ...
        }
    
        //Displays the Integrated Support landing page (Solution Article List Activity) from where users can do the following
        //View solutions,
        //Search solutions,
        //  Start a new conversation,
        //View existing conversations update/ unread count etc
        public final static void	showSupport() {
            ...
        }
    
    }

### The Native Interface

The final native interface is nearly identical to our public API, except in cases where the public API included non-primitive parameters.
    
    
    public interface MobihelpNative extends NativeInterface {
    
        /**
         * @return the appId
         */
        public String config_getAppId();
    
        /**
         * @param appId the appId to set
         */
        public void config_setAppId(String appId);
    
        /**
         * @return the appSecret
         */
        public String config_getAppSecret();
    
        /**
         * @param appSecret the appSecret to set
         */
        public void config_setAppSecret(String appSecret);
        /**
         * @return the domain
         */
        public String config_getDomain();
        /**
         * @param domain the domain to set
         */
        public void config_setDomain(String domain) ;
    
        /**
         * @return the feedbackType
         */
        public int config_getFeedbackType() ;
    
        /**
         * @param feedbackType the feedbackType to set
         */
        public void config_setFeedbackType(int feedbackType);
    
        /**
         * @return the launchCountForReviewPrompt
         */
        public int config_getLaunchCountForReviewPrompt() ;
        /**
         * @param launchCountForReviewPrompt the launchCountForReviewPrompt to set
         */
        public void config_setLaunchCountForReviewPrompt(int launchCountForReviewPrompt);
        /**
         * @return the prefetchSolutions
         */
        public boolean config_isPrefetchSolutions();
        /**
         * @param prefetchSolutions the prefetchOptions to set
         */
        public void config_setPrefetchSolutions(boolean prefetchSolutions);
        /**
         * @return the autoReplyEnabled
         */
        public boolean config_isAutoReplyEnabled();
    
        /**
         * @param autoReplyEnabled the autoReplyEnabled to set
         */
        public void config_setAutoReplyEnabled(boolean autoReplyEnabled) ;
    
        /**
         * @return the enhancedPrivacyModeEnabled
         */
        public boolean config_isEnhancedPrivacyModeEnabled() ;
    
        /**
         * @param enhancedPrivacyModeEnabled the enhancedPrivacyModeEnabled to set
         */
        public void config_setEnhancedPrivacyModeEnabled(boolean enhancedPrivacyModeEnabled) ;
    
    
    
        //Attach the given custom data (key-value pair) to the conversations/tickets.
        public void	addCustomData(String key, String value);
        //Attach the given custom data (key-value pair) to the conversations/tickets with the ability to flag sensitive data.
        public void addCustomDataWithSensitivity(String key, String value, boolean isSensitive);
        //Clear all breadcrumb data.
        public void	clearBreadCrumbs() ;
        //Clear all custom data.
        public void	clearCustomData();
        //Clears User information.
        public void	clearUserData();
        //Retrieve the number of unread items across all the conversations for the user synchronously i.e.
        public int getUnreadCount();
    
        //Retrieve the number of unread items across all the conversations for the user asynchronously, count is delivered to the supplied UnreadUpdatesCallback instance Note : This may return 0 or stale value when there is no network connectivity etc
        public void	getUnreadCountAsync(int callbackId);
    
        public void initNative();
    
        //Attaches the given text as a breadcrumb to the conversations/tickets.
        public  void leaveBreadCrumb(String crumbText);
        //Set the email of the user to be reported on the Freshdesk Portal
    
        public void setUserEmail(String email);
    
        //Set the name of the user to be reported on the Freshdesk Portal.
        public void setUserFullName(String name);
    
        //Display the App Rating dialog with option to Rate, Leave feedback etc
        public void	showAppRateDialog();
        //Directly launch Conversation list screen from anywhere within the application
        public void showConversations();
    
        //Directly launch Feedback Screen from anywhere within the application.
        public void	showFeedbackWithArgs(String subject, String description);
        //Directly launch Feedback Screen from anywhere within the application.
        public void	showFeedback();
    
        //Displays the Support landing page (Solution Article List Activity) where only solutions tagged with the given tags are displayed.
        public void	showSolutionsWithTags(String tags, String separator);
    
        //Displays the Support landing page (Solution Article List Activity) from where users can do the following
        //View solutions,
        //Search solutions,
        public void	showSolutions();
        //Displays the Integrated Support landing page where only solutions tagged with the given tags are displayed.
        public void	showSupportWithTags(String tags, String separator);
    
        //Displays the Integrated Support landing page (Solution Article List Activity) from where users can do the following
        //View solutions,
        //Search solutions,
        //  Start a new conversation,
        //View existing conversations update/ unread count etc
        public void	showSupport();
    }

Notice also, that the native interface includes a set of methods with names prefixed with `config__`. This is just a naming conventions I used to identify methods that map to the `MobihelpConfig` class. I could have used a separate native interface for these, but decided to keep all the native stuff in one class for simplicity and maintainability.

### Connecting the Public API to the Native Interface

So we have a public API, and we have a native interface. The idea is that the public API should be a thin wrapper around the native interface to smooth out rough edges that are likely to exist due to the strict set of rules involved in native interfaces. We’ll, therefore, use delegation inside the `Mobihelp` class to provide it a reference to an instance of `MobihelpNative`:
    
    
    public class Mobihelp {
        private static MobihelpNative peer;

We’ll initialize this `peer` inside the `init()` method of the `Mobihelp` class. Notice, though that `init()` is private since we have provided abstractions for the Android and iOS apps separately:
    
    
        //Initialize the Mobihelp support section with necessary app configuration.
        public final static void initAndroid(MobihelpConfig config) {
            if ("and".equals(Display.getInstance().getPlatformName())) {
                init(config);
            }
        }
    
        public final static void initIOS(MobihelpConfig config) {
            if ("ios".equals(Display.getInstance().getPlatformName())) {
                init(config);
            }
        }
    
        private static void init(MobihelpConfig config) {
            peer = (MobihelpNative)NativeLookup.create(MobihelpNative.class);
            peer.config_setAppId(config.getAppId());
            peer.config_setAppSecret(config.getAppSecret());
            peer.config_setAutoReplyEnabled(config.isAutoReplyEnabled());
            peer.config_setDomain(config.getDomain());
            peer.config_setEnhancedPrivacyModeEnabled(config.isEnhancedPrivacyModeEnabled());
            if (config.getFeedbackType() != null) {
                peer.config_setFeedbackType(config.getFeedbackType().ordinal());
            }
            peer.config_setLaunchCountForReviewPrompt(config.getLaunchCountForReviewPrompt());
            peer.config_setPrefetchSolutions(config.isPrefetchSolutions());
            peer.initNative();
    
        }

**Things to Notice** :

  1. The `initAndroid()` and `initIOS()` methods include a check to see if they are running on the correct platform. Ultimately they both call `init()`.

  2. The `init()` method, uses the `NativeLookup` class to instantiate our native interface.

### Implementing the Glue Between Public API and Native Interface

For most of the methods in the `Mobihelp` class, we can see that the public API will just be a thin wrapper around the native interface. E.g. the public API implementation of `setUserFullName(String)` is:
    
    
    public final static void setUserFullName(String name) {
        peer.setUserFullName(name);
    }

For some other methods, the public API needs to break apart the parameters into a form that the native interface can accept. E.g. the `init()` method, shown above, takes a `MobihelpConfig` object as a parameter, but it passed the properties of the `config` object individually into the native interface.

Another example, is the `showSupport(ArrayList<String> tags)` method. The corresponding native interface method that is wraps is `showSupport(String tags, String separator)` – i.e it needs to merge all tags into a single delimited string, and pass then to the native interface along with the delimiter used. The implementation is:
    
    
        public final static void showSupport(ArrayList<String> tags) {
            String separator = findUnusedSeparator(tags);
            StringBuilder sb = new StringBuilder();
            for (String tag : tags) {
                sb.append(tag).append(separator);
            }
            peer.showSupportWithTags(sb.toString().substring(0, sb.length()-separator.length()), separator);
        }

The only other non-trivial wrapper is the `getUnreadCountAsync()` method that we discussed before:
    
    
       public final static void getUnreadCountAsync(UnreadUpdatesCallback callback) {
            int callbackId = MobihelpNativeCallback.registerUnreadUpdatesCallback(callback);
            peer.getUnreadCountAsync(callbackId);
        }

## In the Next Instalment …​

In [part 2](http://www.codenameone.com/blog/integrating-3rd-party-native-sdks-part-2.html) of this series I’ll cover the native Android implementation, and part 3 will cover the native iOS implementation.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Steve Nganga** — September 29, 2015 at 4:43 pm ([permalink](https://www.codenameone.com/blog/integrating-3rd-party-native-sdks-part-1.html#comment-22265))

> just what i have been waiting for
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fintegrating-3rd-party-native-sdks-part-1.html)


### **Moacir Schmidt** — October 18, 2015 at 3:37 pm ([permalink](https://www.codenameone.com/blog/integrating-3rd-party-native-sdks-part-1.html#comment-22219))

> Excellent!
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fintegrating-3rd-party-native-sdks-part-1.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}

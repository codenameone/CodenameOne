---
title: Automatic Build Hints Configuration
slug: automatic-build-hints-configuration
url: /blog/automatic-build-hints-configuration/
original_url: https://www.codenameone.com/blog/automatic-build-hints-configuration.html
aliases:
- /blog/automatic-build-hints-configuration.html
date: '2017-01-17'
author: Shai Almog
---

![Header Image](/blog/automatic-build-hints-configuration/new-features-4.jpg)

We try to make Codename One “seamless”, this expresses itself in many small details such as the automatic detection of permissions on Android etc. The build servers go a long way in setting up the environment as intuitive. But it’s not enough, build hints are often confusing and obscure. It’s just hard to abstract the mess that is native mobile OS’s and the odd policies from Apple/Google…​

E.g. a common problem developers face is location code that doesn’t work in iOS. This is due to the `ios.locationUsageDescription` build hint that’s required. The reason we added that build hint was a requirement by Apple to provide a description for every app that uses the location service.

We could detect usage of the API in the servers and inject some random string into place and in fact that was what we were about to do with [issue 1415](https://github.com/codenameone/CodenameOne/issues/1415) but then it occurred to us that there is a much simpler way that will also provide far more power…​

We added two new API’s to `Display`:
    
    
    /**
     * Returns the build hints for the simulator, this will only work in the debug environment and it's
     * designed to allow extensions/API's to verify user settings/build hints exist
     * @return map of the build hints that isn't modified without the codename1.arg. prefix
     */
    public Map<String, String> getProjectBuildHints() {}
    
    /**
     * Sets a build hint into the settings while overwriting any previous value. This will only work in the
     * debug environment and it's designed to allow extensions/API's to verify user settings/build hints exist.
     * Important: this will throw an exception outside of the simulator!
     * @param key the build hint without the codename1.arg. prefix
     * @param value the value for the hint
     */
    public void setProjectBuildHint(String key, String value) {}

Both of these allow us to detect if a build hint is set and if not (or if set incorrectly) set its value…​

So now if you will use the location API from the simulator and you didn’t define `ios.locationUsageDescription` we will implicitly define a string there. The cool thing is that you will now see that string in your settings and you would be able to customize it easily.

However, this gets way better than just that trivial example!

The real value is for 3rd party libraries, e.g. Google Maps or Parse. They can inspect the build hints in the simulator and show an error in case of a misconfiguration. They can even show a setup UI. Demos that need special keys in place can force the developer to set them up properly before continuing. We plan to make extensive use of this feature moving forward.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}

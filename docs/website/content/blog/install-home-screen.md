---
title: Install on Home Screen
slug: install-home-screen
url: /blog/install-home-screen/
original_url: https://www.codenameone.com/blog/install-home-screen.html
aliases:
- /blog/install-home-screen.html
date: '2018-12-03'
author: Shai Almog
---

![Header Image](/blog/install-home-screen/html5-banner.jpg)

We talked about our support for [Progressive Web Apps before](/blog/progressive-web-apps.html). We added quite a few enhancements since that support was introduced and it’s a pretty powerful feature. Personally I consider it a killer feature, even if Google decides to [ban your account](https://www.reddit.com/r/androiddev/comments/9mpyyi/google_play_developer_account_terminated_due_to/) you can still distribute your app.

One of the cool features is the seamlessness. Most of the functionality “just works”. However, there are some cases where we need explicit hints due to the different behavior of desktop/mobile and web.

One such case is installation. PWA’s support an icon on the device home screen, but you need to explicitly ask the browser to install that icon. That’s unique to PWA’s and requires a new API to support that behavior. That’s why we introduced `onCanInstallOnHomescreen`, `canInstallOnHomescreen()` and `promptInstallOnHomescreen()` to help with that process. You can use them as:
    
    
    onCanInstallOnHomescreen(()->{
        if (canInstallOnHomescreen()) {
            if (promptInstallOnHomescreen()) {
                // User accepted installation
            } else {
                // user rejected installation
            }
        }
    });

__ |  This code expects `import static com.codename1.ui.CN.*;`  
---|---  
  
The code would prompt the user to install on the home screen in OS’s where this is appropriate. Notice that this will prompt the user once to install on the home screen so you don’t need additional guards against duplicate prompts.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}

---
title: Questions of the Week XXVIII
slug: questions-of-the-week-xxviii
url: /blog/questions-of-the-week-xxviii/
original_url: https://www.codenameone.com/blog/questions-of-the-week-xxviii.html
aliases:
- /blog/questions-of-the-week-xxviii.html
date: '2016-10-20'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-xxviii/qanda-friday2.jpg)

Unlike last week this has mostly been a calm week with the exception of some downtime we had on the  
certificate wizard (Apple tightened TLS access) there was hardly anything major going on. We are focusing  
a lot of our efforts on refining our offering on all fronts e.g.  
[better demos](/sql-playground-sql-tutorial-in-the-browser-iphone-ios-android-windows/),  
[themes](/blog/template-clean-modern-ui-kit.html),  
[docs](/manual/), [compatibility](/blog/file-url-java-mobile-compatibility.html) and  
[tools](/blog/further-refined-cross-platform-mobile-gui-builder.html).

Things will probably stay relatively easy until November although I think a big focus will shift into handling  
some of the bugs we have lined up for 3.6.

Due to some painful regressions in the GUI builder we decided to release another plugin update which isn’t ideal as  
we try to release as few of those as possible and focus on library updates which are more lightweight. Hopefully  
this will be the last update before mid November.

On stack overflow things were as usual:

### App broken after iOS build

This might have happened because sending a build updates the libraries if there is a new version. This is a  
problem that occasionally happens on NetBeans where the AST cache gets corrupted. It’s solvable in  
NetBeans by removing the caches, not sure how that’s done in intellij

[Read on stackoverflow…​](http://stackoverflow.com/questions/40136685/codename-one-app-broken-after-ios-build)

### Back navigation does not stop youtube video in WebBrowser

The `browserComponent.destroy()` isn’t very intuitive

[Read on stackoverflow…​](http://stackoverflow.com/questions/40142655/back-navigation-does-not-stop-youtube-video-in-webbrowser/40144218#40144218)

### Preference class picks wrong method

When we built this API initially it looked like a good idea to name all the methods with the same name  
and differentiate them by argument type, in retrospect that was a mistake…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/40103538/preference-class-picks-wrong-method)

### App version number programmatically

I’m not sure how we can make this simpler, I’ve thought about adding a dedicated API but people don’t find  
`getUDID()` either…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/40101244/codename-one-app-version-number-programmatically)

### NSAllowsArbitraryLoadsInWebContent in CN1

That’s exactly the use case for `ios.plistInject`

[Read on stackoverflow…​](http://stackoverflow.com/questions/40085375/nsallowsarbitraryloadsinwebcontent-in-cn1)

### Using CN1 bluetooth is not scanning devices on Android

Bluetooth is a bit painful as an API

[Read on stackoverflow…​](http://stackoverflow.com/questions/40081892/using-cn1-bluetooth-is-not-scanning-devices-on-android)

### SSL error- Connention reset by peer on using Generate Certificates for ios builds

We had an issue with Apple servers just in the week Steve took off on vacation. Luckily he stepped in because  
I was totally on the wrong track here…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/40081738/ssl-error-connention-reset-by-peer-on-using-generate-certificates-for-ios-build)

### AutoCompleteTextField does not respect minimum length on load

This should be fixed by now with the new update

[Read on stackoverflow…​](http://stackoverflow.com/questions/40076937/autocompletetextfield-does-not-respect-minimum-length-on-load)

### Autocomplete text field firing selection event on load

This is related to text field events that are “over eager” and would fire twice rather than miss a change

[Read on stackoverflow…​](http://stackoverflow.com/questions/40076238/autocomplete-text-field-firing-selection-event-on-load)

### How can I access sqlite database on a webserver in codename one

We get a lot of questions on accessing remote DBMS’s on servers. I’d love to have a short decent explanation  
on why this is a bad idea without going into NAT’s, security and the other problems.

[Read on stackoverflow…​](http://stackoverflow.com/questions/40068127/how-can-i-access-sqlite-database-on-a-webserver-in-codename-one)

### Pass Data to other Forms and update Form

We might want to create a post with a discussion of the best practices for state within an application but this  
is a bit of a big subject

[Read on stackoverflow…​](http://stackoverflow.com/questions/40059169/pass-data-to-other-forms-and-update-form)

### How to mock server / http requests in Codename One for testing?

Our mock testing tools are non-existant which is a shame. We should have a mock implementation of Codename One  
and it should be pretty trivial to build…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/40057677/how-to-mock-server-http-requests-in-codename-one-for-testing)

### How I can access to the webcam in cn1 windows desktop build?

JavaSE/FX is pretty problematic about basic things like that…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/40053132/how-i-can-access-to-the-webcamm-in-cn1-windows-desktop-build)

### Slider control: border is invisible until I press and drag it

There is an open issue on this in github which we **really** need to fix by now…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/40051067/slider-control-border-is-invisible-until-i-press-and-drag-it)

### Codename One AnalyticsService suddenly completely blocks the application flow

Sometimes these things are really hard to track…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/40050765/codename-one-analyticsservice-suddenly-completely-blocks-the-application-flow)

### How to access sqlite database from webserver and insert record using web services in codenameone

This question seems to confuse the use of sqlite and webservices

[Read on stackoverflow…​](http://stackoverflow.com/questions/40049987/how-to-access-sqlite-database-from-webserver-and-insert-record-using-web-service)

### How to modify the height of the statusbar in codenameone?

We made this even easier in the recent update where we provided fine grained control over the status bar

[Read on stackoverflow…​](http://stackoverflow.com/questions/40047546/how-to-modify-the-height-of-the-statusbar-in-codenameone)

### CodenameOne Background color

This is a huge deficiency in the designer tool, it should prevent users from changing the color when a border  
is in effect

[Read on stackoverflow…​](http://stackoverflow.com/questions/40045050/codenameone-background-color)

### CodenameOne playing video / size and position

We made some improvements to video playback that were required due to regression we got after the new-peer switch

[Read on stackoverflow…​](http://stackoverflow.com/questions/40042946/codenameone-playing-video-size-and-position)

### Codename one list scrolled down when returning to form

If this is with the old GUI builder then I have a good answer but you need to ask the right question…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/40032365/codename-one-list-scrolled-down-when-returning-to-form)

### How to bundle sounds with Codename One?

You can play arbitrary media files easily

[Read on stackoverflow…​](http://stackoverflow.com/questions/40029586/how-to-bundle-sounds-with-codename-one)

### App launch on startup in codename one

This seems to be a common request but not really doable across platforms

[Read on stackoverflow…​](http://stackoverflow.com/questions/40013604/app-launch-on-startup-in-codename-one)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}

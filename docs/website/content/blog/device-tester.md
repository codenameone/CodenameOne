---
title: Device Tester
slug: device-tester
url: /blog/device-tester/
original_url: https://www.codenameone.com/blog/device-tester.html
aliases:
- /blog/device-tester.html
date: '2016-07-04'
author: Shai Almog
---

![Header Image](/blog/device-tester/device-tester.png)

A common issue we get from developers relates to minor differences between devices which are often very  
hard to quantify. They are also very hard to explain to the developers in some occasions. One of the biggest  
points of difficulty is density which is a very hard concept to grasp and it’s often hard to know which image will  
be used from the set of multi images or how many pixels will be used for 1 millimeter.

To make this process slightly easier we created a new demo that is remarkably simple but we are hoping it  
would also be remarkably helpful. It just prints on the screen the settings and values of all the important  
details mostly from the [Display class](/javadoc/com/codename1/ui/Display/).

This demo also allows you to send the details to your email directly so you can send it to a user who is experiencing  
issues and understand some things about their device. This will also make it simpler for you to communicate issues  
with us as we won’t need as much back and forth with device details.

You can check out the full repository on github [here](https://github.com/codenameone/DeviceTester) and you can  
use that to get proper debug logic for your app. We hope to add it to the IDE’s so it will be a part of the new app  
wizard dialog.

My OnePlus One outputs something like this, which you can see is pretty useful if you want to understand the device  
type we are dealing with:
    
    
    Density: DENSITY_HD
    Platform Name: and
    User Agent: Dalvik/2.1.0 (Linux; U; Android 6.0.1; A0001 Build/MMB29X)
    OS:Android
    OS Version: 6.0.1
    UDID:865800025623619
    MSISDN:
    Display Width X Height: 1080X1920
    1mm In Pixels: 18.898
    Language: en
    Locale: US
    Currency Symbol: $
    Are Mutable Images Fast: false
    Can Dial: true
    Can Force Orientation: true
    Has Camera: true
    Badging: false
    Desktop: false
    Tablet: false
    Gaussian Blur Support: true
    Get All Contacts Fast: true
    Multi Touch: true
    PICKER_TYPE_DATE: true
    PICKER_TYPE_DATE_AND_TIME: false
    PICKER_TYPE_STRINGS: true
    PICKER_TYPE_TIME: true
    Native Share: true
    Native Video Player Controls: true
    Notification: true
    Open Native Navigation: true
    Screen Saver Disable: true
    Simulator: false

The Java code to produce this is pretty trivial albeit a bit verbose:
    
    
    public void start() {
        if(current != null){
            current.show();
            return;
        }
        Form hi = new Form("Basic Details", BoxLayout.y());
        Display d = Display.getInstance();
        String density = "";
        switch(Display.getInstance().getDeviceDensity()) {
            case Display.DENSITY_2HD:
                density = "DENSITY_2HD";
                break;
            case Display.DENSITY_4K:
                density = "DENSITY_4K";
                break;
            case Display.DENSITY_560:
                density = "DENSITY_560";
                break;
            case Display.DENSITY_HD:
                density = "DENSITY_HD";
                break;
            case Display.DENSITY_HIGH:
                density = "DENSITY_HIGH";
                break;
            case Display.DENSITY_LOW:
                density = "DENSITY_LOW";
                break;
            case Display.DENSITY_MEDIUM:
                density = "DENSITY_MEDIUM";
                break;
            case Display.DENSITY_VERY_HIGH:
                density = "DENSITY_VERY_HIGH";
                break;
            case Display.DENSITY_VERY_LOW:
                density = "DENSITY_VERY_LOW";
                break;
        }
    
        double pixelsPerMM = (((double)d.convertToPixels(1000)) / 1000.0);
        L10NManager l10n = L10NManager.getInstance();
    
        hi.add("Density:").
                add(new SpanLabel(density)).
                add(" ").
                add("Platform Name:").
                add(new SpanLabel(d.getPlatformName())).
                add(" ").
                add("User Agent:").
                add(new SpanLabel(d.getProperty("User-Agent", ""))).
                add(" ").
                add("OS:").
                add(new SpanLabel(d.getProperty("OS", ""))).
                add(" ").
                add("OS Version:").
                add(new SpanLabel(d.getProperty("OSVer", ""))).
                add(" ").
                add("UDID:").
                add(new SpanLabel(d.getUdid())).
                add(" ").
                add("MSISDN:").
                add(new SpanLabel(d.getMsisdn())).
                add(" ").
                add("Display Width X Height:").
                add(new SpanLabel(d.getDisplayWidth() + "X" + d.getDisplayHeight())).
                add(" ").
                add("1mm In Pixels:").
                add(new SpanLabel(l10n.format(pixelsPerMM))).
                add(" ").
                add("Language:").
                add(new SpanLabel(l10n.getLanguage())).
                add(" ").
                add("Locale:").
                add(new SpanLabel(l10n.getLocale())).
                add(" ").
                add("Currency Symbol:").
                add(new SpanLabel(l10n.getCurrencySymbol())).
                add(" ").
                add(uneditableCheck("Are Mutable Images Fast", d.areMutableImagesFast())).
                add(uneditableCheck("Can Dial", d.canDial())).
                add(uneditableCheck("Can Force Orientation", d.canForceOrientation())).
                add(uneditableCheck("Has Camera", d.hasCamera())).
                add(uneditableCheck("Badging", d.isBadgingSupported())).
                add(uneditableCheck("Desktop", d.isDesktop())).
                add(uneditableCheck("Tablet", d.isTablet())).
                add(uneditableCheck("Gaussian Blur Support", d.isGaussianBlurSupported())).
                add(uneditableCheck("Get All Contacts Fast", d.isGetAllContactsFast())).
                add(uneditableCheck("Multi Touch", d.isMultiTouch())).
                add(uneditableCheck("PICKER_TYPE_DATE", d.isNativePickerTypeSupported(Display.PICKER_TYPE_DATE))).
                add(uneditableCheck("PICKER_TYPE_DATE_AND_TIME", d.isNativePickerTypeSupported(Display.PICKER_TYPE_DATE_AND_TIME))).
                add(uneditableCheck("PICKER_TYPE_STRINGS", d.isNativePickerTypeSupported(Display.PICKER_TYPE_STRINGS))).
                add(uneditableCheck("PICKER_TYPE_TIME", d.isNativePickerTypeSupported(Display.PICKER_TYPE_TIME))).
                add(uneditableCheck("Native Share", d.isNativeShareSupported())).
                add(uneditableCheck("Native Video Player Controls", d.isNativeVideoPlayerControlsIncluded())).
                add(uneditableCheck("Notification", d.isNotificationSupported())).
                add(uneditableCheck("Open Native Navigation", d.isOpenNativeNavigationAppSupported())).
                add(uneditableCheck("Screen Saver Disable", d.isScreenSaverDisableSupported())).
                add(uneditableCheck("Simulator", d.isSimulator()));
    
        final String densityStr = density;
        hi.getToolbar().addMaterialCommandToRightBar("", FontImage.MATERIAL_SEND, e -> {
            StringBuilder body = new StringBuilder("Density: ").
                append(densityStr).
                append("n").
                append("Platform Name: ").
                append(d.getPlatformName()).
                append("n").
                append("User Agent: ").
                append(d.getProperty("User-Agent", "")).
                append("n").
                append("OS: ").
                append(d.getProperty("OS", "")).
                append("n").
                append("OS Version: ").
                append(d.getProperty("OSVer", "")).
                append("n").
                append("UDID: ").
                append(d.getUdid()).
                append("n").
                append("MSISDN: ").
                append(d.getMsisdn()).
                append("n").
                append("Display Width X Height: ").
                append(d.getDisplayWidth()).append("X").append(d.getDisplayHeight()).
                append("n").
                append("1mm In Pixels: ").
                append(l10n.format(pixelsPerMM)).
                append("n").
                append("Language: ").
                append(l10n.getLanguage()).
                append("n").
                append("Locale: ").
                append(l10n.getLocale()).
                append("n").
                append("Currency Symbol: ").
                append(l10n.getCurrencySymbol()).
                append("nAre Mutable Images Fast: ").
                append(d.areMutableImagesFast()).
                append("nCan Dial: ").
                append(d.canDial()).
                append("nCan Force Orientation: ").append(d.canForceOrientation()).
                append("nHas Camera: ").append(d.hasCamera()).
                append("nBadging: ").append(d.isBadgingSupported()).
                append("nDesktop: ").append(d.isDesktop()).
                append("nTablet: ").append(d.isTablet()).
                append("nGaussian Blur Support: ").append(d.isGaussianBlurSupported()).
                append("nGet All Contacts Fast: ").append(d.isGetAllContactsFast()).
                append("nMulti Touch: ").append(d.isMultiTouch()).
                append("nPICKER_TYPE_DATE: ").append(d.isNativePickerTypeSupported(Display.PICKER_TYPE_DATE)).
                append("nPICKER_TYPE_DATE_AND_TIME: ").append(d.isNativePickerTypeSupported(Display.PICKER_TYPE_DATE_AND_TIME)).
                append("nPICKER_TYPE_STRINGS: ").append(d.isNativePickerTypeSupported(Display.PICKER_TYPE_STRINGS)).
                append("nPICKER_TYPE_TIME: ").append(d.isNativePickerTypeSupported(Display.PICKER_TYPE_TIME)).
                append("nNative Share: ").append(d.isNativeShareSupported()).
                append("nNative Video Player Controls: ").append(d.isNativeVideoPlayerControlsIncluded()).
                append("nNotification: ").append(d.isNotificationSupported()).
                append("nOpen Native Navigation: ").append(d.isOpenNativeNavigationAppSupported()).
                append("nScreen Saver Disable: ").append(d.isScreenSaverDisableSupported()).
                append("nSimulator: ").append(d.isSimulator());
    
            Message msg = new Message(body.toString());
    
            Display.getInstance().sendMessage(new String[] { Display.getInstance().getProperty("built_by_user", "[[email protected]](/cdn-cgi/l/email-protection)") },
                    "Device Details", msg);
        });
    
        hi.show();
    }
    
    private CheckBox uneditableCheck(String t, boolean v) {
        CheckBox c = new CheckBox(t);
        c.setSelected(v);
        c.setEnabled(false);
        return c;
    }
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **bryan** — July 5, 2016 at 11:02 pm ([permalink](/blog/device-tester/#comment-22885))

> bryan says:
>
> Very useful – thanks.
>



### **bryan** — July 5, 2016 at 11:54 pm ([permalink](/blog/device-tester/#comment-21515))

> bryan says:
>
> as per issue 1808, Display.getInstance().sendMessage() doesn’t work on WP10.
>



### **Imriel** — July 12, 2016 at 5:59 am ([permalink](/blog/device-tester/#comment-22625))

> Imriel says:
>
> How to check with device memory?
>



### **Shai Almog** — July 13, 2016 at 3:54 am ([permalink](/blog/device-tester/#comment-22634))

> Shai Almog says:
>
> You can’t do that reliably. Devices have a mixed notion of memory segmentation and we don’t really know the available memory in some cases.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}

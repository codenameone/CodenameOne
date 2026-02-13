---
title: LOCALIZE/TRANSLATE MY APPLICATION? APPLY I18N/L10N (INTERNATIONALIZATION/LOCALIZATION)
  TO MY APP?
slug: how-do-i-localizetranslate-my-application-apply-i18nl10n-internationalizationlocalization-to-my-app
url: /how-do-i/how-do-i-localizetranslate-my-application-apply-i18nl10n-internationalizationlocalization-to-my-app/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-localizetranslate-my-application-apply-i18nl10n-internationalizationlocalization-to-my-app.html
tags:
- basic
- ui
description: Codename One features seamless localization and BiDi RTL support
youtube_id: 32mkZymqa6E
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-12-1.jpg
---

{{< youtube "32mkZymqa6E" >}} 

#### Script

###### Transcript

In this short video we’ll discuss some of the core concepts of internationalization and localization which are often abbreviated as i18n and l10n. We’ll start by defining these two big terms. Internationalization is the system of making an application adaptable to the various locales which means the application will be flexible when we need to make it work for users in different countries.

Localization is the process of adapting a specific application to a new locale, this includes translation but that’s a bit of an over simplification of the purpose of localization.

Locale goes far deeper than just language translation. Locale covers the way dates are arranged, the way numbers are represented, the way sentences are phrased and far more than that… I’ll talk about some of those things as we move forward.

Translation is the first thing we encounter in localization tasks and probably the most important aspect. Codename One makes translation seamless. You can just install a resource bundle into the UIManager. A resource bundle is a set of key value pairs so the key would be a string in english and the value would be the same string in a different language.

This gets translated seamlessly so if you set the key from the bundle as the text to the a label that label will be seamlessly localized. You don’t need to litter your code with calls to a localize method…

You can obviously invoke the localize method of UIManager if you have a special case but you don’t need to for most cases.

One of the big things we omitted from Codename One is message format. That class solves some complex edge case formatting but is also a half baked solution. A sentence like “This is your 3rd try” will not work well with message format so we think there are probably better ways to implement that functionality rather than repeat over engineered mistakes from Java SE.

One of the important aspects of localization is culture. A great example in this sense is color which especially in oriental cultures has very different meanings. An example would be red which means stop or problem in the west but it might mean something different in the east. We can extract that into the resource bundle.

Localization of a Codename One application starts in the designer tool. Open the theme.res file for your application and select the localization section. You can add a resource bundle here to set the keys and values.

You can add additional locales using this button. In this case I added the iw locale.

Notice that the text is translated in the iw column. In this case it’s pretty simple to just edit the column value in the table.  
Notice the last highlighted row which uses the @rtl notation. That’s a special case marker indicating whether the language is a right to left language. Notice that the iw locale is marked right to left, we’ll discuss RTL languages soon  
But first check out the buttons in the bottom. With these you can add, remove or rename complex properties in the tool

Moving on to the code lets review this line by line.  
We get the language from the localization manager tool, this is a short iso code for the language and not the actual string name  
It should be lower case and non-null by default but I’m just playing safe here…  
The locale sometimes changes iso codes so some platforms might still be using the old “he” code instead of the newer “iw” code.  
There are two important methods here. First is set bundle, we set the key value pairs for the current language. The second is getL10N where we get the localization bundle for iw from the theme.  
Notice we ignore English as that is the default language for the application.

Now here is a really neat trick to test this. Right click the project and select project properties.  
In the IDE’s project properties you can set the vm arguments passed to the simulator and manipulate the language of the JVM. This makes debugging localization very easy. Just enter the run section and add the build hint -Duser.language=iw and it will become the language of the JVM. This saves you the need of updating your OS localization settings to debug locale issues.

Up until now we only discussed translation and while I gave you a glimpse at the L10NManager class it’s a pretty big class that has a lot of locale specific methods that should help in the localization process. Another important class is the simple date format, notice that it exists in the standard Java packages too but that’s problematic. The version of simple date format from the java.text package will be inconsistent between OS’s since it will use the native VM simple date format on Java SE and Android but for other platforms it will fallback to our simpler implementation. Using the one from the l10n package guarantees better cross platform consistency.

L10NManager has multiple format methods that make the process of formatting numbers & dates much easier. Currency and related values should usually use that class for formatting.

I mentioned RTL before but I didn’t explain it well. Some languages such as Hebrew and Arabic are written from right to left. Probably because they are so ancient that they were mostly carved on rock and it was easier holding the hammer in the right hand. In modern times this is a source of great pain from smearing of ink while writing to the basic bidi problem.

Bidi represents mixing an RTL language with a left to right language or numbers. Numbers are still drawn from left to right in these languages so when you type text in a text field the cursor will literally “jump” to accommodate the reverse flow in such a case…. This makes the problem not just a right to left problem but rather a bi-directional problem or bidi for short.

One of the core expectations of working in such languages is UI reversal. Books in RTL languages are the mirror images of latin books in the sense that you turn the pages in the opposite direction. Text is aligned to the right and everything should be flipped. The same is true for a UI we expect alignment in reverse as well as reverse order for the components within the layouts. This is handled mostly seamlessly by Codename One layouts. They reverse the element order and flip sides when RTL mode is active, so if you add something to border layout EAST in RTL it would act like you added it to WEST and so forth. Aligning an object to the left in RTL would be like aligning it to the right and visa versa.

If we look at the UI of the restaurant app in bidi mode we can see several cases where this behaves as expected. The menu and play button are reversed. But notice the play button points in the opposite direction, that’s exactly one of those nuances we need to pay attention to as the icon itself should be flipped for bidi. Notice that al lthe text is aligned to the right as well instead of the left.

Thanks for watching, I hope you found this helpful.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}

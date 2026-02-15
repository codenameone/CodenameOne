---
title: Toolbar Search Mode
slug: toolbar-search-mode
url: /blog/toolbar-search-mode/
original_url: https://www.codenameone.com/blog/toolbar-search-mode.html
aliases:
- /blog/toolbar-search-mode.html
date: '2016-06-12'
author: Shai Almog
---

![Header Image](/blog/toolbar-search-mode/toolbar-search-mode.jpg)

The [Toolbar](/javadoc/com/codename1/ui/Toolbar/) is a pretty flexible  
API for handling the title area. It allows a lot of things including search but up until now that very common  
use case was not a standard part of the API.

It is now, we just added a new API that effectively allows you to bind search to a form and just get the text  
searched as part of the callback. The `Toolbar` handles everything else including replacing the title area  
and returning it back to its original state when you are done.

You can also customize the appearance of the search bar by using the UIID’s: `ToolbarSearch`, `TextFieldSearch` &  
`TextHintSearch`.

In the sample below we fetch all the contacts from the device and enable search thru them, notice it expects  
and image called `duke.png` which is really just the default Codename One icon renamed and placed in the src  
folder:
    
    
    Image duke = null;
    try {
        duke = Image.createImage("/duke.png");
    } catch(IOException err) {
        Log.e(err);
    }
    int fiveMM = Display.getInstance().convertToPixels(5);
    final Image finalDuke = duke.scaledWidth(fiveMM);
    Toolbar.setGlobalToolbar(true);
    Form hi = new Form("Search", BoxLayout.y());
    hi.add(new InfiniteProgress());
    Display.getInstance().scheduleBackgroundTask(()-> {
        // this will take a while...
        Contact[] cnts = Display.getInstance().getAllContacts(true, true, true, true, false, false);
        Display.getInstance().callSerially(() -> {
            hi.removeAll();
            for(Contact c : cnts) {
                MultiButton m = new MultiButton();
                m.setTextLine1(c.getDisplayName());
                m.setTextLine2(c.getPrimaryPhoneNumber());
                Image pic = c.getPhoto();
                if(pic != null) {
                    m.setIcon(fill(pic, finalDuke.getWidth(), finalDuke.getHeight()));
                } else {
                    m.setIcon(finalDuke);
                }
                hi.add(m);
            }
            hi.revalidate();
        });
    });
    
    hi.getToolbar().addSearchCommand(e -> {
        String text = (String)e.getSource();
        if(text == null || text.length() == 0) {
            // clear search
            for(Component cmp : hi.getContentPane()) {
                cmp.setHidden(false);
                cmp.setVisible(true);
            }
            hi.getContentPane().animateLayout(150);
        } else {
            text = text.toLowerCase();
            for(Component cmp : hi.getContentPane()) {
                MultiButton mb = (MultiButton)cmp;
                String line1 = mb.getTextLine1();
                String line2 = mb.getTextLine2();
                boolean show = line1 != null && line1.toLowerCase().indexOf(text) > -1 ||
                        line2 != null && line2.toLowerCase().indexOf(text) > -1;
                mb.setHidden(!show);
                mb.setVisible(show);
            }
            hi.getContentPane().animateLayout(150);
        }
    }, 4);
    
    hi.show();

### Up Next

I think this shows some of the directions we will be taking with Codename One components as we move forward.  
Future components from us will make more use of the icon fonts and material design icons to allow a default  
look that “just works” everywhere.

You would still be able to customize everything like you always did but unlike the past, we hope the default look  
will start off as both functional and attractive.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Mac Flanegan** — February 17, 2017 at 9:08 pm ([permalink](/blog/toolbar-search-mode/#comment-23368))

> Mac Flanegan says:
>
> Hi Guys,
>
> How do I translate the text (“Search” ) of TextHintSearch?
>
> I’m testing on Android and I notice a strange behavior.  
> Is it possible to delete any forrm animation? To avoid appearing to be reloaded?
>
> I have commented hi.getContentPane().animateLayout(150); but still flickering the form when click in back.
>
> When testing on Android (did not test on others), and start typing, does not show the magnifying glass icon in “Enter” key of keyboard, is possible?
>
> And also does not accept ENTER in search text field. To perform the search I have to hit back (on device) and only then the text is accepted and search performed.  
> I believe the component is interpreting ENTER as a new line rather than executing the command.


### **Shai Almog** — February 18, 2017 at 10:57 am ([permalink](/blog/toolbar-search-mode/#comment-23288))

> Shai Almog says:
>
> Hi,  
> I’ll make it so “[m.search](<http://m.search>)” in the resource bundle will allow you to localize this. This video is a bit old but it discusses localizations: [https://www.codenameone.com…]([/how-do-i—localizetranslate-my-application-apply-i18nl10n-internationalizationlocalization-to-my-app.html](https://www.codenameone.com/how-do-i---localizetranslate-my-application-apply-i18nl10n-internationalizationlocalization-to-my-app/))
>
> Currently we don’t support disabling the animation but what did you do that triggered this? It’s hard for me to visualize the issue you’re seeing.
>
> The magnifying glass issue is a simple fix I’m surprised we didn’t notice that. It should be fixed in the coming update.
>
> Search happens as you type so I’m not sure how you are seeing that behavior. I think the problem is that you aren’t using revalidate or animateLayout to show your changes to the UI and so the search only updates when we do it on the back button.


### **Mac Flanegan** — February 18, 2017 at 12:47 pm ([permalink](/blog/toolbar-search-mode/#comment-23196))

> Mac Flanegan says:
>
> Thanks,
>
> I’ll work a little on it.
>
> PS: It may be interesting to leave the SearchTextField field public.  
> So I could make small hacks like search.getSearchTextField (). PutClientProperty (“searchField”, Boolean.TRUE);


### **Shai Almog** — February 19, 2017 at 7:15 am ([permalink](/blog/toolbar-search-mode/#comment-23139))

> Shai Almog says:
>
> The problem with doing things like this is that the more you expose people end up relying on edge cases of the API and you can’t change them. E.g. in the future we might want to support iOS style search where the text field appears below the title. If we expose too much of the implementation details this can become a problem.
>
> Notice you can still add features to this thru a pull request to our main repository which is really easy to do: [https://www.codenameone.com…](</blog/how-to-use-the-codename-one-sources/>)


### **Nils Lamb** — August 4, 2017 at 12:08 pm ([permalink](/blog/toolbar-search-mode/#comment-23325))

> Nils Lamb says:
>
> I added the “[m.search](<http://m.search>)” key into my custom resource bundle. This currently contains the locales de and en, which works fine for the rest of the application. However, the search hint text which defaults to “Search” in english, does not use the values from the [m.search](<http://m.search>) key.  
> Is this implemented already?


### **Shai Almog** — August 5, 2017 at 6:23 am ([permalink](/blog/toolbar-search-mode/#comment-23406))

> Shai Almog says:
>
> Yes it was implemented but looking at the code it’s possible this wasn’t 100% correct for all cases. I made a fix which will hopefully resolve the issue you are seeing.


### **Yaakov Gesher** — February 27, 2018 at 1:25 pm ([permalink](/blog/toolbar-search-mode/#comment-23773))

> Yaakov Gesher says:
>
> Hi, is there a way to filter out the events from the SearchBar so that I know when the ENTER key was pressed? I’m interested in only executing a search when the user initiates it by pressing the search key


### **Shai Almog** — February 28, 2018 at 8:59 am ([permalink](/blog/toolbar-search-mode/#comment-23905))

> Shai Almog says:
>
> No. Right now the search bar is only designed for interactive search.  
> You can create a completely custom toolbar with the samples in the Toolbar class in the JavaDoc. You can change the datachange listener to an action listener or done listener in that code.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}

---
title: CREATE A GORGEOUS SIDEMENU
slug: how-do-i-create-gorgeous-sidemenu
url: /how-do-i/how-do-i-create-gorgeous-sidemenu/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-create-gorgeous-sidemenu.html
tags:
- basic
- featured
- ui
description: Great UI needs great navigational interface
youtube_id: 99DAeP9LG6c
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-8-1.jpg
---

{{< youtube "99DAeP9LG6c" >}} 

#### Transcript

Hello!  
In this short video I’m going to walk you thru the steps of creating a gorgeous side menu user interface.  
We’ll start by creating a simple hello world style application with no frills. I’m using a native theme and a bare bones application so nothing pre-existing will disturb us.

As you can see there isn’t much here. We’ll add new entries into the toolbar for the different pieces of the side menu.  
Now I’m going to use add material command to the side menu which adds command text and an icon from the standard material design icons built into Codename One. That will give us something to work with, it’s mostly basic stuff you can use whatever you want in terms of commands. The styling isn’t mentioned it’s only the icons and the text.  
Now we’ll open the theme file and try to make the styling “look good”. First I’ll open the side menu in the preview section. This makes the entries for side component and side panel appear in the combo box.

I’ll start with the side navigation panel which is pretty easy, I’m using a white background that’s completely opaque. I press OK to save this.

For the side command I want to pick an opaque color for the background and white for the foreground. I’m picking a bluish/purple color that looks right usually I pick from a pallet but here I whinged it and I think it came out reasonably well.

I’m making sure alignment is left just to be sure, for padding I’m setting it all to be relatively large. 3 millimeters which gives everything a good feel and spacing. This is important for finger touch sensitivity.

I’m setting margin to 0 except for the bottom one pixel which will leave a nice white line by showing off the background. I’m also setting the border to empty so it isn’t inherited from the native theme like it is on iOS.

I’m picking a good looking font making sure it is large enough I’m using millimeters so it’s sized correctly for all OS’s and I’m overriding the derived text decoration which has a value in the iOS native theme so it can impact the final look.

In the selected tab I’m adding a new side command entry that derives from the unselected version. I’m picking a new color that’s slightly deeper and will make the selected style appear selected. I’m copying and pasting this to the pressed style as well.

Clicking the theme again allows me to refresh it and see how everything will look on the simulator when we’re done.

Next I’ll add the icon image into the theme as a multi image so I can use it within the side menu as a good looking logo.

Back in the IDE I get the image I just added into the theme. I then place this image into a Container that goes into the top of the side menu. I do that by creating a border layout container, I place the image label on the east side thus aligning it to the right side of the container.

I then add a new tagline label to the south of the container and give it a tagline style so we can customize its look.

The side menu tagline is just a side command style that was slightly adapted, I’m removing the padding and margin because the whole section will be wrapped in a side command.

The next thing I’ll do is update the font to a smaller size and italic styling so it will feel like a tagline.

Now that we did all this lets go back into the IDE and tie this together with the top bar which now we can use as a side command UIID by giving it the same color and padding as regular commands.

We tie the whole thing together by adding the component, the top bar to the toolbar side menu.

Finally I’m styling the status bar side menu which is the spacing on top left for iOS devices. I’m setting it to 0 and now we’re ready to run the app!

This is the final result! This is how the side menu looks.  
You can use a better icon or logo which will make this look much better. You can use a background image for the top bar and these small things can make a huge difference.

Thanks for watching, I hope you found this useful.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}

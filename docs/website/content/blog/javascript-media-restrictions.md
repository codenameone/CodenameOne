---
title: Javascript Media Restrictions to be Aware Of
slug: javascript-media-restrictions
url: /blog/javascript-media-restrictions/
original_url: https://www.codenameone.com/blog/javascript-media-restrictions.html
aliases:
- /blog/javascript-media-restrictions.html
date: '2019-12-18'
author: Steve Hannah
---

![Header Image](/blog/javascript-media-restrictions/html5-banner.jpg)

Codename One provides `play()` and `pause()` methods as part of the `Media` interface, which you can use to start and stop media clips programmatically. If you are deploying your app to the browser using the Javascript port, then you should be aware of some restrictions imposed by modern web browsers on media playback, and how to work around them.

Modern browsers will only allow audio playback that is triggered by direct user interaction. This basically means that the user needs to press a button to trigger audio playback.

If your app fits into this model (playing sounds only in response to user button presses), then you don’t have to worry about this restriction. But if you want to play sounds in other contexts, you might bump into this restriction. For example, you might want to play a sound in a **chat** app, in response to someone else’s message being posted (i.e. in response to a server event). Or you might want to play a sound in response to voice input, or location changes, or when a timer goes off.

So what happens if you call `mySoundClip.play()`, and the browser disallows it on the grounds that it wasn’t in direct response to user interaction? Our media API catches this failure, and displays a popup-dialog saying “Media ready” with a “Play” button as shown here:

![MediaPlayNow](/blog/javascript-media-restrictions/MediaPlayNow.png)

When the user presses the “Play” button, then the media will start playing because this time it should “meet” the browser’s user-interaction requirements due to the user’s click on the ‘play’ button.

### Make Sure to Call cleanup()

The “media ready” popup dialog shouldn’t present a major problem for your app’s user experience. However, if you’re playing several audio clips, one after another, it could become annoying for the user to have to keep pressing “Play” for each clip. Luckily, the browser will “remember” if the user has authorized audio playback on a particular media element, so if you play your cards right, you may only need to prompt the user to play the first sound clip, and the subsequent ones will just be allowed by the browser. In the Javascript port, each `Media` object is mapped to its own `HTMLMediaElement` (an `<audio>` or `<video>` DOM element). It uses a pool of media elements to try and reuse the underlying media elements if it can. A `Media` object’s underlying element is only returned to the pool when its `cleanup()` method is called. If you’re playing a series of audio clips in a row, you should make sure to call the `cleanup()` method on each sound clip, before the next one plays, to avoid being forced by the browser to display “Media Ready/Play” dialog.

For example, suppose you’re need to play a series of sound clips, one after another. You might do something like:
    
    
    Media media;
    List<String> clipsUrls = ...;
    
    void play(int index) {
        If (media != null) {
            media.cleanup();
        }
        media = createMedia(clipUrls.get(index), false, () ->{
            callSerially(()->{
                play(index+1);
            });
        });
    }
    
    play(0);

In this example, I’m using a single property, `media`, which we will reuse for each sound clip, and I have a list of URLs for sound clips I want to play sequentially. The `play(index)` method plays the clip at the specified index – but first, it cleans up the previous sound clip. This is extra important in the Javascript port since this will return the sound clip’s `<audio>` element to the pool, so that it (and it’s granted permissions) can be reused for the next sound clip.

We use the completion handler in `createMedia()` to be notified when the sound clip has finished playing, at which point it calls `play(index+1)` – i.e. it plays the next clip. Notice that I place the `play()` call inside `callSerially()` so that it is deferred to the next dispatch. This may not be strictly necessary, but it avoids any problems that might occur trying to call `cleanup()` from inside the media’s completed callback.

Finally we start the chain off by calling `play(0)` to play the first sound clip. If `play(0)` is called in response to a user interaction (e.g. inside a button’s action listener), then the sound clip should play unimpeded. But if it is called outside of this context, then a dialog may be displayed to prompt the user to play the sound clip.

Check out the [AsyncMediaSample](https://github.com/codenameone/CodenameOne/blob/master/Samples/samples/AsyncMediaSample/AsyncMediaSample.java) in the [Samples](https://github.com/codenameone/CodenameOne/tree/master/Samples) project for a complete working example that follows this workflow. That example just runs a continuous cycle of recording audio from the microphone, then playing back what you recorded. If you run this example on an iOS device (which seems to be the most strict about its media playback restrictions), you’ll be prompted to play the media at the beginning – but won’t be asked again for subsequent clips.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}

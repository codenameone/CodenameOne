---
title: Async Play and Pause Support
slug: media-async-play-and-pause-support
url: /blog/media-async-play-and-pause-support/
original_url: https://www.codenameone.com/blog/media-async-play-and-pause-support.html
aliases:
- /blog/media-async-play-and-pause-support.html
date: '2019-12-12'
author: Steve Hannah
---

![Header Image](/blog/media-async-play-and-pause-support/new-features-1.jpg)

We’ve recently released a few updates to our Media APIs in order to support a wider variety of use cases. One of these upgrades is the new `AsyncMedia` interface, which includes async play and pause support.

The `Media` interface has always had `play()` and `pause()` methods, but it didn’t provide an easy way to detect when they had taken effect. On most platforms, this hasn’t been a major issue because the delay between when `play()` is called, and when the media **actually** starts playing is small, or negligible. But our Javascript port presented some new challenges due to strict browser permissions surrounding media playback. In some cases, which I’ll discuss in detail in my next blog post, the user will be prompted to provide permission to play audio, or access the microphone, when `play()` is called. This means that the delay between `play()` and when the media actually starts playing may be significant.

### New AsyncMedia interface

The new `AsyncMedia` interface extends `Media` and adds some new methods. Among these new methods, you’ll find `playAsync()` and `pauseAsync()` which return `PlayRequest` and `PauseRequest` objects respectively.

Since `MediaManager` returns media as `Media` objects, you’ll need to convert them into `AsyncMedia` objects in order to access the new functionality. You can use the new `MediaManager.getAsyncMedia(Media)` method for this purpose. Almost all `Media` objects in Codename One already implement `AsyncMedia`, so the `getAsyncMedia()` will usually just return the same object, but casted to `AsyncMedia`. In the rare case where the `Media` object, doesn’t already implement `AsyncMedia`, `getAsyncMedia()` will return a proxy wrapper around the original media object, that includes support for the new Async APIs.

Let’s look at an example:
    
    
    import static com.codename1.media.MediaManager.*;
    import com.codename1.media.*;
    
    //...
    
    boolean playPending, playing;
    // ...
    
    AsyncMedia media = getAsyncMedia(createMedia(mediaUrl, false));
    playPending = true;
    media.playAsync().ready(m->{
        playPending = false;
        Playing = true;
    }).except(ex->{
        playPending = false;
        Dialog.show("Failed to play", "Failed to play media: "+ex.getMessage(), "OK", null);
    });
    media.addMediaStateChangeListener(evt->{
        Playing = (evt.getNewState() == State.Playing);
    });

This code is a complete example of how you can keep track of the playing state of your media. It keeps track of both whether there is a play request pending, and whether it is currently playing. Technically you don’t need to keep your own variable for keeping track of the the “playing” state, as you can just call `media.isPlaying()`, or `media.getState()` at any time. I use a separate playing variable here just to illustrate how to synchronize your application state with the state of your media.

#### How it works

`playAsync()` returns a `PlayRequest` object, which is a subclass of `AsyncResource<Media>`. Its “ready” callback will be executed once playing has begun. The “except” callback will be called if playback fails due to an error.

You can also use the **mediaStateChangeListeners** of the `AsyncMedia` object to keep track of changes to state. Whenever the media starts playing, or stops playing, it will fire one of these events.

The new `pauseAsync()` method works similarly.

Check out the [AsyncMediaSample](https://github.com/codenameone/CodenameOne/blob/master/Samples/samples/AsyncMediaSample/AsyncMediaSample.java) sample in the [Samples project](https://github.com/codenameone/CodenameOne/tree/master/Samples) for a full working example that makes use of `playAsync()`.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}

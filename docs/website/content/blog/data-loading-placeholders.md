---
title: Using Component Placeholders While Loading Data
slug: data-loading-placeholders
url: /blog/data-loading-placeholders/
original_url: https://www.codenameone.com/blog/data-loading-placeholders.html
aliases:
- /blog/data-loading-placeholders.html
date: '2019-12-05'
author: Steve Hannah
---

![Header Image](/blog/data-loading-placeholders/new-features-2.jpg)

In my last post I introduced the new CN.invokeWithoutBlocking() method as a means of ensuring that your UI construction code doesn’t run into any “blocking” that could negatively affect user experience. In cases where you need to perform a network request to help build your UI, I offered some recommendations for moving blocking code out of the critical paths. One recommended pattern was to insert placeholder content into your components, which you replace with the actual data once it has been received from the server. That pattern goes something like:
    
    
    Form f = new Form("Hello", BoxLayout.y());
    Label nameLabel = new Label();
    nameLabel.setText("placeholder");
    AsyncResource<MyData> request = fetchDataAsync();
    request.ready(data -> {
        nameLabel.setText(data.getName());
        f.revalidateWithAnimationSafety();
    });
    f.add(nameLabel);
    f.show();

The concept here is that, the `fetchDataAsync()` method is performing an asynchronous network request in the background but returns an AsyncResource object immediately which will be notified when the response if received. Therefore, the contents of the `ready( data → {…​})` block are executed some time after the form has already been built and shown.

This solved a significant user experience problem, in that the form can be shown to the user immediately without waiting for the network request to complete. However, it introduced a new problem, which is that our `nameLabel` is displaying placeholder text for some period of time before it is replaced with the actual data.

To help solve this problem, I have added some new progress animations that are specifically designed to be used as placeholders while a component’s data is being loaded. These animations include methods for easily swapping themselves with other components while they are loading, and then seamlessly swapping back once the component is finished loading. Currently there are two ProgressAnimation classes:

  1. **CircleProgress** – A circle that progressively draws itself then erases itself. This is a variation on the classic infinite progress animation, but it is more flexible than the built-in `InfiniteProgress` class.

  2. **LoadingTextAnimation** – This animation appears to be a paragraph of text that is being typed progressively one word at a time – except instead of words, it just renders filled rectangles. This is useful for indicating that a block of text is loading.

### Example using CircleProgress
    
    
    Form f = new Form("Hello", new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE));
    Form prev = CN.getCurrentForm();
    Toolbar tb = new Toolbar();
    f.setToolbar(tb);
    tb.addCommandToLeftBar("Back", null, evt->{
        prev.showBack();
    });
    Label nameLabel = new Label();
    $(nameLabel).setAlignment(CENTER);
    nameLabel.setText("placeholder");
    f.add(BorderLayout.CENTER, nameLabel);
    // Replace the label by a CircleProgress to indicate that it is loading.
    CircleProgress.markComponentLoading(nameLabel)
            .getStyle().setFgColor(0xff0000);
    
    AsyncResource<MyData> request = fetchDataAsync();
    request.ready(data -> {
        nameLabel.setText(data.getName());
    
        // Replace the progress with the nameLabel now that
        // it is ready, using a fade transition
        CircleProgress.markComponentReady(nameLabel, CommonTransitions.createFade(300));
    });
    
    f.show();

And the result looks like:

  
Your browser does not support the video tag.  

### Example Using LoadingTextAnimation
    
    
    Form f = new Form("Hello", new BorderLayout(BorderLayout.CENTER_BEHAVIOR_SCALE));
    Form prev = CN.getCurrentForm();
    Toolbar tb = new Toolbar();
    f.setToolbar(tb);
    tb.addCommandToLeftBar("Back", null, evt->{
        prev.showBack();
    });
    SpanLabel profileText = new SpanLabel();
    
    profileText.setText("placeholder");
    f.add(BorderLayout.CENTER, profileText);
    // Replace the label by a CircleProgress to indicate that it is loading.
    LoadingTextAnimation.markComponentLoading(profileText);
    
    AsyncResource<MyData> request = fetchDataAsync();
    request.ready(data -> {
        profileText.setText(data.getProfileText());
    
        // Replace the progress with the nameLabel now that
        // it is ready, using a fade transition
        LoadingTextAnimation.markComponentReady(profileText, CommonTransitions.createFade(300));
    });
    
    f.show();

And the result looks like:

  
Your browser does not support the video tag.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — February 8, 2020 at 7:21 am ([permalink](https://www.codenameone.com/blog/data-loading-placeholders.html#comment-21378))

> [Francesco Galgani](https://lh6.googleusercontent.com/-4K0ax_DVJf4/AAAAAAAAAAI/AAAAAAAAAAA/AMZuuckEd1kcni0y8k6NMzNtxwOCEPatQQ/photo.jpg) says:
>
> Thank you Steve!  
> What is the utility of `CommonProgressAnimations.EmptyAnimation`?
>



### **Steve Hannah** — February 10, 2020 at 12:49 pm ([permalink](https://www.codenameone.com/blog/data-loading-placeholders.html#comment-21382))

> [Steve Hannah](https://lh3.googleusercontent.com/a-/AAuE7mBmUCgKSZtJ2cqeHgj6bdPY2AAQ10roHlMpgRWc) says:
>
> Sometimes you may just want to hide a component until its data is properly loaded, but you don’t want to show any visual animation. That’s when EmptyAnimation is useful.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}

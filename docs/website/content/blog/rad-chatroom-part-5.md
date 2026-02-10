---
title: RAD Chat Room – Part 5
slug: rad-chatroom-part-5
url: /blog/rad-chatroom-part-5/
original_url: https://www.codenameone.com/blog/rad-chatroom-part-5.html
aliases:
- /blog/rad-chatroom-part-5.html
date: '2020-05-28'
author: Steve Hannah
---

![Header Image](/blog/rad-chatroom-part-5/chat-ui-kit-feature.jpg)

This is part 5 of the RAD Chatroom tutorial. You can find part 1 [here](/blog/rad-chatroom-part-1.html), part 2 [here](/blog/rad-chatroom-part-2.html), part 3 [here](/blog/rad-chatroom-part-3.html) and part 4 [here](/blog/rad-chatroom-part-4.html).

### Adding A Photo Capture Feature

Most messaging applications include the ability to add photos to messages. Let’s add this feature to our chat app now.

First we’ll define a new action called “capturePhoto”, and add to the the `TEXT_ACTIONS` category of our view node.
    
    
    public static final ActionNode capturePhoto = action(
            icon(FontImage.MATERIAL_CAMERA)
    );
    
    ...
    
    ViewNode viewNode = new ViewNode(
        actions(ChatRoomView.SEND_ACTION, send),
        actions(ProfileAvatarView.PROFILE_AVATAR_CLICKED_MENU, phone, videoConference),
        actions(ChatBubbleView.CHAT_BUBBLE_LONG_PRESS_MENU, likeAction),
        actions(ChatBubbleView.CHAT_BUBBLE_BADGES, likedBadge),
        actions(ChatRoomView.TEXT_ACTIONS, capturePhoto) __**(1)**
    );

__**1** | Added capturePhoto action to the TEXT_ACTIONS category so that it will appear as a button beside the text field.  
---|---  
  
And we’ll also add a handler for this action, which will capture a photo, and emed the photo in a message that we will add to the chat room’s view model.
    
    
    addActionListener(capturePhoto, evt->{
        evt.consume();
        String photoPath = Capture.capturePhoto();
        if (photoPath == null) {
            // User canceled the photo capture
            return;
        }
    
        File photos = new File("photos"); __**(1)**
        photos.mkdirs();
        Entity entity = evt.getEntity();
        File photo = new File(photos, System.currentTimeMillis()+".png");
        try (InputStream input = FileSystemStorage.getInstance().openInputStream(photoPath);
                OutputStream output = FileSystemStorage.getInstance().openOutputStream(photo.getAbsolutePath())) {
            Util.copy(input, output);
    
            ChatBubbleView.ViewModel message = new ChatBubbleView.ViewModel();
            message.attachmentImageUrl(photo.getAbsolutePath()); __**(2)**
            message.isOwn(true);
            message.date(new Date());
            EntityList messages = entity.getEntityList(ChatRoom.messages); __**(3)**
            if (messages == null) {
                throw new IllegalStateException("This chat room has no messages list set up");
            }
            messages.add(message); __**(4)**
    
        } catch (IOException ex) {
            Log.e(ex);
            ToastBar.showErrorMessage(ex.getMessage());
        }
    });

__**1** | We will create a directory named “photos” where we store all of the photos for the app.  
---|---  
__**2** | Set the path of this photo under attachmentImageUrl. The ChatBubbleView will accept http, https, and file URLs, as well as storage keys. It will render them correctly in the view according to the type of URL it is.  
__**3** | The “entity” of this event is the view model for the ChatRoomView. Here we use the ChatRoom.messages tag to access the messages list in a loosely coupled way. This code will work even if we change the class that we use for the ChatRoomView’s view model.  
__**4** | Adding the message to the messages entity list will trigger a list change event and it will be rendered automatically in the chat room.  
  
Now, let’s fire the chat up again and take it for a test drive.

![The capturePhoto action is rendered as a button beside the input text field](/blog/rad-chatroom-part-5/rad-chat-room-23.png)

Figure 1. The capturePhoto action is rendered as a button beside the input text field

You should now be able to click on the “capture photo” button to capture an image. In the simulator, it will open a file dialog to select an image. On device, it will activate the devices camera so that you can take a photo. After capturing an image, it should be added to the chat inside a message bubble as shown below:

![Photo appears in chat after capture](/blog/rad-chatroom-part-5/rad-chat-room-24.png)

Figure 2. Photo appears in chat after capture

### Linking to a Back-end Chat Server

In this tutorial we created a mock chat application in order to demostrate the ChatRoomView, which is a user interface component. It did not include any integration with a server so it doesn’t allow you to actually chat with other people. Linking to a server is not difficult, and the MVC architecture of this example should make it very clear how the integration should occur. I’ll leave this integration as an exercise for the reader. As a starting point, I recommend checking out the [cn1-websockets](https://github.com/shannah/cn1-websockets) library, and its chat demo.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Gareth Murfin** — May 29, 2020 at 10:28 am ([permalink](https://www.codenameone.com/blog/rad-chatroom-part-5.html#comment-21408))

> Super cool!!.. is there a github link for the finished product so we can check it out? I think you should cover the server side too to be honest, what would it link to, firebase?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frad-chatroom-part-5.html)


### **Steve Hannah** — May 29, 2020 at 3:09 pm ([permalink](https://www.codenameone.com/blog/rad-chatroom-part-5.html#comment-21411))

> I’ll likely add some sample servers at some point, but that isn’t really the focus here. Most people will either want to roll their own server anyways. There are so many different server-side technologies out there, and most devs are already married to one or another, so a sample server would basically just be to make the demo more interactive. 
>
> The most likely “server-side” sample would be to plug this into the existing WebSockets chat demo.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frad-chatroom-part-5.html)


### **Lukman Jaji** — May 29, 2020 at 7:29 pm ([permalink](https://www.codenameone.com/blog/rad-chatroom-part-5.html#comment-21410))

> [Lukman Jaji](https://lh3.googleusercontent.com/a-/AOh14Gjee6sridKOKBngKXUpIHSBJjBaoJbuEK3h_iRT) says:
>
> Hi Steve. This is really cool. However, I installed the ap for this. When I snapped a photo, there is some delay before the photo is added to the chat. I am guessing it’s some resizing operations going on? No?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frad-chatroom-part-5.html)


### **Steve Hannah** — May 29, 2020 at 9:41 pm ([permalink](https://www.codenameone.com/blog/rad-chatroom-part-5.html#comment-21409))

> [Steve Hannah](https://lh3.googleusercontent.com/a-/AAuE7mBmUCgKSZtJ2cqeHgj6bdPY2AAQ10roHlMpgRWc) says:
>
> Thanks for reminding me. This has been reported by some other users as well. Yes, it is likely related to hi-res cameras and taking some time to resize the photos. This can be resolved by providing size parameters to the capturePhoto() method. I’ll be updating the sample to do this soon.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frad-chatroom-part-5.html)


### **Gareth Murfin** — July 14, 2020 at 10:52 pm ([permalink](https://www.codenameone.com/blog/rad-chatroom-part-5.html#comment-24296))

> [Gareth Murfin](https://lh3.googleusercontent.com/a-/AOh14GiKSl5jm7N1Rsw8eobcYTOzEcg7dMk62FKKC_SboA) says:
>
> I’d like to see a way that we can link this chat system to firebase. Is there any starting point for this? examples etc in cn1.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frad-chatroom-part-5.html)


### **Shai Almog** — July 15, 2020 at 2:33 am ([permalink](https://www.codenameone.com/blog/rad-chatroom-part-5.html#comment-24295))

> Shai Almog says:
>
> We don’t have builtin support for firebase features other than push at this time.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frad-chatroom-part-5.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}

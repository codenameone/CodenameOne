---
title: CREATE A LIST OF ITEMS
slug: how-do-i-create-a-list-of-items-the-easy-way
url: /how-do-i/how-do-i-create-a-list-of-items-the-easy-way/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-create-a-list-of-items-the-easy-way.html
tags:
- basic
- ui
description: Infinite lists of items are powerful tools
youtube_id: 0m7Bay4g93k
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-11-1.jpg
---

{{< youtube "0m7Bay4g93k" >}} 

#### Transcript

In this short video I’ll review the basics of creating a list of items in Codename One. But there is a catch, Codename One has a List class and we won’t be using it. The list class is designed for a very elaborate type of list that isn’t as useful for common day devices. Unless you have a very extreme and specific use case you probably shouldn’t use it as it is slower & harder to use than the alternative!

Instead we’ll use the box layout on the Y axis and add components to it. I’ll focus on multi-button as this is a very common component for this type of use but you can add any custom component you want.

In larger sets we can use infinite container or infinite scroll adapter. This allows us to fetch elements dynamically and avoid the overhead of creating hundreds or thousands of components at once.

Lets start with a hello world for a box layout list. This is pretty standard Codename One code, lets go over the different pieces.

First we create the list container, notice we set it to scroll on the Y axis. This allows us to scroll through the list which is crucial. Notice that by default a Form is already scrollable on the Y axis but I’ve set the layout to border layout which implicitly disables scrolling. It’s important that scrolling shouldn’t “nest” as it’s impossible to pick the right scrollbar with a touch interface.

In this case I just added a thousand entries to the list one by one. The list is relatively simple with no actual functionality other than counting the entries.

Notice I place the list in the CENTER of the border layout. This is crucial as center in the border layout stretches the container to fit the screen exactly.  
This is the resulting list that can be scrolled up and down by swiping.

That’s all good and well but in the real world we need lists to be more dynamic than that. We need to fetch data in batches. That’s why we have the infinite container class which allows us to fetch components as the user scrolls through the list. This is the exact same code from before but it creates the list entries in batches instead of as a single block.  
The `fetchComponents` method is invoked with an index in the list and the amount of elements to return. It then creates and returns those elements. This implicitly adds the elements as we scroll and implements the pull to refresh functionality.

This might be obvious but just in case you don’t know you can set an icon for every entry and just add an action listener to get events for a specific entry in the list. This is very convenient.  
This is relatively simple in terms of design, you can check out some of the more elaborate design of a list we have in the kitchen sink demo.

Up until now we did simple demos, this is a screenshot of a contacts list from my device using this sort of API and it was generated with this code. Notice I blurred a few entries since these are my actual phone contacts and I’d like to keep their privacy… This is done with the code here, let’s go over it.

First we need a placeholder image for the common case where a contact doesn’t have a profile picture or when we are still loading the profile picture.

When we are in the first element which will happen when the form loads or after a “pull to refresh” I load the contacts. Notice I could have used if contacts equals null but that would have done nothing in the case of pull to refresh as contacts would have been initialized already. By checking against zero I implicitly support the pull to refresh behavior which just calls the fetch method over again.  
The contacts API can be a bit slow sometimes which is why you shouldn’t fetch “everything” with one request. That’s why the method accepts all of these boolean values to indicate what we need from the contact itself. Setting all of these to true will slow you down significantly so you should generally load just what you need which in this case is contacts with a phone number and full name.

The infinite container has no idea how many elements we might have. So we need to check if the amount of elements requested exceeds the total and if the index is out of bounds. If the former is true we need to reduce the amount and return a smaller array. If the latter is true we need to return null which will stop future calls to fetch components unless pull to refresh is triggered again.

The rest is pretty close to the code we had before where we loop and create multi buttons but in this case we just fill them up with the content details and the placeholder image.

However, you might recall we didn’t fetch the image for the contact and that might be pretty expensive to load… So the trick is to call this method on a button by button case where we fetch ONLY the image but we don’t just invoke that as it would kill performance.  
For this we use the new `callSeriallyOnIdle()` method. This method works like `callSerially` by performing the code in the next event dispatch thread cycle. However, in this case the code will only occur when the phone is idle and there are no other urgent events.

So if we are in idle state we can just ask for the contacts image using the specific API then fill up the UI. Since this is an infinite list this will only be invoked for the “amount” number of entries per cycle and that means it should be reasonably efficient.

Moving to the next page we can see that not much is left, we just return the array and add the list to the center.  
I didn’t spend much time on refinement and some of the nicer effects you can achieve but you can check out the kitchen sink demo where the contacts section features a swipe UI with many special effects such as generated icons per letter.

Adding search to the infinite list is pretty easy.  
We need a search string variable that is both modifiable and accessible in the inner class so I added a member to the parent class.

The rest of the code is identical, I just filter the basic contacts entry. A change in search will refresh the list and invoke fetch components for index zero. If in this case I have a search filter I can loop over the contacts and filter them into a new array list. I can then create a new smaller contacts array that matches the search.

The search string can be bound to the variable using the toolbar search API that’s builtin. The call to refresh has a similar effect to pull to refresh. It allows me to filter the list dynamically and efficiently.

Thanks for watching, I hope you found this helpful.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}

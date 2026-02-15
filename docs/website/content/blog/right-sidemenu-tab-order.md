---
title: Right SideMenu and Tab Order
slug: right-sidemenu-tab-order
url: /blog/right-sidemenu-tab-order/
original_url: https://www.codenameone.com/blog/right-sidemenu-tab-order.html
aliases:
- /blog/right-sidemenu-tab-order.html
date: '2018-06-05'
author: Shai Almog
---

![Header Image](/blog/right-sidemenu-tab-order/new-features-4.jpg)

As I mentioned the other day, we have a lot of new features and announcements. Today I’d like to discuss the upcoming right side menu bar and new tab order functionality.

### Right SideMenu

The old `SideMenuBar` supported the placement of the menu anywhere we want. We could place it in the left/right/top or bottom. The problem was that this support was painfully unreliable and inconsistent.

When we migrated to the `Toolbar` API we ignored that support and only included the left side menu support. For some of us this isn’t a big deal. Just a small limitation. However, if your app is localized to an RTL language this might be a problem.

What’s RTL

RTL stands for Right to Left to indicate languages written in the opposite direction to most western languages. Typically these are old languages specifically:

  * Arabic

  * Aramaic

  * Azeri

  * Dhivehi/Maldivian

  * Hebrew

  * Kurdish (Sorani)

  * Persian/Farsi

  * Urdu

Historically, these languages date to a time when writing was engraved with a hammer and a chisel. It was easier to hold the hammer in the right hand (the more dominant hand for 90% of the population) and thus write from the right side.

As ink and paper took over languages that didn’t have a writing system before picked a direction that wouldn’t smear the ink.

The problem of RTL languages is far more difficult as numbers or other languages are still written from left to right. This is called: bidi (bi-directional). That means a sentence starts from the right, skips to the left to show a number then goes back.

Because RTL languages are read from the right side users expect the entire UI to be reversed. That means all UI elements should be aligned to the right and all components should be in the exact flipped order. This is done automatically in Codename One see the [i18n tutorial](/how-do-i---localizetranslate-my-application-apply-i18nl10n-internationalizationlocalization-to-my-app/).

Up until now we couldn’t do this for the side menu but now that [Francesco Galgani](https://github.com/jsfan3) introduced support for [right side menu in his PR](https://github.com/codenameone/CodenameOne/pull/2437) and [followup PR](https://github.com/codenameone/CodenameOne/pull/2439) this is finally possible!

With next weeks update you could invoke a version of the add to side menu method that lets you add an element to the right side.

### Input Cycle

Codename One has its roots well before the move to touch devices. In the old devices one would navigate with 4 direction buttons and our entire focus system revolves around that. This is a powerful system as it lets us work with unique input devices such as TV’s etc.

However, it has some limitations specifically with the virtual keyboard cycle. When we edit a field and press NEXT we would expect the keyboard to move to the next text field. That’s easy. But what if the next field is a `Picker`?

Now you can override this behavior so the next element will work correctly, we were also able to simplify this similarly to the way that `tabIndex` works in HTML as this system doesn’t need the full functionality of the original focus code.

#### Tab Index

Component now has a `preferredTabIndex` property which specifies the tab order of a component. There are 3 types of values that you can set here.

  * A value of -1 means that the component is not tab-focusable (i.e. you will never get to this field by tabbing or hitting Next/Prev in the keyboard). This is the default value for all components except `TextArea`, and `Picker` – which are currently the only components that are tabbable.

  * A value of 0 means that the component is tab-focusable, and the order will be dictated by the logical document order. (The logical document order is defined in the layout managers).

  * A value greater than 0 explicitly sets the tab order. Be careful with this as if you mix explicitly set tab orders (i.e. `tabIndex > 0`) with implicitly set tab orders (i.e. `tabIndex == 0`) in the same form, you may get unexpected results.

If you want to query the form for tab order, you can use any of the new methods on `Form`.

#### Finding Next/Prev Components.

  * `Form.getNextComponent(Component current)`, which gets the next component by tab order given some currently focused component.

  * `Form.getPreviousComponent(Component current)`, which gets the previous component.

  * `Form.getTabIterator(Component current)`, which gets an iterator that allows you to iterate through all of the tabbable components in the form in their tab order, starting at some currently focused component.

#### Making a Component Tab-Focusable

If you are designing a new component that should be tab-focusable, you should just set the `preferredTabIndex` to 0.

`cmp.setPreferredTabIndex(0);`

If you want to make the component so it won’t be tab-focusable, just do

`cmp.setPreferredTabIndex(-1);`

There is also a convenience method `setTraversable(boolean)` which wraps these calls to make them more intuitive. E.g.:

`cmp.setTraversable(true)` is the same as calling `cmp.setPreferredTabIndex(0)`, and `cmp.setTraversable(false)` is the same as calling `cmp.setPreferredTabIndex(-1)`.  
Default Tab Orders

It is recommended that you just stick with `preferredTabIndex=0` if you want your component to be tabbable, because it’s a lot of work to explicitly declare tab order for all of your fields. Then the tab order will be automatically calculated based on the logical order of the fields on the form. This default ordering is dictated by the layout manager. For indexed layout managers (e.g. `BoxLayout.Y`, `BoxLayout.X`, `GridLayout`, `FlowLayout`), the tab order is simply the component order in the container. Constraint-based layout managers like `BorderLayout` and `TableLayout` define their own orders that make logical sense. `LayeredLayout` defines an order based on the x,y coordinates of its children, roughly moving top-left to bottom-right.

If you are developing your own layout manager and want a default tab order that is different than just the component order within the container, you should override the following methods:

`overridesTabIndices(Container)` – Return true to indicate that the layout manager provides its own traversal order.

`getChildrenInTraversalOrder(Container parent)` – return array of Components in the parent in the order that they should be traversed. This returned array doesn’t need to include all of the child components, only the ones that should be considered in traversal – but it may also include components that shouldn’t be included in traversal, as the ineligible components will be filtered out at a later step. In fact some of the elements of this array may be null, and those will be filtered out.

See [BorderLayout](https://github.com/codenameone/CodenameOne/blob/master/CodenameOne/src/com/codename1/ui/layouts/BorderLayout.java#L885-L893) for an example or [TableLayout](https://github.com/codenameone/CodenameOne/blob/master/CodenameOne/src/com/codename1/ui/table/TableLayout.java#L1096-L1111) for another example.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}

/* 
    Document   : package
    Created on : Oct 11, 2007, 10:38:26 AM
    Author     : Shai Almog
*/

/// Lists are highly customizable and serve as the basis for `com.codename1.ui.ComboBox` and
///     other components (such as carousels etc) they employ a similar MVC approach to
///     Swing including the renderer pattern, **notice** that [we strongly discourage usage of lists](https://www.codenameone.com/blog/avoiding-lists.html)...
///  `com.codename1.ui.list.ListCellRenderer`
///     allows us to customize the appearance of a list entry, it works as a
///     "rubber stamp" by drawing the rendered component and discarding its state thus
///     allowing very large lists with very little component state overhead.
///
///     `com.codename1.ui.list.ListModel` allows us to represent the underlying
///     data structure for the `com.codename1.ui.List`/`com.codename1.ui.ComboBox`
///     without requiring all the data to reside in memory or in a specific structure.
///     This allows a model to represent a data source of any type, coupled with the renderer the
///     data source can be returned in an internal representation state and still be rendered
///     properly to the screen.
package com.codename1.ui.list;

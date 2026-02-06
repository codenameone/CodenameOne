/// Layout managers allow a `com.codename1.ui.Container` to
/// arrange its components by a set of rules that adapt to specific
/// densities (ppi - pixels per inch). A layout manager is an arranging algorithm encapsulated
/// by an abstract class implementation that places components absolutely based
/// on the "hints" received.
///
/// Layout Managers that ship with Codename One
///
/// `com.codename1.ui.layouts.FlowLayout` is default layout manager, simple, flexible and with a few caveats.
///
/// `com.codename1.ui.layouts.BorderLayout` is ubiquitous thru Codename One code.
///
/// `com.codename1.ui.layouts.BorderLayout` can also behave differently based on the center behavior flag
///
/// `com.codename1.ui.layouts.BoxLayout` Y axis is a the work-horse of component lists
///
/// `com.codename1.ui.layouts.BoxLayout` X axis is a simpler replacement to flow layout and has grow/no grow
/// variants.
///
/// `com.codename1.ui.layouts.GridLayout` arranges elements in a grid where all elements have an equal size. It
/// can auto adapt
/// the column count.
///
/// `com.codename1.ui.table.TableLayout` is similar in many regards to HTML tables. Notice that its
/// in the `com.codename1.ui.table` package and not in this package.
///
/// `com.codename1.ui.layouts.LayeredLayout` is unique in the sense that it is meant to costruct layers
/// and not the UI positions. It only lays out on the Z axis.
///
/// `com.codename1.ui.layouts.GridBagLayout` was added to Codename One to ease the porting of Swing/AWT
/// applications.
///
/// `com.codename1.ui.layouts.mig.MigLayout` is a popular 3rd party layout manager its Codename One
/// port is experimental.
package com.codename1.ui.layouts;

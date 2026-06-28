package com.codename1.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marks a class whose instances ParparVM may allocate on the C call stack
/// instead of the garbage-collected heap. Each {@code new} of such a class
/// becomes a method-scoped C struct: no {@code malloc}, no heap registration,
/// no GC mark/sweep work -- the object simply dies with the frame.
///
/// This is an unchecked developer assertion, intended for internal short-lived
/// value/temporary types where escape is known by construction: the marked
/// class's instances MUST NOT escape the frame that creates them -- they may not
/// be stored to another object's field, into an array, or to a static; may not
/// be returned; and may not be passed to anything that retains a reference past
/// the creating call. Violating this yields a dangling pointer once the frame
/// unwinds. Because a single per-site struct is reused across loop iterations,
/// only one instance per allocation site may be live at a time (the natural
/// case for a temporary created, used, and discarded within the loop body).
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface StackAllocate {
}

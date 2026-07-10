package com.codename1.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marks a class whose constructor-created primitive-array fields are fully
/// ENCAPSULATED, letting ParparVM allocate each instance TOGETHER with those
/// arrays as a single heap block ("fused object"): one allocation instead of
/// several, one object for the garbage collector instead of several, and the
/// arrays share the owner's cache lines. A fused array keeps a completely
/// ordinary header -- all reading code, `System.arraycopy`, iteration and
/// bounds checks work unchanged -- but it has no independent GC identity: it
/// is never separately tracked, marked for reclamation, or swept; it simply
/// lives and dies with its owner.
///
/// The translator fuses a constructor-assigned field when the constructor
/// contains an unconditional `this.f = new T[n]` where `T` is a primitive type
/// and `n` is a constructor parameter or constant (bounds-check guards that
/// throw are fine before it). Constructors keep chaining normally, and every
/// other instantiation path -- reflection, deserialization, oversized arrays
/// that do not fit a fused block -- transparently falls back to ordinary
/// separate allocations with identical semantics.
///
/// THE CONTRACT: the marked class must never let a fused array reference
/// escape into another object's field, an array element, or a static that can
/// outlive the instance. Holding it in locals, passing it as a call argument,
/// or copying its contents out is always safe (the VM keeps the whole block
/// alive through any reference into it held on a thread stack). Typical
/// candidates: string/buffer classes owning their `char[]`, image types owning
/// their pixel `int[]`/`byte[]` data.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Fused {
}

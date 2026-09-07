/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *  
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Codename One through http://www.codenameone.com/ if you 
 * need additional information or have any questions.
 */

package java.lang.ref;
/**
 * Abstract base class for reference objects. This class defines the operations common to all reference objects. Because reference objects are implemented in close cooperation with the garbage collector, this class may not be subclassed directly.
 * Since: JDK1.2, CLDC 1.1
 *
 * <p><b>The four fields below are a contract with the collector, not ordinary
 * state.</b> Three separate places know their names literally, and renaming one
 * without the others produces a build that compiles and silently stops
 * collecting -- or, worse, one that clears a reference whose referent is still
 * in use:</p>
 *
 * <ul>
 * <li>{@code ByteCodeClass} suppresses the usual {@code gcMarkObject} for
 *     {@code objReference} in {@code __GC_MARK_java_lang_ref_Reference} and
 *     emits a {@code cn1GcDiscoverReference} call in its place, handing the
 *     collector the addresses of these fields. That suppression is what makes
 *     the referent a weak edge instead of a strong one.</li>
 * <li>The same class adds the SATB load barrier and the touch stamp to
 *     {@code get_field_java_lang_ref_Reference_objReference}, which is the
 *     accessor every {@code get()} below compiles into.</li>
 * <li>{@code cn1GcProcessReferences} in {@code cn1_globals.m} reads and writes
 *     all four through those addresses.</li>
 * </ul>
 *
 * <p>Consequently {@code objReference} must stay the ONLY object-typed field in
 * this class: the translator's opt-out is keyed on the class and field name, and
 * a second reference field would be traced strongly with nothing to say so.</p>
 */
public abstract class Reference{
    /**
     * The referent. Deliberately package private and declared HERE rather than in
     * WeakReference, so that one translator opt-out and one collector pass cover
     * every subclass.
     */
    Object objReference;

    /**
     * Cycles since the last {@link #get()}, maintained by the collector, and the
     * "hot" input to the soft-reference retention policy.
     *
     * <p>{@code TOUCHED} (-1) is written by the field accessor on every read --
     * a store of an immediate, which is the whole per-get cost of ranking, and
     * why the ranking is done this way rather than by reading a clock or an
     * epoch counter. The collector converts a -1 back to 0 and increments
     * everything else once per cycle, so the value is an age in collections.</p>
     *
     * <p>It also carries the safety property that lets the collector clear a
     * reference at all while mutators run: see the discussion of
     * {@code cn1GcProcessReferences}.</p>
     */
    int cn1TouchAge = TOUCHED;

    /**
     * {@link #STRENGTH_WEAK} or {@link #STRENGTH_SOFT}, set by the subclass
     * constructor.
     *
     * <p>The collector needs to tell the two apart and deliberately does NOT do
     * it by comparing class pointers: that would make the runtime depend on a
     * generated class symbol that the dead-code pass is entitled to remove, and
     * would answer wrongly for a user-written subclass of either.</p>
     */
    int cn1Strength;

    /**
     * The mark value of the cycle that last aged this reference, so that a
     * reference reached more than once in a cycle ages exactly once.
     *
     * <p>Being reached twice is normal rather than exceptional: force-marking
     * re-runs mark functions over already-marked objects once per statics pass
     * and again for the constant pool, so the collector sees popular references
     * several times per cycle.</p>
     */
    int cn1AgedCycle;

    /** {@link #cn1TouchAge} value meaning "read since the collector last aged this". */
    static final int TOUCHED = -1;

    /** A {@link #cn1Strength} that is never retained once the referent is unreachable. */
    static final int STRENGTH_WEAK = 0;

    /** A {@link #cn1Strength} that is retained while recently used and memory allows. */
    static final int STRENGTH_SOFT = 1;

    Reference(Object ref, int strength) {
        this.objReference = ref;
        this.cn1Strength = strength;
    }

    /**
     * Clears this reference object.
     */
    public void clear(){
        objReference = null;
    }

    /**
     * Returns this reference object's referent. If this reference object has been cleared, either by the program or by the garbage collector, then this method returns null.
     *
     * <p>This compiles to {@code get_field_java_lang_ref_Reference_objReference},
     * which the translator gives a SATB load barrier and the touch stamp. Both
     * belong on the ACCESSOR rather than here: every read of the field goes
     * through it, including any the optimizer generates, whereas a barrier
     * written in Java would cover only the one call site below.</p>
     */
    public java.lang.Object get(){
        return objReference;
    }
}

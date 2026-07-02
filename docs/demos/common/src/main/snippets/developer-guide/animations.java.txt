// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::animations-java-001[]
MorphTransition morph = MorphTransition.create(300)
        .snapshotMode(true)
        .morph("card");
nextForm.setTransitionInAnimator(morph);
nextForm.show();
// end::animations-java-001[]

// tag::animations-java-002[]
/**
 * Useful when passing a value that might not exist to a function, e.g. When we
 * pass a form that we might need to construct dynamically later on.
 */
public interface LazyValue<T> {
    /**
     * Returns the actual value.
     *
     * @param args optional arguments for the creation of the lazy value
     * @return the value
     */
    T get(Object... args);
}
// end::animations-java-002[]

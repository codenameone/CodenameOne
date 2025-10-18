package com.codename1.ui;

/**
 * An Interface that any Component that could be released from the parent
 * Form can implement. E.g. when a button is held down with a finger and dragged
 * outside of the component bounds a release event might be discarded. With
 * this interface we can register our desire for that release event even in that
 * case.
 */

public interface ReleasableComponent {

    /**
     * Returns true if this is an auto-released Component.
     * An Auto-released Component will be disarmed when a drag is happening within it
     */
    boolean isAutoRelease();

    /**
     * Sets the auto released mode of the Component
     */
    void setAutoRelease(boolean autoRelease);

    /**
     * Indicates a radius in which a pointer release will still have effect. Notice that this only applies to
     * pointer release events and not to pointer press events
     *
     * @return the releaseRadius
     */
    int getReleaseRadius();

    /**
     * Indicates a radius in which a pointer release will still have effect. Notice that this only applies to
     * pointer release events and not to pointer press events
     *
     * @param releaseRadius the releaseRadius to set
     */
    void setReleaseRadius(int releaseRadius);

    /**
     * Function that would be called by the parent Form to put the Component in its released state
     */
    void setReleased();
}

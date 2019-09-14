package com.codename1.ui;

/** An Interface that any Component that could be released from the parent Form can implement */

public interface IReleasable {

	/**
	 * Returns true if this is an auto-released Component.
     * An Auto-released Component will be disarmed when a drag is happening within it 
     */
	public boolean isAutoRelease();
	
	 /**
     * Sets the auto released mode of the Component
     */
	public void setAutoRelease(boolean autoRelease);
	
	 /**
     * Indicates a radius in which a pointer release will still have effect. Notice that this only applies to
     * pointer release events and not to pointer press events
     * @return the releaseRadius
     */
    public int getReleaseRadius();

    /**
     * Indicates a radius in which a pointer release will still have effect. Notice that this only applies to
     * pointer release events and not to pointer press events
     * @param releaseRadius the releaseRadius to set
     */
    public void setReleaseRadius(int releaseRadius);
	
    /**
     * Function that would be called by the parent Form to put the Component in its released state 
     */
	public void setReleased();
}

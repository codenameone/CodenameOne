package net.sourceforge.retroweaver.event;

/**
 * A callback interface to indicate weaving status.
 */
public interface WeaveListener {
	void weavingStarted(String msg);

	void weavingPath(String sourcePath);

	void weavingCompleted(String msg);

	void weavingError(String msg);

}

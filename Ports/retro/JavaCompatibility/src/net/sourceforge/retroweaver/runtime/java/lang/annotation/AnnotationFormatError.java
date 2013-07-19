package net.sourceforge.retroweaver.runtime.java.lang.annotation;

/**
 * A mirror of java.lang.annotation.AnnotationFormatError.
 * 
 * @author Toby Reyelts
 */
public class AnnotationFormatError extends Error {

	public AnnotationFormatError(final String message) {
		super(message);
	}

	public AnnotationFormatError(final String message, final Throwable cause) {
		super(message);
	}

	public AnnotationFormatError(final Throwable cause) {
		super(cause.toString());
	}
}

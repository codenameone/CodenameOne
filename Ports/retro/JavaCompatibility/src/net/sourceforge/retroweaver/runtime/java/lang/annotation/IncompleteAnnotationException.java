package net.sourceforge.retroweaver.runtime.java.lang.annotation;

import net.sourceforge.retroweaver.runtime.java.lang.annotation.Annotation;

/**
 * A mirror of java.lang.annotation.IncompleteAnnotationException.
 * 
 * @author Toby Reyelts
 */
public class IncompleteAnnotationException extends RuntimeException {

	private final Class<? extends Annotation> annotationType_;

	private final String elementName_;

	public IncompleteAnnotationException(final Class<? extends Annotation> annotationType, final String elementName) {
		super(elementName + " in " + annotationType);
		this.annotationType_ = annotationType;
		this.elementName_ = elementName;
	}

	public Class<? extends Annotation> annotationType() {
		return annotationType_;
	}

	public String elementName() {
		return elementName_;
	}

}

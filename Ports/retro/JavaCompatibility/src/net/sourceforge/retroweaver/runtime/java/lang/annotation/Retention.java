
package net.sourceforge.retroweaver.runtime.java.lang.annotation;

import net.sourceforge.retroweaver.runtime.java.lang.annotation.Target;
import net.sourceforge.retroweaver.runtime.java.lang.annotation.ElementType;
import net.sourceforge.retroweaver.runtime.java.lang.annotation.Documented;

public @Documented @Retention( RetentionPolicy.RUNTIME) @Target( ElementType.ANNOTATION_TYPE) @interface Retention {
  public RetentionPolicy value();
}

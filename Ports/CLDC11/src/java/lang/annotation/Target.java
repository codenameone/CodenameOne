
package java.lang.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Documented;

public @Documented @Retention( RetentionPolicy.RUNTIME ) @Target( ElementType.ANNOTATION_TYPE ) @interface Target {
  public ElementType[] value();
}


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.annotations;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Helper annotations for asynchronous computation.
 * Used for example in IntelliJ IDEA's debugger for async stacktraces feature.
 *
 * @author egor
 */
public final class Async {

  /**
   * Prohibited default constructor.
   */
  private Async() {
    throw new AssertionError("Async should not be instantiated");
  }

  /**
   * Indicates that the marked method schedules async computation.
   * Scheduled object is either {@code this}, or the annotated parameter value.
   */
  @Retention(RetentionPolicy.CLASS)
  @Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.PARAMETER})
  public @interface Schedule {}

  /**
   * Indicates that the marked method executes async computation.
   * Executed object is either {@code this}, or the annotated parameter value.
   * This object needs to match with the one annotated with {@link Schedule}
   */
  @Retention(RetentionPolicy.CLASS)
  @Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.PARAMETER})
  public @interface Execute {}
}
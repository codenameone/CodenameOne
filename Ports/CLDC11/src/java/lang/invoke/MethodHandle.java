package java.lang.invoke;

/**
 * @deprecated these classes are used internally for Lambda compatibility
 */
public abstract class MethodHandle {
  public MethodHandle() {
  }

  public java.lang.invoke.MethodType type() {
      return null;
  }

  public final java.lang.Object invokeExact(java.lang.Object... a) throws java.lang.Throwable {
      return null;
  }

  public final java.lang.Object invoke(java.lang.Object... a) throws java.lang.Throwable {
      return null;
  }

  public java.lang.Object invokeWithArguments(java.lang.Object... a) throws java.lang.Throwable {
      return null;
  }

  public java.lang.Object invokeWithArguments(java.util.List<?> a) throws java.lang.Throwable {
      return null;
  }

  public java.lang.invoke.MethodHandle asType(java.lang.invoke.MethodType a) {
      return null;
  }

  public java.lang.invoke.MethodHandle asSpreader(java.lang.Class<?> a, int b) {
      return null;
  }

  public java.lang.invoke.MethodHandle asCollector(java.lang.Class<?> a, int b) {
      return null;
  }

  public java.lang.invoke.MethodHandle asVarargsCollector(java.lang.Class<?> a) {
      return null;
  }

  public boolean isVarargsCollector() {
      return false;
  }

  public java.lang.invoke.MethodHandle asFixedArity() {
      return null;
  }
  public java.lang.invoke.MethodHandle bindTo(java.lang.Object a) {
      return null;
  }
}

package java.lang.invoke;

/**
 * @deprecated these classes are used internally for Lambda compatibility
 */
public abstract class CallSite {
  public CallSite() {}
  
  public MethodType type() {
      return null;
  }
  
  public abstract MethodHandle getTarget();
  
  public abstract void setTarget(MethodHandle mh);
  
  public abstract java.lang.invoke.MethodHandle dynamicInvoker();
}

using System;

namespace com.codename1.impl
{
    public class SoftRef
    {
        WeakReference w;

        public SoftRef(object obj)
        {
            w = new WeakReference(obj);
        }

        public object get()
        {
            object o = (object)w.Target;
            return o;
        }
    }
}

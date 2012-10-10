using System;
using System.Net;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Documents;
using System.Windows.Ink;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Shapes;

namespace com.codename1.impl
{
    public class SoftRef : java.lang.Object
    {
        global::System.WeakReference w;

        public SoftRef(java.lang.Object obj)
        {
            w = new WeakReference(obj);
        }

        public java.lang.Object get()
        {
            java.lang.Object o = (java.lang.Object)w.Target;
            return o;
        }
    }
}

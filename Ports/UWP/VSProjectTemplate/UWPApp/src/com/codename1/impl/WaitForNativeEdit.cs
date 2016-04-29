using System;
using System.Threading.Tasks;

namespace com.codename1.impl
{
    class WaitForNativeEdit : java.lang.Object, java.lang.Runnable
    {
        public virtual void run()
        {
            while (SilverlightImplementation.textInputInstance != null)
            {
                Task.Run(() => Task.Delay(TimeSpan.FromMilliseconds(1))).GetAwaiter().GetResult();
            }
        }
    }
}

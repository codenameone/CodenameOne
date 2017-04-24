using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using IKVM.Runtime;

namespace com.codename1.impl
{

    public class NativeThreadHelperImpl : NativeThreadHelper
    {
        public override NativeThread CurrentThread
        {
            get
            {
                return NativeThreadImpl.CurrentThread;
            }
        }

        public override NativeThread NewThread()
        {
            return new NativeThreadImpl();
        }

        public override bool QueueUserWorkItem(NativeWaitCallback callback)
        {
            return NativeThreadPoolImpl.QueueUserWorkItem(callback);
        }

        public override bool QueueUserWorkItem(NativeWaitCallbackWithArg callback, object obj)
        {
            return NativeThreadPoolImpl.QueueUserWorkItem(callback, obj);

        }

        public override void Sleep(int ms)
        {
            Task t = Task.Delay(ms);
            t.Wait();
        }
    }

    public class NativeThreadImpl : IKVM.Runtime.NativeThread

    {

        private Task peerTask;
        private static Dictionary<int, NativeThread> threads = new Dictionary<int, NativeThread>();
        private static NativeThreadImpl mainThread = new NativeThreadImpl();


        private string _name;
        private bool _isBackground;
        private int _priority = NativeThreadPriority.Normal;



        public override void init(NativeThreadStart start)
        {

            peerTask = new Task(delegate ()
            {
                start.Invoke();
            }, new System.Threading.CancellationToken(), TaskCreationOptions.LongRunning);
            lock(threads)
            {
                threads.Add(peerTask.Id, this);
            }

        }

        private static void cleanThreads()
        {
            lock(threads)
            {
                List<int> toRemove = new List<int>();
                foreach (KeyValuePair<int, NativeThread> entry in threads)
                {
                    NativeThreadImpl thread = (NativeThreadImpl)entry.Value;
                    if (thread.peerTask != null)
                    {
                        switch (thread.peerTask.Status)
                        {
                            case TaskStatus.Canceled:
                            case TaskStatus.Faulted:
                            case TaskStatus.RanToCompletion:
                                toRemove.Add(thread.peerTask.Id);
                                break;
                        }
                    }
                }
                foreach (int id in toRemove)
                {
                    threads.Remove(id);
                }
            }
        }

        public override void Start()
        {

            peerTask.Start();

        }

        

        public override bool Join(int millisecondsTimeout)
        {

            return peerTask.Wait(millisecondsTimeout);

        }

        public override void Join()
        {

            peerTask.Wait();

        }

       

        public static NativeThread CurrentThread
        {
            get
            {
                if (Task.CurrentId == null)
                {
                    return mainThread;
                }
                NativeThread nt;
                if (threads.TryGetValue(Task.CurrentId != null ? (int)Task.CurrentId : -1, out nt))
                {
                    return nt;
                }
                return mainThread;

            }
        }

        public override int ThreadState
        {
            get
            {

                switch (peerTask.Status)
                {
                    case TaskStatus.Canceled:
                    case TaskStatus.Faulted:
                    case TaskStatus.RanToCompletion:
                        return NativeThreadState.Stopped;
                    case TaskStatus.Created:
                    case TaskStatus.WaitingForActivation:
                    case TaskStatus.WaitingToRun:
                        return NativeThreadState.Unstarted;
                    case TaskStatus.Running:
                        return NativeThreadState.Running;
                    case TaskStatus.WaitingForChildrenToComplete:
                        return NativeThreadState.WaitSleepJoin;



                }

                throw new NotImplementedException(); ;
            }
        }


    }

    public class NativeThreadPoolImpl
    {
        public static bool QueueUserWorkItem(NativeWaitCallback callback)
        {

            Task.Run(delegate ()
            {
                callback.Invoke();
            });


            return true;
        }

        public static bool QueueUserWorkItem(NativeWaitCallbackWithArg callback, object obj)
        {

            Task.Run(delegate ()
            {
                callback.Invoke(obj);
            }
            );

            return true;
        }
    }
}
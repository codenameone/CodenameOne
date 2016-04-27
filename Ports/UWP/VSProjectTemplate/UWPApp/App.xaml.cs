using IKVM.Attributes;
using IKVM.Internal;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Runtime.InteropServices.WindowsRuntime;
using Windows.ApplicationModel;
using Windows.ApplicationModel.Activation;
using Windows.Foundation;
using Windows.Foundation.Collections;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Controls.Primitives;
using Windows.UI.Xaml.Data;
using Windows.UI.Xaml.Input;
using Windows.UI.Xaml.Media;
using Windows.UI.Xaml.Navigation;

namespace UWPApp
{
    /// <summary>
    /// Provides application-specific behavior to supplement the default Application class.
    /// </summary>
    sealed partial class App : Application
    {
        /// <summary>
        /// Initializes the singleton application object.  This is the first line of authored code
        /// executed, and as such is the logical equivalent of main() or WinMain().
        /// </summary>
        public App()
        {
            //Microsoft.ApplicationInsights.WindowsAppInitializer.InitializeAsync(
            //    Microsoft.ApplicationInsights.WindowsCollectors.Metadata |
            //    Microsoft.ApplicationInsights.WindowsCollectors.Session);
            this.InitializeComponent();
            this.Suspending += OnSuspending;
        }

        /// <summary>
        /// Invoked when the application is launched normally by the end user.  Other entry points
        /// will be used such as when the application is launched to open a specific file.
        /// </summary>
        /// <param name="e">Details about the launch request and process.</param>
        protected override void OnLaunched(LaunchActivatedEventArgs e)
        {
            System.Diagnostics.Debug.WriteLine("in OnLaunched1");
            //IKVMReflectionHelper.Initialize();
#if DEBUG
            if (System.Diagnostics.Debugger.IsAttached)
            {
                this.DebugSettings.EnableFrameRateCounter = true;
            }
#endif

            Frame rootFrame = Window.Current.Content as Frame;

            // Do not repeat app initialization when the Window already has content,
            // just ensure that the window is active
            if (rootFrame == null)
            {
                // Create a Frame to act as the navigation context and navigate to the first page
                rootFrame = new Frame();

                rootFrame.NavigationFailed += OnNavigationFailed;

                if (e.PreviousExecutionState == ApplicationExecutionState.Terminated)
                {
                    //TODO: Load state from previously suspended application
                }

                // Place the frame in the current Window
                Window.Current.Content = rootFrame;
            }

            if (rootFrame.Content == null)
            {
                // When the navigation stack isn't restored navigate to the first page,
                // configuring the new page by passing required information as a navigation
                // parameter
                rootFrame.Navigate(typeof(MainPage), e.Arguments);
            }
            // Ensure the current window is active
            Window.Current.Activate();
        }

        /// <summary>
        /// Invoked when Navigation to a certain page fails
        /// </summary>
        /// <param name="sender">The Frame which failed navigation</param>
        /// <param name="e">Details about the navigation failure</param>
        void OnNavigationFailed(object sender, NavigationFailedEventArgs e)
        {
            throw new Exception("Failed to load Page " + e.SourcePageType.FullName);
        }

        /// <summary>
        /// Invoked when application execution is being suspended.  Application state is saved
        /// without knowing whether the application will be terminated or resumed with the contents
        /// of memory still intact.
        /// </summary>
        /// <param name="sender">The source of the suspend request.</param>
        /// <param name="e">Details about the suspend request.</param>
        private void OnSuspending(object sender, SuspendingEventArgs e)
        {
            var deferral = e.SuspendingOperation.GetDeferral();
            //TODO: Save application state and stop any background activity
            deferral.Complete();
        }
    }

    class IKVMReflectionHelper : RuntimeReflectionHelper
    {
        public static void Initialize()
        {
            Instance = new IKVMReflectionHelper();
            java.lang.System.setOut(new DebugPrintStream());
            java.lang.System.setErr(new DebugPrintStream());
            //java.lang.System.setOut(new java.io.OutputStreamProxy())
        }

        private IKVMReflectionHelper()
        {

            System.Diagnostics.Debug.WriteLine("In IKVMReflectionHelper 1");
            java.lang.System.setOut(new DebugPrintStream());
        }

        public override Module GetTypeModule(Type type)
        {
            System.Diagnostics.Debug.WriteLine("In getTypeModule 1");
            return type.GetTypeInfo().Module;
        }

        public override object[] GetCustomAttributesImpl(Module mod, Type attributeType, bool inherit)
        {
            System.Diagnostics.Debug.WriteLine("In GetCustomAttributesImpl 1");
#if !DEBUG || true
            var name = mod.Name;
            if (mod.Name.Contains("IKVM.OpenJDK") || name.Contains("CodenameOne") || name.Contains("HelloWindows"))
            {
                return new JavaModuleAttribute[] { new JavaModuleAttribute() };
            }
#endif
            var s = mod.GetCustomAttributes(attributeType).ToArray();
            return s;
        }

        public override int SortFieldByTokenImpl(FieldInfo field1, FieldInfo field2)
        {
            return 1;
        }

        public override Module[] GetModules(Assembly assembly)

        {
            System.Diagnostics.Debug.WriteLine("In getModules 1");
#if WINDOWS_UAP
            return assembly.GetModules();
#else
            return assembly.Modules.ToArray();
#endif
        }

        public override RemappedClassAttribute[] GetRemappedClasses(Assembly assambly)
        {
#if !DEBUG || true
            return new[] {
                //new RemappedClassAttribute("java.lang.AutoCloseable", typeof(IDisposable)),
                new RemappedClassAttribute("java.lang.Comparable", typeof(IComparable)),
                new RemappedClassAttribute("java.lang.Object", typeof(object)),
                new RemappedClassAttribute("java.lang.String", typeof(string)),
                new RemappedClassAttribute("java.lang.Throwable", typeof(Exception))
            };
#else
            var s = assambly.GetCustomAttributes<RemappedClassAttribute>().ToArray();
            return s;
#endif
        }

        public override Type GetModuleType(Module mod, string className)
        {
            System.Diagnostics.Debug.WriteLine("In getModuleType 1");
            return mod.Assembly.GetType(className);
        }

        public override Assembly GetCoreAssembly()
        {
            System.Diagnostics.Debug.WriteLine("In getCoreAssemblt 1");
            return typeof(java.lang.Object).GetTypeInfo().Assembly;
        }

        public override void Mark(object obj)

        {
            System.Diagnostics.Debug.WriteLine("In Mark " + obj);
        }

        class DebugPrintStream : java.io.PrintStream
        {
            public DebugPrintStream()
                : base(new DebugOutputStream())
            {
            }

            public override void println(object x)
            {
                System.Diagnostics.Debug.WriteLine(x);

            }

            public override void println(string x)
            {
                System.Diagnostics.Debug.WriteLine(x);
            }

            public override void println()
            {
                System.Diagnostics.Debug.WriteLine("");
            }

        }

        class DebugOutputStream : java.io.OutputStream
        {
            public override void write(int b)
            {
                // ignore
            }


        }


    }


}

/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
// Windows (WinUI 3 / Fluent) native reference app for the Codename One fidelity suite.
//
// Desktop counterpart to ios-native-ref/NativeRef.swift. It renders real WinUI controls in
// a real, composited window and writes reference tiles plus a capture-manifest.json.
//
// The manifest is not bookkeeping here, it is the point. `windows-latest` is Windows
// SERVER, where Mica and Acrylic fall back to a plain solid brush rather than failing, and
// where Segoe UI Variable -- the font every WinUI control uses -- may be absent. Capture a
// reference under those conditions and you get Fluent-with-the-materials-off, commit it as
// "the Windows 11 reference", tune the Codename One theme until it matches the fallback,
// and ship a theme that looks wrong on every actual Windows 11 machine. The fidelity
// metric cannot detect this: two sides that degrade into the same flat render score HIGH.
//
// So this app refuses to produce a reference set it cannot vouch for. In probe mode it
// answers the environment questions and exits; in capture mode it additionally writes
// tiles, but only after the same assertions pass.
using System.Runtime.InteropServices;
using System.Text;
using Microsoft.UI.Composition.SystemBackdrops;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;
using Microsoft.UI;
using Microsoft.UI.Windowing;
using Windows.UI.ViewManagement;

namespace Cn1NativeRef;

public static class Program
{
    [STAThread]
    static void Main(string[] args)
    {
        Microsoft.UI.Xaml.Application.Start(_ => new App());
    }
}

public partial class App : Application
{
    private Window _window;
    private readonly List<string> _blockers = new();
    private string _outDir;
    private bool _occluded;
    private string _stage = "(not started)";
    private string _clientBackground = "(not sampled)";
    private IntPtr _hwnd;
    private bool _isProbe;

    protected override void OnLaunched(LaunchActivatedEventArgs args)
    {
        _outDir = Environment.GetEnvironmentVariable("NATIVEREF_OUT")
                  ?? throw new InvalidOperationException("NATIVEREF_OUT is not set");
        Directory.CreateDirectory(_outDir);
        _isProbe = (Environment.GetEnvironmentVariable("NATIVEREF_MODE") ?? "probe") != "capture";

        _window = new Window { Title = "cn1-native-ref" };

        // Mica is what makes a Fluent surface look like Windows 11 rather than like a flat
        // grey box. Requesting it tells us nothing -- the request succeeds either way -- so
        // the answer comes from IsSupported plus whether the system says transparency
        // effects are even on, and both go in the manifest.
        var backdrop = new MicaBackdrop();
        bool micaSupported = MicaController.IsSupported();
        _window.SystemBackdrop = backdrop;

        // Always-on-top as well as foreground. The foreground check happens before the
        // BitBlt, and without this a dialog appearing in between could still slide over the
        // window in the gap -- which is a race that would show up as an occasional wrong
        // capture rather than a consistent one, and those are far worse to diagnose.
        _hwnd = WinRT.Interop.WindowNative.GetWindowHandle(_window);
        var idEarly = Microsoft.UI.Win32Interop.GetWindowIdFromWindow(_hwnd);
        if (AppWindow.GetFromWindowId(idEarly).Presenter is OverlappedPresenter op)
        {
            op.IsAlwaysOnTop = true;
        }

        var root = new StackPanel { Orientation = Orientation.Vertical, Spacing = 12, Margin = new Thickness(24) };
        var button = new Button { Content = "Default" };
        root.Children.Add(button);
        _window.Content = root;

        root.Loaded += async (_, _) => await OnReadyAsync(root, button, micaSupported);
        _window.Activate();
    }

    private async Task OnReadyAsync(FrameworkElement root, Button button, bool micaSupported)
    {
        var ui = new UISettings();
        bool transparency = ui.AdvancedEffectsEnabled;
        bool animations = ui.AnimationsEnabled;
        var accent = ui.GetColorValue(UIColorType.Accent);
        double rasterScale = root.XamlRoot?.RasterizationScale ?? 0;
        string fontFamily = button.FontFamily?.Source ?? "(none)";
        bool segoeVariable = FontIsInstalled("Segoe UI Variable Text");

        // Each of these produces a reference that is subtly, silently wrong rather than
        // one that fails, which is why they are blockers and not warnings.
        if (!micaSupported)
        {
            _blockers.Add("MicaController.IsSupported() is false: this OS cannot draw the "
                + "Mica backdrop, so every Fluent surface here is a flat fallback brush.");
        }
        if (!transparency)
        {
            _blockers.Add("Transparency effects are OFF (UISettings.AdvancedEffectsEnabled "
                + "is false), which disables Mica and Acrylic regardless of OS support. "
                + "This is the Windows Server default.");
        }
        if (!segoeVariable)
        {
            _blockers.Add("Segoe UI Variable is not installed. Every WinUI control would be "
                + "measured in a substitute face, so the text residual would be font "
                + "availability rather than theme fidelity.");
        }
        if (rasterScale != 1.0 && rasterScale != 0)
        {
            // Not fatal, but it must be recorded and matched on the Codename One side or
            // the absolute-position metric compares tiles of different sizes.
            Console.WriteLine($"NATIVEREF:WARN rasterization scale is {rasterScale}, not 1.0");
        }

        // Waiting for XAML to paint happens FIRST, before anything awaits, because it is the
        // only part of this that touches XAML at all. CompositionTarget.Rendering must be
        // subscribed from the UI thread, and after an await the continuation is not reliably
        // on it -- the previous run proved that precisely: COMException 0x8001010E,
        // RPC_E_WRONGTHREAD, at stage 'await-frames'. Everything after this point is Win32,
        // which does not care which thread calls it.
        _stage = "await-frames";
        var drawn = new TaskCompletionSource<bool>();
        int frames = 0;
        EventHandler<object> onFrame = null;
        onFrame = (_, _) =>
        {
            if (++frames >= 3)
            {
                CompositionTarget.Rendering -= onFrame;
                drawn.TrySetResult(true);
            }
        };
        CompositionTarget.Rendering += onFrame;
        await Task.WhenAny(drawn.Task, Task.Delay(5000));
        Console.WriteLine($"NATIVEREF:INFO composed frames observed: {frames}");
        if (frames == 0)
        {
            _blockers.Add("XAML never presented a frame, so the client area would be "
                + "captured empty while DWM still draws the title bar -- which is what a "
                + "blocked UI thread looks like");
        }

        // Capture a real tile even in probe mode. A green build is not a rendered one:
        // AppxGeneratePriEnabled is off (see NativeRef.csproj), so WinUI's own .pri files
        // are not expanded into the output, and if that mattered the controls would come
        // back unstyled or absent rather than failing loudly. A picture is the only thing
        // that distinguishes "built" from "drew a Fluent button".
        await CaptureWindowAsync();

        WriteManifest(micaSupported, transparency, animations, accent, rasterScale, fontFamily, segoeVariable);

        foreach (var b in _blockers)
        {
            Console.Error.WriteLine($"NATIVEREF:BLOCKER {b}");
        }
        Console.WriteLine($"NATIVEREF:DONE exit={(_blockers.Count > 0 ? 20 : 0)}");
        Console.Out.Flush();
        Environment.Exit(_blockers.Count > 0 ? 20 : 0);
    }

    [DllImport("user32.dll")] private static extern IntPtr GetDC(IntPtr hWnd);
    [DllImport("user32.dll")] private static extern int ReleaseDC(IntPtr hWnd, IntPtr hDC);
    [DllImport("gdi32.dll")] private static extern IntPtr CreateCompatibleDC(IntPtr hdc);
    [DllImport("gdi32.dll")] private static extern IntPtr CreateCompatibleBitmap(IntPtr hdc, int w, int h);
    [DllImport("gdi32.dll")] private static extern IntPtr SelectObject(IntPtr hdc, IntPtr h);
    [DllImport("gdi32.dll")] private static extern bool DeleteObject(IntPtr h);
    [DllImport("gdi32.dll")] private static extern bool DeleteDC(IntPtr hdc);
    [DllImport("gdi32.dll")] private static extern bool BitBlt(IntPtr dst, int x, int y, int w, int h,
        IntPtr src, int sx, int sy, uint rop);
    [DllImport("dwmapi.dll")] private static extern int DwmFlush();
    [DllImport("user32.dll")] private static extern bool SetForegroundWindow(IntPtr hWnd);
    [DllImport("user32.dll")] private static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")] private static extern bool BringWindowToTop(IntPtr hWnd);
    [DllImport("user32.dll")] private static extern uint GetWindowThreadProcessId(IntPtr hWnd, IntPtr pid);
    [DllImport("user32.dll")] private static extern bool AttachThreadInput(uint attach, uint attachTo, bool fAttach);
    [DllImport("kernel32.dll")] private static extern uint GetCurrentThreadId();
    [DllImport("user32.dll", SetLastError = true)]
    private static extern bool SystemParametersInfo(uint action, uint param, IntPtr pv, uint winIni);
    [DllImport("user32.dll")] private static extern bool PostMessage(IntPtr hWnd, uint msg, IntPtr w, IntPtr l);
    [DllImport("user32.dll")] private static extern bool IsWindow(IntPtr hWnd);

    private const uint SPI_SETFOREGROUNDLOCKTIMEOUT = 0x2001;
    private const uint SPIF_SENDCHANGE = 0x02;
    private const uint WM_CLOSE = 0x0010;
    [DllImport("user32.dll")] private static extern IntPtr WindowFromPoint(POINT p);
    [DllImport("user32.dll")] private static extern IntPtr GetAncestor(IntPtr hWnd, uint flags);

    [StructLayout(LayoutKind.Sequential)]
    private struct POINT { public int X, Y; }

    private const uint GA_ROOT = 2;
    [DllImport("user32.dll")] private static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
    [DllImport("user32.dll")] private static extern bool GetWindowRect(IntPtr hWnd, out RECT r);
    [DllImport("user32.dll")] private static extern bool GetClientRect(IntPtr hWnd, out RECT r);
    [DllImport("user32.dll")] private static extern bool ClientToScreen(IntPtr hWnd, ref POINT p);
    [DllImport("user32.dll", CharSet = CharSet.Unicode)] private static extern int GetWindowTextW(IntPtr hWnd, StringBuilder s, int max);

    [StructLayout(LayoutKind.Sequential)]
    private struct RECT { public int Left, Top, Right, Bottom; }

    private const int SW_MINIMIZE = 6;
    private const int SW_SHOW = 5;
    private const int SW_RESTORE = 9;

    private const uint SRCCOPY = 0x00CC0020;
    private const uint CAPTUREBLT = 0x40000000;

    /// Grabs what DWM actually put on the screen, which is the only way the Mica backdrop
    /// appears at all: it is drawn behind the window by the compositor and is not part of
    /// the XAML visual tree, so RenderTargetBitmap would silently return the widget without
    /// its material. This is the direct analogue of the iOS reference capturing a real
    /// UIWindow rather than re-rendering a layer off-screen.
    private async Task CaptureWindowAsync()
    {
        try
        {
            _stage = "hwnd";
            var hwnd = _hwnd;

            // Reading the SCREEN means reading whatever is on top of it. The first run that
            // got this far captured the Windows out-of-box privacy dialog sitting over this
            // app, reported zero blockers, and looked entirely plausible -- a 768x519 image
            // full of real controls. So the window is forced to the front and the result is
            // verified, rather than assumed from the fact that we asked for it.
            _stage = "show";
            ShowWindow(hwnd, SW_RESTORE);
            ShowWindow(hwnd, SW_SHOW);

            _stage = "window-rect";
            if (!GetWindowRect(hwnd, out RECT r))
            {
                _blockers.Add("GetWindowRect failed; the window region is unknown");
                return;
            }
            int w = r.Right - r.Left, h = r.Bottom - r.Top;
            if (w <= 0 || h <= 0)
            {
                _blockers.Add($"the window has no size ({w}x{h}); nothing was composited");
                return;
            }

            // Everything from here down is Win32, which is thread agnostic. The XAML part --
            // waiting for presented frames -- deliberately happens before this method is
            // called, while we are still provably on the UI thread.
            DwmFlush();
            await Task.Delay(400);

            _stage = "minimise-occluders";
            var centre = new POINT { X = r.Left + w / 2, Y = r.Top + h / 2 };
            IntPtr atCentre = GetAncestor(WindowFromPoint(centre), GA_ROOT);

            // The runner desktop ships with a "Microsoft account" out-of-box window that
            // sits above even an always-on-top presenter, so asking politely for the
            // foreground is not enough. Minimise whatever is covering us, and keep going in
            // case several are stacked. This is a throwaway CI desktop whose only purpose is
            // to photograph this window; there is nothing here to be polite to.
            for (int i = 0; i < 5 && atCentre != hwnd && atCentre != IntPtr.Zero; i++)
            {
                // Closed, not just minimised. Minimising moved it out of the frame but it
                // KEPT the foreground -- measured: "the window is unoccluded but never became
                // active (foreground is 0x1020A \"Microsoft account\")" with that same
                // window already minimised. A window that is gone cannot hold the foreground.
                // Minimise remains the fallback for anything that refuses to close.
                Console.WriteLine($"NATIVEREF:INFO clearing 0x{atCentre.ToInt64():X} "
                    + $"({DescribeWindow(atCentre)}) which is covering the capture area");
                PostMessage(atCentre, WM_CLOSE, IntPtr.Zero, IntPtr.Zero);
                await Task.Delay(400);
                if (IsWindow(atCentre))
                {
                    Console.WriteLine($"NATIVEREF:INFO 0x{atCentre.ToInt64():X} would not close; minimising");
                    ShowWindow(atCentre, SW_MINIMIZE);
                }
                await Task.Delay(250);
                BringWindowToTop(hwnd);
                atCentre = GetAncestor(WindowFromPoint(centre), GA_ROOT);
            }

            _occluded = atCentre != hwnd;
            if (_occluded)
            {
                _blockers.Add("something is covering this window, so the capture would be of "
                    + $"it and not of us ({DescribeForegroundWindow()}, "
                    + $"at centre 0x{atCentre.ToInt64():X}). Screen capture reads whatever is "
                    + "on top, and a picture of the wrong window still looks like a picture.");
                return;
            }
            // Only NOW is it worth asking for the foreground. The earlier attempt ran before
            // the occluders were minimised and could never have succeeded: Win32 refuses
            // SetForegroundWindow to a process that is not already foreground, and the
            // out-of-box window held it. Nothing retried once the obstacle was gone, so the
            // window ended up unoccluded but inactive -- visible, and rendered in the
            // inactive style.
            //
            // Activation is not cosmetic for a reference set. An inactive window renders its
            // title bar differently and does not get the Mica backdrop, so capturing one
            // would encode a look no user sees, which is the same class of error as
            // capturing with Reduce Transparency on under macOS.
            _stage = "activate";
            bool isForeground = await TryTakeForegroundAsync(hwnd);
            if (!isForeground)
            {
                _blockers.Add("the window is unoccluded but never became active "
                    + $"({DescribeForegroundWindow()}), so its title bar and backdrop would "
                    + "be captured in the inactive style");
                return;
            }
            // Activating can re-order windows, so confirm nothing slid back over us.
            atCentre = GetAncestor(WindowFromPoint(centre), GA_ROOT);
            if (atCentre != hwnd)
            {
                _occluded = true;
                _blockers.Add($"activating let 0x{atCentre.ToInt64():X} "
                    + $"({DescribeWindow(atCentre)}) back over the window");
                return;
            }
            await Task.Delay(300);
            Console.WriteLine("NATIVEREF:INFO active and unoccluded");

            var pos = new { X = r.Left, Y = r.Top };
            _stage = "bitblt";
            IntPtr screen = GetDC(IntPtr.Zero);
            IntPtr mem = CreateCompatibleDC(screen);
            IntPtr bmp = CreateCompatibleBitmap(screen, w, h);
            IntPtr old = SelectObject(mem, bmp);
            bool ok = BitBlt(mem, 0, 0, w, h, screen, pos.X, pos.Y, SRCCOPY | CAPTUREBLT);
            SelectObject(mem, old);

            if (!ok)
            {
                _blockers.Add("BitBlt of the window region failed");
            }
            else
            {
                using var image = System.Drawing.Image.FromHbitmap(bmp);
                var name = _isProbe ? "probe_Button_normal_light" : "Button_normal_light";
                var path = Path.Combine(_outDir, name + ".png");
                image.Save(path, System.Drawing.Imaging.ImageFormat.Png);
                Console.WriteLine($"NATIVEREF:wrote {name} {w}x{h}");
                // Deliberately the CLIENT area, not the whole window. The title bar is drawn
                // by DWM whatever the app does, so a whole-window uniformity test passes an
                // utterly empty window -- which is exactly what it did: a captured
                // cn1-native-ref frame with a correct Windows 11 title bar and nothing at
                // all beneath it.
                // Sample the client background well away from the button and record it.
                // Whether the Mica backdrop is actually reaching the window is otherwise an
                // eyeball judgement on a PNG: a flat theme fill and a Mica surface over a
                // pale desktop look similar at a glance, and "mica_supported: true" only
                // says the OS could draw it, not that this window got it.
                GetClientRect(hwnd, out RECT cr);
                var origin = new POINT { X = 0, Y = 0 };
                ClientToScreen(hwnd, ref origin);
                int cx = origin.X - r.Left, cy = origin.Y - r.Top;
                int cw = cr.Right - cr.Left, ch = cr.Bottom - cr.Top;
                if (cw > 0 && ch > 0 && cx >= 0 && cy >= 0 && cx + cw <= w && cy + ch <= h)
                {
                    var bg = image.GetPixel(cx + cw * 3 / 4, cy + ch * 3 / 4);
                    _clientBackground = $"#{bg.R:X2}{bg.G:X2}{bg.B:X2}";
                    Console.WriteLine($"NATIVEREF:INFO client background sample {_clientBackground}");
                }
                if (cw > 0 && ch > 0 && cx >= 0 && cy >= 0 && cx + cw <= w && cy + ch <= h
                    && IsUniform(image, cx, cy, cw, ch))
                {
                    _blockers.Add($"{name} has an empty client area ({cw}x{ch} of one flat "
                        + "colour): the window chrome drew but the content did not");
                }
            }

            DeleteObject(bmp);
            DeleteDC(mem);
            ReleaseDC(IntPtr.Zero, screen);
        }
        catch (Exception e)
        {
            // COMException's Message is routinely empty, and "capture threw COMException:"
            // is worth nothing to whoever reads it next. Record where it got to, the
            // HRESULT, and the frame it came from -- a probe whose failures are not
            // self-describing just converts one unknown into another.
            _blockers.Add($"capture threw {e.GetType().Name} (0x{e.HResult:X8}) at stage "
                + $"'{_stage}': {(string.IsNullOrWhiteSpace(e.Message) ? "(no message)" : e.Message)}");
            Console.Error.WriteLine($"NATIVEREF:EXCEPTION stage={_stage} {e}");
        }
    }


    /// Takes the foreground, working around the Win32 rule that a process which is not
    /// already foreground may not call SetForegroundWindow. Attaching to the current
    /// foreground window's input thread lifts that restriction for the duration.
    private static async Task<bool> TryTakeForegroundAsync(IntPtr hwnd)
    {
        // Windows enforces a foreground LOCK TIMEOUT: after another process has been
        // activated, SetForegroundWindow is refused for a period regardless of the
        // input-thread attach. Setting it to zero is the documented way to opt out, and on a
        // CI desktop whose only job is to photograph one window there is nothing to protect
        // the user from.
        SystemParametersInfo(SPI_SETFOREGROUNDLOCKTIMEOUT, 0, IntPtr.Zero, SPIF_SENDCHANGE);

        for (int i = 0; i < 20 && GetForegroundWindow() != hwnd; i++)
        {
            IntPtr fg = GetForegroundWindow();
            uint fgThread = GetWindowThreadProcessId(fg, IntPtr.Zero);
            uint thisThread = GetCurrentThreadId();
            bool attached = fgThread != 0 && fgThread != thisThread
                            && AttachThreadInput(fgThread, thisThread, true);
            try
            {
                BringWindowToTop(hwnd);
                SetForegroundWindow(hwnd);
            }
            finally
            {
                if (attached) AttachThreadInput(fgThread, thisThread, false);
            }
            await Task.Delay(150);
        }
        return GetForegroundWindow() == hwnd;
    }

    /// Names whatever is actually in front, so a capture failure says which window stole
    /// the screen instead of leaving someone to guess from the picture.
    private static string DescribeForegroundWindow()
    {
        var fg = GetForegroundWindow();
        return $"foreground is 0x{fg.ToInt64():X} \"{DescribeWindow(fg)}\"";
    }

    private static string DescribeWindow(IntPtr h)
    {
        try
        {
            var buf = new StringBuilder(256);
            int n = GetWindowTextW(h, buf, buf.Capacity);
            return n > 0 ? buf.ToString() : "(untitled)";
        }
        catch
        {
            return "(unidentifiable)";
        }
    }

    /// A uniformly coloured tile is the classic captured-before-present result, and it
    /// scores as a perfect match against another blank tile rather than as a failure.
    private static bool IsUniform(System.Drawing.Bitmap image, int x0, int y0, int w, int h)
    {
        var first = image.GetPixel(x0, y0);
        int stepX = Math.Max(1, w / 32), stepY = Math.Max(1, h / 32);
        for (int y = y0; y < y0 + h; y += stepY)
            for (int x = x0; x < x0 + w; x += stepX)
                if (image.GetPixel(x, y) != first) return false;
        return true;
    }

    private static bool FontIsInstalled(string family)
    {
        try
        {
            var dir = Environment.GetFolderPath(Environment.SpecialFolder.Windows);
            var fonts = Path.Combine(dir, "Fonts");
            // Segoe UI Variable ships as SegUIVar*.ttf; matching the file rather than
            // asking a font-fallback API is deliberate, because font fallback answers
            // "something will render" for every family name ever passed to it.
            return Directory.Exists(fonts)
                   && Directory.GetFiles(fonts, "SegUIVar*.ttf").Length > 0;
        }
        catch
        {
            return false;
        }
    }

    private void WriteManifest(bool micaSupported, bool transparency, bool animations,
        Windows.UI.Color accent, double rasterScale, string fontFamily, bool segoeVariable)
    {
        var sb = new StringBuilder();
        sb.AppendLine("{");
        sb.AppendLine("  \"schema\": 1,");
        sb.AppendLine("  \"platform\": \"windows\",");
        sb.AppendLine($"  \"golden_set\": \"{Env("CN1SS_FIDELITY_GOLDEN_SET", "windows-11-fluent")}\",");
        sb.AppendLine($"  \"mode\": \"{(_isProbe ? "probe" : "capture")}\",");
        sb.AppendLine("  \"os\": {");
        sb.AppendLine($"    \"version\": \"{Environment.OSVersion.Version}\",");
        sb.AppendLine($"    \"description\": \"{Escape(RuntimeInformation.OSDescription)}\",");
        sb.AppendLine($"    \"architecture\": \"{RuntimeInformation.OSArchitecture}\",");
        sb.AppendLine($"    \"product\": \"{Escape(ReadRegistry("ProductName"))}\",");
        sb.AppendLine($"    \"display_version\": \"{Escape(ReadRegistry("DisplayVersion"))}\",");
        sb.AppendLine($"    \"build\": \"{Escape(ReadRegistry("CurrentBuildNumber"))}\",");
        sb.AppendLine($"    \"installation_type\": \"{Escape(ReadRegistry("InstallationType"))}\"");
        sb.AppendLine("  },");
        sb.AppendLine("  \"toolkit\": {");
        sb.AppendLine("    \"name\": \"WinUI3\",");
        sb.AppendLine("    \"deployment\": \"unpackaged-self-contained\",");
        sb.AppendLine($"    \"dotnet\": \"{Escape(RuntimeInformation.FrameworkDescription)}\"");
        sb.AppendLine("  },");
        sb.AppendLine("  \"display\": {");
        sb.AppendLine($"    \"rasterization_scale\": {rasterScale.ToString(System.Globalization.CultureInfo.InvariantCulture)}");
        sb.AppendLine("  },");
        sb.AppendLine("  \"appearance\": {");
        sb.AppendLine($"    \"mica_supported\": {Json(micaSupported)},");
        sb.AppendLine($"    \"transparency_effects\": {Json(transparency)},");
        sb.AppendLine($"    \"animations_enabled\": {Json(animations)},");
        sb.AppendLine($"    \"accent_color\": \"#{accent.R:X2}{accent.G:X2}{accent.B:X2}\"");
        sb.AppendLine("  },");
        sb.AppendLine("  \"capture\": {");
        sb.AppendLine($"    \"occluded\": {Json(_occluded)},");
        sb.AppendLine($"    \"was_foreground\": {Json(GetForegroundWindow() == _hwnd)},");
        sb.AppendLine($"    \"client_background\": \"{Escape(_clientBackground)}\"");
        sb.AppendLine("  },");
        sb.AppendLine("  \"fonts\": {");
        sb.AppendLine($"    \"control_family\": \"{Escape(fontFamily)}\",");
        sb.AppendLine($"    \"segoe_ui_variable_installed\": {Json(segoeVariable)}");
        sb.AppendLine("  },");
        sb.Append("  \"blockers\": [");
        sb.Append(string.Join(", ", _blockers.Select(b => $"\"{Escape(b)}\"")));
        sb.AppendLine("]");
        sb.AppendLine("}");

        var path = Path.Combine(_outDir, "capture-manifest.json");
        File.WriteAllText(path, sb.ToString());
        Console.WriteLine($"NATIVEREF:INFO wrote {path}");
    }

    private static string Env(string name, string fallback)
        => Environment.GetEnvironmentVariable(name) is { Length: > 0 } v ? v : fallback;

    private static string Json(bool b) => b ? "true" : "false";

    /// Escapes for JSON, control characters included. A window title arrived carrying
    /// embedded NULs and wrote them raw into the manifest, which made it unparseable -- and
    /// a raw control byte in a text file is precisely what scripts/check-control-characters.py
    /// exists to prevent, because it turns the file binary to every tool that reads it.
    private static string Escape(string s)
    {
        if (string.IsNullOrEmpty(s)) return string.Empty;
        var sb = new StringBuilder(s.Length);
        foreach (var c in s)
        {
            switch (c)
            {
                case '\\': sb.Append("\\\\"); break;
                case '"': sb.Append("\\\""); break;
                case '\n': sb.Append("\\n"); break;
                case '\r': sb.Append("\\r"); break;
                case '\t': sb.Append("\\t"); break;
                default:
                    if (char.IsControl(c)) sb.Append($"\\u{(int)c:X4}");
                    else sb.Append(c);
                    break;
            }
        }
        return sb.ToString();
    }

    private static string ReadRegistry(string name)
    {
        try
        {
            using var key = Microsoft.Win32.Registry.LocalMachine.OpenSubKey(
                @"SOFTWARE\Microsoft\Windows NT\CurrentVersion");
            return key?.GetValue(name)?.ToString() ?? "";
        }
        catch
        {
            return "";
        }
    }
}

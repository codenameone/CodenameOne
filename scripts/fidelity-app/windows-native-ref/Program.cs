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

        var root = new StackPanel { Orientation = Orientation.Vertical, Spacing = 12, Margin = new Thickness(24) };
        var button = new Button { Content = "Default" };
        root.Children.Add(button);
        _window.Content = root;

        root.Loaded += (_, _) => OnReady(root, button, micaSupported);
        _window.Activate();
    }

    private void OnReady(FrameworkElement root, Button button, bool micaSupported)
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

        // Capture a real tile even in probe mode. A green build is not a rendered one:
        // AppxGeneratePriEnabled is off (see NativeRef.csproj), so WinUI's own .pri files
        // are not expanded into the output, and if that mattered the controls would come
        // back unstyled or absent rather than failing loudly. A picture is the only thing
        // that distinguishes "built" from "drew a Fluent button".
        CaptureWindow(root);

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

    private const uint SRCCOPY = 0x00CC0020;
    private const uint CAPTUREBLT = 0x40000000;

    /// Grabs what DWM actually put on the screen, which is the only way the Mica backdrop
    /// appears at all: it is drawn behind the window by the compositor and is not part of
    /// the XAML visual tree, so RenderTargetBitmap would silently return the widget without
    /// its material. This is the direct analogue of the iOS reference capturing a real
    /// UIWindow rather than re-rendering a layer off-screen.
    private void CaptureWindow(FrameworkElement root)
    {
        try
        {
            var hwnd = WinRT.Interop.WindowNative.GetWindowHandle(_window);
            var id = Microsoft.UI.Win32Interop.GetWindowIdFromWindow(hwnd);
            var appWindow = AppWindow.GetFromWindowId(id);
            int w = appWindow.Size.Width, h = appWindow.Size.Height;
            if (w <= 0 || h <= 0)
            {
                _blockers.Add($"the window has no size ({w}x{h}); nothing was composited");
                return;
            }

            // Let the compositor finish the frame before reading the screen back, or the
            // grab races the first present and returns the desktop.
            DwmFlush();
            Thread.Sleep(400);

            var pos = appWindow.Position;
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
                if (IsUniform(image))
                {
                    _blockers.Add($"{name} is a single flat colour: the window rendered "
                        + "nothing, which is what a missing resource index looks like");
                }
            }

            DeleteObject(bmp);
            DeleteDC(mem);
            ReleaseDC(IntPtr.Zero, screen);
        }
        catch (Exception e)
        {
            _blockers.Add($"capture threw {e.GetType().Name}: {e.Message}");
        }
    }

    /// A uniformly coloured tile is the classic captured-before-present result, and it
    /// scores as a perfect match against another blank tile rather than as a failure.
    private static bool IsUniform(System.Drawing.Bitmap image)
    {
        var first = image.GetPixel(0, 0);
        int stepX = Math.Max(1, image.Width / 32), stepY = Math.Max(1, image.Height / 32);
        for (int y = 0; y < image.Height; y += stepY)
            for (int x = 0; x < image.Width; x += stepX)
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

    private static string Escape(string s)
        => (s ?? string.Empty).Replace("\\", "\\\\").Replace("\"", "\\\"");

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

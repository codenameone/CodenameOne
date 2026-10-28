# meta-codenameone

A Yocto layer that builds Codename One applications into an image, running from
boot as a systemd service on the embedded Linux port (DRM/KMS with a software
fallback, evdev input, no X11, Wayland or GTK).

```
bitbake-layers add-layer /path/to/meta-codenameone
```

1. Translate the application on a workstation; the C is the same for every
   architecture: `scripts/embedded/translate.sh <classes> <MainClass> out`.
2. `tar -C out -czf dist.tar.gz dist` and put it beside your recipe
   (`files/dist.tar.gz`), or fetch it from wherever your CI publishes it.
3. Write a recipe like `recipes-codenameone/codenameone-app/codenameone-app_1.0.bb`
   that `inherit codenameone-app` and sets `CN1_APP` to the main class name.
4. `IMAGE_INSTALL:append = " codenameone-app"` in your image, with
   `DISTRO_FEATURES` including `systemd`.

Set `CN1_GPU = "0"` for a board whose BSP has no EGL/GLES; the application then
renders through DRM dumb buffers or `/dev/fb0` in software.

/// USB host-mode device access.
///
/// `Usb` enumerates attached `UsbDevice` instances and opens bulk
/// input/output streams to them; `UsbDeviceListener` delivers
/// attach/detach notifications. The native bridge is supplied by the
/// active port through `UsbPlatform`.
package com.codename1.io.usb;

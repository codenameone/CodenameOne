# Builds a Codename One application from the translated project ParparVM
# writes (scripts/embedded/translate.sh, or the linux-native build target with
# the linux.embedded hint), and installs it as a systemd service that owns the
# display from boot.
#
# A recipe sets SRC_URI to that translated directory (a tarball of dist/ or a
# git repository holding it) and CN1_APP to the executable's name. The C is
# architecture independent, so translating once on a workstation is enough for
# every MACHINE.
#
#   CN1_APP           executable name (the application's main class)
#   CN1_ENVIRONMENT   space separated KEY=value pairs for the service
#   CN1_GPU           "1" links DRM/KMS, EGL and GLES when the BSP has them

inherit cmake pkgconfig systemd

CN1_APP ?= "${PN}"
CN1_ENVIRONMENT ?= ""
CN1_GPU ?= "1"

DEPENDS += "freetype libpng jpeg zlib openssl curl"
DEPENDS += "${@'libdrm virtual/egl virtual/libgles2 libepoxy' if d.getVar('CN1_GPU') == '1' else ''}"
RDEPENDS:${PN} += "ttf-dejavu-sans"

S = "${WORKDIR}/dist"
OECMAKE_SOURCEPATH = "${S}"
EXTRA_OECMAKE += "-DCN1_LINUX_EMBEDDED=ON"

# The generated CMakeLists.txt records the absolute directory it was translated
# into; point it at the unpacked copy.
do_configure:prepend() {
    sed -i 's|^set(CN1_APP_SOURCE_ROOT .*|set(CN1_APP_SOURCE_ROOT "${S}/${CN1_APP}-src")|' ${S}/CMakeLists.txt
}

do_install() {
    install -d ${D}${bindir} ${D}${systemd_system_unitdir}
    install -m 0755 ${B}/${CN1_APP} ${D}${bindir}/${CN1_APP}
    cat > ${D}${systemd_system_unitdir}/${CN1_APP}.service <<UNIT
[Unit]
Description=Codename One application ${CN1_APP}
After=systemd-udev-settle.service
Conflicts=getty@tty1.service

[Service]
ExecStart=${bindir}/${CN1_APP}
Restart=always
RestartSec=1
StandardInput=tty
TTYPath=/dev/tty1
TTYReset=yes
TTYVHangup=yes
$(for e in ${CN1_ENVIRONMENT}; do echo "Environment=$e"; done)

[Install]
WantedBy=multi-user.target
UNIT
}

SYSTEMD_SERVICE:${PN} = "${CN1_APP}.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"
FILES:${PN} += "${systemd_system_unitdir}/${CN1_APP}.service"

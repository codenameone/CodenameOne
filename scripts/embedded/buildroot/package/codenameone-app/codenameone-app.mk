################################################################################
#
# codenameone-app
#
################################################################################

CODENAMEONE_APP_VERSION = 1.0
CODENAMEONE_APP_SITE = $(call qstrip,$(BR2_PACKAGE_CODENAMEONE_APP_DIST))
CODENAMEONE_APP_SITE_METHOD = local
CODENAMEONE_APP_NAME_EXE = $(call qstrip,$(BR2_PACKAGE_CODENAMEONE_APP_NAME))
CODENAMEONE_APP_DEPENDENCIES = freetype libpng jpeg zlib \
	openssl libcurl libdrm host-pkgconf
CODENAMEONE_APP_CONF_OPTS = -DCN1_LINUX_EMBEDDED=ON

# The generated CMakeLists.txt records the absolute directory it was
# translated into; point it at Buildroot's copy.
define CODENAMEONE_APP_RELOCATE
	$(SED) 's|^set(CN1_APP_SOURCE_ROOT .*|set(CN1_APP_SOURCE_ROOT "$(@D)/$(CODENAMEONE_APP_NAME_EXE)-src")|' \
		$(@D)/CMakeLists.txt
endef
CODENAMEONE_APP_PRE_CONFIGURE_HOOKS += CODENAMEONE_APP_RELOCATE

define CODENAMEONE_APP_INSTALL_TARGET_CMDS
	$(INSTALL) -D -m 0755 $(@D)/$(CODENAMEONE_APP_NAME_EXE) \
		$(TARGET_DIR)/usr/bin/$(CODENAMEONE_APP_NAME_EXE)
endef

# BusyBox init: started last, restarted when it exits.
define CODENAMEONE_APP_INSTALL_INIT_SYSV
	grep -q codenameone-app $(TARGET_DIR)/etc/inittab || \
		echo "tty1::respawn:/usr/bin/$(CODENAMEONE_APP_NAME_EXE) # codenameone-app" >> $(TARGET_DIR)/etc/inittab
endef

define CODENAMEONE_APP_INSTALL_INIT_SYSTEMD
	$(INSTALL) -d $(TARGET_DIR)/usr/lib/systemd/system
	printf '[Unit]\nDescription=Codename One application\nConflicts=getty@tty1.service\n\n[Service]\nExecStart=/usr/bin/%s\nRestart=always\nRestartSec=1\nStandardInput=tty\nTTYPath=/dev/tty1\n\n[Install]\nWantedBy=multi-user.target\n' \
		$(CODENAMEONE_APP_NAME_EXE) > $(TARGET_DIR)/usr/lib/systemd/system/codenameone-app.service
endef

$(eval $(cmake-package))

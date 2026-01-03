package com.codename1.ui;

class RefreshThemeRunnable implements Runnable {
    private final Component cmp;

    public RefreshThemeRunnable(Component cmp) {
        this.cmp = cmp;
    }

    public void run() {
        cmp.refreshTheme(false);
    }
}

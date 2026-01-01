package com.codename1.ui;

import com.codename1.ui.animations.ComponentAnimation;

class RefreshThemeCallback extends ComponentAnimation {
    private final Runnable r;

    public RefreshThemeCallback(Runnable r) {
        this.r = r;
    }

    @Override
    public boolean isInProgress() {
        return false;
    }

    @Override
    protected void updateState() {
        r.run();
    }
}

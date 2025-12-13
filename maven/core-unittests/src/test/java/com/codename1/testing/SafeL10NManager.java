package com.codename1.testing;

import com.codename1.l10n.L10NManager;
import java.util.Date;

public class SafeL10NManager extends L10NManager {
    public SafeL10NManager(String language, String country) {
        super(language, country);
    }

    @Override
    public String formatDateShortStyle(Date date) {
        return "01/01/2023";
    }

    @Override
    public String formatDateLongStyle(Date date) {
        return "January 1, 2023";
    }

    @Override
    public String formatDateTime(Date date) {
        return "01/01/2023 12:00";
    }

    @Override
    public String formatDateTimeMedium(Date date) {
        return "Jan 1, 2023 12:00";
    }

    @Override
    public String formatDateTimeShort(Date date) {
        return "01/01/23 12:00";
    }
}

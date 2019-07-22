package org.waarp.openr66.protocol.configuration;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.waarp.common.utility.SystemPropertyUtil;
import org.waarp.openr66.context.ErrorCode;

public class Messages {
    private static final String BUNDLE_NAME = "messages"; //$NON-NLS-1$

    private static ResourceBundle RESOURCE_BUNDLE = null;
    private static String slocale = "en";

    static {
        setSlocale(SystemPropertyUtil.get(R66SystemProperties.OPENR66_LOCALE, "en"));
        if (getSlocale() == null || getSlocale().isEmpty()) {
            setSlocale("en");
        }
        init(new Locale(getSlocale()));
    }

    public static void init(Locale locale) {
        if (locale == null) {
            setSlocale("en");
            locale = new Locale(getSlocale());
        } else {
            setSlocale(locale.getLanguage());
        }
        RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME, locale);
    }

    public static String getString(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    public static String getString(String key, Object... args) {
        try {
            String source = RESOURCE_BUNDLE.getString(key);
            return MessageFormat.format(source, args);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    /**
     * @return the slocale
     */
    public static String getSlocale() {
        return slocale;
    }

    /**
     * @param slocale the slocale to set
     */
    public static void setSlocale(String slocale) {
        Messages.slocale = slocale;
    }
}

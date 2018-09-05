package hudson.plugins.android_emulator;

import hudson.Util;
import hudson.plugins.android_emulator.util.Utils;

import java.io.Serializable;

public interface Constants {

    /** The locale to which Android emulators default if not otherwise specified. */
    static final String DEFAULT_LOCALE = "en_US";

    /** Locales supported: http://developer.android.com/sdk/android-3.0.html#locs */
    static final String[] EMULATOR_LOCALES = {
        "ar_EG", "ar_IL", "bg_BG", "ca_ES", "cs_CZ", "da_DK", "de_AT", "de_CH",
        "de_DE", "de_LI", "el_GR", "en_AU", "en_CA", "en_GB", "en_IE", "en_IN",
        "en_NZ", "en_SG", "en_US", "en_ZA", "es_ES", "es_US", "fi_FI", "fr_BE",
        "fr_CA", "fr_CH", "fr_FR", "he_IL", "hi_IN", "hr_HR", "hu_HU", "id_ID",
        "it_CH", "it_IT", "ja_JP", "ko_KR", "lt_LT", "lv_LV", "nb_NO", "nl_BE",
        "nl_NL", "pl_PL", "pt_BR", "pt_PT", "ro_RO", "ru_RU", "sk_SK", "sl_SI",
        "sr_RS", "sv_SE", "th_TH", "tl_PH", "tr_TR", "uk_UA", "vi_VN", "zh_CN",
        "zh_TW"
    };

    /** Commonly-used hardware properties that can be emulated. */
    static final String[] HARDWARE_PROPERTIES = {
        "hw.accelerometer", "hw.battery", "hw.camera", "hw.dPad", "hw.gps",
        "hw.gsmModem", "hw.keyboard", "hw.ramSize", "hw.sdCard",
        "hw.touchScreen", "hw.trackBall", "vm.heapSize"
    };

    /** Common ABIs. */
    static final String[] TARGET_ABIS = {
        "armeabi", "armeabi-v7a", "mips", "x86", "x86_64"
    };

    /** Name of the snapshot image we will use. */
    static final String SNAPSHOT_NAME = "jenkins";

    // From hudson.Util.VARIABLE
    static final String REGEX_VARIABLE = "\\$([A-Za-z0-9_]+|\\{[A-Za-z0-9_]+\\}|\\$)";
    static final String REGEX_AVD_NAME = "[a-zA-Z0-9._-]+";
    static final String REGEX_LOCALE = "[a-z]{2}_[A-Z]{2}";
    static final String REGEX_SCREEN_DENSITY = "[0-9]{2,4}|(?i)(x?x?h|[lm])dpi";
    static final String REGEX_SCREEN_RESOLUTION = "[0-9]{3,4}x[0-9]{3,4}";
    static final String REGEX_SCREEN_RESOLUTION_ALIAS = "(([HQ]|F?W[SQ]?)V|WX)GA(720|800|-[LP])?";
    static final String REGEX_SCREEN_RESOLUTION_FULL = REGEX_SCREEN_RESOLUTION_ALIAS +"|"+ REGEX_SCREEN_RESOLUTION;
    static final String REGEX_SD_CARD_SIZE = "(?i)([0-9]{1,12}) ?([KM])[B]?";
    static final String REGEX_SNAPSHOT = "[0-9]+ +"+ SNAPSHOT_NAME +" +[0-9.]+[KMGT] ";

}


enum SnapshotState {
    NONE,
    INITIALISE,
    BOOT
}

class AndroidPlatform implements Serializable {

    private static final long serialVersionUID = 4L;

    static final AndroidPlatform SDK_1_1 = new AndroidPlatform("1.1", 2);
    static final AndroidPlatform SDK_1_5 = new AndroidPlatform("1.5", 3);
    static final AndroidPlatform SDK_1_6 = new AndroidPlatform("1.6", 4);
    static final AndroidPlatform SDK_2_0 = new AndroidPlatform("2.0", 5);
    static final AndroidPlatform SDK_2_0_1 = new AndroidPlatform("2.0.1", 6);
    static final AndroidPlatform SDK_2_1 = new AndroidPlatform("2.1", 7);
    static final AndroidPlatform SDK_2_2 = new AndroidPlatform("2.2", 8);
    static final AndroidPlatform SDK_2_3 = new AndroidPlatform("2.3", 9);
    static final AndroidPlatform SDK_2_3_3 = new AndroidPlatform("2.3.3", 10);
    static final AndroidPlatform SDK_3_0 = new AndroidPlatform("3.0", 11);
    static final AndroidPlatform SDK_3_1 = new AndroidPlatform("3.1", 12);
    static final AndroidPlatform SDK_3_2 = new AndroidPlatform("3.2", 13);
    static final AndroidPlatform SDK_4_0 = new AndroidPlatform("4.0", 14);
    static final AndroidPlatform SDK_4_0_3 = new AndroidPlatform("4.0.3", 15);
    static final AndroidPlatform SDK_4_1 = new AndroidPlatform("4.1", 16);
    static final AndroidPlatform SDK_4_2 = new AndroidPlatform("4.2", 17);
    static final AndroidPlatform SDK_4_3 = new AndroidPlatform("4.3", 18);
    static final AndroidPlatform SDK_4_4 = new AndroidPlatform("4.4", 19);
    static final AndroidPlatform SDK_4_4W = new AndroidPlatform("4.4W", 20);
    static final AndroidPlatform SDK_5_0 = new AndroidPlatform("5.0", 21);
    static final AndroidPlatform SDK_5_1 = new AndroidPlatform("5.1", 22);
    static final AndroidPlatform SDK_6_0 = new AndroidPlatform("6.0", 23);
    static final AndroidPlatform SDK_7_0 = new AndroidPlatform("7.0", 24);
    static final AndroidPlatform SDK_7_1 = new AndroidPlatform("7.1", 25);
    static final AndroidPlatform SDK_8_0 = new AndroidPlatform("8.0", 26);
    static final AndroidPlatform SDK_8_1 = new AndroidPlatform("8.1", 27);
    static final AndroidPlatform SDK_9_0 = new AndroidPlatform("9.0", 28);
    static final AndroidPlatform[] ALL = new AndroidPlatform[] { SDK_1_1, SDK_1_5, SDK_1_6, SDK_2_0,
        SDK_2_0_1, SDK_2_1, SDK_2_2, SDK_2_3, SDK_2_3_3, SDK_3_0, SDK_3_1, SDK_3_2, SDK_4_0,
        SDK_4_0_3, SDK_4_1, SDK_4_2, SDK_4_3, SDK_4_4, SDK_4_4W, SDK_5_0, SDK_5_1, SDK_6_0, SDK_7_0, SDK_7_1, 
        SDK_8_0, SDK_8_1, SDK_9_0 };

    private final String name;
    private final int level;
    private final boolean isAddon;

    private AndroidPlatform(String name, int level) {
        this.name = name;
        this.isAddon = level <= 0;
        if (isAddon) {
            level = Utils.getApiLevelFromPlatform(name);
        }
        this.level = level;
    }

    private AndroidPlatform(String name) {
        this(name, -1);
    }

    public static AndroidPlatform valueOf(String version) {
        if (Util.fixEmptyAndTrim(version) == null) {
            return null;
        }

        for (AndroidPlatform preset : ALL) {
            if (version.equals(preset.name) || version.equals(String.valueOf(preset.level))
                    || version.equals(preset.getTargetName())) {
                return preset;
            }
        }

        return new AndroidPlatform(version);
    }

    public boolean isCustomPlatform() {
        return isAddon;
    }

    public String getTargetName() {
        if (isCustomPlatform()) {
            return name;
        }

        return "android-"+ level;
    }

    public int getSdkLevel() {
        return level;
    }

    @Override
    public String toString() {
        return name;
    }

}



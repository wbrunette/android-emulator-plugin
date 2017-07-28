package hudson.plugins.android_emulator.sdk;

import hudson.plugins.android_emulator.SdkInstallationException;

public enum Tool {
    ADB("adb", ".exe", new PlatformToolLocator()),
    SDKMANAGER("sdkmanager", ".bat", new ToolBinLocator()),
    AVDMANAGER("avdmanager", ".bat", new ToolBinLocator()),
    EMULATOR("emulator", ".exe", new DefaultEmulatorLocation()),
    EMULATOR_ARM("emulator-arm", ".exe", new DefaultEmulatorLocation()),
    EMULATOR_MIPS("emulator-mips", ".exe", new DefaultEmulatorLocation()),
    EMULATOR_X86("emulator-x86", ".exe", new DefaultEmulatorLocation()),
    EMULATOR64_ARM("emulator64-arm", ".exe", new DefaultEmulatorLocation()),
    EMULATOR64_MIPS("emulator64-mips", ".exe", new DefaultEmulatorLocation()),
    EMULATOR64_X86("emulator64-x86", ".exe", new DefaultEmulatorLocation()),
    MKSDCARD("mksdcard", ".exe", new DefaultEmulatorLocation());

    public static Tool[] EMULATORS = new Tool[] { EMULATOR,
           EMULATOR_ARM,   EMULATOR_MIPS,   EMULATOR_X86,
           EMULATOR64_ARM, EMULATOR64_MIPS, EMULATOR64_X86
    };

    public static Tool[] REQUIRED = new Tool[] {
        ADB, SDKMANAGER, AVDMANAGER, EMULATOR
    };

    public final String executable;
    public final String windowsExtension;
    public final ToolLocator toolLocator;

    Tool(String executable, String windowsExtension) {
        this(executable, windowsExtension, new DefaultToolLocator());
    }

    Tool(String executable, String windowsExtension, ToolLocator toolLocator) {
        this.executable = executable;
        this.windowsExtension = windowsExtension;
        this.toolLocator = toolLocator;
    }

    public String getExecutable(boolean isUnix) {
        if (isUnix) {
            return executable;
        }
        return executable + windowsExtension;
    }

    public String findInSdk(AndroidSdk androidSdk) throws SdkInstallationException {
        return toolLocator.findInSdk(androidSdk, this);
    }

    public static String[] getAllExecutableVariants() {
        return getAllExecutableVariants(values());
    }

    public static String[] getAllExecutableVariants(final Tool[] tools) {
        String[] executables = new String[tools.length * 2];
        for (int i = 0, n = tools.length; i < n; i++) {
            executables[i*2] = tools[i].getExecutable(true);
            executables[i*2+1] = tools[i].getExecutable(false);
        }

        return executables;
    }

    @Override
    public String toString() {
        return executable;
    }
}

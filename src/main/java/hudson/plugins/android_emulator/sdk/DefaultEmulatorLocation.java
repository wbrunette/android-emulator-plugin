package hudson.plugins.android_emulator.sdk;

import hudson.plugins.android_emulator.SdkInstallationException;

public class DefaultEmulatorLocation implements ToolLocator {

	@Override
	public String findInSdk(AndroidSdk androidSdk, Tool tool) throws SdkInstallationException {
		return "/emulator/";
	}

}

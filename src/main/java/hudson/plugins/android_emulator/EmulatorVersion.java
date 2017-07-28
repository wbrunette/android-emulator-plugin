package hudson.plugins.android_emulator;

public class EmulatorVersion {

	private static final String SEMI_COLON = ";";
	private static final String REMOTE_PACKAGE_TYPE_IMAGE = "system-images";

	public enum AndroidApi {
		DEFAULT("default"), GOOGLE("google_apis");

		public final String pathString;

		private AndroidApi(String pathSubString) {
			pathString = pathSubString;
		}

		public static AndroidApi parse(String api) {
			if (api != null) {
				for (AndroidApi tmp : AndroidApi.values()) {
					if (api.contains(tmp.pathString)) {
						return tmp;
					}
				}
			}

			return DEFAULT;
		}

	}

	public enum AndroidHardware {
		X86("x86"), X86_64("x86_64"), ARMEABI_V7A("armeabi-v7a"), ARM64_V8A("arm64-v8a");

		public final String pathString;

		private AndroidHardware(String pathSubString) {
			pathString = pathSubString;
		}

		public static AndroidHardware parse(String hardware) {
			if (hardware != null) {
				for (AndroidHardware hw : AndroidHardware.values()) {
					if (hardware.contains(hw.pathString)) {
						return hw;
					}
				}
			}

			return X86;
		}
	}

	private final AndroidPlatform platform;
	private final AndroidApi api;
	private final AndroidHardware hardware;

	public EmulatorVersion(String os, String abi) {
		platform = AndroidPlatform.valueOf(os);
		api = AndroidApi.parse(abi);
		hardware = AndroidHardware.parse(abi);
	}
	
	public String getEmulatorVersionString(){
		return REMOTE_PACKAGE_TYPE_IMAGE + SEMI_COLON + platform.getTargetName() + SEMI_COLON + api.pathString + SEMI_COLON + hardware.pathString;
	}
	
	public AndroidPlatform getOSPlatform() {
		return platform;
	}
	
}

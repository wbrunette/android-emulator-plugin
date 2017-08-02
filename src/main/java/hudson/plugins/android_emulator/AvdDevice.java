package hudson.plugins.android_emulator;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public enum AvdDevice implements Serializable {
	NEXUS_7(AvdDeviceSpecs.NEXUS_7_SPECS, AvdDeviceSpecs.NEXUS_7_SKIN_NAME),
	NEXUS_7_NO_SKIN(AvdDeviceSpecs.NEXUS_7_SPECS, null);

	private final String specs;
	private final String skin;

	private AvdDevice(String avdDeviceSpecs, String skinName) {
		specs = avdDeviceSpecs;
		skin = skinName;
	}

	public Map<String, String> getDeviceSpecs() {
		Map<String, String> specMap = new HashMap<String, String>();

		if (specs != null) {

			StringTokenizer tokenizeList = new StringTokenizer(specs, AvdDeviceSpecs.DELEMIN);

			while (tokenizeList.hasMoreTokens()) {
				String keyValue = tokenizeList.nextToken();
				String parts[] = keyValue.split("=");
				if (parts.length != 2) {
					continue;
				}
				specMap.put(parts[0], parts[1]);
			}
		}
		
		if (skin != null) {
			specMap.put("skin.name", skin);
			String skinPath = "/skins/" + skin;
			specMap.put("skin.path", skinPath);
		}
		
		return specMap;

	}

}

package hudson.plugins.android_emulator;

import java.io.Serializable;

public class AvdDeviceSpecs implements Serializable {
	
	private static final long serialVersionUID = -7211323867009992722L;

	public static final String DELEMIN = ";";
	
	public static final String NEXUS_7_SPECS = "hw.device.manufacturer=Google;"
			+ "hw.device.name=Nexus 7 2013" + DELEMIN
			+ "hw.lcd.density=320" + DELEMIN
			+ "showDeviceFrame=yes" + DELEMIN
			+ "skin.dynamic=yes" + DELEMIN
			+ "skin.name=nexus_7_2013" + DELEMIN;
	
	public static final String NEXUS_7_SKIN_NAME = "nexus_7_2013";
	
	
}

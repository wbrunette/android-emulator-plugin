package hudson.plugins.android_emulator.sdk;

import junit.framework.TestCase;

import java.io.IOException;

public class AndroidSdkTest extends TestCase {

    public void testGetSdkToolsMajorVersion() throws Exception {
        assertEquals(0, createSdkWithTools(null).getSdkToolsMajorVersion());
        assertEquals(20, createSdkWithTools("20").getSdkToolsMajorVersion());
        assertEquals(20, createSdkWithTools("20.0").getSdkToolsMajorVersion());
        assertEquals(20, createSdkWithTools("20.0.1").getSdkToolsMajorVersion());
        assertEquals(21, createSdkWithTools("21 rc3").getSdkToolsMajorVersion());
        assertEquals(22, createSdkWithTools("22.6").getSdkToolsMajorVersion());
    }

    private static AndroidSdk createSdkWithTools(String version) {
        AndroidSdk sdk = null;
        try {
            sdk = new AndroidSdk(null, null);
            sdk.setSdkToolsVersion(version);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sdk;
    }

}
package hudson.plugins.android_emulator;

import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.plugins.android_emulator.AndroidEmulator.HardwareProperty;
import hudson.plugins.android_emulator.sdk.AndroidSdk;
import hudson.plugins.android_emulator.sdk.Tool;
import hudson.plugins.android_emulator.util.Utils;
import hudson.remoting.Callable;
import hudson.util.ArgumentListBuilder;
import hudson.util.StreamCopyThread;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.PushbackInputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.regex.Pattern;
import jenkins.security.MasterToSlaveCallable;

class EmulatorConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private String avdName;
    private EmulatorVersion emulatorVersion;
    private AvdDevice avdDevice;
    private String deviceLocale;
    private String sdCardSize;
    private boolean wipeData;
    private final boolean showWindow;
    private final String commandLineOptions;
    private final String androidSdkHome;
    private final String executable;
    private final String avdNameSuffix;

    private EmulatorConfig(String avdName, boolean wipeData, boolean showWindow, String commandLineOptions, String androidSdkHome, String executable, String
            avdNameSuffix) {
        this.avdName = avdName;
        this.wipeData = wipeData;
        this.showWindow = showWindow;
        this.commandLineOptions = commandLineOptions;
        this.androidSdkHome = androidSdkHome;
        this.executable = executable;
        this.avdNameSuffix = avdNameSuffix;
    }

    private EmulatorConfig(String osVersion, String avdDevice, 
            String deviceLocale, String sdCardSize, boolean wipeData, boolean showWindow, String commandLineOptions, String targetAbi, String androidSdkHome,
            String executable, String avdNameSuffix)
                throws IllegalArgumentException {
        if (osVersion == null || avdDevice == null ) {
            throw new IllegalArgumentException("Valid OS version and device properties must be supplied.");
        }

        // Normalise incoming variables
        int targetLength = osVersion.length();
        if (targetLength > 2 && osVersion.startsWith("\"") && osVersion.endsWith("\"")) {
            osVersion = osVersion.substring(1, targetLength - 1);
        }
        
        this.avdDevice = AvdDevice.valueOf(avdDevice.toUpperCase());

        if (deviceLocale != null && deviceLocale.length() > 4) {
            deviceLocale = deviceLocale.substring(0, 2).toLowerCase() +"_"
                + deviceLocale.substring(3).toUpperCase();
        }

        this.emulatorVersion = new EmulatorVersion(osVersion, targetAbi);
        if (this.emulatorVersion == null) {
            throw new IllegalArgumentException(
                    "OS version not recognised: " + osVersion);
        }
        
        this.deviceLocale = deviceLocale;
        this.sdCardSize = sdCardSize;
        this.wipeData = wipeData;
        this.showWindow = showWindow;
        this.commandLineOptions = commandLineOptions;
        if (targetAbi != null && targetAbi.startsWith("default/")) {
            targetAbi = targetAbi.replace("default/", "");
        }
    
        this.androidSdkHome = androidSdkHome;
        this.executable = executable;
        this.avdNameSuffix = avdNameSuffix;
    }

    public static final EmulatorConfig create(String avdName, String osVersion, String avdDevice,
            String deviceLocale, String sdCardSize, boolean wipeData,
            boolean showWindow, String commandLineOptions, String targetAbi,
            String androidSdkHome, String executable, String avdNameSuffix) {
        if (Util.fixEmptyAndTrim(avdName) == null) {
            return new EmulatorConfig(osVersion, avdDevice, deviceLocale, sdCardSize, wipeData,
                    showWindow, commandLineOptions, targetAbi, androidSdkHome, executable, avdNameSuffix);
        }

        return new EmulatorConfig(avdName, wipeData, showWindow, commandLineOptions, androidSdkHome, executable,
                avdNameSuffix);
    }

    public static final String getAvdName(String avdName, String osVersion, String avdDevice, String deviceLocale, String targetAbi, String avdNameSuffix) {
        try {
            return create(avdName, osVersion, avdDevice, deviceLocale, null, false, false,
                    null, targetAbi, null, null, avdNameSuffix).getAvdName();
        } catch (IllegalArgumentException e) {}
        return null;
    }

    public boolean isNamedEmulator() {
        return avdName != null && emulatorVersion == null;
    }

    public String getAvdName() {
        if (isNamedEmulator()) {
            return avdName;
        }

        return getGeneratedAvdName();
    }

    public String getEmulatorVersionString() {
    	return emulatorVersion.getEmulatorVersionString();
    }
    
    public AndroidPlatform getEumulatorOSPlatform() {
    	return emulatorVersion.getOSPlatform();
    }
    
    private String getGeneratedAvdName() {
        String locale = getDeviceLocale().replace('_', '-');
        String platform = getEmulatorVersionString().replaceAll("[^a-zA-Z0-9._-]", "_");
        String device = getDeviceString().replaceAll("[^a-zA-Z0-9._-]", "_");
        String suffix = "";
        if (avdNameSuffix != null) {
            suffix = "_" + avdNameSuffix.replaceAll("[^a-zA-Z0-9._-]", "-");
        }

        return String.format("hudson_%s_%s_%s%s", locale, device,  platform, suffix);
    }

    public String getDeviceString() {
    	return avdDevice.name();
    }



    public String getDeviceLocale() {
        if (deviceLocale == null) {
            return Constants.DEFAULT_LOCALE;
        }
        return deviceLocale;
    }

    public String getDeviceLanguage() {
        return getDeviceLocale().substring(0, 2);
    }

    public String getDeviceCountry() {
        return getDeviceLocale().substring(3);
    }

    public String getSdCardSize() {
        return sdCardSize;
    }

    public void setShouldWipeData() {
        wipeData = true;
    }

    public boolean shouldWipeData() {
        return wipeData;
    }

    public boolean shouldShowWindow() {
        return showWindow;
    }

    public Tool getExecutable() {
        for (Tool t : Tool.EMULATORS) {
            if (t.executable.equals(executable)) {
                return t;
            }
        }
        return Tool.EMULATOR;
    }

    /**
     * Gets a task that ensures that an Android AVD exists for this instance's configuration.
     *
     * @param androidSdk  The Android SDK to use.
     * @param listener The listener to use for logging.
     * @return A Callable that will handle the detection/creation of an appropriate AVD.
     */
    public Callable<Boolean, AndroidEmulatorException> getEmulatorCreationTask(AndroidSdk androidSdk,
                                                                               BuildListener listener) {
        return new EmulatorCreationTask(androidSdk, listener);
    }

    /**
     * Gets a task that updates the hardware properties of the AVD for this instance.
     *
     *
     * @param hardwareProperties  The hardware properties to update the AVD with.
     * @param listener The listener to use for logging.
     * @return A Callable that will update the config of the current AVD.
     */
    public Callable<Void, IOException> getEmulatorConfigTask(HardwareProperty[] hardwareProperties,
                                                             BuildListener listener, AndroidSdk sdk) {
        return new EmulatorConfigTask(hardwareProperties, listener, sdk);
    }

    /**
     * Gets a task that writes an empty emulator auth file to the machine where the AVD will run.
     *
     * @return A Callable that will write an empty auth file.
     */
    public Callable<Void, IOException> getEmulatorAuthFileTask() {
        return new EmulatorAuthFileTask();
    }

    /**
     * Gets a task that deletes the AVD corresponding to this instance's configuration.
     *
     *
     * @param listener The listener to use for logging.
     * @return A Callable that will delete the AVD with for this configuration.
     */
    public Callable<Boolean, Exception> getEmulatorDeletionTask(TaskListener listener) {
        return new EmulatorDeletionTask(listener);
    }

    private File getAvdHome(final String homeDir) {
        return new File(homeDir, ".android/avd/");
    }
    
    private File getAvdHome(final File homeDir) {
        return new File(homeDir, ".android/avd/");
    }

    private Map<String,String>  parseAvdConfigFile(String homeDir, String reason) throws IOException {
    	return parseAvdConfigFile(new File(homeDir), reason);
    }
    
    private File getAvdDirectory(final File homeDir) {
    	if(androidSdkHome == null) {
    		return new File(getAvdHome(homeDir), getAvdName() +".avd");
    	} else {
    		return getAvdHome(homeDir);
    	}
    }

    public File getAvdMetadataFile() {
        final File homeDir = Utils.getHomeDirectory(androidSdkHome);
        return new File(getAvdHome(homeDir), getAvdName() + ".ini");
    }

    private File getAvdConfigFile(File homeDir) {
		return new File(getAvdDirectory(homeDir), "config.ini");
    }

    private Map<String,String> parseAvdConfigFile(File homeDir, String reason) throws IOException {
        File configFile = getAvdConfigFile(homeDir);
        return Utils.parseConfigFile(configFile, reason);
    }

    private void writeAvdConfigFile(String homeDir, Map<String,String> values) throws FileNotFoundException {
    	writeAvdConfigFile(new File(homeDir), values);
    }
    
    private void writeAvdConfigFile(File homeDir, Map<String,String> values) throws FileNotFoundException {
        StringBuilder sb = new StringBuilder();

        for (String key : values.keySet()) {
            sb.append(key);
            sb.append("=");
            sb.append(values.get(key));
            sb.append("\r\n");
        }

        File configFile = new File(getAvdDirectory(homeDir), "config.ini");
        PrintWriter out = new PrintWriter(configFile);
        out.print(sb.toString());
        out.flush();
        out.close();
    }

    /**
     * Sets or overwrites a key-value pair in the AVD config file.
     *
     * @param homeDir AVD home directory.
     * @param key Key to set.
     * @param value Value to set.
     * @throws EmulatorCreationException If reading or writing the file failed.
     */
    private void setAvdConfigValue(String homeDir, String key, String value, PrintStream logger)
            throws EmulatorCreationException {
        Map<String, String> configValues;
        try {
        	String reason = "Waylon - Adding KEY:" + key + " VALUE:" + value + " File:" + homeDir;
        	AndroidEmulator.log(logger, reason);
            configValues = parseAvdConfigFile(homeDir, reason);
            configValues.put(key, value);
            writeAvdConfigFile(homeDir, configValues);
        } catch (IOException e) {
            throw new EmulatorCreationException(Messages.AVD_CONFIG_NOT_READABLE(), e);
        }
    }

    /**
     * Gets the command line arguments to pass to "emulator" based on this instance.
     *
     * @return A string of command line arguments.
     */
    public String getCommandArguments(int userPort, int adbPort, int callbackPort,
            int consoleTimeout) {
        StringBuilder sb = new StringBuilder();

        // Tell the emulator to use certain ports
        sb.append(String.format(" -ports %s,%s", userPort, adbPort));

        // Ask the emulator to report to us on the given port, once initial startup is complete
        sb.append(String.format(" -report-console tcp:%s,max=%s", callbackPort, consoleTimeout));

        // Set the locale to be used at startup
        if (!isNamedEmulator()) {
            sb.append(" -prop persist.sys.language=");
            sb.append(getDeviceLanguage());
            sb.append(" -prop persist.sys.country=");
            sb.append(getDeviceCountry());
        }

        // Set the ID of the AVD we want to start
        sb.append(" -avd ");
        sb.append(getAvdName());

        
		sb.append(" -no-snapshot ");

        // Options
        if (shouldWipeData()) {
            sb.append(" -wipe-data");
        }
        if (!shouldShowWindow()) {
            sb.append(" -no-window");
        }
        if (commandLineOptions != null) {
            sb.append(" ");
            sb.append(commandLineOptions);
        }

        return sb.toString();
    }

    /**
     * A task that locates or creates an AVD based on our local state.
     *
     * Returns {@code TRUE} if an AVD already existed with these properties, otherwise returns
     * {@code FALSE} if an AVD was newly created, and throws an AndroidEmulatorException if the
     * given AVD or parts required to generate a new AVD were not found.
     */
    private final class EmulatorCreationTask extends MasterToSlaveCallable<Boolean, AndroidEmulatorException> implements Serializable {

        private static final long serialVersionUID = 1L;
        private final AndroidSdk androidSdk;

        private final BuildListener listener;
        private transient PrintStream logger;

        public EmulatorCreationTask(AndroidSdk androidSdk, BuildListener listener) {
            this.androidSdk = androidSdk;
            this.listener = listener;
        }

        public Boolean call() throws AndroidEmulatorException {
            if (logger == null) {
                logger = listener.getLogger();
            }

            final File homeDir = Utils.getHomeDirectory(androidSdk.getSdkHome());
            final File avdDirectory = getAvdDirectory(homeDir);
            
            try {
            	AndroidEmulator.log(logger, "Waylon - homeDir:"  + homeDir.getCanonicalPath() + " avdDirectory:" + avdDirectory.getCanonicalPath());
            } catch (Exception e) {
            	AndroidEmulator.log(logger, "Waylon - PROBLEM!!!! COULD NOT GET homeDir OR avdDirectory");
            }
            final boolean emulatorExists = getAvdConfigFile(homeDir).exists();

            // Can't do anything if a named emulator doesn't exist
            if (isNamedEmulator() && !emulatorExists) {
                throw new EmulatorDiscoveryException(Messages.AVD_DOES_NOT_EXIST(avdName, avdDirectory));
            }

            // Check whether AVD needs to be created
            boolean createSdCard = false;
            if (emulatorExists) {
                // AVD exists: check whether there's anything still to be set up
                File sdCardFile = new File(getAvdDirectory(homeDir), "sdcard.img");
                boolean sdCardRequired = getSdCardSize() != null;

                // Flag that we need to generate an SD card, if there isn't one existing
                if (sdCardRequired && !sdCardFile.exists()) {
                    createSdCard = true;
                }

                // If everything is ready, then return
                if (!createSdCard) {
                    return true;
                }
            } else {
                AndroidEmulator.log(logger, Messages.CREATING_AVD(avdDirectory));
            }

            // We can't continue if we don't know where to find emulator images or tools
            if (!androidSdk.hasKnownRoot()) {
                throw new EmulatorCreationException(Messages.SDK_NOT_SPECIFIED());
            }
            final File sdkRoot = new File(androidSdk.getSdkRoot());
            if (!sdkRoot.exists()) {
                throw new EmulatorCreationException(Messages.SDK_NOT_FOUND(androidSdk.getSdkRoot()));
            }

            // If we need create an SD card for an existing emulator, do so
            if (createSdCard) {
                AndroidEmulator.log(logger, Messages.ADDING_SD_CARD(sdCardSize, getAvdName()));
                if (!createSdCard(homeDir)) {
                    throw new EmulatorCreationException(Messages.SD_CARD_CREATION_FAILED());
                }

                // Update the AVD config file
                setAvdConfigValue(androidSdkHome, "sdcard.size", sdCardSize, logger);
            }

            // Return if everything is now ready for use
            if (emulatorExists) {
                return true;
            }

            // Build up basic arguments to `android` command
            final StringBuilder args = new StringBuilder(100);
            args.append("create avd ");

            args.append(" -n ");
            args.append(getAvdName());
            
            String quotedVersionString = "\"" + emulatorVersion.getEmulatorVersionString() + "\"";
            AndroidEmulator.log(logger, "Waylon - quoted version: " + quotedVersionString);
            args.append(" -k ");
            args.append(emulatorVersion.getEmulatorVersionString());
            
            // set location of avd directory
            if(androidSdkHome != null) {
            	File avdLoc = getAvdHome(androidSdkHome);
            	String avdLocStr = avdLoc.getAbsolutePath();
            	args.append(" -p ");
            	args.append(avdLocStr);
            }

            if (sdCardSize != null) {
                args.append(" -c ");
                args.append(sdCardSize);
            }
         
            // Overwrite any existing files
            args.append(" -f ");
            
            boolean isUnix = !Functions.isWindows();
            ArgumentListBuilder builder = Utils.getToolCommand(androidSdk, isUnix, Tool.AVDMANAGER, args.toString());

            // Log command line used, for info
            AndroidEmulator.log(logger, builder.toStringWithQuote());

            for(String arg: builder.toList()) {
            	AndroidEmulator.log(logger, "Waylon - arg: " + arg);
            }
            
            // Run!
            boolean avdCreated = false;
            final Process process;
            try {
                ProcessBuilder procBuilder = new ProcessBuilder(builder.toList());
                for(String cmd: procBuilder.command()) {
                	AndroidEmulator.log(logger, "Waylon - commands: " + cmd);
                }
                
                String fullcommand = "";
                for(String cmd: procBuilder.command()) {
                	fullcommand = fullcommand + " " + cmd;
                }
                
                AndroidEmulator.log(logger, "Waylon - full commands: " + fullcommand);
                
                if (androidSdk.hasKnownHome()) {
                    procBuilder.environment().put("ANDROID_SDK_HOME", androidSdk.getSdkHome());
                }
                process = procBuilder.start();
            } catch (IOException ex) {
                throw new EmulatorCreationException(Messages.AVD_CREATION_FAILED());
            }

            // Redirect process's stderr to a stream, for logging purposes
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            new StreamCopyThread("", process.getErrorStream(), stderr).start();

            try {
            	Thread.sleep(1000);
            } catch(InterruptedException e) {
            	// do nothing
            }
            
            // Command may prompt us whether we want to further customise the AVD.
            // Just "press" Enter to continue with the selected target's defaults.
            try {
                boolean processAlive = true;

                // Block until the command outputs something (or process ends)
                final PushbackInputStream in = new PushbackInputStream(process.getInputStream(), 10);
                int len = in.read();
                if (len == -1) {
                	AndroidEmulator.log(logger, "Waylon - length is -1");
                    // Check whether the process has exited badly, as sometimes no output is valid.
                    // e.g. When creating an AVD with Google APIs, no user input is requested.
                    if (process.waitFor() != 0) {
                        AndroidEmulator.log(logger, Messages.AVD_CREATION_FAILED());
                        String errOutput = stderr.toString();
                        String output = stdout.toString();
                        
                        AndroidEmulator.log(logger, "Waylon - Output:" + output);
                        AndroidEmulator.log(logger, "Waylon - ErrOutput:" + errOutput);
                        throw new EmulatorCreationException(Messages.AVD_CREATION_FAILED());
                    }
                    processAlive = false;
                }
                in.unread(len);
                AndroidEmulator.log(logger, "Waylon - Before return");
                // Write CRLF, if required
                if (processAlive) {
                    AndroidEmulator.log(logger, "Waylon - Issue return");
                    final OutputStream stream = process.getOutputStream();
                    stream.write('\r');
                    stream.write('\n');
                    stream.flush();
                    stream.close();
                }
                AndroidEmulator.log(logger, "Waylon - AFTER return");
                // read the rest of stdout (for debugging purposes)
                Util.copyStream(in, stdout);
                in.close();

                // Wait for happy ending
                if (process.waitFor() == 0) {
                    // Do a sanity check to ensure the AVD was really created
                    avdCreated = getAvdConfigFile(homeDir).exists();
                    
                    // Update the AVD config file with bigger internal storage if emulator is 5 or 5.1
                    AndroidPlatform os = emulatorVersion.getOSPlatform();
                    if(AndroidPlatform.SDK_5_0.equals(os) || AndroidPlatform.SDK_5_1.equals(os)) {                    		
                    	setAvdConfigValue(androidSdkHome, "disk.dataPartition.size", "1024", logger);
                    }
                }

            } catch (IOException e) {
                throw new EmulatorCreationException(Messages.AVD_CREATION_ABORTED(), e);
            } catch (InterruptedException e) {
                throw new EmulatorCreationException(Messages.AVD_CREATION_INTERRUPTED(), e);
            } finally {
                process.destroy();
            }

            // For reasons unknown, the return code may not be correctly reported on Windows.
            // So check whether stderr contains failure info (useful for other platforms too).
            String errOutput = stderr.toString();
            String output = stdout.toString();
            
            AndroidEmulator.log(logger, "Waylon - Output:" + output);
            AndroidEmulator.log(logger, "Waylon - ErrOutput:" + errOutput);

            
            if (errOutput.contains("Error: Package path is not valid")) {
                AndroidEmulator.log(logger, Messages.INVALID_AVD_TARGET(emulatorVersion.getEmulatorVersionString()));
                AndroidEmulator.log(logger, errOutput);
                avdCreated = false;
                errOutput = null;
            } else if (output.contains("Error: Package path is not valid")) {
                AndroidEmulator.log(logger, Messages.MORE_THAN_ONE_ABI(emulatorVersion.getEmulatorVersionString(), output), true);
                AndroidEmulator.log(logger, output);
                avdCreated = false;
                errOutput = null;
            }

            
            // Check everything went ok
            if (!avdCreated) {
                if (errOutput != null && errOutput.length() != 0) {
                    AndroidEmulator.log(logger, stderr.toString(), true);
                }
                throw new EmulatorCreationException(Messages.AVD_CREATION_FAILED());
            }

            // Done!
            return false;
        }

        private boolean createSdCard(File homeDir) {
            // Build command: mksdcard 32M /home/foo/.android/avd/whatever.avd/sdcard.img
            ArgumentListBuilder builder = Utils.getToolCommand(androidSdk, !Functions.isWindows(), Tool.MKSDCARD, null);
            builder.add(sdCardSize);
            builder.add(new File(getAvdDirectory(homeDir), "sdcard.img"));

            // Run!
            try {
                ProcessBuilder procBuilder = new ProcessBuilder(builder.toList());
                if (androidSdkHome != null) {
                    procBuilder.environment().put("ANDROID_SDK_HOME", androidSdkHome);
                }
                procBuilder.start().waitFor();
            } catch (InterruptedException ex) {
                return false;
            } catch (IOException ex) {
                return false;
            }

            return true;
        }
    }

    /**
     * A task that updates the hardware properties of this AVD config.
     *
     * Throws an IOException if the AVD's config could not be read or written.
     */
    private final class EmulatorConfigTask extends MasterToSlaveCallable<Void, IOException> {

        private static final long serialVersionUID = 1L;

        private final HardwareProperty[] hardwareProperties;
        private final BuildListener listener;
        private final AndroidSdk androidSdk;
        private transient PrintStream logger;

        public EmulatorConfigTask(HardwareProperty[] hardwareProperties, BuildListener listener, AndroidSdk androidSdk) {
            this.hardwareProperties = hardwareProperties;
            this.listener = listener;
            this.androidSdk = androidSdk;
        }

        public Void call() throws IOException {
            if (logger == null) {
                logger = listener.getLogger();
            }

            final File homeDir = Utils.getHomeDirectory(androidSdkHome);

            // Parse the AVD's config
            Map<String, String> configValues;
            configValues = parseAvdConfigFile(homeDir, "Setting hardware properties directly from jenkins project configure");
            
            Map<String, String> avdProperties = avdDevice.getDeviceSpecs();
            configValues.putAll(avdProperties);
            
            AndroidEmulator.log(logger, "Waylon - Avd Device is:" + avdDevice.toString());
            for(Map.Entry<String, String> entry : avdProperties.entrySet()) {
            	AndroidEmulator.log(logger, String.format("Device Prop: %s: %s", entry.getKey(), entry.getValue()));
            }
            
            // Insert any hardware properties we want to override
            AndroidEmulator.log(logger, Messages.SETTING_HARDWARE_PROPERTIES());
            for (HardwareProperty prop : hardwareProperties) {
                AndroidEmulator.log(logger, String.format("%s: %s", prop.key, prop.value));
                configValues.put(prop.key, prop.value);
            }

            // Update config file
            writeAvdConfigFile(homeDir, configValues);

            return null;
        }
    }

    /** Writes an empty emulator auth file. */
    private final class EmulatorAuthFileTask extends MasterToSlaveCallable<Void, IOException> {

        private static final long serialVersionUID = 1L;

        public Void call() throws IOException {
            // Create an empty auth file to prevent the emulator telnet interface from requiring authentication
            final File userHome = Utils.getHomeDirectory();
            if (userHome != null) {
                try {
                    FilePath authFile = new FilePath(userHome).child(".emulator_console_auth_token");
                    authFile.write("", "UTF-8");
                } catch (IOException e) {
                    throw new IOException(String.format("Failed to write auth file to %s.", userHome, e));
                } catch (InterruptedException e) {
                    throw new IOException(String.format("Interrupted while writing auth file to %s.", userHome, e));
                }
            }

            return null;
        }

    }

    /** A task that deletes the AVD corresponding to our local state. */
    private final class EmulatorDeletionTask extends MasterToSlaveCallable<Boolean, Exception> {

        private static final long serialVersionUID = 1L;

        private final TaskListener listener;
        private transient PrintStream logger;

        public EmulatorDeletionTask(TaskListener listener) {
            this.listener = listener;
        }

        public Boolean call() throws Exception {
            if (logger == null) {
                logger = listener.getLogger();
            }

            // Check whether the AVD exists
            final File homeDir = Utils.getHomeDirectory(androidSdkHome);
            final File avdDirectory = getAvdDirectory(homeDir);
            final boolean emulatorExists = avdDirectory.exists();
            if (!emulatorExists) {
                AndroidEmulator.log(logger, Messages.AVD_DIRECTORY_NOT_FOUND(avdDirectory));
                return false;
            }

            // Recursively delete the contents
            new FilePath(avdDirectory).deleteRecursive();

            // Delete the metadata file
            getAvdMetadataFile().delete();

            // Success!
            return true;
        }

    }

}

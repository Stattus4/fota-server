package fotaserver.manifest;

import fotaserver.manifest.ManifestComponentLoadFirmware.LoadFirmware;
import fotaserver.manifest.ManifestComponentUpdateConfig.UpdateConfig;
import fotaserver.manifest.ManifestWrapper.Manifest;

public class ManifestFactory {

	public static ManifestWrapper get(String device) {
		ManifestWrapper manifestWrapper = new ManifestWrapper();

		Manifest manifest = manifestWrapper.getManifest();
		manifest.setVersion(1);
		manifest.setSequenceNumber(1);

		ManifestComponentUpdateConfig manifestComponentUpdateConfig = new ManifestComponentUpdateConfig();

		UpdateConfig updateConfig = manifestComponentUpdateConfig.getUpdateConfig();
		updateConfig.setSamplingRate(900);
		updateConfig.setUpdateRate(900);

		manifest.addComponent(manifestComponentUpdateConfig);

		ManifestComponentLoadFirmware manifestComponentLoadFirmware = new ManifestComponentLoadFirmware();

		LoadFirmware loadFirmware = manifestComponentLoadFirmware.getLoadFirmware();
		loadFirmware.setUri("https://example.com/firmware_v1.1.bin");
		loadFirmware.setImageSize(123456);

		manifest.addComponent(manifestComponentLoadFirmware);

		return manifestWrapper;
	}
}

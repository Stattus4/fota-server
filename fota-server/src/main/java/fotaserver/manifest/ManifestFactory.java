package fotaserver.manifest;

public class ManifestFactory {

	public ManifestWrapper get(String device) {
		Manifest manifest = new Manifest();

		manifest.setVersion(1);
		manifest.setSequenceNumber(1);

		return new ManifestWrapper(manifest);
	}
}

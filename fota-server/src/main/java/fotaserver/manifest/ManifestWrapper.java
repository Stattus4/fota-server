package fotaserver.manifest;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ManifestWrapper {

	private final Manifest manifest;

	public ManifestWrapper(Manifest manifest) {
		this.manifest = manifest;
	}

	@JsonProperty("manifest")
	public Manifest getManifest() {
		return manifest;
	}
}

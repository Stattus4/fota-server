package fotaserver.manifest;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ManifestComponentLoadFirmware {

	@JsonProperty("load-firmare")
	private final LoadFirmware loadFirmware = new LoadFirmware();

	public LoadFirmware getLoadFirmware() {
		return loadFirmware;
	}

	public static class LoadFirmware {

		@JsonProperty("image-size")
		private int imageSize;

		@JsonProperty("uri")
		private String uri;

		public void setImageSize(int imageSize) {
			this.imageSize = imageSize;
		}

		public void setUri(String uri) {
			this.uri = uri;
		}
	}
}

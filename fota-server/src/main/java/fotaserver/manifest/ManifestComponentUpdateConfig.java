package fotaserver.manifest;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ManifestComponentUpdateConfig {

	@JsonProperty("update-config")
	private final UpdateConfig updateConfig = new UpdateConfig();

	public UpdateConfig getUpdateConfig() {
		return updateConfig;
	}

	public static class UpdateConfig {

		@JsonProperty("sampling-rate")
		private int samplingRate;

		@JsonProperty("update-rate")
		private int updateRate;

		public void setSamplingRate(int samplingRate) {
			this.samplingRate = samplingRate;
		}

		public void setUpdateRate(int updateRate) {
			this.updateRate = updateRate;
		}
	}
}

package fotaserver.manifest;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Manifest {

	@JsonProperty("version")
	private int version;

	@JsonProperty("sequence-number")
	private int sequenceNumber;

	private List<Object> components = new ArrayList<>();

	public void addComponent(Object component) {
		this.components.add(component);
	}

	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	@JsonProperty("components")
	public List<Object> getComponents() {
		return components;
	}
}

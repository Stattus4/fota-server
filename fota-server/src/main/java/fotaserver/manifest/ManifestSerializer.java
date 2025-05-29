package fotaserver.manifest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

public class ManifestSerializer {

	private final ObjectMapper cborMapper = new ObjectMapper(new CBORFactory());
	private final ObjectMapper jsonMapper = new ObjectMapper();

	public byte[] serialize(Object o, Format format) throws JsonProcessingException {
		if (format == Format.CBOR) {
			return cborMapper.writeValueAsBytes(o);
		} else if (format == Format.JSON) {
			return jsonMapper.writeValueAsBytes(o);
		}

		throw new RuntimeException();
	}

	public enum Format {
		CBOR, JSON;

		public static Format fromString(String format) {
			if (format == null) {
				return CBOR;
			}

			try {
				return Format.valueOf(format.toUpperCase());
			} catch (IllegalArgumentException e) {
				return CBOR;
			}
		}
	}
}

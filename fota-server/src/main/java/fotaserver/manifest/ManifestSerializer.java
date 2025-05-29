package fotaserver.manifest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

public class ManifestSerializer {

	public static byte[] serialize(Object o, Format format) throws JsonProcessingException {
		ObjectMapper mapper;

		if (format == Format.JSON) {
			mapper = new ObjectMapper();

			return mapper.writeValueAsBytes(o);
		}

		mapper = new ObjectMapper(new CBORFactory());

		return mapper.writeValueAsBytes(o);
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

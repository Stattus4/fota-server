package fotaserver.server.resources;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.UriQueryParameter;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fotaserver.manifest.ManifestFactory;
import fotaserver.manifest.ManifestSerializer;
import fotaserver.manifest.ManifestSerializer.Format;
import fotaserver.manifest.ManifestWrapper;

public class ResourceManifest extends CoapResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(ResourceManifest.class);

	public ResourceManifest(String name) {
		super(name);

		getAttributes().setTitle(this.getClass().getSimpleName());

		LOGGER.info("CoapResource added");
	}

	@Override
	public void handleGET(CoapExchange exchange) {
		try {
			UriQueryParameter uriQueryParameter = exchange.getRequestOptions().getUriQueryParameter();
			String device = uriQueryParameter.getArgument("device");

			String formatString = uriQueryParameter.getArgument("format", "cbor");
			Format format = Format.fromString(formatString);

			ManifestWrapper manifestWrapper = ManifestFactory.get(device);

			byte[] payload = ManifestSerializer.serialize(manifestWrapper, format);

			int mediaType = (format == Format.JSON) ? MediaTypeRegistry.APPLICATION_JSON
					: MediaTypeRegistry.APPLICATION_CBOR;

			LOGGER.info("Success - SourceContext: {} RequestCode: {} RequestOptions: {} RequestPayloadSize: {}",
					exchange.getSourceContext().toString(), exchange.getRequestCode(),
					exchange.getRequestOptions().toString(), exchange.getRequestPayloadSize());

			exchange.respond(ResponseCode.CONTENT, payload, mediaType);
		} catch (Exception e) {
			LOGGER.info("Error - SourceContext: {} RequestCode: {} RequestOptions: {} ExceptionMessage: {}",
					exchange.getSourceContext().toString(), exchange.getRequestCode(),
					exchange.getRequestOptions().toString(), e.getMessage());

			exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR, "Internal server error.");
		}
	}
}

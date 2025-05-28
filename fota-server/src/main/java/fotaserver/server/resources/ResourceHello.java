package fotaserver.server.resources;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceHello extends CoapResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(ResourceHello.class);

	public ResourceHello(String name) {
		super(name);

		getAttributes().setTitle(this.getClass().getSimpleName());

		LOGGER.info("CoapResource added");
	}

	@Override
	public void handleGET(CoapExchange exchange) {
		try {
			LOGGER.info("Success - SourceContext: {} RequestCode: {} RequestOptions: {} RequestPayloadSize: {}",
					exchange.getSourceContext().toString(), exchange.getRequestCode(),
					exchange.getRequestOptions().toString(), exchange.getRequestPayloadSize());

			exchange.respond(ResponseCode.CONTENT, "Hello!");
		} catch (Exception e) {
			LOGGER.info("Error - SourceContext: {} RequestCode: {} RequestOptions: {} ExceptionMessage: {}",
					exchange.getSourceContext().toString(), exchange.getRequestCode(),
					exchange.getRequestOptions().toString(), e.getMessage());

			exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR, "Internal server error.");
		}
	}
}

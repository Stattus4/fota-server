package fotaserver.server;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.server.resources.MyIpResource;
import org.eclipse.californium.elements.config.CertificateAuthenticationMode;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.Configuration.DefinitionsProvider;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.MdcConnectionListener;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.californium.scandium.config.DtlsConfig.DtlsRole;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fotaserver.server.resources.ResourceHello;
import fotaserver.server.util.CredentialsUtil;
import fotaserver.server.util.CredentialsUtil.Mode;

public class CfSecureServer extends CoapServer {

	private static final Logger LOGGER = LoggerFactory.getLogger(CfSecureServer.class);

	private static final File CONFIG_FILE = new File("Californium3.properties");
	private static final String CONFIG_HEADER = "Californium CoAP Properties";

	private static DefinitionsProvider DEFAULTS = new DefinitionsProvider() {

		@Override
		public void applyDefinitions(Configuration config) {
			config.set(DtlsConfig.DTLS_ROLE, DtlsRole.SERVER_ONLY);
			config.set(DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY, false);
			config.set(DtlsConfig.DTLS_PRESELECTED_CIPHER_SUITES, CipherSuite.STRONG_ENCRYPTION_PREFERENCE);
		}
	};

	public static final List<Mode> SUPPORTED_MODES = Arrays.asList(Mode.PSK, Mode.ECDHE_PSK, Mode.RPK, Mode.X509,
			Mode.WANT_AUTH, Mode.NO_AUTH);

	static {
		CoapConfig.register();
		DtlsConfig.register();
	}

	public static void main(String[] args) {
		try {
			LOGGER.info("Starting CfSecureServer ...");

			Configuration configuration = Configuration.createWithFile(CONFIG_FILE, CONFIG_HEADER, DEFAULTS);
			Configuration.setStandard(configuration);

			int coapSecurePort = configuration.get(CoapConfig.COAP_SECURE_PORT);

			DtlsConnectorConfig.Builder builder = DtlsConnectorConfig.builder(configuration)
					.setAddress(new InetSocketAddress(coapSecurePort));

			// CredentialsUtil.setupCid(new String[] { "CID:6" }, builder);

			List<CredentialsUtil.Mode> modes = new ArrayList<>(
					Arrays.asList(CredentialsUtil.Mode.PSK, CredentialsUtil.Mode.X509));

			CredentialsUtil.setupCredentials(builder, CredentialsUtil.SERVER_NAME, modes);

			builder.set(DtlsConfig.DTLS_CLIENT_AUTHENTICATION_MODE, CertificateAuthenticationMode.NEEDED);
			builder.setConnectionListener(new MdcConnectionListener());

			DTLSConnector connector = new DTLSConnector(builder.build());

			CoapEndpoint.Builder coapBuilder = new CoapEndpoint.Builder().setConfiguration(configuration)
					.setConnector(connector);

			CfSecureServer server = new CfSecureServer();

			server.addResources();
			server.addEndpoint(coapBuilder.build());
			server.start();

			Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

			LOGGER.info("Started CfSecureServer");
		} catch (Exception e) {
			LOGGER.error("Failed to start CfSecureServer: {}", e.getMessage(), e);
		}
	}

	private void addResources() {
		tryAddResource(() -> new MyIpResource(MyIpResource.RESOURCE_NAME, true), MyIpResource.class.getName());
		tryAddResource(() -> new ResourceHello("hello"), ResourceHello.class.getName());
	}

	private void tryAddResource(Supplier<CoapResource> resourceSupplier, String resourceName) {
		try {
			add(resourceSupplier.get());
		} catch (Exception e) {
			LOGGER.error("Failed to add CoapResource [{}] ExceptionMessage: {}", resourceName, e.getMessage());
		}
	}
}

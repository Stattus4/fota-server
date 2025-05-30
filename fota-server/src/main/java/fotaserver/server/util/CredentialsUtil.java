package fotaserver.server.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.X509KeyManager;

import org.eclipse.californium.elements.config.CertificateAuthenticationMode;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.util.SslContextUtil;
import org.eclipse.californium.elements.util.SslContextUtil.Credentials;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite.KeyExchangeAlgorithm;
import org.eclipse.californium.scandium.dtls.pskstore.MultiPskStore;
import org.eclipse.californium.scandium.dtls.x509.KeyManagerCertificateProvider;
import org.eclipse.californium.scandium.dtls.x509.SingleCertificateProvider;
import org.eclipse.californium.scandium.dtls.x509.StaticCertificateVerifier;
import org.eclipse.californium.scandium.dtls.x509.StaticCertificateVerifier.Builder;

/**
 * Credentials utility for setup DTLS credentials.
 */
public class CredentialsUtil {

	private static final String FOTASERVER_SECURE_SERVER_PSK_IDENTITY_ENV = "FOTASERVER_SECURE_SERVER_PSK_IDENTITY";
	private static final String FOTASERVER_SECURE_SERVER_PSK_SECRET_ENV = "FOTASERVER_SECURE_SERVER_PSK_SECRET";

	/**
	 * Credentials mode.
	 */
	public enum Mode {
		/**
		 * Preshared secret keys.
		 */
		PSK,
		/**
		 * EC DHE, preshared secret keys.
		 */
		ECDHE_PSK,
		/**
		 * Raw public key certificates.
		 */
		RPK,
		/**
		 * X.509 certificates.
		 */
		X509,
		/**
		 * raw public key certificates just trusted (client only).
		 */
		RPK_TRUST,
		/**
		 * X.509 certificates just trusted (client only).
		 */
		X509_TRUST,
		/**
		 * Client authentication wanted (server only).
		 */
		WANT_AUTH,
		/**
		 * No client authentication (server only).
		 */
		NO_AUTH,
	}

	/**
	 * Default list of modes for clients.
	 * 
	 * Value is PSK, RPK, X509.
	 */
	public static final List<Mode> DEFAULT_CLIENT_MODES = Arrays.asList(Mode.PSK, Mode.RPK, Mode.X509);

	/**
	 * Default list of modes for servers.
	 * 
	 * Value is PSK, ECDHE_PSK, RPK, X509.
	 */
	public static final List<Mode> DEFAULT_SERVER_MODES = Arrays.asList(Mode.PSK, Mode.ECDHE_PSK, Mode.RPK, Mode.X509);

	// from ETSI Plugtest test spec
	public static final String PSK_IDENTITY;
	public static final byte[] PSK_SECRET;

	public static final String OPEN_PSK_IDENTITY = "Client_identity";
	public static final byte[] OPEN_PSK_SECRET = "secretPSK".getBytes();

	// CID
	public static final String OPT_CID = "CID:";
	public static final int DEFAULT_CID_LENGTH = 6;

	// from demo-certs
	public static final String SERVER_NAME = "server.*";
	public static final String CLIENT_NAME = "client";
	private static final String TRUST_NAME = "root";
	private static final char[] TRUST_STORE_PASSWORD = "rootPass".toCharArray();
	private static final char[] KEY_STORE_PASSWORD = "endPass".toCharArray();
	private static final String KEY_STORE_LOCATION = "certs/keyStore.jks";
	private static final String TRUST_STORE_LOCATION = "certs/trustStore.jks";

	private static final String[] OPT_CID_LIST = { OPT_CID };

	static {
		String pskIdentityEnv = System.getenv(FOTASERVER_SECURE_SERVER_PSK_IDENTITY_ENV);
		String pskSecretEnv = System.getenv(FOTASERVER_SECURE_SERVER_PSK_SECRET_ENV);

		if (pskIdentityEnv != null && !pskIdentityEnv.isEmpty()) {
			PSK_IDENTITY = pskIdentityEnv;
		} else {
			PSK_IDENTITY = "identity";
		}

		if (pskSecretEnv != null && !pskSecretEnv.isEmpty()) {
			PSK_SECRET = pskSecretEnv.getBytes(StandardCharsets.UTF_8);
		} else {
			PSK_SECRET = "qwerty".getBytes(StandardCharsets.UTF_8);
		}
	}

	/**
	 * Get opt-cid for argument.
	 * 
	 * @param arg command line argument
	 * @return opt-cid, of {@code null}, if argument is no opt-cid
	 */
	private static String getOptCid(String arg) {
		for (String opt : OPT_CID_LIST) {
			if (arg.startsWith(opt)) {
				return opt;
			}
		}
		return null;
	}

	/**
	 * Setup connection id configuration.
	 * 
	 * Supports "CID:length" for using CID after the handshake, "CID+:length" to use
	 * a CID even during the handshake.
	 * 
	 * @param args    command line arguments
	 * @param builder dtls configuration builder.
	 */
	public static void setupCid(String[] args, DtlsConnectorConfig.Builder builder) {
		for (String mode : args) {
			String opt = getOptCid(mode);
			if (opt != null) {
				String value = mode.substring(opt.length());
				int cidLength = DEFAULT_CID_LENGTH;
				try {
					cidLength = Integer.parseInt(value);
					if (cidLength < 0) {
						System.err.println("'" + value + "' is negative! Use cid-lenght default " + DEFAULT_CID_LENGTH);
						cidLength = DEFAULT_CID_LENGTH;
					}
				} catch (NumberFormatException e) {
					System.err.println("'" + value + "' is no number! Use cid-lenght default " + DEFAULT_CID_LENGTH);
				}
				builder.set(DtlsConfig.DTLS_CONNECTION_ID_LENGTH, cidLength);
				if (cidLength == 0) {
					System.out.println("Enable cid support");
				} else {
					System.out.println("Use " + cidLength + " bytes cid");
				}
			}
		}
	}

	/**
	 * Parse arguments to modes.
	 * 
	 * @param args      arguments
	 * @param defaults  default modes to use, if argument is empty or only contains
	 *                  {@link Mode#NO_AUTH}.
	 * @param supported supported modes
	 * @return array of modes.
	 */
	public static List<Mode> parse(String[] args, List<Mode> defaults, List<Mode> supported) {
		List<Mode> modes;
		if (args.length == 0) {
			modes = new ArrayList<>();
		} else {
			modes = new ArrayList<>(args.length);
			for (String mode : args) {
				if (getOptCid(mode) != null) {
					continue;
				}
				try {
					modes.add(Mode.valueOf(mode));
				} catch (IllegalArgumentException ex) {
					throw new IllegalArgumentException("Argument '" + mode + "' unkown!");
				}
			}
		}
		if (supported != null) {
			for (Mode mode : modes) {
				if (!supported.contains(mode)) {
					throw new IllegalArgumentException("Mode '" + mode + "' not supported!");
				}
			}
		}
		if (defaults != null) {
			if (modes.isEmpty()
					|| (modes.size() == 1 && (modes.contains(Mode.NO_AUTH) || modes.contains(Mode.WANT_AUTH)))) {
				// adjust defaults, also for only "NO_AUTH"
				modes.addAll(defaults);
			}
		}
		return modes;
	}

	/**
	 * Setup credentials for DTLS connector.
	 * 
	 * If PSK is provided and no PskStore is already set for the builder, a
	 * {@link MultiPskStore} containing {@link #PSK_IDENTITY} assigned with
	 * {@link #PSK_SECRET}, and {@link #OPEN_PSK_IDENTITY} assigned with
	 * {@link #OPEN_PSK_SECRET} set. If PSK is provided with other mode(s) and
	 * loading the certificates failed, this is just treated as warning and the
	 * configuration is setup to use PSK only.
	 * 
	 * If RPK is provided, the certificates loaded for the provided alias and this
	 * certificate is used as identity.
	 * 
	 * If X509 is provided, the trusts are also loaded an set additionally to the
	 * credentials for the alias.
	 * 
	 * The Modes can be mixed. If RPK is before X509 in the list, RPK is set as
	 * preferred.
	 * 
	 * Examples:
	 * 
	 * <pre>
	 * PSK, RPK setup for PSK an RPK.
	 * RPK, X509 setup for RPK and X509, prefer RPK
	 * PSK, X509, RPK setup for PSK, RPK and X509, prefer X509
	 * </pre>
	 * 
	 * @param config           DTLS configuration builder. May be already
	 *                         initialized with PskStore.
	 * @param certificateAlias alias for certificate to load as credentials.
	 * @param modes            list of supported mode. If a RPK is in the list
	 *                         before X509, or RPK is provided but not X509, then
	 *                         the RPK is setup as preferred.
	 * @throws IllegalArgumentException if loading the certificates fails for some
	 *                                  reason
	 */
	public static void setupCredentials(DtlsConnectorConfig.Builder config, String certificateAlias, List<Mode> modes) {

		boolean ecdhePsk = modes.contains(Mode.ECDHE_PSK);
		boolean plainPsk = modes.contains(Mode.PSK);
		boolean psk = ecdhePsk || plainPsk;

		if (psk && config.getIncompleteConfig().getPskStore() == null) {
			// Pre-shared secret keys
			MultiPskStore pskStore = new MultiPskStore();
			pskStore.setKey(PSK_IDENTITY, PSK_SECRET);
			pskStore.setKey(OPEN_PSK_IDENTITY, OPEN_PSK_SECRET);
			config.setPskStore(pskStore);
		}
		boolean noAuth = modes.contains(Mode.NO_AUTH);
		boolean x509Trust = modes.contains(Mode.X509_TRUST);
		boolean rpkTrust = modes.contains(Mode.RPK_TRUST);
		int x509 = modes.indexOf(Mode.X509);
		int rpk = modes.indexOf(Mode.RPK);
		boolean certificate = false;

		if (noAuth) {
			if (x509Trust) {
				throw new IllegalArgumentException(Mode.NO_AUTH + " doesn't support " + Mode.X509_TRUST);
			}
			if (rpkTrust) {
				throw new IllegalArgumentException(Mode.NO_AUTH + " doesn't support " + Mode.RPK_TRUST);
			}
			config.set(DtlsConfig.DTLS_CLIENT_AUTHENTICATION_MODE, CertificateAuthenticationMode.NONE);
		} else if (modes.contains(Mode.WANT_AUTH)) {
			config.set(DtlsConfig.DTLS_CLIENT_AUTHENTICATION_MODE, CertificateAuthenticationMode.WANTED);
		}
		Configuration configuration = config.getIncompleteConfig().getConfiguration();
		Builder trustBuilder = StaticCertificateVerifier.builder();
		if (x509 >= 0 || rpk >= 0) {
			try {
				// try to read certificates
				KeyManager[] credentials = SslContextUtil.loadKeyManager(
						SslContextUtil.CLASSPATH_SCHEME + KEY_STORE_LOCATION, certificateAlias, KEY_STORE_PASSWORD,
						KEY_STORE_PASSWORD);
				if (!noAuth) {
					if (x509 >= 0) {
						Certificate[] trustedCertificates = SslContextUtil.loadTrustedCertificates(
								SslContextUtil.CLASSPATH_SCHEME + TRUST_STORE_LOCATION, TRUST_NAME,
								TRUST_STORE_PASSWORD);
						trustBuilder.setTrustedCertificates(trustedCertificates);
					}
					if (rpk >= 0) {
						trustBuilder.setTrustAllRPKs();
					}
				}
				List<CertificateType> types = new ArrayList<>();
				if (x509 >= 0 && rpk >= 0) {
					if (rpk < x509) {
						types.add(CertificateType.RAW_PUBLIC_KEY);
						types.add(CertificateType.X_509);
					} else {
						types.add(CertificateType.X_509);
						types.add(CertificateType.RAW_PUBLIC_KEY);
					}
				} else if (x509 >= 0) {
					types.add(CertificateType.X_509);
				} else if (rpk >= 0) {
					types.add(CertificateType.RAW_PUBLIC_KEY);
				}
				X509KeyManager keyManager = SslContextUtil.getX509KeyManager(credentials);
				KeyManagerCertificateProvider certificateProvider = new KeyManagerCertificateProvider(keyManager,
						types);
				config.setCertificateIdentityProvider(certificateProvider);
				certificate = true;
			} catch (GeneralSecurityException e) {
				e.printStackTrace();
				System.err.println("certificates are invalid!");
				if (psk) {
					System.err.println("Therefore certificates are not supported!");
				} else {
					throw new IllegalArgumentException(e.getMessage());
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("certificates are missing!");
				if (psk) {
					System.err.println("Therefore certificates are not supported!");
				} else {
					throw new IllegalArgumentException(e.getMessage());
				}
			}
		}
		if (x509Trust) {
			// trust all
			trustBuilder.setTrustAllCertificates();
		}
		if (rpkTrust) {
			// trust all
			trustBuilder.setTrustAllRPKs();
		}
		if (trustBuilder.hasTrusts()) {
			certificate = true;
			config.setCertificateVerifier(trustBuilder.build());
		}
		List<CipherSuite> ciphers = configuration.get(DtlsConfig.DTLS_PRESELECTED_CIPHER_SUITES);
		List<CipherSuite> selectedCiphers = new ArrayList<>();
		for (CipherSuite cipherSuite : ciphers) {
			KeyExchangeAlgorithm keyExchange = cipherSuite.getKeyExchange();
			if (keyExchange == KeyExchangeAlgorithm.PSK) {
				if (plainPsk) {
					selectedCiphers.add(cipherSuite);
				}
			} else if (keyExchange == KeyExchangeAlgorithm.ECDHE_PSK) {
				if (ecdhePsk) {
					selectedCiphers.add(cipherSuite);
				}
			} else if (keyExchange == KeyExchangeAlgorithm.EC_DIFFIE_HELLMAN) {
				if (certificate) {
					selectedCiphers.add(cipherSuite);
				}
			}
		}
		configuration.set(DtlsConfig.DTLS_PRESELECTED_CIPHER_SUITES, selectedCiphers);
	}

	/**
	 * Load credentials from pem file.
	 * 
	 * Example to load x509 credentials from a pem file.
	 * 
	 * @param config      dtls configuration to set the identity from the file
	 * @param certificate file name
	 * @since 3.4
	 */
	public static void loadCredentials(DtlsConnectorConfig.Builder config, String certificate) {
		try {
			Credentials credentials = SslContextUtil.loadCredentials(certificate);
			SingleCertificateProvider identity = new SingleCertificateProvider(credentials.getPrivateKey(),
					credentials.getCertificateChain(), CertificateType.X_509);
			config.setCertificateIdentityProvider(identity);
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e.getMessage());
		}
	}
}

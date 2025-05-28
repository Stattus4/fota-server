package fotaserver.server;

public class ServerLauncher {

	private static final String FOTASERVER_SERVER_START_ENV = "FOTASERVER_SERVER_START";
	private static final String FOTASERVER_SECURE_SERVER_START_ENV = "FOTASERVER_SECURE_SERVER_START";

	public static void main(String[] args) {
		if (shouldStart(FOTASERVER_SERVER_START_ENV)) {
			startThread("CfServer", () -> CfServer.main(null));
		}

		if (shouldStart(FOTASERVER_SECURE_SERVER_START_ENV)) {
			startThread("CfSecureServer", () -> CfSecureServer.main(null));
		}
	}

	private static boolean shouldStart(String envVar) {
		String value = System.getenv(envVar);

		if (value == null) {
			return true;
		}

		return Boolean.parseBoolean(value);
	}

	private static void startThread(String threadName, Runnable target) {
		Thread thread = new Thread(() -> {
			try {
				target.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		thread.setName(threadName);
		thread.start();
	}
}

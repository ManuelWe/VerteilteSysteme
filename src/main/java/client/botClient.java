package client;

//sends random text to server in random intervals; closed when server down
public class botClient implements Runnable {
	Client client = null;

	public botClient(String serverAddress, WebClient webClient) {
		client = new Client(serverAddress, webClient, true);
	}

	@Override
	public void run() {
		for (int i = 0; i < 10; i++) {
			try {
				Thread.sleep(4000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			client.sendMessage("blabla" + i);
		}
	}
}

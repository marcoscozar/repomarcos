package practicachat;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CommunicationThreads extends Thread {

	private Socket socketClient;
	private ConcurrentHashMap<String, Player> players;
	private List<CommunicationThreads> threads;
	private String username;
	private String assignedColor;
	private PrintWriter writer;

	private static final List<String> forbiddenWords = new ArrayList<>();

	static {
		loadCensuredWords();
	}

	public CommunicationThreads(Socket socketClient, ConcurrentHashMap<String, Player> players,
								List<CommunicationThreads> threads) {
		this.socketClient = socketClient;
		this.players = players;
		this.threads = threads;
	}

	@Override
	public void run() {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(socketClient.getInputStream()))) {
			writer = new PrintWriter(socketClient.getOutputStream(), true);

			username = reader.readLine();
			if (username == null || username.trim().isEmpty()) {
				username = "Invitado";
			}

			assignedColor = ServerStart.obtainColor();
			players.put(username, new Player(username, assignedColor));

			ServerStart.sendToAll("[SERVIDOR]: " + assignedColor + username + " se ha unido al chat.\u001B[0m");

			String message;
			while ((message = reader.readLine()) != null) {
				if (message.startsWith("[REGISTRO]:")) {
					System.out.println("[REGISTRO] " + message.substring(11).trim());
				} else if (message.startsWith("/")) {
					manageCommand(message);
				} else {
					String censuredWord = censorWord(message);
					ServerStart.sendToAll(assignedColor + "[" + username + "]: " + censuredWord + "\u001B[0m");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			players.remove(username);
			ServerStart.removeThread(this);
			ServerStart.sendToAll("[SERVIDOR]: " + assignedColor + username + " ha abandonado el chat.\u001B[0m");
			ServerStart.returnColor(assignedColor);

			try {
				socketClient.close();
			} catch (IOException e) {
				System.out.println("Has abandonado el chat, hasta la próxima.");
			}
		}
	}


	private void manageCommand(String comando) {
		String[] partes = comando.split(" ", 3);
		switch (partes[0]) {
			case "/atacar":
				manageAttack(partes.length > 1 ? partes[1] : null);
				break;
			case "/resumen":
				showSummary();
				break;
			case "/mio":
				showOwnData();
				break;
			case "/dar":
				manageDonation(partes);
				break;
			case "/salir":
				closeConnection();
				break;
			default:
				sendMessage("[SERVIDOR]: Comando no reconocido.");
		}
	}

	private void manageAttack(String objetivo) {
		if (objetivo == null || !players.containsKey(objetivo)) {
			sendMessage("[SERVIDOR]: Usuario no encontrado.");
			return;
		}

		Player atacante = players.get(username);
		Player atacado = players.get(objetivo);

		if (atacante.getMoney() < 5 || atacado.getHp() <= 0) {
			ServerStart.sendToAll("[SERVIDOR]: " + username + " atacó a " + objetivo + " pero no surtió efecto.");
			return;
		}

		atacante.reduceMoney(5);
		atacado.reduceHp(10);

		ServerStart.sendToAll("[SERVIDOR]: " + username + " atacó a " + objetivo + " (-10 PV).");
	}

	private void showSummary() {
		StringBuilder resumen = new StringBuilder("\n=== Estado de los jugadores ===\n");
		for (Player player : players.values()) {
			resumen.append(player).append("\n");
		}
		ServerStart.sendToAll(resumen.toString());
	}
	private void showOwnData() {
		Player player = players.get(username);
		String datos = "=== Tus datos ===\n" +
				"PV: " + player.getHp() + "\n" +
				"Dinero: " + player.getMoney();
		sendMessage(datos);
	}

	private void manageDonation(String[] partes) {
		if (partes.length < 3) {
			sendMessage("[SERVIDOR]: Uso incorrecto. Usa /dar <cantidad> <jugador>.");
			return;
		}

		try {
			int cantidad = Integer.parseInt(partes[1]);
			String receptor = partes[2];

			if (!players.containsKey(receptor)) {
				sendMessage("[SERVIDOR]: Usuario no encontrado.");
				return;
			}

			Player donante = players.get(username);
			Player destinatario = players.get(receptor);

			if (donante.getMoney() < cantidad) {
				sendMessage("[SERVIDOR]: Saldo insuficiente para la donación.");
				return;
			}

			if (destinatario.getMoney() + cantidad > 100) {
				sendMessage("[SERVIDOR]: La donación excede el límite de dinero permitido para el destinatario.");
				return;
			}

			donante.reduceMoney(cantidad);
			destinatario.addMoney(cantidad);

			for (CommunicationThreads hilo : threads) {
				if (hilo.username.equals(receptor)) {
					hilo.sendMessage("[SERVIDOR]: El jugador " + username + " te ha donado " + cantidad + " monedas.");
					break;
				}
			}

		} catch (NumberFormatException e) {
			sendMessage("[SERVIDOR]: La cantidad debe ser un número entero.");
		}
	}

	private void closeConnection() {
		try {
			socketClient.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendMessage(String message) {
		writer.println(message);
	}

	private static void loadCensuredWords() {
		try (BufferedReader reader = new BufferedReader(new FileReader("censored.txt"))) {
			String line;
			while ((line = reader.readLine()) != null) {
				forbiddenWords.add(line.trim().toLowerCase());
			}
		} catch (IOException e) {
			System.err.println("[SERVIDOR]: No se pudo cargar el archivo censored.txt. " +
					"El sistema funcionará sin censura.");
		}
	}

	private String censorWord(String message) {
		String censoredWord = message;
		for (String word : forbiddenWords) {
			censoredWord = censoredWord.replaceAll("(?i)" + word, "*".repeat(word.length()));
		}
		return censoredWord;
	}
}




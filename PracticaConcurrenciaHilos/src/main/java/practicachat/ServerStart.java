package practicachat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ServerStart {

	private static final int PORT = 5555;
	private static final int INITIAL_HP = 50;
	private static final int INITIAL_MONEY = 25;

	private static final String[] COLORS = {
			"\u001B[31m", // Rojo
			"\u001B[32m", // Verde
			"\u001B[33m", // Amarillo
			"\u001B[34m", // Azul
			"\u001B[35m"  // Magenta
	};

	private static final ConcurrentHashMap<String, Player> players = new ConcurrentHashMap<>();
	private static final List<CommunicationThreads> threads = new ArrayList<>();
	private static final List<String> availableColors = new ArrayList<>();

	public static void main(String[] args) {
		synchronized (availableColors) {
			for (String color : COLORS) {
				availableColors.add(color);
			}
		}

		try (ServerSocket serverSocket = new ServerSocket(PORT)) {
			System.out.println("[SERVIDOR]: Iniciando en el puerto " + PORT);

			while (true) {
				Socket socketClient = serverSocket.accept();
				CommunicationThreads thread = new CommunicationThreads(
						socketClient, players, threads);
				synchronized (threads) {
					threads.add(thread);
				}
				thread.start();
			}
		} catch (IOException e) {
			System.out.println("[SERVIDOR]: " + Thread.currentThread().getName() + "se ha salido del chat.");
		}
	}

	public static synchronized void sendToAll(String message) {
		synchronized (threads) {
			for (CommunicationThreads thread : threads) {
				if (thread == Thread.currentThread()){
					if(message.startsWith("[SERVIDOR]")){
						thread.sendMessage(message);
					}else{
						thread.sendMessage("\t\t\t\t\t\t" + message);
					}
				}else{
					thread.sendMessage(message);
				}
			}
		}
	}

	public static synchronized void removeThread(CommunicationThreads hilo) {
		synchronized (threads) {
			threads.remove(hilo);
		}
	}

	public synchronized static String obtainColor() {
		if (availableColors.isEmpty()) return "\u001B[37m";
		return availableColors.remove(0);
	}

	public synchronized static void returnColor(String color) {
		availableColors.add(0, color);
	}

}


package practicachat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ClientStart {

	static String username = "";
	public static void main(String[] args) {
		try (Socket socket = new Socket("localhost", 5555);
			 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			 PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
			 Scanner scanner = new Scanner(System.in)) {

			System.out.println("[CLIENTE]: Conectando al servidor...");
			System.out.println("[CLIENTE]: Ingresa tu nombre de usuario:");
			username = scanner.nextLine();
			writer.println(username);

			sendToServer(writer, "[CLIENTE]: Conectando al servidor...");
			sendToServer(writer, "[CLIENTE]: El usuario " + username + " se ha registrado.");

			Thread receptor = new Thread(() -> {
				try {
					String message;
					while ((message = reader.readLine()) != null) {
						System.out.println(message);
					}
				} catch (IOException e) {
				System.out.println("Se ha perdido la conexion con el servidor");
				}
			});
			receptor.start();

			System.out.println("[CLIENTE]: Escribe tus mensajes (o comandos):");
			String entry;
			do {
				entry = scanner.nextLine();
				writer.println(entry);
				sendToServer(writer, entry);
			} while (!entry.equalsIgnoreCase("/salir"));
			System.out.println("Has abandonado el chat, hasta la proxima.");
			receptor.interrupt();

		} catch (IOException e) {
			System.out.println("Se acabó la aplicación");
		}
	}
	private static void sendToServer(PrintWriter escritor, String mensaje) {
		escritor.println("[REGISTRO]: " + username +": "+ mensaje);
	}
}

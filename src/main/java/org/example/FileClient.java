package org.example;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class FileClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) {
        try {
            // Forbind til server først
            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
            System.out.println("Forbundet til server på " + SERVER_HOST + ":" + SERVER_PORT);
            System.out.println("Skriv 'exit' for at afslutte\n");
            
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    System.out.print("Kommando: ");
                    String command = scanner.nextLine().trim();

                    if (command.equalsIgnoreCase("exit")) {
                        System.out.println("Afslutter...");
                        break;
                    }

                    if (command.isEmpty()) {
                        continue;
                    }

                    try {
                        // Send kommando som den er
                        out.write((command + "\n").getBytes());
                        out.flush();
                        System.out.println("Sendt: " + command);

                        // Læs response header
                        String response = readLine(in);
                        System.out.println("Svar: " + response);

                        if (response.startsWith("OK|")) {
                            long fileSize = Long.parseLong(response.substring(3));
                            System.out.println("Modtager fil (" + fileSize + " bytes)...");
                            
                            String filename = command.substring(command.indexOf("|") + 1).trim();
                            receiveFile(in, filename, fileSize);
                            System.out.println("✓ Modtaget");
                            
                            // Vis filindhold hvis mindre end 1KB
                            if (fileSize < 1024) {
                                System.out.println("Indhold:");
                                System.out.println(new String(Files.readAllBytes(Paths.get(filename))));
                            }
                            System.out.println();
                        } else if (response.startsWith("ERROR|")) {
                            String errorMsg = response.substring(6);
                            System.out.println("Serverfejl: " + errorMsg + "\n");
                        }
                    } catch (IOException e) {
                        System.err.println("Fejl under overførsel: " + e.getMessage() + "\n");
                    }
                }
            }

            socket.close();

        } catch (ConnectException e) {
            System.err.println("❌ FEJL: Kan ikke forbinde til server!");
            System.err.println("   Server kører ikke på " + SERVER_HOST + ":" + SERVER_PORT);
            System.err.println("   Start server først med: java -cp target/classes org.example.FileServer");
        } catch (IOException e) {
            System.err.println("❌ FEJL: " + e.getMessage());
        }
    }

    private static String readLine(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1 && c != '\n') {
            sb.append((char) c);
        }
        return sb.toString();
    }

    private static void receiveFile(InputStream in, String filename, long fileSize) throws IOException {
        byte[] buffer = new byte[(int) fileSize];
        int bytesRead = 0;
        int totalRead = 0;

        while (totalRead < fileSize) {
            bytesRead = in.read(buffer, totalRead, (int) (fileSize - totalRead));
            if (bytesRead == -1) {
                throw new IOException("Uventet EOF ved filmodtagelse");
            }
            totalRead += bytesRead;
        }

        Files.write(Paths.get(filename), buffer);
    }
}

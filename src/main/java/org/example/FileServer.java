package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileServer {
    private static final int PORT = 5000;
    private static final String FILES_DIR = "./files";

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server lytter på port " + PORT);
            System.out.println("Filmappe: " + new File(FILES_DIR).getAbsolutePath());

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Klient forbundet: " + clientSocket.getInetAddress());
                handleClient(clientSocket);
            }
        } catch (IOException e) {
            System.err.println("Server fejl: " + e.getMessage());
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (InputStream in = clientSocket.getInputStream();
             OutputStream out = clientSocket.getOutputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {

            String request;
            while ((request = reader.readLine()) != null) {
                request = request.trim();
                System.out.println("Modtaget: " + request);

                if (request.startsWith("GET|")) {
                    String filename = request.substring(4).trim();
                    sendFile(filename, out);
                } else {
                    sendError("Ugyldig kommando", out);
                }
            }

        } catch (IOException e) {
            System.err.println("Fejl ved håndtering af klient: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Fejl ved lukking af socket: " + e.getMessage());
            }
        }
    }

    private static void sendFile(String filename, OutputStream out) throws IOException {
        // Validering: afvis "../"
        if (filename.contains("..")) {
            sendError("Path traversal ikke tilladt", out);
            return;
        }

        Path filePath = Paths.get(FILES_DIR, filename);
        File file = filePath.toFile();

        if (!file.exists() || !file.isFile()) {
            sendError("Fil ikke fundet: " + filename, out);
            return;
        }

        try {
            byte[] fileData = Files.readAllBytes(filePath);
            String header = "OK|" + fileData.length + "\n";
            out.write(header.getBytes());
            out.write(fileData);
            out.flush();
            System.out.println("Fil sendt: " + filename + " (" + fileData.length + " bytes)");
        } catch (IOException e) {
            sendError("Fejl ved læsning af fil: " + e.getMessage(), out);
        }
    }

    private static void sendError(String message, OutputStream out) throws IOException {
        String errorMsg = "ERROR|" + message + "\n";
        out.write(errorMsg.getBytes());
        out.flush();
        System.out.println("Fejl sendt: " + message);
    }
}

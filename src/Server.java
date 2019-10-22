package src;

import java.net.*;
import java.io.*;

public class Server {
    //initialize socket and input stream
    private ServerSocket server = null;
    private int currentNumber = 0;

    // constructor with port
    public Server(int port) {
        // starts server and waits for a connection
        try {
            server = new ServerSocket(port);
            System.out.println("******************************************************");
            System.out.println("\tWelcome to the server interface");
            System.out.println("******************************************************");
            System.out.println("Currently there is now functionality except seeing");
            System.out.println("incoming connections/disconnections");
            System.out.println("******************************************************");
            System.out.println("Log files: ");

            while (true) {
                Socket clientSocket = null;
                clientSocket = server.accept();
                new Thread(new clientSocketThread(clientSocket)).start();
            }
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    public class clientSocketThread implements Runnable {
        private Socket clientSocket;
        private DataInputStream in = null;
        private DataOutputStream out;

        private clientSocketThread(Socket clientSocket) throws IOException {
            this.clientSocket = clientSocket;
            out = new DataOutputStream(clientSocket.getOutputStream());
            in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
        }

        private void closeConnection() {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            currentNumber--;
        }

        public void run() {
            currentNumber++;
            String clientID = Integer.toString(currentNumber);
            try {
                out.writeUTF(clientID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Client " + clientID + " is now connected to the socket");
            String line = "";
            while (!line.equals("Over")) {
                try {
                    line = in.readUTF();
                    System.out.println("Client " + clientID + ": \"" + line + "\"");
                    out.writeUTF("Message received");
                } catch (IOException i) {
                    line = "Over";
                }
            }
            System.out.println("Closing connection with Client " + clientID);
            closeConnection();
        }
    }
}

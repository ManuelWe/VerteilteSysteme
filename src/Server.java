import java.net.*;
import java.io.*;

public class Server {
    //initialize socket and input stream
    private ServerSocket server = null;

    // constructor with port
    public Server(int port) {
        // starts server and waits for a connection
        try {
            server = new ServerSocket(port);
            System.out.println("Server started");
            System.out.println("Waiting for a client ...");

            while (true) {
                Socket clientSocket = null;
                clientSocket = server.accept();
                new Thread(new clientSocketThread(clientSocket)).start();
            }
        } catch (IOException i) {
            System.out.println(i);
        }
    }

    public class clientSocketThread implements Runnable {

        public clientSocketThread(Socket clientSocket) throws IOException {
            this.clientSocket = clientSocket;
            out = new DataOutputStream(clientSocket.getOutputStream());
        }

        private final Socket clientSocket;
        private DataInputStream in = null;
        DataOutputStream out;


        public void run() {
            // takes input from the client socket
            try {
                in = new DataInputStream(
                        new BufferedInputStream(clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }

            String socketID = "";
            while (socketID == "") {
                try {
                    socketID = in.readUTF();
                    System.out.println("Socket " + socketID + " registered!");
                    //send server id back
                    out.writeUTF("1");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            String line = "";
            // reads message from client until "Over" is sent
            while (!line.equals("Over")) {
                try {
                    line = in.readUTF();
                    System.out.println("Socket " + socketID + ": " + line);
                    out.writeUTF("Copy!");
                } catch (IOException i) {
                    //when socket unreachable, close connection
                    line = "Over";
                }
            }
            System.out.println("Closing connection with Socket " + socketID);

            // close connection
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
        }
    }


    public static void main(String args[]) {
        Server server = new Server(5000);
    }
}

// A Java program for a Client

import java.net.*;
import java.io.*;

public class Client2 {
    // initialize socket and input output streams
    private Socket socket = null;
    private DataInputStream input = null;
    private DataInputStream serverResponse = null;
    private DataOutputStream out = null;

    // constructor to put ip address and port
    public Client2(String address, int port) {
        // establish a connection
        try {
            socket = new Socket(address, port);

            // takes input from terminal
            input = new DataInputStream(System.in);

            // sends output to the socket
            out = new DataOutputStream(socket.getOutputStream());
            //send id
            out.writeUTF("2");
        } catch (UnknownHostException u) {
            System.out.println(u);
        } catch (IOException i) {
            System.out.println(i);
        }

        // string to read message from input
        String line = "";
        try {
            serverResponse = new DataInputStream(
                    new BufferedInputStream(socket.getInputStream()));
            System.out.println("Connected to Server " + serverResponse.readUTF());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // keep reading until "Over" is input
        while (!line.equals("Over")) {
            try {
                line = input.readLine();
                out.writeUTF(line);
                System.out.println(serverResponse.readUTF());
            } catch (IOException i) {
                System.out.println(i);
            }
        }

        // close the connection
        try {
            input.close();
            out.close();
            socket.close();
        } catch (IOException i) {
            System.out.println(i);
        }
    }

    public static void main(String args[]) {
        Client2 client = new Client2("127.0.0.1", 5000);
    }
}
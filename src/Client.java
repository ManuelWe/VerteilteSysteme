// A Java program for a Client

import java.net.*;
import java.io.*;
import java.util.Scanner;

public class Client {
    // initialize socket and input output streams
    private Socket socket = null;
    private Scanner input;
    private DataInputStream serverResponse = null;
    private DataOutputStream out = null;

    // constructor to put ip address and port
    public Client(String address, int port) {
        System.out.println("******************************************************");
        System.out.println("\tWelcome to the client interface");
        System.out.println("******************************************************");
        System.out.println("Currently you are only able to send messages to the");
        System.out.println("server by typing them into here (\"Over\" to close)");
        try {
            socket = new Socket(address, port);
            input = new Scanner(System.in);
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException u) {
            u.printStackTrace();
        }

        String line = "";
        try {
            serverResponse = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            System.out.println("You are currently connected to socket " + serverResponse.readUTF());
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("******************************************************");
        while (!line.equals("Over")) {
            System.out.printf("Your input: ");
            try {
                line = input.nextLine();
                out.writeUTF(line);
                System.out.println(serverResponse.readUTF());
            } catch (IOException i) {
                System.out.println(i);
            }
        }
        System.out.println("Closing connection to server");
        closeConnection();
    }
    private void closeConnection() {
        try {
            input.close();
            out.close();
            socket.close();
        } catch (IOException i) {
            i.printStackTrace();
        }
    }
}
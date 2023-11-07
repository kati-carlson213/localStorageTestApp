import java.io.*;
import java.net.*;
import java.util.*;

class Server {
  private ServerSocket server = null;
  private String storagePath = "received_files"; // Directory for storing received files

  public Server(int port, int backlog, String IP) {
    try {
      server = new ServerSocket(port, backlog, InetAddress.getByName(IP));
      System.out.println("Server started.");

      while (true) {
        Socket socket = server.accept();
        new ClientHandler(socket).start();
      }
    } catch (Exception e) {
      System.out.println("Error: " + e.getMessage());
    }
  }

  public static void main(String args[]) {
    Server server = new Server(5000, 0, "127.0.0.1");
  }

  class ClientHandler extends Thread {
    private Socket socket;

    public ClientHandler(Socket socket) {
      this.socket = socket;
    }

    @Override
    public void run() {
      try {
        InputStream inputStream = socket.getInputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;

        while (true) {
          int headerLength = inputStream.read(buffer, 0, buffer.length);
          if (headerLength <= 0) {
            break; // No more data to read
          }

          String header = new String(buffer, 0, headerLength);

          if (header.startsWith("retrieve:")) {
            // Handle file retrieval request
            String fileName = header.substring(9, header.length() - 2); // Extract file name
            receiveFile(fileName, inputStream);
          }
        }
      } catch (Exception e) {
        System.out.println("Error: " + e.getMessage());
      } finally {
        try {
          socket.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    private void receiveFile(String fileName, InputStream inputStream) {
      try {
        // Create a FileOutputStream to write the received file
        File outputFile = new File(storagePath, fileName);
        FileOutputStream fileOutputStream = new FileOutputStream(outputFile);

        byte[] buffer = new byte[1024];
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
          fileOutputStream.write(buffer, 0, bytesRead);
        }

        fileOutputStream.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}

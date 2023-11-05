import java.io.*;
import java.net.*;
import java.nio.file.*;

class Server {
  private ServerSocket server = null;

  public Server(int port, int backlog, String IP) {
    try {
      server = new ServerSocket(port, backlog, InetAddress.getByName(IP));

      while (true) {
        Socket socket = server.accept();
        new ClientHandler(socket).start();
      }
    } catch (Exception e) {
      System.out.println("Error: " + e.getMessage());
    }
  }

  public static void main(String args[]) {
    Server server = new Server(5000, 0, "127.0.0.1"); //change IP on both server + client side to machine's IP when testing to see how it works with public IP's  
  }
}

class ClientHandler extends Thread {
  private Socket socket;

  public ClientHandler(Socket socket) {
    this.socket = socket;
  }

  @Override
  public void run() {
    try {
      // Create a directory to store received files
      File storageDirectory = new File("received_files");
      storageDirectory.mkdir();

      InputStream inputStream = socket.getInputStream();
      byte[] buffer = new byte[1024];
      int bytesRead;

      while (true) {
        int headerLength = inputStream.read(buffer, 0, buffer.length);
        if (headerLength <= 0) {
          break; // No more data to read
        }

        String header = new String(buffer, 0, headerLength);
        int startIndex = header.indexOf("filename:");
        if (startIndex != -1) {
          int endIndex = header.indexOf("\r\n", startIndex);
          if (endIndex != -1) {
            String fileName = header.substring(startIndex + 9, endIndex);

            File receivedFile = new File(storageDirectory, fileName);

            try (FileOutputStream fileOutputStream = new FileOutputStream(receivedFile)) {
              while (true) {
                bytesRead = inputStream.read(buffer, 0, buffer.length);
                if (bytesRead <= 0) {
                  break; // No more data to read
                }
                if (new String(buffer, 0, bytesRead).contains("END_OF_FILE")) {
                  break; // End of current file
                }
                fileOutputStream.write(buffer, 0, bytesRead);
              }

            } catch (Exception e) {
              System.out.println("Error saving file: " + e.getMessage());
            }
          }
        }
      }
    } catch (Exception e) {
      System.out.println("Error receiving file: " + e.getMessage());
    } finally {
      try {
        socket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}

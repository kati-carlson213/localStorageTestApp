import java.io.*;
import java.net.*;
import java.nio.file.*;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import javax.crypto.spec.GCMParameterSpec;



class Server {
  private ServerSocket server = null;
  private static final String decryptedFilesDirectoryPath = "C:\\Users\\katie\\decrypted_files";

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
    Server server = new Server(5000, 0, "127.0.0.1");
  }

  class ClientHandler extends Thread {
    private Socket socket;


    String encryptionKey = "DIIPuSZAyzysvCtBpPTgBLuFKWJFDZR1"; //be sure to edit code to secure key in actual implementation -- this is just a placeholder for testing
    String initVector = "jSabBhKCIDek"; //be sure to randomly generate + secure in actual implementation


    public ClientHandler(Socket socket) {
      this.socket = socket;
    }

    private void decryptFile(InputStream inputStream, OutputStream outputStream) {
      try {
        IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
        byte[] keyData = encryptionKey.getBytes(StandardCharsets.UTF_8);

        byte[] ivBytes = iv.getIV();

        SecretKeySpec key = new SecretKeySpec(keyData, "AES");

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, ivBytes));

        

        CipherInputStream cipherInputStream = new CipherInputStream(inputStream, cipher);

        byte[] buffer = new byte[1024];
        int bytesRead;

        while ((bytesRead = cipherInputStream.read(buffer)) != -1) {
          outputStream.write(buffer, 0, bytesRead);

        }

        // Close the CipherInputStream
        cipherInputStream.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }



    private String getFileNameFromHeader(String header) {
      int startIndex = header.indexOf("filename:");
      if (startIndex != -1) {
        int endIndex = header.indexOf("\r\n", startIndex);
        if (endIndex != -1) {
          return header.substring(startIndex + 9, endIndex);
        }
      }
      return "default_filename"; // Return a default filename if the header is invalid
    }


    @Override
    public void run() {
      try {
        File receivedFilesDirectory = new File("encrypted_files");
        File decryptedFilesDirectory = new File(decryptedFilesDirectoryPath); // Use the absolute path

        receivedFilesDirectory.mkdir();
        decryptedFilesDirectory.mkdir();

        byte[] buffer = new byte[1024];

        while (true) {
          int headerLength = socket.getInputStream().read(buffer, 0, buffer.length);
          if (headerLength <= 0) {
            break; // No more data to read
          }

          String header = new String(buffer, 0, headerLength);

          ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
          while (true) {
            int bytesRead = socket.getInputStream().read(buffer, 0, buffer.length);
            if (bytesRead <= 0) {
              break; // No more data to read
            }

            // Check for "END_OF_FILE" delimiter and skip it
            String dataChunk = new String(buffer, 0, bytesRead);
            if (dataChunk.contains("END_OF_FILE")) {
              bytesRead -= "END_OF_FILE\r\n".getBytes().length; // Subtract delimiter length
            }

            dataStream.write(buffer, 0, bytesRead);

            // Check again for "END_OF_FILE" delimiter
            if (dataChunk.contains("END_OF_FILE")) {
              break; // End of the current file
            }
          }

          byte[] receivedData = dataStream.toByteArray();

            String fileName = getFileNameFromHeader(header);

            File receivedFile = new File(receivedFilesDirectory, fileName);
            try (FileOutputStream fileOutputStream = new FileOutputStream(receivedFile)) {
              fileOutputStream.write(receivedData, 0, receivedData.length);
            } catch (Exception e) {
              System.out.println("Error saving received file: " + e.getMessage());
            }


            File decryptedFile = new File(decryptedFilesDirectory, fileName);
            try (FileOutputStream fileOutputStream = new FileOutputStream(decryptedFile)) {
              decryptFile(new ByteArrayInputStream(receivedData), fileOutputStream);
            } catch (Exception e) {
              System.out.println("Error saving decrypted file: " + e.getMessage());
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
}


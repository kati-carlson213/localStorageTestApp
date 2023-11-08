package com.example.localstoragetestapp;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class MainActivity extends AppCompatActivity {

    private static final String IP = "10.0.2.2";
    private static final int port = 5000;
    private Socket socket = null;

    private Button selectFileButton;
    private TextView connectionStatus;

    private static final int PICK_FILE_REQUEST = 1;



    String encryptionKey = "DIIPuSZAyzysvCtBpPTgBLuFKWJFDZR1"; //be sure to edit code to secure key in actual implementation -- this is just a placeholder for testing
    String initVector = "jSabBhKCIDek"; //be sure to randomly generate + secure for actual implementation




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        selectFileButton = findViewById(R.id.selectFileButton);
        connectionStatus = findViewById(R.id.connectionStatus);

        connectToServer();

        selectFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void connectToServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(IP, port);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            connectionStatus.setText("Connected to server");
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            connectionStatus.setText("Connection failed");
                        }
                    });
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK) {
            List<Uri> selectedFileUris = new ArrayList<>();
            Uri selectedFileUri = data.getData();
            selectedFileUris.add(selectedFileUri);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    sendFilesToServer(selectedFileUris);
                }
            }).start();
        }
    }




    private void sendFilesToServer(List<Uri> fileUris) {
        try {
            if (socket != null) {
                OutputStream outputStream = socket.getOutputStream();

                for (Uri fileUri : fileUris) {
                    InputStream inputStream = getContentResolver().openInputStream(fileUri);
                    String fileName = getFileNameFromUri(fileUri);
                    ByteArrayOutputStream encryptedData = encryptFile(inputStream);

                    // Send the filename and encrypted data
                    outputStream.write(("filename:" + fileName + "\r\n").getBytes());
                    outputStream.write(encryptedData.toByteArray());
                }

                // Add the "END_OF_FILE" delimiter separately for each file
                outputStream.write("END_OF_FILE\r\n".getBytes());
                outputStream.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private ByteArrayOutputStream encryptFile(InputStream inputStream) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
            byte[] ivBytes = iv.getIV();

            byte[] keyData = encryptionKey.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec key = new SecretKeySpec(keyData, "AES");

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, ivBytes));

            ByteArrayOutputStream encryptedOutput = new ByteArrayOutputStream();
            CipherOutputStream cipherOutputStream = new CipherOutputStream(encryptedOutput, cipher);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                cipherOutputStream.write(buffer, 0, bytesRead);
            }

            // Close the CipherOutputStream
            cipherOutputStream.close();

            return encryptedOutput;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private String getFileNameFromUri(Uri fileUri) {
        String fileName = "default_filename";
        Cursor cursor = getContentResolver().query(fileUri, null, null, null, null);

        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            cursor.moveToFirst();
            fileName = cursor.getString(nameIndex);
            cursor.close();
        }

        return fileName;
    }
}



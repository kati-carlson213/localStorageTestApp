package com.example.localstoragetestapp;

import android.content.ClipData;
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
import java.io.*;
import java.net.*;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    private static final String IP = "10.0.2.2";
    private static final int port = 5000;
    private Socket socket = null;
    private Button selectFileButton;
    private TextView connectionStatus;
    private static final int PICK_FILE_REQUEST = 1;

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
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // Allow multiple files selection
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK) {
            List<Uri> selectedFileUris = new ArrayList<>();
            ClipData clipData = data.getClipData();

            if (clipData != null) {
                // If multiple files are selected
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    Uri selectedFileUri = clipData.getItemAt(i).getUri();
                    selectedFileUris.add(selectedFileUri);
                }
            } else {
                // If only one file is selected
                Uri selectedFileUri = data.getData();
                selectedFileUris.add(selectedFileUri);
            }

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

                    // Send the filename and file data
                    outputStream.write(("filename:" + fileName + "\r\n").getBytes());

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                    // Add the "END_OF_FILE" delimiter separately for each file
                    outputStream.write("END_OF_FILE\r\n".getBytes());
                }

                outputStream.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
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

package com.peerbits.nfccardread.data_manager;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.peerbits.nfccardread.NfcHome;
import com.peerbits.nfccardread.R;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class FileLogger {

    private final String TAG = "fileLogDebug";

    private final StringBuilder mStringBuilder;
    public static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION = 99;

    private boolean isHeaderExists = false;

    User user = NfcHome.getUser();

    public FileLogger() {
        mStringBuilder = new StringBuilder();

        appendHeader(user.toString());
    }

    public void appendHeader(String header) {
        if (!isHeaderExists) {
            mStringBuilder.append(header);
            mStringBuilder.append("\n");
        }

        isHeaderExists = true;
    }

    public void appendLine(String line) {
        mStringBuilder.append(line);
        mStringBuilder.append("\n");
    }

    public void uploadFile(String timestamp) {

        try {
            //create file
            String fileName = user.getId() + timestamp;
            File file = createFile(fileName);

            //add contents to the file
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(mStringBuilder.toString());
            fileWriter.close();

            StorageUploader uploader = new StorageUploader();
            uploader.uploadFileFirebase(file);
            Log.d(TAG, "File uploaded successfully");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private File createFile(String name) {

        if (isExternalStorageWritable()) {
            File externalDirectory = Environment.getExternalStorageDirectory();
            File appDirectory = new File(externalDirectory, "SMRL");
            File logFile = new File(appDirectory,  name + ".txt");

            if (!appDirectory.exists()) {
                try{
                    boolean status = appDirectory.mkdirs();
                }catch (SecurityException e) {
                    e.printStackTrace();
                }

            }

            // create log file
            if (!logFile.exists()) {
                boolean status = false;
                try {
                    status = logFile.createNewFile();
                    Log.d(TAG, "File created succesfully");
                    return logFile;
                } catch (IOException e) {
                    Log.e(TAG, "logFile.createNewFile(): ", e);
                    e.printStackTrace();
                }
                Log.e(TAG, "logFile.createNewFile() created: " + status);
            } else {
                return logFile;
            }



        } else {
            Log.e(TAG, "createFile isExternalStorageWritable Error");
        }
        return null;


    }


    /* Checks if external storage is available for read and write */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public boolean checkRuntimeWriteExternalStoragePermission(Context context, final Activity activity) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                new AlertDialog.Builder(activity)
                        .setTitle(R.string.write_external_storage_permission_title)
                        .setMessage(R.string.write_external_storage_permission_text)
                        .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                requestForWriteExternalStoragePermission(activity);
                            }
                        })
                        .create()
                        .show();

            } else {
                requestForWriteExternalStoragePermission(activity);
            }
            Log.e(TAG, "checkRuntimeWriteExternalStoragePermission() FALSE");
            return false;
        } else {
            Log.e(TAG, "checkRuntimeWriteExternalStoragePermission() TRUE");
            return true;
        }
    }

    private void requestForWriteExternalStoragePermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION);
    }
}

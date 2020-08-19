package com.example.downloadmanagersample;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private Button btn_download;
    private long downloadID;

    // using broadcast method
    private BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Fetching the download id received with the broadcast
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            //Checking if the received broadcast is for our enqueued download by matching download id
            if (downloadID == id) {
                Toast.makeText(MainActivity.this, "Download Completed", Toast.LENGTH_SHORT).show();
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_download = findViewById(R.id.download_btn);
        // using broadcast method
        registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        btn_download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                beginDownload();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // using broadcast method
        unregisterReceiver(onDownloadComplete);
    }

    private void beginDownload() {
        String url = "http://speedtest.ftp.otenet.gr/files/test10Mb.db";
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        fileName = fileName.substring(0, 1).toUpperCase() + fileName.substring(1);

        String destination = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/";
        //Delete update file if exists
        File file = new File(destination);
        if (file.exists()) file.delete();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)// Visibility of the download Notification
//                .setDestinationUri(Uri.fromFile(file))// Uri of the destination file
                    .setDestinationUri(Uri.parse("file://" + destination + fileName))// Uri of the destination file
                    .setTitle(fileName)// Title of the Download Notification
                    .setDescription("Downloading")// Description of the Download Notification
                    .setRequiresCharging(false)// Set if charging is required to begin the download
                    .setAllowedOverMetered(true)// Set if download is allowed on Mobile network
                    .setAllowedOverRoaming(true);// Set if download is allowed on roaming network
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            downloadID = downloadManager.enqueue(request);// enqueue puts the download request in the queue.

            // using query method
            boolean finishDownload = false;
            int progress;
            while (!finishDownload) {
                Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(downloadID));
                if (cursor.moveToFirst()) {
                    int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    switch (status) {
                        case DownloadManager.STATUS_FAILED: {
                            finishDownload = true;
                            break;
                        }
                        case DownloadManager.STATUS_PAUSED:
                            break;
                        case DownloadManager.STATUS_PENDING:
                            break;
                        case DownloadManager.STATUS_RUNNING: {
                            final long total = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                            if (total >= 0) {
                                final long downloaded = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                                progress = (int) ((downloaded * 100L) / total);
                                // if you use downloadmanger in async task, here you can use like this to display progress.
                                // Don't forget to do the division in long to get more digits rather than double.
                                //  publishProgress((int) ((downloaded * 100L) / total));
                            }
                            break;
                        }
                        case DownloadManager.STATUS_SUCCESSFUL: {
                            progress = 100;
                            // if you use aysnc task
                            // publishProgress(100);
                            finishDownload = true;
                            Toast.makeText(MainActivity.this, "Download Completed", Toast.LENGTH_SHORT).show();
                            break;
                        }
                    }
                }
            }
        }
    }
}
package com.mcc.ocr;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.mcc.ocr.OCRConnectionManager.OCRHistory;
import static com.mcc.ocr.OCRConnectionManager.getImage;

public class DisplayActivity extends AppCompatActivity {

    CustomListAdapter customAdapter;

    private static void SaveFile(String text) {

        String root = Environment.getExternalStorageDirectory().toString();

        File myDir = new File(root + "/OCR_downloads");
        if (!myDir.exists())
            myDir.mkdirs();
        String fname = "ocrCapture_" + String.valueOf(new Date().toString()) + ".txt";
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            out.write(text.getBytes());
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        final ListView yourListView = (ListView) findViewById(R.id.MyList);

        /* Handle requests for the download of a file*/
        yourListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                System.out.println(customAdapter.records.get(position).getText());
                final int ID = position;
                // 1. Instantiate an AlertDialog.Builder with its constructor
                AlertDialog.Builder builder = new AlertDialog.Builder(DisplayActivity.this);
                // 2. Chain together various setter methods to set the dialog characteristics
                builder.setMessage("Do you want to save the current OCR ? ")
                        .setTitle("Save File");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int ied) {
                        DisplayActivity.SaveFile(customAdapter.records.get(ID).getText());
                        // User clicked OK button
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                return false;
            }
        });
        final CountDownLatch latch = new CountDownLatch(1);
        final Object[] js = new Object[2];
        yourListView.setBackgroundColor(Color.parseColor("#C6E2FF"));
        boolean op = MainActivity.h_story;
        //System.out.println("History? " + op);
        if (op == true) {
            List<OCRecord> history = new LinkedList<>();
            Thread uiThread = new HandlerThread("UIHandler") {
                @Override
                public void run() {
                    js[0] = (Object) OCRHistory(LoginActivity.USERNAME.replaceAll("\\D+", ""));
                    // System.out.println(((JSONObject) js[0]).toString());
                    latch.countDown();

                }
            };
            uiThread.start();
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            JSONObject jsdata = null;
            try {
                jsdata = ((JSONObject) js[0]).getJSONObject("data");

                Iterator itr = jsdata.keys();
                while (itr.hasNext()) {
                    Object element = itr.next();
                    JSONObject record = jsdata.getJSONObject((String) itr.next());
                    JSONArray filesInrecord = record.getJSONArray("items");
                    for (int i = 0; i < filesInrecord.length(); ++i) {
                        JSONObject rec = filesInrecord.getJSONObject(i);
                        final CountDownLatch latch2 = new CountDownLatch(1);
                        final File[] f = new File[2];
                        final String s = (String) rec.get("thumb_id");
                        boolean first = true;
                        Bitmap b;
                        Thread uiThread2 = new HandlerThread("UIHandler") {
                            @Override
                            public void run() {
                                f[0] = getImage(s);
                                // System.out.println("hohooh");
                                //System.out.println(f[0].toString());
                                latch2.countDown();

                            }
                        };
                        if (first) {
                            uiThread2.start();
                            try {
                                latch2.await();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            b = BitmapFactory.decodeStream(new FileInputStream(f[0]));//BitmapFactory.decodeResource(this.getResources(),R.drawable.example_appwidget_preview);
                        }//System.out.println(rec.toString());
                        else {
                            b = BitmapFactory.decodeResource(this.getResources(), R.drawable.example_appwidget_preview);

                        }
                        //System.out.println(b);
                        //BitmapFactory.decodeStream(new FileInputStream(f));
                        OCRecord reco = new OCRecord(b, (String) rec.get("text"),
                                new java.util.Date(),//Date.valueOf((String) rec.get("date")),
                                null, true);
                        //  System.out.println(reco);

                        history.add(reco);
                    }
                    OCRConnectionManager.dataexchanged = 0;
                    //System.out.println(history);

                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            customAdapter = new CustomListAdapter(this, R.layout.activity_image_list, history);

        }
        // get data from the table by the ListAdapter
        else
            customAdapter = new CustomListAdapter(this, R.layout.activity_image_list, OCRecordStorage.getStorageContent());

        yourListView.post(new Runnable() {
            public void run() {
                yourListView.setAdapter(customAdapter);
            }
        });
    }


}

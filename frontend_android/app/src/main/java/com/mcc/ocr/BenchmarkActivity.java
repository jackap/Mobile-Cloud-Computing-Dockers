package com.mcc.ocr;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BenchmarkActivity extends AppCompatActivity {

    private TextView tv1, tv2, tv3, tv4;
    private TextView tv5, tv6, tv7, tv8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_benchmark);
        tv1 = (TextView)findViewById(R.id.tv1);
        tv2 = (TextView)findViewById(R.id.tv2);
        tv3 = (TextView)findViewById(R.id.tv3);
        tv4 = (TextView)findViewById(R.id.tv4);
        tv5 = (TextView)findViewById(R.id.tv5);
        tv6 = (TextView)findViewById(R.id.tv6);
        tv7 = (TextView)findViewById(R.id.tv7);
        tv8 = (TextView)findViewById(R.id.tv8);
        TextView tvNet = (TextView) findViewById(R.id.tvNet);
        tvNet.setText(String.valueOf(OCRConnectionManager.dataexchanged)+"B");
        OCRConnectionManager.dataexchanged = 0;
        List<OCRecord> OCRecords = OCRecordStorage.getStorageContent();
        List<Long> remote_ocr = new ArrayList<Long>();
        List<Long> local_ocr = new ArrayList<Long>();

        for (OCRecord r:OCRecords){
            if(r.getIsRemote())
                remote_ocr.add(r.getTimeTaken());
            else
                local_ocr.add(r.getTimeTaken());
        }

        Long sum = 0L;
        for(Long t:remote_ocr){
            sum += t;
        }
        tv1.setText(String.valueOf(sum/1000.0)+"s");
        tv2.setText(String.valueOf(Collections.max(remote_ocr)/1000.0)+"s");
        tv3.setText(String.valueOf(Collections.min(remote_ocr)/1000.0)+"s");
        tv4.setText(String.valueOf(sum/1000.0/remote_ocr.size())+"s");

        sum = 0L;
        for(Long t:local_ocr){
            sum += t;
        }
        tv5.setText(String.valueOf(sum/1000.0));
        tv6.setText(String.valueOf(Collections.max(local_ocr)/1000.0)+"s");
        tv7.setText(String.valueOf(Collections.min(local_ocr)/1000.0)+"s");
        tv8.setText(String.valueOf(sum/1000.0/local_ocr.size())+"s");
    }
}

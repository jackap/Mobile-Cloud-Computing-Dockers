package com.mcc.ocr;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.googlecode.leptonica.android.ReadFile;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.mcc.ocr.R.id.history;
import static com.mcc.ocr.Utils.getDataColumn;
import static com.mcc.ocr.Utils.isDownloadsDocument;
import static com.mcc.ocr.Utils.isExternalStorageDocument;
import static com.mcc.ocr.Utils.isMediaDocument;

public class MainActivity extends AppCompatActivity implements  Button.OnClickListener{
    public static boolean h_story = false;
    private static final String TAG = "MainActivity";
    //Request Code Id
    private static final int REQUESTCODE_GALLERY_LOCALLY = 1;
    private static final int REQUESTCODE_CAMERA_LOCALLY = 2;
    private static final int REQUESTCODE_GALLERY_REMOTELY = 3;
    private static final int REQUESTCODE_CAMERA_REMOTELY = 4;
    private static final int REQUESTCODE_GALLERY_BENCH = 5;
    private static final int REQUESTCODE_CAMERA_BENCH = 6;
    //Message Id
    private static final int MSG_CAMERA_REMOTE_FINISHED = 0;
    private static final int MSG_GALLERY_REMOTE_FINISHED = 1;
    private static final int MSG_BENCHMARK_FINISHED = 2;

    private static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/OCR/";
    private static final String LANG = "eng";
    private static final int THUMBSIZE = 60;
    private static boolean PHOTOS_FROM_ALBUM = false;

    private Switch mSwitch;
    private Button mLocal, mRemote, mBenchmark, mHistory;
    private View mProgressView;
    private View mMainPageView;

    private TessBaseAPI baseApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRemote = (Button) findViewById(R.id.btnRemote);
        mBenchmark = (Button) findViewById(R.id.btnBench);
        mLocal = (Button) findViewById(R.id.btnLocal);
        mHistory = (Button) findViewById(history);
        mRemote.setOnClickListener(this);
        mLocal.setOnClickListener(this);
        mBenchmark.setOnClickListener(this);
        mHistory.setOnClickListener(this);
        mSwitch = (Switch)findViewById(R.id.sw);
        mSwitch.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PHOTOS_FROM_ALBUM = isChecked;
            }
        });
        mMainPageView = findViewById(R.id.main_page);
        mProgressView = findViewById(R.id.main_progress);

        initOCR();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnRemote:
                h_story = false;
                executeOCR(REQUESTCODE_GALLERY_REMOTELY, REQUESTCODE_CAMERA_REMOTELY);
                break;
            case R.id.btnLocal:
                h_story = false;
                executeOCR(REQUESTCODE_GALLERY_LOCALLY, REQUESTCODE_CAMERA_LOCALLY);
                break;
            case R.id.btnBench:
                h_story = false;
                executeOCR(REQUESTCODE_GALLERY_BENCH, REQUESTCODE_CAMERA_BENCH);
                break;
            case history: /* User wants to see the history*/
                h_story = true;
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, DisplayActivity.class);
                startActivity(intent);
              /*  new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject js = OCRConnectionManager.OCRHistory("6");
                            JSONObject jsdata = js.getJSONObject("data");
                            Iterator itr = jsdata.keys();
                            while(itr.hasNext()) {

                                Object element = itr.next();
                                System.out.println(jsdata.getJSONObject((String)itr.next()));
                                System.out.print(element + " ");

                            }


                            //Log.i(TAG, "history = "+jsdata.getJSONObject("0365a23d-fbfe-4531-a426-c442b3849340").toString());
                        }catch (Exception e){
                            Log.i(TAG, "e="+e.getMessage());
                        }

                    }
                }).start();*/
                break;
        }
    }

    private void executeOCR(int requestcode1, int requestcode2){
        if(PHOTOS_FROM_ALBUM){
            Intent photoPickerIntent = new Intent();
            photoPickerIntent.setType("image/*");
            photoPickerIntent.setAction(Intent.ACTION_GET_CONTENT);
            photoPickerIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(Intent.createChooser(photoPickerIntent, "Select Picture"), requestcode1);
        }else{
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Uri mPhotoUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    new ContentValues());
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, requestcode2);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == 0) //if no action from user, then return directly
            return;

        OCRecordStorage.init();
        Intent intent;
        showProgress(true);

        switch(requestCode){
            case REQUESTCODE_CAMERA_LOCALLY:
                doOCRLocally_PhotosFromCapture(data);
                showProgress(false);
                intent = new Intent();
                intent.setClass(MainActivity.this, DisplayActivity.class);
                startActivity(intent);
                break;
            case REQUESTCODE_CAMERA_REMOTELY:
                doOCRRemotely_PhotosFromCapture(data, MSG_CAMERA_REMOTE_FINISHED);
                break;
            case REQUESTCODE_CAMERA_BENCH:
                doOCRLocally_PhotosFromCapture(data);
                doOCRRemotely_PhotosFromCapture(data, MSG_BENCHMARK_FINISHED);
                break;
            case REQUESTCODE_GALLERY_LOCALLY:
                doOCRLocally_PhotosFromAlbum(data);
                showProgress(false);
                intent = new Intent();
                intent.setClass(MainActivity.this, DisplayActivity.class);
                startActivity(intent);
                break;
            case REQUESTCODE_GALLERY_REMOTELY:
                doOCRRemotely_PhotosFromAlbum(data, MSG_GALLERY_REMOTE_FINISHED);
                break;
            case REQUESTCODE_GALLERY_BENCH:
                doOCRLocally_PhotosFromAlbum(data);
                doOCRRemotely_PhotosFromAlbum(data, MSG_BENCHMARK_FINISHED);
                break;
        }
    }

    private void doOCRLocally_PhotosFromCapture(Intent data){
        Bitmap bm = (Bitmap) data.getExtras().get("data");
        Bitmap ThumbImage = ThumbnailUtils.extractThumbnail(bm, THUMBSIZE, THUMBSIZE);
        Long start = System.currentTimeMillis();
        baseApi.setImage(bm);
        String recognizedText = baseApi.getUTF8Text();
        Long end = System.currentTimeMillis();
        OCRecordStorage.addRecord(new OCRecord(ThumbImage, recognizedText, new Date(), end - start, false));
    }

    private void doOCRRemotely_PhotosFromCapture(Intent data, final int msg_id){
        final Bitmap bm = (Bitmap) data.getExtras().get("data");
        final Bitmap ThumbImage = ThumbnailUtils.extractThumbnail(bm, THUMBSIZE, THUMBSIZE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                File f = saveBitmapFile(bm, "temp");
                List<File> list = new ArrayList<File>();
                list.add(f);
                Long start = System.currentTimeMillis();
                JSONObject js = OCRConnectionManager.OCRemote(list);
                try {
                    JSONObject result = js.getJSONObject("result");
                    JSONArray jslist = result.getJSONArray("texts");
                    JSONObject info = jslist.getJSONObject(0);
                    //  System.out.println(info.toString());
                    OCRecordStorage.addRecord(new OCRecord(ThumbImage, info.getString("text"), new Date(), System.currentTimeMillis() - start, true));
                    mHandler.obtainMessage(msg_id).sendToTarget();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void doOCRLocally_PhotosFromAlbum(Intent data){
        if (data.getClipData() == null && data.getData() != null) {
            String res = getRealPathFromURI(getApplicationContext(), data.getData());
            File mf = new File(res == null ? "" : res);
            if (!mf.exists()) {
                System.out.println("file does not exist");
                return;
            }
            Bitmap ThumbImage = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(res), THUMBSIZE, THUMBSIZE);
            Long start = System.currentTimeMillis();
            baseApi.setImage(ReadFile.readFile(mf));
            String recognizedText = baseApi.getUTF8Text();
            Long end = System.currentTimeMillis();
            OCRecordStorage.addRecord(new OCRecord(ThumbImage, recognizedText, new Date(), end - start, false));
        } else if (data.getClipData() != null) {
            int count = data.getClipData().getItemCount();
            int n_elem = count;
            for (int i = 0; i < count; ++i) {
                Uri uri = data.getClipData().getItemAt(i).getUri();
                String res = getRealPathFromURI(getApplicationContext(), uri);
                File mf = new File(res == null ? "" : res);
                if (!mf.exists()) {
                    n_elem--;
                    continue;
                }
                Bitmap ThumbImage = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(res), THUMBSIZE, THUMBSIZE);
                Long start = System.currentTimeMillis();
                baseApi.setImage(ReadFile.readFile(mf));
                String recognizedText = baseApi.getUTF8Text();
                Long end = System.currentTimeMillis();
                OCRecordStorage.addRecord(new OCRecord(ThumbImage, recognizedText, new Date(), end - start, false));
            }
        }
    }

    private void doOCRRemotely_PhotosFromAlbum(Intent data, final int msg_id){
        if (data.getClipData() == null && data.getData() != null) {
            String res = getRealPathFromURI(getApplicationContext(), data.getData());
            File mf = new File(res == null ? "" : res);
            if (!mf.exists()) {
                System.out.println("file does not exist");
                return;
            }
            final Bitmap ThumbImage = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(res), THUMBSIZE, THUMBSIZE);
            final Bitmap bm = BitmapFactory.decodeFile(mf.getPath());
            new Thread(new Runnable() {
                @Override
                public void run() {
                File f = saveBitmapFile(bm, "temp");
                List<File> list = new ArrayList<File>();
                list.add(f);
                Long start = System.currentTimeMillis();
                JSONObject js = OCRConnectionManager.OCRemote(list);

                try {
                    JSONObject result = js.getJSONObject("result");
                    JSONArray jslist = result.getJSONArray("texts");
                    JSONObject info = jslist.getJSONObject(0);
                    //System.out.println(info.toString());
                    OCRecordStorage.addRecord(new OCRecord(ThumbImage, info.getString("text"), new Date(), System.currentTimeMillis() - start, true));
                    mHandler.obtainMessage(msg_id).sendToTarget();
                } catch (Exception e){
                    e.printStackTrace();
                }
                }
            }).start();
        } else if (data.getClipData() != null) {
            int count = data.getClipData().getItemCount();

            Log.i(TAG, "User selected " + count + " images");
            int n_elem = count;
            List<File> flist = new ArrayList<File>();
            List<Bitmap> bmlist = new ArrayList<Bitmap>();
            for (int i = 0; i < count; ++i) {
                Uri uri = data.getClipData().getItemAt(i).getUri();
                String path = uri.getPath();
                String res = getRealPathFromURI(getApplicationContext(), uri);
                File mf = new File(res == null ? "" : res);
                if (!mf.exists()) {
                    n_elem--;
                    continue;
                }
                Bitmap ThumbImage = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(res), THUMBSIZE, THUMBSIZE);
                Bitmap bm = BitmapFactory.decodeFile(mf.getPath());
                File f = saveBitmapFile(bm, mf.getName());
                flist.add(f);
                bmlist.add(ThumbImage);
            }
            final List<File> flist_remote = flist;
            final List<Bitmap> bmlist_remote = bmlist;
            new Thread(new Runnable() {
                @Override
                public void run() {
                Long start = System.currentTimeMillis();
                JSONObject js = OCRConnectionManager.OCRemote(flist_remote);
                try {
                    JSONObject result = js.getJSONObject("result");
                    JSONArray jslist = result.getJSONArray("texts");
                    for(int i=0; i<jslist.length(); i++){
                        JSONObject info = jslist.getJSONObject(i);
                        OCRecordStorage.addRecord(new OCRecord(bmlist_remote.get(i), info.getString("text"), new Date(), System.currentTimeMillis() - start, true));
                    }
                    mHandler.obtainMessage(msg_id).sendToTarget();
                } catch (Exception e){
                    e.printStackTrace();
                }
                }
            }).start();
        }
    }

    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Intent intent;
            switch (msg.what){
                case MSG_CAMERA_REMOTE_FINISHED:
                    showProgress(false);
                    intent = new Intent();
                    intent.setClass(MainActivity.this, DisplayActivity.class);
                    startActivity(intent);
                    break;
                case MSG_GALLERY_REMOTE_FINISHED:
                    showProgress(false);
                    intent = new Intent();
                    intent.setClass(MainActivity.this, DisplayActivity.class);
                    startActivity(intent);
                    break;
                case MSG_BENCHMARK_FINISHED:
                    showProgress(false);
                    intent = new Intent();
                    intent.setClass(MainActivity.this, BenchmarkActivity.class);
                    startActivity(intent);
                    break;
            }
        }
    };

    public File saveBitmapFile(Bitmap bitmap, String name){
        File file=new File(Environment.getExternalStorageDirectory().toString()+"/"+name+".jpg");
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    public String getRealPathFromURI(android.content.Context context, Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    public void initOCR() {
        String[] paths = new String[]{DATA_PATH, DATA_PATH + "tessdata/"};

        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.i(TAG, "ERROR: Creation of directory " + DATA_PATH + " on sdcard failed");
                    return;
                } else {
                    Log.i(TAG, "Created directory " + DATA_PATH + " on sdcard");
                }
            }
        }

        if (!(new File(DATA_PATH + "tessdata/eng.traineddata")).exists()) {
            try {
                InputStream in = getResources().openRawResource(R.raw.eng);
                OutputStream out = new FileOutputStream(DATA_PATH + "tessdata/eng.traineddata");

                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
            } catch (IOException e) {
                Log.i(TAG, "Was unable to copy eng traineddata " + e.toString());
            }
        }

        baseApi = new TessBaseAPI();
        baseApi.init(DATA_PATH, LANG);

    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mMainPageView.setVisibility(show ? View.GONE : View.VISIBLE);
            mMainPageView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mMainPageView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mMainPageView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}

package com.peerbits.nfccardread;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.peerbits.nfccardread.data_manager.FileLogger;

import java.util.Calendar;
import java.util.TimeZone;

public class NfcHome extends Activity implements View.OnClickListener {

    public static final String TAG = NfcHome.class.getSimpleName();
    private RelativeLayout rlRead;
    //private RelativeLayout rlWrite;
   // private RelativeLayout rlCreditCard;
    //private ImageView ivHomeicon;
    //private Animation mAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nfc_home);
        initViews();
        rlRead.setOnClickListener(this);

        //rlWrite.setOnClickListener(this);
        //rlCreditCard.setOnClickListener(this);


    }

    private void initViews() {

        rlRead = findViewById(R.id.rlReadNFCTAG);
        //rlWrite = findViewById(R.id.rlWriteWithNFC);
        //rlCreditCard = findViewById(R.id.rlCreditCard);
        //ivHomeicon = findViewById(R.id.ivHomeicon);
        //mAnimation = AnimationUtils.loadAnimation(NfcHome.this, R.anim.swinging);
    }


    @Override
    public void onClick(View view) {
        Intent intent;
        switch (view.getId()) {
            case R.id.rlReadNFCTAG:
                intent = new Intent(this, NFCRead.class);
                this.startActivity(intent);
             //   testUpload();
                break;

            //case R.id.rlWriteWithNFC:
             //   intent = new Intent(this, NFCWrite.class);
              //  this.startActivity(intent);
              //  break;

            //case R.id.rlCreditCard:
              //  intent = new Intent(this, NFCCardReading.class);
               // this.startActivity(intent);
               // break;
        }
    }


    /**
     * Test function to demonstrate file upload independent of actual collected data
     */
    private void testUpload() {

        String currentISO8601Timestamp = String.format("%tFT%<tTZ.%<tL",
                Calendar.getInstance(TimeZone.getTimeZone("Z")));

        String text = "This is a test String";
        try {
            //create file logger and get runtime permissions
            FileLogger mFileLogger = new FileLogger();
            mFileLogger.checkRuntimeWriteExternalStoragePermission(this, this);

            mFileLogger.appendHeader("Data at " + currentISO8601Timestamp);
            mFileLogger.appendLine(text);

            //upload file
            mFileLogger.uploadFile(currentISO8601Timestamp);

            //display file saved message
            Toast.makeText(getBaseContext(), "File saved successfully!",
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    @Override
    public void onResume() {
        super.onResume();
        //ivHomeicon.startAnimation(mAnimation);
    }

    @Override
    public void onPause() {
        //ivHomeicon.clearAnimation();
        super.onPause();
    }
}

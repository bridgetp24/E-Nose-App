package com.peerbits.nfccardread;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bhargavms.dotloader.DotLoader;
import com.peerbits.nfccardread.data_manager.FileLogger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.TimeZone;

public class NFCRead extends Activity {

    public static final String TAG = "NFCReadDebug";
    private TextView tvNFCMessage;
    private NfcAdapter mNfcAdapter;
    private DotLoader dotloader;
    private ImageView ivBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nfc_read);
        initViews();
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        Log.d(TAG, "NFCRead OnCreate");

    }

    private void initViews() {
        tvNFCMessage = findViewById(R.id.tvNFCMessage);
        dotloader = findViewById(R.id.text_dot_loader);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        ivBack = findViewById(R.id.ivBack);
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        IntentFilter techDetected = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        IntentFilter[] nfcIntentFilter = new IntentFilter[]{techDetected, tagDetected, ndefDetected};

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        if (mNfcAdapter != null)
            mNfcAdapter.enableForegroundDispatch(this, pendingIntent, nfcIntentFilter, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null)
            mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        patchTag(tag);
        if (tag != null) {
            readFromNFC(tag, intent);
        }
    }


    public Tag patchTag(Tag oTag) {
        if (oTag == null)
            return null;

        String[] sTechList = oTag.getTechList();

        Parcel oParcel, nParcel;

        oParcel = Parcel.obtain();
        oTag.writeToParcel(oParcel, 0);
        oParcel.setDataPosition(0);

        int len = oParcel.readInt();
        byte[] id = null;
        if (len >= 0) {
            id = new byte[len];
            oParcel.readByteArray(id);
        }
        int[] oTechList = new int[oParcel.readInt()];
        oParcel.readIntArray(oTechList);
        Bundle[] oTechExtras = oParcel.createTypedArray(Bundle.CREATOR);
        int serviceHandle = oParcel.readInt();
        int isMock = oParcel.readInt();
        IBinder tagService;
        if (isMock == 0) {
            tagService = oParcel.readStrongBinder();
        } else {
            tagService = null;
        }
        oParcel.recycle();

        int nfca_idx = -1;
        int mc_idx = -1;

        for (int idx = 0; idx < sTechList.length; idx++) {
            if (sTechList[idx] == NfcA.class.getName()) {
                nfca_idx = idx;
            } else if (sTechList[idx] == MifareClassic.class.getName()) {
                mc_idx = idx;
            }
        }

        if (nfca_idx >= 0 && mc_idx >= 0 && oTechExtras[mc_idx] == null) {
            oTechExtras[mc_idx] = oTechExtras[nfca_idx];
        } else {
            return oTag;
        }

        nParcel = Parcel.obtain();
        nParcel.writeInt(id.length);
        nParcel.writeByteArray(id);
        nParcel.writeInt(oTechList.length);
        nParcel.writeIntArray(oTechList);
        nParcel.writeTypedArray(oTechExtras, 0);
        nParcel.writeInt(serviceHandle);
        nParcel.writeInt(isMock);
        if (isMock == 0) {
            nParcel.writeStrongBinder(tagService);
        }
        nParcel.setDataPosition(0);
        Tag nTag = Tag.CREATOR.createFromParcel(nParcel);
        nParcel.recycle();

        return nTag;
    }


    private void readFromNFC(Tag tag, Intent intent) {

        try {
            Ndef ndef = Ndef.get(tag);

            if (ndef != null) {
                ndef.connect();
                NdefMessage ndefMessage = ndef.getNdefMessage();

                if (ndefMessage != null) {
                    /*String message = new String(ndefMessage.getRecords()[0].getPayload());
                    Log.d(TAG, "NFC found.. "+"readFromNFC: "+message );
                    tvNFCMessage.setText(message);*/

                    Parcelable[] messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

                    if (messages != null) {
                        NdefMessage[] ndefMessages = new NdefMessage[messages.length];
                        for (int i = 0; i < messages.length; i++) {
                            ndefMessages[i] = (NdefMessage) messages[i];
                        }
                        NdefRecord record = ndefMessages[0].getRecords()[0];

                        byte[] payload = record.getPayload();
                        String text = new String(payload);

                        tvNFCMessage.setText(text);
                        dotloader.setVisibility(View.GONE);
                        String currentISO8601Timestamp = String.format("%tFT%<tTZ.%<tL",
                                Calendar.getInstance(TimeZone.getTimeZone("Z")));

                        Log.e("tag", "vahid  -->  " + text);
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

                        ndef.close();

                    }

                } else {
                    Toast.makeText(this, "Not able to read from NFC, Please try again...", Toast.LENGTH_LONG).show();

                }
            } else {
                NdefFormatable format = NdefFormatable.get(tag);
                String [] techList = tag.getTechList();

                for(String permission : techList ) {
                    Log.d(TAG, "Permission: " + permission);

                }

                if (format != null) {
                    try {
                        format.connect();

                        try{
//
                            //can't add message on the main thread...
//                            NdefMessage testMessage = getTestMessage();
//                            format.format(testMessage);

                            NdefMessage ndefMessage = ndef.getNdefMessage();
                            String message = new String(ndefMessage.getRecords()[0].getPayload());
                            Log.d(TAG, "NFC found.. " + "readFromNFC: " + message);
                            tvNFCMessage.setText(message);
                            ndef.close();

                        } catch(IOException e) {
                            Log.d(TAG, "IO Exception caught");
                            e.printStackTrace();
                            Toast.makeText(this, "Not able to read from NFC, Please try again...", Toast.LENGTH_LONG).show();
                        }catch(NullPointerException e) {
                            Log.d(TAG, "Null pointer exception caught");

                            Toast.makeText(this, "Not able to read from NFC, Please try again...", Toast.LENGTH_LONG).show();
                        } finally{
                           // format.close();
                        }


                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(this, "NFC is not readable", Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public NdefMessage getTestMessage() {
        String msg = "Test Message";
        byte[] languageCode;
        byte[] msgBytes;
        languageCode = "en".getBytes(StandardCharsets.US_ASCII);
        msgBytes = msg.getBytes(StandardCharsets.UTF_8);

        byte[] messagePayload = new byte[1 + languageCode.length
                + msgBytes.length];
        messagePayload[0] = (byte) 0x02; // status byte: UTF-8 encoding and
        // length of language code is 2
        System.arraycopy(languageCode, 0, messagePayload, 1,
                languageCode.length);
        System.arraycopy(msgBytes, 0, messagePayload, 1 + languageCode.length,
                msgBytes.length);

        NdefMessage message;
        NdefRecord[] records = new NdefRecord[1];
        NdefRecord textRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                NdefRecord.RTD_TEXT, new byte[]{}, messagePayload);
        records[0] = textRecord;
        message = new NdefMessage(records);
        return message;
    }
}

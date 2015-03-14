package com.chariotinstruments.chariotgauge;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class PSensor extends Activity {
    /** Called when the activity is first created. */

    //Constants..
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT      = 2;

    // Debugging
    private static final String TAG = "ProjectSensor";
    private static final boolean D  = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ         = 2;
    public static final int MESSAGE_WRITE        = 3;
    public static final int MESSAGE_DEVICE_NAME  = 4;
    public static final int MESSAGE_TOAST        = 5;

    //Used to show whats new dialog.
    private static final String PRIVATE_PREF = "myapp";
    private static final String VERSION_KEY  = "version_number";

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST       = "toast";
    
    //DEBUG
    private boolean debug;

    //Global Variables.
    TextView  titleText;
    Typeface  typeFaceBtn;
    Typeface  typeFaceTitle;
    Button    btnConnect;
    Button    btnSettings;
    Button    btnWideband;
    Button    btnBoost;
    Button    btnOil;
    Button    btnCustom;
    Button    btnMulti1;
    Button    btnMulti2;
    Button    btnRPM;
    Button    btnSpeed;
    Button    btnVolts;

    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothSerialService mSerialService = null;
    int intReadMsgPrevious = 0;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);

        //Set the screen to the main.xml layout.
        setContentView(R.layout.psensor_layout);

        //Show the whats new dialog if this is the first time run
        showWhatsNew();

        //Get the instances of the layout objects.
        titleText       = (TextView) findViewById(R.id.title_text);
        btnConnect      = (Button)   findViewById(R.id.connectBtn);
        btnSettings     = (Button)   findViewById(R.id.settingsBtn);
        btnWideband     = (Button)   findViewById(R.id.widebandBtn);
        btnBoost        = (Button)   findViewById(R.id.boostBtn);
        btnOil          = (Button)   findViewById(R.id.oilBtn);
        btnCustom       = (Button)   findViewById(R.id.customBtn);
        btnMulti1       = (Button)   findViewById(R.id.multiBtn1);
        btnMulti2       = (Button)   findViewById(R.id.multiBtn2);
        btnRPM          = (Button)   findViewById(R.id.rpmBtn);
        btnSpeed        = (Button)   findViewById(R.id.speedBtn);
        btnVolts        = (Button)   findViewById(R.id.voltBtn);
        
        try {
            typeFaceBtn     = Typeface.createFromAsset(getAssets(), "fonts/CaviarDreams_Bold.ttf");
            typeFaceTitle   = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Bold.ttf");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        //Initialize debug
        debug = false;

        //Set the font of the title text
        titleText.setTypeface(typeFaceTitle);
        btnConnect.setTypeface(typeFaceBtn);
        btnSettings.setTypeface(typeFaceBtn);
        btnWideband.setTypeface(typeFaceBtn);
        btnBoost.setTypeface(typeFaceBtn);
        btnOil.setTypeface(typeFaceBtn);
        btnCustom.setTypeface(typeFaceBtn);
        btnMulti1.setTypeface(typeFaceBtn);
        btnMulti2.setTypeface(typeFaceBtn);
        btnRPM.setTypeface(typeFaceBtn);
        btnSpeed.setTypeface(typeFaceBtn);
        btnVolts.setTypeface(typeFaceBtn);


        //Check if there is a BluetoothSerialService object being passed back. If true then don't run through initial setup.
        Object obj = PassObject.getObject();
        //Assign it to global mSerialService variable in this activity.
        if(!debug){
            mSerialService = (BluetoothSerialService)obj;
        }

        if(mSerialService != null){
            //Update the BluetoothSerialService instance's handler to this activities.
            mSerialService.setHandler(mHandler);
            //Update the connection status on the dashboard.
            if (getConnectionState() == BluetoothSerialService.STATE_CONNECTED) {
                btnConnect.setText("Disconnect");
            }else{
                btnConnect.setText("Connect");
            }
        }else{
            //Looks like an initial launch - Call the method that sets up bluetooth on the device.
            btnConnect.setText("Connect");
            if(!debug){
                setupBT();
            }
        }
    }
    


    public void onDestroy() {
        super.onDestroy();

        if (mSerialService != null){
            Log.d(TAG, "onDestroy()");
            mSerialService.stop();
        }
        
    }

    public void onResume(){
        super.onResume();
        if(!debug){
            mSerialService.setHandler(mHandler);
        }
    }


    public int getConnectionState() {
        return mSerialService.getState();
    }

    public void setupBT(){

        //Get the bluetooth device adapter, if there is not one, toast.
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "This device does not support Bluetooth", Toast.LENGTH_SHORT).show();
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        if(mSerialService==null){
            mSerialService = new BluetoothSerialService(this, mHandler);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

        case REQUEST_CONNECT_DEVICE: 

            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                //txtView.setText(txtView.getText()+ "\n\n" + device.getName() + "\n" +device.getAddress());
                mSerialService.connect(device);                
            }
            break;

        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                //If OK (device contains bluetooth connectivity, user did not click "no"):
                Toast.makeText(getApplicationContext(), "Enabled Bluetooth OK", Toast.LENGTH_SHORT).show();              
            }else{
                //If NOT OK, say so.
                Toast.makeText(getApplicationContext(), "Bluetooth NOT enabled or not Present", Toast.LENGTH_SHORT).show();
            }
        }
    }


    public void connectDevice(){
        if (getConnectionState() == BluetoothSerialService.STATE_NONE) {
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
        }else if(getConnectionState() == BluetoothSerialService.STATE_CONNECTED){
            if(mSerialService != null){
                mSerialService.stop();
            }
            //mSerialService.start(); //--potential error, leaving for now.
//        }else if(getConnectionState() == BluetoothSerialService.STATE_CONNECTING){
//            if(mSerialService != null){
//                mSerialService.stop(); 
//            }
//            //mSerialService.start(); //--potential error, leaving for now.
        }
    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE1: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothSerialService.STATE_CONNECTED:
                    btnConnect.setClickable(true);
                    btnConnect.setText("Connected! \n Tap to Disconnect");
                    break;
                case BluetoothSerialService.STATE_CONNECTING:
                    btnConnect.setText("Connecting...");
                    break;
                case BluetoothSerialService.STATE_LISTEN:
                    btnConnect.setClickable(true);
                    break;
                case BluetoothSerialService.STATE_NONE:
                    btnConnect.setClickable(true);
                    btnConnect.setText("Connect");
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                ////mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case MESSAGE_READ:
                int intReadMessage = 0;
                
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);

                try {
                    intReadMessage = Integer.parseInt(readMessage);
                    intReadMsgPrevious = intReadMessage;
                } catch (NumberFormatException e) {
                    intReadMessage = intReadMsgPrevious;	
                }

                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                // mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                //                Toast.makeText(getApplicationContext(), "Connected to "
                //                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                        Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

    private void showWhatsNew() {
        SharedPreferences sharedPref    = getSharedPreferences(PRIVATE_PREF, this.MODE_PRIVATE);
        int currentVersionNumber        = 0;
        int savedVersionNumber          = sharedPref.getInt(VERSION_KEY, 0);

        try {
            PackageInfo pi          = getPackageManager().getPackageInfo(getPackageName(), 0);
            currentVersionNumber    = pi.versionCode;
        } catch (Exception e) {}

        if (currentVersionNumber > savedVersionNumber) {
            showWhatsNewDialog();
            Editor editor   = sharedPref.edit();
            editor.putInt(VERSION_KEY, currentVersionNumber);
            editor.commit();
        }
    }

    private void showWhatsNewDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view               = inflater.inflate(R.layout.dialog_whatsnew, null);
        Builder builder         = new AlertDialog.Builder(this);
        builder.setView(view).setTitle("Whats New").setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.create().show();
    }

    public void onClickActivity (View v){
        int id = v.getId();
        switch (id){
        case R.id.connectBtn:
            connectDevice();
            break;
        case R.id.settingsBtn:
            PassObject.setObject(mSerialService);
            startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
            break;
        case R.id.widebandBtn:
            PassObject.setObject(mSerialService);
            startActivity(new Intent(getApplicationContext(), WidebandActivity.class));
            //startActivity(new Intent(getApplicationContext(), SingleChartActivity.class));
            break;
        case R.id.customBtn:
            PassObject.setObject(mSerialService);
            startActivity(new Intent(getApplicationContext(), TemperatureActivity.class));
            break;
        case R.id.boostBtn:
            PassObject.setObject(mSerialService);
            startActivity(new Intent(getApplicationContext(), BoostActivity.class));
            break;
        case R.id.rpmBtn:
            PassObject.setObject(mSerialService);
            startActivity(new Intent(getApplicationContext(), RPMActivity.class));
            break;
        case R.id.oilBtn:
            PassObject.setObject(mSerialService);
            startActivity(new Intent(getApplicationContext(), OilActivity.class));
            break;
        case R.id.speedBtn:
            PassObject.setObject(mSerialService);
            startActivity(new Intent(getApplicationContext(), SpeedActivity.class));
            break;
        case R.id.voltBtn:
            PassObject.setObject(mSerialService);
            startActivity(new Intent(getApplicationContext(), VoltageActivity.class));
            break;
        case R.id.multiBtn1:
            PassObject.setObject(mSerialService);
            startActivity(new Intent(getApplicationContext(), TwoGaugeActivity.class));
            break;
        case R.id.multiBtn2:
            PassObject.setObject(mSerialService);
            startActivity(new Intent(getApplicationContext(), FourGaugeActivity.class));
            break;
        default: 
            break;
        }
    }
}


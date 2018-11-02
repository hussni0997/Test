package com.example.hussn.test;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.ParcelUuid;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, AdapterView.OnItemSelectedListener {


    private static final int MY_PERMISSIONS_REQUEST = 1;
    private int REQUEST_CAMERA = 0, SELECT_FILE = 1;
    private static final String TAG = "Main Activity";
    private Button StartBt;
    private Button Send;
    private Spinner Spin;
    private ListView List;
    private TextView Text;
    private Button ConnectBt;
    private ImageView Image;
    private Button Select;
    public SpinAdapter mSpinner;
    public BluetoothAdapter mBTAdapter;
    public ArrayList<BluetoothDevice>BtDevicesArray=new ArrayList<>();
    public ArrayList<String> BtDeviceList = new ArrayList<>();
    public ArrayAdapter<String> ad;
    public BluetoothDevice SelectedTuppy;
    private ParcelUuid[] TpUuid;
    private UUID TupUuid;
    public ArrayAdapter<String> SpinAdapter;
    BtConnectionServices BtConnection;
    private String userChoosenTask;
    private int imageSelected_flag = 0;
    private static Bitmap BWbitmap;
    private int image_bit_flag = 1;
    public ByteBuffer ImageBuffer;

    private int DiscoverableConts=0;



    private final BroadcastReceiver Receiver1 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ( action.equals(mBTAdapter.ACTION_STATE_CHANGED)){
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBTAdapter.ERROR);

                switch (state){
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG,"onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG,"onReceive: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG,"onReceive: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG,"onReceive: STATE TURNING ON");
                        break;
                }


            }
        }
    };



    private final BroadcastReceiver Receiver2 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"onReceive: ACTION FOUND");
            String action = intent.getAction();
            if ( action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device!=null && !BtDevicesArray.contains(device)) {
                    if (device.getName() != null) {
                        if(device.getName().toLowerCase().contains("Tuppy".toLowerCase())){
                            BtDevicesArray.add(device);
                            BtDeviceList.add(device.getName());
                            SpinAdapter.notifyDataSetChanged();
                            //device.createBond();
                            Log.d(TAG, "onReceive: " + device.getName());
                            //ad = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, BtDeviceList);
                            //List.setAdapter(ad);
                            //SpinAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, BtDeviceList);
                            //Spin.setAdapter(SpinAdapter);
                        }
                    }
                }
            }
        }
    };

    private final BroadcastReceiver Receiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {

                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    //Device is in Discoverable Mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "Receiver3: Discoverability Enabled.");
                        DiscoverableConts = 1;
                        startDiscover();
                        break;
                    //Device not in discoverable mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "Receiver3: Discoverability Disabled. Able to receive connections.");
                        DiscoverableConts = 0;
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "Receiver3: Discoverability Disabled. Not able to receive connections.");
                        DiscoverableConts=0;
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "Receiver3: Connecting....");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "Receiver3: Connected.");
                        break;
                }

            }
        }
    };


    String Pin ="0000";
    private final BroadcastReceiver Receiver4 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: ACTION PAIRING REQUEST");
            String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_PAIRING_REQUEST)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                device.setPin(Pin.getBytes());
                Log.d(TAG, "onReceive: setting pin for " + device.getName());
            }

        }
    };




    @Override
    protected void onDestroy(){
        unregisterReceiver(Receiver1);
        unregisterReceiver(Receiver2);
        unregisterReceiver(Receiver3);
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG,"onCreate");

        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        StartBt = findViewById(R.id.button);
        ConnectBt = findViewById(R.id.Connect);
        Send = findViewById(R.id.Send);
        //List = findViewById(R.id.List);
        Spin = findViewById(R.id.spinner);
        Spin.setOnItemSelectedListener(MainActivity.this);
        Text = findViewById(R.id.TextSelect);
        Image = findViewById(R.id.imageView);
        Select = findViewById(R.id.SelectImage);

        SpinAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, BtDeviceList);
        Spin.setAdapter(SpinAdapter);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);
        registerReceiver(Receiver4,filter);

        StartBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableBt();
                while(!mBTAdapter.isEnabled()){
                    //waits for bluetooth to be enabled first
                }
                enableDiscoverable();
                //startDiscover(); cannot be put here because enable discoverable requires permission and immediate
                //request of starting discovery would result in a hold up. Instead startDiscover is initialize at Broadcast
                //Receiver once permission is granted for discoverable

            }
        });

        ConnectBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(BtConnection!=null){
                    BtConnection.CancelConnections();
                }
                if (SelectedTuppy!=null){
                    // Instantiates the class and creates a Server socket listening for SelectedTuppy
                    BtConnection = new BtConnectionServices(MainActivity.this,SelectedTuppy);
                    // Starts Client connection ie. Bluetooth Socket to connect with SellectedTuppy
                    BtConnection.startClient(SelectedTuppy);
                    Toast.makeText(getApplicationContext(), "Connected with "+ SelectedTuppy.getName(), Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(getApplicationContext(), "No Tuppies Selected", Toast.LENGTH_LONG).show();
                }
            }
        });

        Send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(BtConnection!=null){
                    BtConnection.write(Pin.getBytes());
                    if(BWbitmap!=null){
                        UploadImage();
                    }else{
                        Toast.makeText(getApplicationContext(), "No Image Selected", Toast.LENGTH_LONG).show();
                    }

                }else{
                    Toast.makeText(getApplicationContext(), "No Tuppies Selected", Toast.LENGTH_LONG).show();
                }

            }
        });

        Select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }
        });
    }

    public void enableBt(){
        Log.d(TAG,"enableBt On");
        if (mBTAdapter == null){Log.d(TAG,"startstopBT: Device dose not have Bluetooth Capabilities");}
        if (!mBTAdapter.isEnabled()){
            Log.d(TAG,"startstopBT : enabling BT.");
            Intent startBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(startBT);
            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(Receiver1,BTIntent);

        }
        else {
            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(Receiver1,BTIntent);
            Log.d(TAG,"Bluetooth is on");
        }
    }
    public void startDiscover(){
        Log.d(TAG,"startDiscover: looking for unpaired devices");
        if(mBTAdapter.isDiscovering()){
            mBTAdapter.cancelDiscovery();
            //BtDevicesArray.clear();
            //BtDeviceList.clear();
            ad = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, BtDeviceList);
            List.setAdapter(ad);
            Log.d(TAG,"Cancelling discovery");
            checkBTPermissions();
            mBTAdapter.startDiscovery();
            Log.d(TAG,"Starting Discovery");
            IntentFilter ii = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            Log.d(TAG,"Intent filter set");
            registerReceiver(Receiver2,ii);
            Log.d(TAG,"Receiver2 registered to intent filter");
        }
        if(!mBTAdapter.isDiscovering()){
            checkBTPermissions();
            //BtDevicesArray.clear();
            //BtDeviceList.clear();
            mBTAdapter.startDiscovery();
            Log.d(TAG,"Starting Discovery");
            IntentFilter i1 = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            Log.d(TAG,"Intent filter set");
            registerReceiver(Receiver2,i1);
            Log.d(TAG,"Receiver2 registered to intent filter");

        }
    }
    public void enableDiscoverable(){
        Log.d(TAG, "btnEnableDisable_Discoverable: Making device discoverable for 300 seconds.");
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
        IntentFilter intentFilter = new IntentFilter(mBTAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(Receiver3,intentFilter);
    }
    private void checkBTPermissions() {
        Log.d(TAG, "checkBTPermissions: requesting access course/fine location");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION};
        ActivityCompat.requestPermissions(this,permissions,MY_PERMISSIONS_REQUEST);
    }
    // gives user access to take image from Camera or gallery
    private void selectImage() {
        final CharSequence[] items = {
                "Camera",
                "Photo Gallery",
                "Cancel" };


        // Creates dialog box so the user can choose options of camera, gallery or cancel for the source of image
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Select an Image");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                boolean result=Utility.checkPermission(MainActivity.this);

                if (items[item].equals("Camera")) {
                    userChoosenTask ="Camera";
                    if(result) {
                        cameraIntent();
                    }

                } else
                if (items[item].equals("Photo Gallery")) {
                    userChoosenTask ="Photo Gallery";
                    if(result)
                        galleryIntent();

                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }


    // Sends request message to gallery to get content
    private void galleryIntent()
    {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);//
        startActivityForResult(Intent.createChooser(intent, "Select File"),SELECT_FILE);
    }
    // Sends request to Camera to use it to capture image
    private void cameraIntent()
    {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SELECT_FILE) {
                onSelectFromGalleryResult(data);
            } else if (requestCode == REQUEST_CAMERA) {
                onCaptureImageResult(data);
            }
        }
    }

    private void onCaptureImageResult(Intent data) {
        //get the returned data
        Bundle extras = data.getExtras();

        //get the cropped bitmap
        Bitmap capturedImage = extras.getParcelable("data");
        Bitmap resizedImage = Bitmap.createScaledBitmap(capturedImage, 400, 240, true);

        Bitmap newBitmap = convertImage(resizedImage);		// convert color image to bw
        Image.setImageBitmap(newBitmap);				// display the image on App

        //UploadImage.setVisibility(View.VISIBLE);
        imageSelected_flag = 1;

        BWbitmap = newBitmap;
        image_bit_flag=8;
        //check_graylevel();
    }
    @SuppressWarnings("deprecation")
    private void onSelectFromGalleryResult(Intent data) {
        Bitmap bm = null;
        if (data != null) {
            try {
                bm = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), data.getData());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //UploadImage.setVisibility(View.VISIBLE);
        imageSelected_flag = 1;
        Bitmap newBitmap = convertImage(bm);		// convert color image to bw
        Image.setImageBitmap(newBitmap);			// display the image on App

        BWbitmap = newBitmap;
        image_bit_flag=8;
        //check_graylevel();
    }
    // Function : Convert Color Image (input) to Black and White Image (output)
    public static Bitmap convertImage(Bitmap original){
        Bitmap finalImage = Bitmap.createBitmap(original.getWidth(), original.getHeight(), original.getConfig());

        int A, R, G, B;
        int colorPixel;
        int width = original.getWidth();
        int height = original.getHeight();
        int i = 0;

        for (int x = 0; x < width; x++){
            for (int y = 0; y < height; y++){
                colorPixel = original.getPixel(x, y);
                A = Color.alpha(colorPixel);
                R = Color.red(colorPixel);
                G = Color.green(colorPixel);
                B = Color.blue(colorPixel);

                R = G = B = (int) (0.299 * R + 0.587 * G + 0.114 * B);

                finalImage.setPixel(x, y, Color.argb(A, R, G, B));
            }
        }
        return finalImage;
    }
    public void check_graylevel(){
        int i = 0;
        byte pixel_gray = 0x00;

        for (int y = 0; y < 240; y++){
            for (int x = 0; x < 400; x++) {
                // original 16 gray level conversion look-up table
                pixel_gray = (byte) BWbitmap.getPixel(x, y);
                i++;
                if((pixel_gray != (byte)0x00) && (pixel_gray != (byte)0xFF)){
                    image_bit_flag = 8;
                    break;
                } else{
                    image_bit_flag = 1;
                }
            }
        }
    }

    public void UploadImage(){
        ImageBuffer = ByteBuffer.allocate(BWbitmap.getByteCount());
        BWbitmap.copyPixelsToBuffer(ImageBuffer);
        BtConnection.write(ImageBuffer.array());
    }






    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        /*
        Log.d(TAG, "onItemClick: clicked");
        SelectedTuppy = BtDevicesArray.get(position);
        //SelectedTuppy.createBond();
        if(SelectedTuppy!=null && mBTAdapter.getBondedDevices().contains(SelectedTuppy)) {
            Log.d(TAG, "onItemSelected: getting uuid");
            TpUuid = SelectedTuppy.getUuids();
            TupUuid = TpUuid[0].getUuid();
            Text.setText(SelectedTuppy.getName());
            Log.d(TAG, "SelectedTuppy is " + BtDevicesArray.get(position).getName());
        }*/
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG, "onItemSelected: clicked");
        SelectedTuppy = BtDevicesArray.get(position);
        if(!mBTAdapter.getBondedDevices().contains(SelectedTuppy)){
            SelectedTuppy.setPin(Pin.getBytes());
            SelectedTuppy.createBond();
        }

        if(SelectedTuppy!=null && mBTAdapter.getBondedDevices().contains(SelectedTuppy)) {
            Log.d(TAG, "onItemSelected: getting uuid");
            TpUuid = SelectedTuppy.getUuids();
            TupUuid = TpUuid[0].getUuid();
            Text.setText(SelectedTuppy.getName());
            Log.d(TAG, "SelectedTuppy is " + BtDevicesArray.get(position).getName());
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: handling permissions");
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG,"handled as Permission granted");
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    Log.d(TAG,"handled as Permission not granted");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }

    }






}




package com.example.hussn.test;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class SpinAdapter extends BaseAdapter {
    Context context;
    ArrayList<BluetoothDevice> BtDeviceArray;
    LayoutInflater inflter;
    String[] BtList;
    private static final String TAG = "SpinAdapter";

    public SpinAdapter(Context ApplicationContext , ArrayList<BluetoothDevice> BtDevices){

        Log.d(TAG,"Spin Adapter created");
        int i=0;
        this.context = ApplicationContext;
        this.BtDeviceArray = BtDevices;
        inflter = (LayoutInflater.from(ApplicationContext));
        for (BluetoothDevice deviced : BtDevices) {
                BtList[i++] = deviced.getName();
        }
    }

    @Override
    public int getCount() {
        return BtDeviceArray.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        view = inflter.inflate(R.layout.spinner,null);
        TextView BtNames = view.findViewById(R.id.textViewspin);
        BtNames.setText(BtList[i]);
        return view;
    }
}

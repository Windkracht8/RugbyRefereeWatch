/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.windkracht8.rugbyrefereewatch;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.garmin.android.connectiq.IQDevice;

import org.json.JSONObject;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

@SuppressLint("MissingPermission")//Handled by Permissions.hasXPermission
public class DeviceSelect extends Activity implements Comms.Interface{
    private ImageView device_select_loading;
    private LinearLayout device_select_ll;

    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_select);
        findViewById(android.R.id.content).setOnApplyWindowInsetsListener(Main.onApplyWindowInsetsListener);
        device_select_ll = findViewById(R.id.device_select_ll);

        findViewById(R.id.device_select_new).setOnClickListener(v->{
            device_select_loading = findViewById(R.id.device_select_loading);
            ((AnimatedVectorDrawable) device_select_loading.getBackground()).start();
            device_select_loading.setVisibility(View.VISIBLE);
            Executors.newCachedThreadPool().execute(this::loadBTDevices);
        });

        LinearLayout device_select_known = findViewById(R.id.device_select_known);

        findViewById(R.id.device_select_garmin_new).setOnClickListener(v->{
            device_select_loading = findViewById(R.id.device_select_loading);
            ((AnimatedVectorDrawable) device_select_loading.getBackground()).start();
            device_select_loading.setVisibility(View.VISIBLE);
            Executors.newCachedThreadPool().execute(this::loadIQDevices);
        });

        if(Main.comms == null) return;
        Main.comms.knownBTDevices.forEach(d->deviceFound(device_select_known, d));
        Main.comms.knownIQDevices.forEach(d->deviceFound(device_select_known, d));

        try{
            Main.comms.addListener(this);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "DeviceSelect.onCreate Failed to add as a listener: " + e.getMessage());
        }
    }
    @Override public void onDestroy(){
        super.onDestroy();
        if(Main.comms != null) Main.comms.removeListener(this);
    }

    private void loadBTDevices(){
        if(Main.comms == null){
            finishAndRemoveTask();
            return;
        }
        Set<BluetoothDevice> bluetoothDevices = Main.comms.getBondedBTDevices();
        runOnUiThread(()->{
            if(bluetoothDevices == null || bluetoothDevices.isEmpty()){
                ((TextView) findViewById(R.id.device_select_new)).setText(R.string.device_select_none);
            }else{
                device_select_ll.removeAllViews();
                bluetoothDevices.forEach(d->deviceFound(device_select_ll, d));
            }
            device_select_loading.setVisibility(View.GONE);
        });
    }
    private void loadIQDevices(){
        if(Main.comms == null){
            finishAndRemoveTask();
            return;
        }
        List<IQDevice> iQDevices = Main.comms.getBondedIQDevices();
        runOnUiThread(()->{
            if(Main.comms.iQSdkStatus == Comms.IQSdkStatus.GCM_NOT_INSTALLED){
                ((TextView) findViewById(R.id.device_select_garmin_new)).setText(R.string.device_select_garmin_not);
            }else if(Main.comms.iQSdkStatus == Comms.IQSdkStatus.GCM_UPGRADE_NEEDED){
                ((TextView) findViewById(R.id.device_select_garmin_new)).setText(R.string.device_select_garmin_update);
            }else if(iQDevices == null || iQDevices.isEmpty()){
                ((TextView) findViewById(R.id.device_select_garmin_new)).setText(R.string.device_select_garmin_none);
            }else{
                device_select_ll.removeAllViews();
                iQDevices.forEach(d->deviceFound(device_select_ll, d));
            }
            device_select_loading.setVisibility(View.GONE);
        });
    }
    private void deviceFound(LinearLayout layout, BluetoothDevice bluetoothDevice){
        TextView device = new TextView(this, null, 0, R.style.rrwDeviceStyle);
        device.setText(bluetoothDevice.getName());
        layout.addView(device);
        device.setOnClickListener(v->connectDevice(bluetoothDevice));
        device.setOnLongClickListener(v->onDeviceLongClick(bluetoothDevice, v));
    }
    private void deviceFound(LinearLayout layout, IQDevice iQDevice){
        TextView device = new TextView(this, null, 0, R.style.rrwDeviceStyle);
        device.setText(iQDevice.getFriendlyName());
        layout.addView(device);
        device.setOnClickListener(v->connectDevice(iQDevice));
        device.setOnLongClickListener(v->onDeviceLongClick(iQDevice, v));
    }
    private void connectDevice(BluetoothDevice bluetoothDevice){
        if(Main.comms != null) Main.comms.connectBTDevice(bluetoothDevice);
    }
    private void connectDevice(IQDevice iQDevice){
        if(Main.comms != null) Main.comms.connectIQDevice(iQDevice);
    }
    private boolean onDeviceLongClick(BluetoothDevice device, View view){
        new AlertDialog.Builder(this)
                .setMessage(R.string.delete_device)
                .setPositiveButton(R.string.delete, (d, w)->{
                    view.setVisibility(View.GONE);
                    Main.comms.removeKnownBTAddress(device.getAddress());
                })
                .setNegativeButton(R.string.cancel, null)
                .create()
                .show();
        return device != null;
    }
    private boolean onDeviceLongClick(IQDevice device, View view){
        new AlertDialog.Builder(this)
                .setMessage(R.string.delete_device)
                .setPositiveButton(R.string.delete, (d, w)->{
                    view.setVisibility(View.GONE);
                    Main.comms.removeKnownIQId(device.getDeviceIdentifier());
                })
                .setNegativeButton(R.string.cancel, null)
                .create()
                .show();
        return device != null;
    }

    @Override public void onCommsStartDone(){}
    @Override public void onCommsConnecting(String deviceName){
        Intent startDeviceConnect = new Intent(this, DeviceConnect.class);
        startDeviceConnect.putExtra("name", deviceName);
        startActivity(startDeviceConnect);
    }
    @Override public void onCommsConnectFailed(){finishAndRemoveTask();}
    @Override public void onCommsConnected(String x){finishAndRemoveTask();}
    @Override public void onCommsSending(){}
    @Override public void onCommsSendingFinished(){}
    @Override public void onCommsDisconnected(){}
    @Override public void onCommsResponse(Comms.Request.Type t, JSONObject r){}
    @Override public void onCommsError(int e){}
}

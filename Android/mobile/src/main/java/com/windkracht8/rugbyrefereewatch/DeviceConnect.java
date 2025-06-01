/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.windkracht8.rugbyrefereewatch;

import android.app.Activity;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONObject;

public class DeviceConnect extends Activity implements Comms.Interface{
    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_connect);
        findViewById(android.R.id.content).setOnApplyWindowInsetsListener(Main.onApplyWindowInsetsListener);
        String name = getIntent().getStringExtra("name");
        if(name == null) ((TextView)findViewById(R.id.device_connect_name)).setText(R.string.connecting_to);
        ((TextView)findViewById(R.id.device_connect_name)).setText(getString(R.string.connecting_to, name));

        ImageView icon = findViewById(R.id.device_connect_icon);
        ((AnimatedVectorDrawable) icon.getBackground()).start();

        if(Main.comms == null || Main.comms.status != Comms.Status.CONNECTING) finishAndRemoveTask();
        try{
            Main.comms.addListener(this);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "DeviceConnect.onCreate Failed to add as a listener: " + e.getMessage());
        }
    }
    @Override public void onDestroy(){
        super.onDestroy();
        if(Main.comms != null) Main.comms.removeListener(this);
    }
    @Override public void onCommsStartDone(){finishAndRemoveTask();}
    @Override public void onCommsConnecting(String x){}
    @Override public void onCommsConnectFailed(){finishAndRemoveTask();}
    @Override public void onCommsConnected(String x){finishAndRemoveTask();}
    @Override public void onCommsSending(){}
    @Override public void onCommsSendingFinished(){}
    @Override public void onCommsDisconnected(){finishAndRemoveTask();}
    @Override public void onCommsResponse(Comms.Request.Type t, JSONObject r){}
    @Override public void onCommsError(int e){}
}

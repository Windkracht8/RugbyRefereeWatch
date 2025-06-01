/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.windkracht8.rugbyrefereewatch;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

public class Permissions extends Activity{
    static boolean hasBTPermission = false;

    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        if(hasBTPermission) finishAndRemoveTask();
        setContentView(R.layout.permissions);
        findViewById(android.R.id.content).setOnApplyWindowInsetsListener(Main.onApplyWindowInsetsListener);
        findViewById(R.id.permission_nearby).setOnClickListener(v->onNearbyClick());
    }
    private void onNearbyClick(){
        if(hasBTPermission) return;
        if(Build.VERSION.SDK_INT >= 31){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
        }else{
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, 1);
        }
    }
    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for(int i=0; i<permissions.length; i++){
            if(permissions[i].equals(Manifest.permission.BLUETOOTH_CONNECT) ||
                    permissions[i].equals(Manifest.permission.BLUETOOTH)){
                if(grantResults[i] == PackageManager.PERMISSION_GRANTED){
                    hasBTPermission = true;
                    finishAndRemoveTask();
                }else{
                    return;
                }
            }
        }
    }
    static void checkPermissions(Context context){
        if(Build.VERSION.SDK_INT >= 31){
            hasBTPermission = ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }else{
            hasBTPermission = ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED;
        }
    }
}

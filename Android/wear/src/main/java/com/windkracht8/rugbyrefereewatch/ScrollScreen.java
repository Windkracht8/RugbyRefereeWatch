/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.windkracht8.rugbyrefereewatch;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class ScrollScreen extends Activity{
    LinearLayout list;
    TextView label;
    private static int itemHeight = 0;
    private static float scalePerPixel;
    private static float bottom_quarter;
    private static float below_screen;

    @Override public void onCreate(Bundle ignored){
        super.onCreate(null);
        setContentView(R.layout.scroll_screen);
        list = findViewById(R.id.list);
        ScrollView scrollView = findViewById(R.id.scrollView);
        label = findViewById(R.id.label);
        scrollView.requestFocus();

        if(Main.isScreenRound){
            label.setPadding(Main.vh10, Main.vh10, Main.vh10, 0);
            list.setPadding(Main._10dp, 0, Main._10dp, Main.vh25);
            list.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
                @Override public void onGlobalLayout(){
                    list.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    if(itemHeight == 0 && list.getChildCount() > 1){
                        itemHeight = list.getChildAt(1).getHeight();
                        bottom_quarter = Main.vh75 - itemHeight;
                        below_screen = Main.heightPixels - itemHeight;
                        scalePerPixel = 0.2f / Main.vh25;
                    }
                    scaleItems(0);
                }
            });
            scrollView.setOnScrollChangeListener((v, sx, sy, osx, osy)->scaleItems(sy));
        }
    }
    private void scaleItems(int scrollY){
        float top;
        float scale;
        for(int i = 1; i < list.getChildCount(); i++){
            View view = list.getChildAt(i);

            top = view.getY() - scrollY;
            scale = 1.0f;
            if(top < 0){
                //the item is above the screen
                scale = 0.8f;
            }else if(top < Main.vh25){
                //the item is in the top quarter
                scale = 0.8f + (scalePerPixel * top);
            }else if(top > below_screen){
                //the item is below the screen
                scale = 0.8f;
            }else if(top > bottom_quarter){
                //the item is in the bottom quarter
                scale = 1.0f - (scalePerPixel * (top - bottom_quarter));
            }
            view.setScaleX(scale);
            view.setScaleY(scale);
        }
    }
}

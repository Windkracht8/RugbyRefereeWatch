package com.windkracht8.rugbyrefereewatch;

import static com.windkracht8.rugbyrefereewatch.MenuItem.MenuItemType.*;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

public class ConfWatch extends LinearLayout {
    private boolean isInitialized = false;

    public ConfWatch(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(inflater == null){ Toast.makeText(context, R.string.fail_show_conf, Toast.LENGTH_SHORT).show(); return;}
        inflater.inflate(R.layout.conf_watch, this, true);
    }
    public void show(MainActivity ma){
        if(isInitialized){
            this.setVisibility(View.VISIBLE);
            return;
        }
        isInitialized = true;
        int padding = getResources().getDimensionPixelSize(R.dimen.menu_item_padding)*4;
        int height_per_item = ((MainActivity.heightPixels/4) - padding) / 3;

        LinearLayout llConfWatch = findViewById(R.id.llConfWatch);
        for(MenuItem.MenuItemType menuItemType : new MenuItem.MenuItemType[]{SCREEN_ON, TIMER_TYPE, RECORD_PLAYER, RECORD_PENS}){
            MenuItem menuItem = new MenuItem(getContext(), null, menuItemType);
            llConfWatch.addView(menuItem);
            menuItem.addOnTouch(ma);
            menuItem.setHeight(height_per_item);
        }
        this.setVisibility(View.VISIBLE);
    }
}

package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

public abstract class ReportEvent extends LinearLayout{
    ReportEvent(Context context, int layout){
        super(context);
        ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layout, this, true);
    }
    void getFieldWidths(){}
    void setFieldWidths(){}
}

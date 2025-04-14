package com.windkracht8.rugbyrefereewatch;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ConfCustom extends ConfScreen{
    @Override public @Nullable View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ){
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        label.setText(R.string.match_type_details);
        for(ConfItem.ConfItemType confItemType : ConfItem.ConfItemType.values()){
            if(!ConfItem.confCustomItemTypes.contains(confItemType)) continue;
            ConfItem confItem = new ConfItem(confActivity, confItemType);
            confItem.setOnClickListener(v->confActivity.openConfScreen(new ConfSpinner(confItemType)));
            super.addItem(confItem);
        }
        return rootView;
    }
}

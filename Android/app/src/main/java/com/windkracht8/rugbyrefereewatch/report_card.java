package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;


public class report_card extends LinearLayout{
    private long matchid;
    private long eventid;
    private TextView tvReason;
    private EditText etReason;

    public report_card(Context context){super(context);}
    public report_card(Context context, JSONObject event, long matchid) {
        super(context);
        this.matchid = matchid;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater != null) {
            inflater.inflate(R.layout.report_card, this, true);
        }

        tvReason = findViewById(R.id.tvReason);
        etReason = findViewById(R.id.etReason);
        tvReason.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tvReasonClick(view);
            }
        });
        etReason.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if(keyCode == KeyEvent.KEYCODE_ENTER){
                    newReason(view);
                    return true;
                }
                return false;
            }
        });
        try {
            eventid = event.getLong("id");

            if(event.has("reason")){
                tvReason.setText(event.getString("reason"));
            }

        } catch (Exception e) {
            Log.e("report_card", "report_card: " + e.getMessage());
        }

    }
    public void tvReasonClick(View view){
        if(matchid == 0){return;}//TODO: matchid is present from version 1.1 of watch app
        CharSequence reason = tvReason.getText() == view.getContext().getResources().getString(R.string.no_card_reason) ? "" : tvReason.getText();
        etReason.setText(reason);
        tvReason.setVisibility(View.GONE);
        etReason.setVisibility(View.VISIBLE);
        etReason.requestFocus();
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(etReason, InputMethodManager.SHOW_IMPLICIT);
    }

    public void newReason(View view){
        if(matchid == 0){return;}//TODO: matchid is present from version 1.1 of watch app
        String reason = etReason.getText().toString();
        tvReason.setText(reason);
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        etReason.setVisibility(View.GONE);
        tvReason.setVisibility(View.VISIBLE);

        MainActivity.updateCardReason(reason, matchid, eventid);
    }
}

package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.EditText;

import org.json.JSONObject;


public class report_card extends LinearLayout {
    private long match_id;
    private long event_id;
    private TextView tvReason;
    private EditText etReason;

    public report_card(Context context){super(context);}
    public report_card(Context context, JSONObject event, long match_id) {
        super(context);
        this.match_id = match_id;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater != null) {
            inflater.inflate(R.layout.report_card, this, true);
        }

        tvReason = findViewById(R.id.tvReason);
        etReason = findViewById(R.id.etReason);
        tvReason.setOnClickListener(this::tvReasonClick);
        etReason.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().contains("\n")){
                    newReason(etReason);
                }
            }
        });
        try {
            event_id = event.getLong("id");

            if(event.has("reason")){
                tvReason.setText(event.getString("reason").replace("\n", " "));
            }

        } catch (Exception e) {
            Log.e("report_card", "report_card: " + e.getMessage());
        }

    }
    public void tvReasonClick(View view){
        CharSequence reason = tvReason.getText() == view.getContext().getResources().getString(R.string.no_card_reason) ? "" : tvReason.getText();
        etReason.setText(reason);
        tvReason.setVisibility(View.GONE);
        etReason.setVisibility(View.VISIBLE);
        etReason.requestFocus();
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(etReason, InputMethodManager.SHOW_IMPLICIT);
    }

    public void newReason(View view){
        String reason = etReason.getText().toString();
        reason = reason.replace("\n", "");
        tvReason.setText(reason);
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        etReason.setVisibility(View.GONE);
        tvReason.setVisibility(View.VISIBLE);

        Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
        intent.putExtra("intentType", "updateCardReason");
        intent.putExtra("source", "report_card");
        intent.putExtra("reason", reason);
        intent.putExtra("match_id", match_id);
        intent.putExtra("event_id", event_id);
        view.getContext().sendBroadcast(intent);
    }
}

package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class report extends LinearLayout {
    private TextView tvHomeName;
    private EditText etHomeName;
    private TextView tvAwayName;
    private EditText etAwayName;
    private TextView tvHomeTrys;
    private TextView tvAwayTrys;
    private TableRow trCons;
    private TextView tvHomeCons;
    private TextView tvAwayCons;
    private TableRow trGoals;
    private TextView tvHomeGoals;
    private TextView tvAwayGoals;
    private TextView tvHomeTot;
    private TextView tvAwayTot;
    private LinearLayout llEvents;
    private Button bShare;

    private JSONObject match;
    private long matchid;

    private int timewidth;
    private int timerwidth;

    public report(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater != null) {
            inflater.inflate(R.layout.report, this, true);
        }

        //Yes this is very ugly, TableLayout does not work when you add TableRows as a separate class
        TextView tvTime = findViewById(R.id.tvTime);
        tvTime.measure(0, 0);
        timewidth = tvTime.getMeasuredWidth();
        TextView tvTimer = findViewById(R.id.tvTimer);
        tvTimer.measure(0, 0);
        timerwidth = tvTimer.getMeasuredWidth();

        tvHomeName = findViewById(R.id.tvHomeName);
        etHomeName = findViewById(R.id.etHomeName);
        tvAwayName = findViewById(R.id.tvAwayName);
        etAwayName = findViewById(R.id.etAwayName);
        tvHomeTrys = findViewById(R.id.tvHomeTrys);
        tvAwayTrys = findViewById(R.id.tvAwayTrys);
        trCons = findViewById(R.id.trCons);
        tvHomeCons = findViewById(R.id.tvHomeCons);
        tvAwayCons = findViewById(R.id.tvAwayCons);
        trGoals = findViewById(R.id.trGoals);
        tvHomeGoals = findViewById(R.id.tvHomeGoals);
        tvAwayGoals = findViewById(R.id.tvAwayGoals);
        tvHomeTot = findViewById(R.id.tvHomeTot);
        tvAwayTot = findViewById(R.id.tvAwayTot);
        llEvents = findViewById(R.id.llEvents);
        bShare = findViewById(R.id.bShare);

        bShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bShareClick(view);
            }
        });
        tvHomeName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tvHomeNameClick(view);
            }
        });
        etHomeName.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if(keyCode == KeyEvent.KEYCODE_ENTER){
                    newHomeName(view);
                    return true;
                }
                return false;
            }
        });
        etHomeName.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    etHomeName.setVisibility(View.GONE);
                    tvHomeName.setVisibility(View.VISIBLE);
                    etHomeName.setText(tvHomeName.getText());
                }
            }
        });
        tvAwayName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tvAwayNameClick(view);
            }
        });
        etAwayName.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if(keyCode == KeyEvent.KEYCODE_ENTER){
                    newAwayName(view);
                    return true;
                }
                return false;
            }
        });
        etAwayName.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    etAwayName.setVisibility(View.GONE);
                    tvAwayName.setVisibility(View.VISIBLE);
                    etAwayName.setText(tvAwayName.getText());
                }
            }
        });
    }

    public void gotMatch(final JSONObject match) {
        this.match = match;
        try {
            this.matchid = match.has("matchid") ? match.getLong("matchid") : 0;//TODO: matchid is present from version 1.1 of watch app
            JSONObject settings = match.getJSONObject("settings");
            JSONObject home = match.getJSONObject("home");
            JSONObject away = match.getJSONObject("away");

            tvHomeName.setText(MainActivity.getTeamName(home));
            tvAwayName.setText(MainActivity.getTeamName(away));
            tvHomeTrys.setText(home.getString("trys"));
            tvAwayTrys.setText(away.getString("trys"));

            if(settings.has("points_con") && settings.getInt("points_con") == 0) {
                trCons.setVisibility(View.GONE);
            }else{
                trCons.setVisibility(View.VISIBLE);
            }
            tvHomeCons.setText(home.getString("cons"));
            tvAwayCons.setText(away.getString("cons"));
            if(settings.has("points_goal") && settings.getInt("points_goal") == 0) {
                trGoals.setVisibility(View.GONE);
            }else{
                trGoals.setVisibility(View.VISIBLE);
            }
            tvHomeGoals.setText(home.getString("goals"));
            tvAwayGoals.setText(away.getString("goals"));
            tvHomeTot.setText(home.getString("tot"));
            tvAwayTot.setText(away.getString("tot"));

            if(llEvents.getChildCount() > 0)
                llEvents.removeAllViews();

            JSONArray events = match.getJSONArray("events");
            Context context = getContext();
            for (int i = 0; i < events.length(); i++) {
                JSONObject event = events.getJSONObject(i);
                llEvents.addView(new report_event(context, event, match, timewidth, timerwidth));
                //TODO: CARD uppercase from version 1.1 of watch app
                if(event.getString("what").toUpperCase().contains("CARD")) {
                    llEvents.addView(new report_card(context, event, matchid));
                }
            }

        }catch (Exception e){
            Log.e("report", "gotMatch: " + e.getMessage());
        }
        bShare.setVisibility(VISIBLE);
    }

    public void tvHomeNameClick(View view){
        if(matchid == 0){return;}//TODO: matchid is present from version 1.1 of watch app
        etHomeName.setText(tvHomeName.getText());
        tvHomeName.setVisibility(View.INVISIBLE);
        etHomeName.setVisibility(View.VISIBLE);
        etHomeName.requestFocus();
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(etHomeName, InputMethodManager.SHOW_IMPLICIT);
    }
    private void newHomeName(View view){
        if(matchid == 0){return;}//TODO: matchid is present from version 1.1 of watch app
        String name = etHomeName.getText().toString();
        tvHomeName.setText(name);
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        etHomeName.setVisibility(View.GONE);
        tvHomeName.setVisibility(View.VISIBLE);

        MainActivity.updateTeamName(name, "home", matchid);
    }
    public void tvAwayNameClick(View view){
        if(matchid == 0){return;}//TODO: matchid is present from version 1.1 of watch app
        etAwayName.setText(tvAwayName.getText());
        tvAwayName.setVisibility(View.INVISIBLE);
        etAwayName.setVisibility(View.VISIBLE);
        etAwayName.requestFocus();
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(etAwayName, InputMethodManager.SHOW_IMPLICIT);
    }
    private void newAwayName(View view){
        if(matchid == 0){return;}//TODO: matchid is present from version 1.1 of watch app
        String name = etAwayName.getText().toString();
        tvAwayName.setText(name);
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        etAwayName.setVisibility(View.GONE);
        tvAwayName.setVisibility(View.VISIBLE);

        MainActivity.updateTeamName(name, "away", matchid);
    }

    public void bShareClick(View view){
        Context context = view.getContext();
        /*
        TODO: create a screenshot for the nice overview, but fix permission issue first
        View screenView = view.getRootView();
        screenView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(screenView.getDrawingCache());
        screenView.setDrawingCacheEnabled(false);

        ContextWrapper cw = new ContextWrapper(screenView.getContext());
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        File file = new File(directory, "screenshot.png");
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            Log.e("report", "bShareClick: " + e.getMessage());
            Toast.makeText(context, "Failed to store image", Toast.LENGTH_SHORT).show();
        }
        Uri uri = Uri.fromFile(file);
        */

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, getShareSubject());
        intent.putExtra(Intent.EXTRA_TEXT, getShareBody());

        try {
            context.startActivity(Intent.createChooser(intent, "Share match report"));
        } catch (Exception e) {
            Log.e("report", "bShareClick: " + e.getMessage());
            Toast.makeText(context, "Failed to share", Toast.LENGTH_SHORT).show();
        }
    }
    private String getShareSubject(){
        String shareSubject = "Match report";
        Log.i("report", "match: " + match.toString());

        try {
            String sMatchdate = "";
            if(matchid > 0){//TODO: matchid is present from version 1.1 of watch app
                Date dMatchdate = new Date(matchid);
                sMatchdate = new SimpleDateFormat("E dd-MM-yyyy HH:mm", Locale.getDefault()).format(dMatchdate);
            }
            shareSubject += " " + sMatchdate;

            JSONObject home = match.getJSONObject("home");
            shareSubject += " " + MainActivity.getTeamName(home);
            shareSubject += " " + home.getString("tot");

            shareSubject += " v";

            JSONObject away = match.getJSONObject("away");
            shareSubject += " " + MainActivity.getTeamName(away);
            shareSubject += " " + away.getString("tot");
        } catch (Exception e) {
            Log.e("report", "getShareSubject: " + e.getMessage());
        }

        return shareSubject;
    }
    private String getShareBody(){
        StringBuilder shareBody = new StringBuilder();

        try {
            shareBody.append(getShareSubject()).append("\n\n");

            JSONObject home = match.getJSONObject("home");
            shareBody.append(MainActivity.getTeamName(home)).append("\n");
            shareBody.append("  Trys: ").append(home.getString("trys")).append("\n");
            shareBody.append("  Conversions: ").append(home.getString("cons")).append("\n");
            shareBody.append("  Goals: ").append(home.getString("goals")).append("\n");
            shareBody.append("  Total: ").append(home.getString("tot")).append("\n");
            //TODO: count cards
            shareBody.append("\n");

            JSONObject away = match.getJSONObject("away");
            shareBody.append(MainActivity.getTeamName(away)).append("\n");
            shareBody.append("  Trys: ").append(away.getString("trys")).append("\n");
            shareBody.append("  Conversions: ").append(away.getString("cons")).append("\n");
            shareBody.append("  Goals: ").append(away.getString("goals")).append("\n");
            shareBody.append("  Total: ").append(away.getString("tot")).append("\n");
            //TODO: count cards
            shareBody.append("\n");

            JSONArray events = match.getJSONArray("events");
            for (int i = 0; i < events.length(); i++) {
                JSONObject event = events.getJSONObject(i);
                shareBody.append(event.getString("time"));
                shareBody.append("    ").append(event.getString("timer"));
                shareBody.append("    ").append(event.getString("what"));
                if(event.has("team")) {
                    if(event.getString("team").equals("home")){
                        shareBody.append("    ").append(MainActivity.getTeamName(home));
                    }else{
                        shareBody.append("    ").append(MainActivity.getTeamName(away));
                    }
                }
                if(event.has("who")) {
                    shareBody.append("    ").append(event.getString("who"));
                }
                if(event.has("reason")) {
                    shareBody.append("\n").append(event.getString("reason")).append("\n");
                }
                shareBody.append("\n");
            }

        } catch (Exception e) {
            Log.e("report", "getShareBody: " + e.getMessage());
        }

        return shareBody.toString();
    }
}

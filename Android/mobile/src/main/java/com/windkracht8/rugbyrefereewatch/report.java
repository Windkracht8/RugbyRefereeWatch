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
    private final TextView tvHomeName;
    private final EditText etHomeName;
    private final TextView tvAwayName;
    private final EditText etAwayName;
    private final TextView tvHomeTries;
    private final TextView tvAwayTries;
    private final TableRow trCons;
    private final TextView tvHomeCons;
    private final TextView tvAwayCons;
    private final TableRow trGoals;
    private final TextView tvHomeGoals;
    private final TextView tvAwayGoals;
    private final TextView tvHomeTot;
    private final TextView tvAwayTot;
    private final LinearLayout llEvents;
    private final Button bView;
    private final Button bShare;

    private JSONObject match;
    private long match_id;

    private final int time_width;
    private final int timer_width;
    private final int score_width;

    private boolean view_full = false;

    public report(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater != null) {
            inflater.inflate(R.layout.report, this, true);
        }

        //Yes this is very ugly, TableLayout does not work when you add TableRows as a separate class
        TextView tvTime = findViewById(R.id.tvTime);
        tvTime.measure(0, 0);
        time_width = tvTime.getMeasuredWidth();
        TextView tvTimer = findViewById(R.id.tvTimer);
        tvTimer.measure(0, 0);
        timer_width = tvTimer.getMeasuredWidth();
        TextView tvScore = findViewById(R.id.tvScore);
        tvScore.measure(0, 0);
        score_width = tvScore.getMeasuredWidth();
        llEvents = findViewById(R.id.llEvents);
        llEvents.removeAllViews();

        tvHomeName = findViewById(R.id.tvHomeName);
        etHomeName = findViewById(R.id.etHomeName);
        tvAwayName = findViewById(R.id.tvAwayName);
        etAwayName = findViewById(R.id.etAwayName);
        tvHomeTries = findViewById(R.id.tvHomeTries);
        tvAwayTries = findViewById(R.id.tvAwayTries);
        trCons = findViewById(R.id.trCons);
        tvHomeCons = findViewById(R.id.tvHomeCons);
        tvAwayCons = findViewById(R.id.tvAwayCons);
        trGoals = findViewById(R.id.trGoals);
        tvHomeGoals = findViewById(R.id.tvHomeGoals);
        tvAwayGoals = findViewById(R.id.tvAwayGoals);
        tvHomeTot = findViewById(R.id.tvHomeTot);
        tvAwayTot = findViewById(R.id.tvAwayTot);
        bView = findViewById(R.id.bView);
        bShare = findViewById(R.id.bShare);

        bView.setOnClickListener(view -> bViewClick());
        bShare.setOnClickListener(this::bShareClick);
        tvHomeName.setOnClickListener(this::tvHomeNameClick);
        etHomeName.setOnKeyListener((view, keyCode, event) -> {
            if(keyCode == KeyEvent.KEYCODE_ENTER){
                newHomeName(view);
                return true;
            }
            return false;
        });
        etHomeName.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                etHomeName.setVisibility(View.GONE);
                tvHomeName.setVisibility(View.VISIBLE);
                etHomeName.setText(tvHomeName.getText());
            }
        });
        tvAwayName.setOnClickListener(this::tvAwayNameClick);
        etAwayName.setOnKeyListener((view, keyCode, event) -> {
            if(keyCode == KeyEvent.KEYCODE_ENTER){
                newAwayName(view);
                return true;
            }
            return false;
        });
        etAwayName.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                etAwayName.setVisibility(View.GONE);
                tvAwayName.setVisibility(View.VISIBLE);
                etAwayName.setText(tvAwayName.getText());
            }
        });
    }

    public void gotMatch(final JSONObject match) {
        this.match = match;
        addScores();
        try {
            this.match_id = match.getLong("matchid");
            JSONObject settings = match.getJSONObject("settings");
            JSONObject home = match.getJSONObject("home");
            JSONObject away = match.getJSONObject("away");

            tvHomeName.setText(MainActivity.getTeamName(home));
            tvAwayName.setText(MainActivity.getTeamName(away));
            tvHomeTries.setText(home.getString("tries"));
            tvAwayTries.setText(away.getString("tries"));

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

            showEvents();
        }catch (Exception e){
            Log.e("report", "gotMatch: " + e.getMessage());
        }
        bView.setVisibility(VISIBLE);
        bShare.setVisibility(VISIBLE);
    }
    private void showEvents(){
        if(llEvents.getChildCount() > 0) llEvents.removeAllViews();
        try {
            JSONArray events = match.getJSONArray("events");
            Context context = getContext();
            for (int i = 0; i < events.length(); i++) {
                JSONObject event = events.getJSONObject(i);
                if(view_full) {
                    llEvents.addView(new report_event_full(context, event, match, time_width, timer_width, score_width));
                }else{
                    llEvents.addView(new report_event(context, event, score_width));
                }
                if(event.getString("what").contains("CARD")) {
                    llEvents.addView(new report_card(context, event, match_id));
                }
            }
        }catch (Exception e){
            Log.e("report", "showEvents: " + e.getMessage());
        }
    }
    private void addScores(){
        try {
            int score_home = 0;
            int score_away = 0;
            JSONObject settings = match.getJSONObject("settings");
            int points_try = settings.getInt("points_try");
            int points_con = settings.getInt("points_con");
            int points_goal = settings.getInt("points_goal");
            JSONArray events = match.getJSONArray("events");
            for (int i = 0; i < events.length(); i++) {
                JSONObject event = events.getJSONObject(i);
                if (event.has("team")) {
                    int points = 0;
                    switch (event.getString("what")) {
                        case "TRY":
                            points = points_try;
                            break;
                        case "CONVERSION":
                            points = points_con;
                            break;
                        case "GOAL":
                            points = points_goal;
                            break;
                    }
                    if(event.getString("team").equals("home")){
                        score_home += points;
                    }else{
                        score_away += points;
                    }
                }
                event.put("score",  score_home + ":" + score_away);
            }
        }catch(Exception e){
            Log.e("report", "getScore: " + e.getMessage());
        }
    }

    public void tvHomeNameClick(View view){
        etHomeName.setText(tvHomeName.getText());
        tvHomeName.setVisibility(View.INVISIBLE);
        etHomeName.setVisibility(View.VISIBLE);
        etHomeName.requestFocus();
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(etHomeName, InputMethodManager.SHOW_IMPLICIT);
    }
    private void newHomeName(View view){
        String name = etHomeName.getText().toString();
        tvHomeName.setText(name);
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        etHomeName.setVisibility(View.GONE);
        tvHomeName.setVisibility(View.VISIBLE);

        Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
        intent.putExtra("intent_type", "updateTeamName");
        intent.putExtra("source", "report");
        intent.putExtra("name", name);
        intent.putExtra("team_id", "home");
        intent.putExtra("match_id", match_id);
        view.getContext().sendBroadcast(intent);
    }
    public void tvAwayNameClick(View view){
        etAwayName.setText(tvAwayName.getText());
        tvAwayName.setVisibility(View.INVISIBLE);
        etAwayName.setVisibility(View.VISIBLE);
        etAwayName.requestFocus();
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(etAwayName, InputMethodManager.SHOW_IMPLICIT);
    }
    private void newAwayName(View view){
        String name = etAwayName.getText().toString();
        tvAwayName.setText(name);
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        etAwayName.setVisibility(View.GONE);
        tvAwayName.setVisibility(View.VISIBLE);

        Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
        intent.putExtra("intent_type", "updateTeamName");
        intent.putExtra("source", "report");
        intent.putExtra("name", name);
        intent.putExtra("team_id", "away");
        intent.putExtra("match_id", match_id);
        view.getContext().sendBroadcast(intent);
    }

    public void bViewClick(){
        view_full = !view_full;
        showEvents();
    }
    public void bShareClick(View view){
        Context context = view.getContext();

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
            Date match_date_d = new Date(match_id);
            String match_date_s = new SimpleDateFormat("E dd-MM-yyyy HH:mm", Locale.getDefault()).format(match_date_d);

            shareSubject += " " + match_date_s;

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
            shareBody.append("  Tries: ").append(home.getString("tries")).append("\n");
            shareBody.append("  Conversions: ").append(home.getString("cons")).append("\n");
            shareBody.append("  Goals: ").append(home.getString("goals")).append("\n");
            shareBody.append("  Total: ").append(home.getString("tot")).append("\n");
            shareBody.append("\n");

            JSONObject away = match.getJSONObject("away");
            shareBody.append(MainActivity.getTeamName(away)).append("\n");
            shareBody.append("  Tries: ").append(away.getString("tries")).append("\n");
            shareBody.append("  Conversions: ").append(away.getString("cons")).append("\n");
            shareBody.append("  Goals: ").append(away.getString("goals")).append("\n");
            shareBody.append("  Total: ").append(away.getString("tot")).append("\n");
            shareBody.append("\n");

            JSONArray events = match.getJSONArray("events");
            for (int i = 0; i < events.length(); i++) {
                JSONObject event = events.getJSONObject(i);
                shareBody.append(event.getString("time"));
                shareBody.append("    ").append(event.getString("timer"));
                if(event.has("score")) {
                    shareBody.append("    ").append(event.getString("score"));
                }
                shareBody.append("    ").append(event.getString("what"));
                if(event.has("team")) {
                    if(event.getString("team").equals("home")){
                        shareBody.append(" ").append(MainActivity.getTeamName(home));
                    }else{
                        shareBody.append(" ").append(MainActivity.getTeamName(away));
                    }
                }
                if(event.has("who")) {
                    shareBody.append(" ").append(event.getString("who"));
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

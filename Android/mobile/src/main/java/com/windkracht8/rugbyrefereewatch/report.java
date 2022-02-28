package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class report extends LinearLayout{
    private LinearLayout llEvents;

    private JSONObject match;
    private long match_id;

    public static int time_width;
    public static int timer_width;
    public static int score_width;

    private int view = 0;

    public report(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(inflater == null){Toast.makeText(context, "Failed to show match", Toast.LENGTH_SHORT).show(); return;}
        inflater.inflate(R.layout.report, this, true);

        TextView tvTime = findViewById(R.id.tvTime);
        tvTime.measure(0, 0);
        time_width = tvTime.getMeasuredWidth();
        TextView tvTimer = findViewById(R.id.tvTimer);
        tvTimer.measure(0, 0);
        timer_width = tvTimer.getMeasuredWidth();
        TextView tvScore = findViewById(R.id.tvScore);
        tvScore.measure(0, 0);
        score_width = tvScore.getMeasuredWidth();
        findViewById(R.id.text_measure).setVisibility(View.GONE);
        llEvents = findViewById(R.id.llEvents);

        findViewById(R.id.bView).setOnClickListener(view -> bViewClick());
        findViewById(R.id.bEdit).setOnClickListener(view -> bEditClick());
        findViewById(R.id.bClose).setOnClickListener(this::bCloseClick);
        findViewById(R.id.bSave).setOnClickListener(this::bSaveClick);
        findViewById(R.id.bShare).setOnClickListener(view -> bShareClick());
    }

    public void gotMatch(final JSONObject match){
        this.match = match;
        view = 0;
        addScores();
        try{
            this.match_id = match.getLong("matchid");
            JSONObject settings = match.getJSONObject("settings");
            JSONObject home = match.getJSONObject("home");
            JSONObject away = match.getJSONObject("away");

            ((TextView)findViewById(R.id.tvHomeName)).setText(MainActivity.getTeamName(home));
            ((EditText)findViewById(R.id.etHomeName)).setText(MainActivity.getTeamName(home));
            ((TextView)findViewById(R.id.tvAwayName)).setText(MainActivity.getTeamName(away));
            ((EditText)findViewById(R.id.etAwayName)).setText(MainActivity.getTeamName(away));
            ((TextView)findViewById(R.id.tvHomeTries)).setText(home.getString("tries"));
            ((TextView)findViewById(R.id.tvAwayTries)).setText(away.getString("tries"));

            if(!settings.has("points_con") || settings.getInt("points_con") == 0){
                findViewById(R.id.trCons).setVisibility(View.GONE);
            }else{
                ((TextView)findViewById(R.id.tvHomeCons)).setText(home.getString("cons"));
                ((TextView)findViewById(R.id.tvAwayCons)).setText(away.getString("cons"));
                findViewById(R.id.trCons).setVisibility(View.VISIBLE);
            }
            if(!settings.has("points_goal") || settings.getInt("points_goal") == 0){
                findViewById(R.id.trGoals).setVisibility(View.GONE);
            }else{
                ((TextView)findViewById(R.id.tvHomeGoals)).setText(home.getString("goals"));
                ((TextView)findViewById(R.id.tvAwayGoals)).setText(away.getString("goals"));
                findViewById(R.id.trGoals).setVisibility(View.VISIBLE);
            }
            ((TextView)findViewById(R.id.tvHomeTot)).setText(home.getString("tot"));
            ((TextView)findViewById(R.id.tvAwayTot)).setText(away.getString("tot"));

            showEvents();
        }catch(Exception e){
            Log.e("report", "gotMatch: " + e.getMessage());
            Toast.makeText(getContext(), "Failed to show match", Toast.LENGTH_SHORT).show();
        }

        findViewById(R.id.bView).setVisibility(VISIBLE);
        findViewById(R.id.bEdit).setVisibility(VISIBLE);
        findViewById(R.id.bClose).setVisibility(GONE);
        findViewById(R.id.bSave).setVisibility(GONE);
        findViewById(R.id.bShare).setVisibility(VISIBLE);
        findViewById(R.id.table).setVisibility(VISIBLE);
        findViewById(R.id.edit_team_names).setVisibility(GONE);
    }
    private void showEvents(){
        if(llEvents.getChildCount() > 0) llEvents.removeAllViews();
        try{
            JSONArray events = match.getJSONArray("events");
            Context context = getContext();
            for(int i = 0; i < events.length(); i++){
                JSONObject event = events.getJSONObject(i);
                switch(view){
                    case 0:
                        llEvents.addView(new report_event(context, event));
                        break;
                    case 1:
                        llEvents.addView(new report_event_full(context, event, match));
                        break;
                    case 2:
                        llEvents.addView(new report_event_edit(context, event));
                        break;
                }
            }
        }catch(Exception e){
            Log.e("report", "showEvents: " + e.getMessage());
            Toast.makeText(getContext(), "Failed to show match", Toast.LENGTH_SHORT).show();
        }
    }
    private void addScores(){
        try{
            int score_home = 0;
            int score_away = 0;
            JSONObject settings = match.getJSONObject("settings");
            int points_try = settings.getInt("points_try");
            int points_con = settings.getInt("points_con");
            int points_goal = settings.getInt("points_goal");
            JSONArray events = match.getJSONArray("events");
            for(int i = 0; i < events.length(); i++){
                JSONObject event = events.getJSONObject(i);
                if(event.has("score"))return;
                if(event.has("team")){
                    int points = 0;
                    switch (event.getString("what")){
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
            Toast.makeText(getContext(), "Failed to show match", Toast.LENGTH_SHORT).show();
        }
    }
    public void bCloseClick(View view){
        InputMethodManager inputMethodManager = (InputMethodManager)view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getApplicationWindowToken(),0);
        bViewClick();
    }
    public void bViewClick(){
        view = view == 0 ? 1 : 0;
        findViewById(R.id.bView).setVisibility(VISIBLE);
        findViewById(R.id.bEdit).setVisibility(VISIBLE);
        findViewById(R.id.bClose).setVisibility(GONE);
        findViewById(R.id.bSave).setVisibility(GONE);
        findViewById(R.id.bShare).setVisibility(VISIBLE);
        findViewById(R.id.table).setVisibility(VISIBLE);
        findViewById(R.id.edit_team_names).setVisibility(GONE);
        showEvents();
    }
    public void bEditClick(){
        view = 2;
        findViewById(R.id.bView).setVisibility(GONE);
        findViewById(R.id.bEdit).setVisibility(GONE);
        findViewById(R.id.bClose).setVisibility(VISIBLE);
        findViewById(R.id.bSave).setVisibility(VISIBLE);
        findViewById(R.id.bShare).setVisibility(GONE);
        findViewById(R.id.table).setVisibility(GONE);
        findViewById(R.id.edit_team_names).setVisibility(VISIBLE);
        showEvents();
    }

    public void bDelClick(int event_id){
        try{
            for(int i=0; i<llEvents.getChildCount(); i++) {
                report_event_edit ree = (report_event_edit) llEvents.getChildAt(i);
                JSONObject event = ree.toJson();
                if(event.getInt("id") == event_id){
                    llEvents.removeViewAt(i);
                    return;
                }
            }
        }catch(Exception e){
            Log.e("report", "bDelClick: " + e.getMessage());
            Toast.makeText(getContext(), "Failed to delete", Toast.LENGTH_SHORT).show();
        }
    }

    public void bSaveClick(View view){
        InputMethodManager inputMethodManager = (InputMethodManager)view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getApplicationWindowToken(),0);
        try{
            JSONObject settings = match.getJSONObject("settings");
            int points_try = settings.getInt("points_try");
            int points_con = settings.getInt("points_con");
            int points_goal = settings.getInt("points_goal");
            int home_tries = 0;
            int home_cons = 0;
            int home_goals = 0;
            int away_tries = 0;
            int away_cons = 0;
            int away_goals = 0;
            int score_home = 0;
            int score_away = 0;

            JSONArray events = new JSONArray();
            for(int i=0; i<llEvents.getChildCount(); i++){
                report_event_edit ree = (report_event_edit)llEvents.getChildAt(i);
                JSONObject event = ree.toJson();
                String what = event.getString("what");
                String score = score_home + ":" + score_away;
                switch(what){
                    case "TRY":
                        if(event.getString("team").equals("home")){
                            home_tries++;
                            score_home += points_try;
                        }else{
                            away_tries++;
                            score_away += points_try;
                        }
                        break;
                    case "CONVERSION":
                        if(event.getString("team").equals("home")){
                            home_cons++;
                            score_home += points_con;
                        }else{
                            away_cons++;
                            score_away += points_con;
                        }
                        break;
                    case "GOAL":
                        if(event.getString("team").equals("home")){
                            home_goals++;
                            score_home += points_goal;
                        }else{
                            away_goals++;
                            score_away += points_goal;
                        }
                        break;
                    default:
                        if(what.startsWith("Result")){
                            what = what.substring(0, what.lastIndexOf(' ')) + " " + score;
                            event.put("what", what);
                        }
                }
                event.remove("score");
                events.put(event);
            }
            match.put("events", events);

            JSONObject home = match.getJSONObject("home");
            String home_team = ((EditText)findViewById(R.id.etHomeName)).getText().toString();
            home.put("team", home_team);
            home.put("tot", score_home);
            home.put("tries", home_tries);
            home.put("cons", home_cons);
            home.put("goals", home_goals);
            match.put("home", home);

            JSONObject away = match.getJSONObject("away");
            String away_team = ((EditText)findViewById(R.id.etAwayName)).getText().toString();
            away.put("team", away_team);
            away.put("tot", score_away);
            away.put("tries", away_tries);
            away.put("cons", away_cons);
            away.put("goals", away_goals);
            match.put("away", away);

            Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
            intent.putExtra("intent_type", "updateMatch");
            intent.putExtra("source", "report");
            intent.putExtra("match", match.toString());
            getContext().sendBroadcast(intent);
            gotMatch(match);
        }catch(Exception e){
            Log.e("report", "bSaveClick: " + e.getMessage());
            Toast.makeText(getContext(), "Failed to save", Toast.LENGTH_SHORT).show();
        }
    }
    public void bShareClick(){
        Context context = getContext();
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, getShareSubject());
        intent.putExtra(Intent.EXTRA_TEXT, getShareBody());
        try{
            context.startActivity(Intent.createChooser(intent, "Share match report"));
        }catch(Exception e){
            Log.e("report", "bShareClick: " + e.getMessage());
            Toast.makeText(context, "Failed to share", Toast.LENGTH_SHORT).show();
        }
    }
    private String getShareSubject(){
        String shareSubject = "Match report";
        try{
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
        }catch(Exception e){
            Log.e("report", "getShareSubject: " + e.getMessage());
            Toast.makeText(getContext(), "Failed to share", Toast.LENGTH_SHORT).show();
        }
        return shareSubject;
    }
    private String getShareBody(){
        StringBuilder shareBody = new StringBuilder();
        try{
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
            for(int i = 0; i < events.length(); i++){
                JSONObject event = events.getJSONObject(i);
                shareBody.append(event.getString("time"));
                shareBody.append("    ").append(event.getString("timer"));
                if(event.has("score")){
                    shareBody.append("    ").append(event.getString("score"));
                }
                shareBody.append("    ").append(event.getString("what"));
                if(event.has("team")){
                    if(event.getString("team").equals("home")){
                        shareBody.append(" ").append(MainActivity.getTeamName(home));
                    }else{
                        shareBody.append(" ").append(MainActivity.getTeamName(away));
                    }
                }
                if(event.has("who")){
                    shareBody.append(" ").append(event.getString("who"));
                }
                if(event.has("reason")){
                    shareBody.append("\n").append(event.getString("reason")).append("\n");
                }
                shareBody.append("\n");
            }

        }catch(Exception e){
            Log.e("report", "getShareBody: " + e.getMessage());
            Toast.makeText(getContext(), "Failed to share", Toast.LENGTH_SHORT).show();
        }
        return shareBody.toString();
    }
}

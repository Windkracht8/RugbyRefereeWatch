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

public class TabReport extends LinearLayout{
    private LinearLayout llEvents;

    private JSONObject match;
    private long match_id;

    public static int time_width;
    public static int timer_width;
    public static int score_width;

    private int view = 0;

    public TabReport(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(inflater == null){Toast.makeText(context, R.string.fail_show_report, Toast.LENGTH_SHORT).show(); return;}
        inflater.inflate(R.layout.tab_report, this, true);

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

    public void loadMatch(final JSONObject match){
        showMatch(match);
        findViewById(R.id.bEdit).setVisibility(VISIBLE);
        findViewById(R.id.tvNoEdit).setVisibility(GONE);
    }
    public void gotMatch(final JSONObject match){
        showMatch(match);
        try{
            if(!match.has("timer")) return;
            JSONObject timer = match.getJSONObject("timer");
            if(!timer.has("status")) return;
            if(timer.getString("status").equals("finished")){
                findViewById(R.id.bEdit).setVisibility(VISIBLE);
                findViewById(R.id.tvNoEdit).setVisibility(GONE);
                return;
            }
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "TabReport.gotMatch timer.status Exception: " + e.getMessage());
            return;
        }

        findViewById(R.id.bEdit).setVisibility(GONE);
        findViewById(R.id.tvNoEdit).setVisibility(VISIBLE);
    }
    private void showMatch(final JSONObject match){
        this.match = match;
        view = 0;
        addScores();
        try{
            this.match_id = match.getLong("matchid");
            JSONObject settings = match.getJSONObject("settings");
            JSONObject home = match.getJSONObject("home");
            JSONObject away = match.getJSONObject("away");

            ((TextView)findViewById(R.id.tvHomeName)).setText(MainActivity.getTeamName(getContext(), home));
            ((EditText)findViewById(R.id.etHomeName)).setText(MainActivity.getTeamName(getContext(), home));
            ((TextView)findViewById(R.id.tvAwayName)).setText(MainActivity.getTeamName(getContext(), away));
            ((EditText)findViewById(R.id.etAwayName)).setText(MainActivity.getTeamName(getContext(), away));
            ((TextView)findViewById(R.id.tvHomeTries)).setText(home.getString("tries"));
            ((TextView)findViewById(R.id.tvAwayTries)).setText(away.getString("tries"));

            if(!settings.has("points_con") || settings.getInt("points_con") == 0 ||
                    (home.getInt("cons") == 0 && away.getInt("cons") == 0)
            ){
                findViewById(R.id.trCons).setVisibility(View.GONE);
            }else{
                ((TextView)findViewById(R.id.tvHomeCons)).setText(home.getString("cons"));
                ((TextView)findViewById(R.id.tvAwayCons)).setText(away.getString("cons"));
                findViewById(R.id.trCons).setVisibility(View.VISIBLE);
            }
            if(!home.has("pen_tries") || !away.has("pen_tries") ||
                    (home.getInt("pen_tries") == 0 && away.getInt("pen_tries") == 0)
            ){
                findViewById(R.id.trPenTries).setVisibility(View.GONE);
            }else{
                ((TextView)findViewById(R.id.tvHomePenTries)).setText(home.getString("pen_tries"));
                ((TextView)findViewById(R.id.tvAwayPenTries)).setText(away.getString("pen_tries"));
                findViewById(R.id.trPenTries).setVisibility(View.VISIBLE);
            }
            if(!settings.has("points_goal") || settings.getInt("points_goal") == 0 ||
                    !home.has("goals") || !away.has("goals") ||
                    (home.getInt("goals") == 0 && away.getInt("goals") == 0)
            ){
                findViewById(R.id.trGoals).setVisibility(View.GONE);
            }else{
                ((TextView)findViewById(R.id.tvHomeGoals)).setText(home.getString("goals"));
                ((TextView)findViewById(R.id.tvAwayGoals)).setText(away.getString("goals"));
                findViewById(R.id.trGoals).setVisibility(View.VISIBLE);
            }
            if(!settings.has("points_goal") || settings.getInt("points_goal") == 0 ||
                    !home.has("pen_goals") || !away.has("pen_goals") ||
                    (home.getInt("pen_goals") == 0 && away.getInt("pen_goals") == 0)
            ){
                findViewById(R.id.trPenGoals).setVisibility(View.GONE);
            }else{
                ((TextView)findViewById(R.id.tvHomePenGoals)).setText(home.getString("pen_goals"));
                ((TextView)findViewById(R.id.tvAwayPenGoals)).setText(away.getString("pen_goals"));
                findViewById(R.id.trPenGoals).setVisibility(View.VISIBLE);
            }
            if(!settings.has("points_goal") || settings.getInt("points_goal") == 0 ||
                    !home.has("drop_goals") || !away.has("drop_goals") ||
                    (home.getInt("drop_goals") == 0 && away.getInt("drop_goals") == 0)
            ){
                findViewById(R.id.trDropGoals).setVisibility(View.GONE);
            }else{
                ((TextView)findViewById(R.id.tvHomeDropGoals)).setText(home.getString("drop_goals"));
                ((TextView)findViewById(R.id.tvAwayDropGoals)).setText(away.getString("drop_goals"));
                findViewById(R.id.trDropGoals).setVisibility(View.VISIBLE);
            }
            if(!home.has("pens") || !away.has("pens") ||
                    (home.getInt("pens") == 0 && away.getInt("pens") == 0)
            ){
                findViewById(R.id.trPens).setVisibility(View.GONE);
            }else{
                ((TextView)findViewById(R.id.tvHomePens)).setText(home.getString("pens"));
                ((TextView)findViewById(R.id.tvAwayPens)).setText(away.getString("pens"));
                findViewById(R.id.trPens).setVisibility(View.VISIBLE);
            }

            ((TextView)findViewById(R.id.tvHomeTot)).setText(home.getString("tot"));
            ((TextView)findViewById(R.id.tvAwayTot)).setText(away.getString("tot"));

            showEvents();
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "TabReport.gotMatch Exception: " + e.getMessage());
            Toast.makeText(getContext(), R.string.fail_show_match, Toast.LENGTH_SHORT).show();
        }

        findViewById(R.id.bView).setVisibility(VISIBLE);
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
                        llEvents.addView(new ReportEvent(context, event));
                        break;
                    case 1:
                        llEvents.addView(new ReportEventFull(context, event, match));
                        break;
                    case 2:
                        llEvents.addView(new ReportEventEdit(context, event));
                        break;
                }
            }
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "TabReport.showEvents Exception: " + e.getMessage());
            Toast.makeText(getContext(), R.string.fail_show_match, Toast.LENGTH_SHORT).show();
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
                        case "PENALTY TRY":
                            points = points_try + points_con;
                            break;
                        case "GOAL":
                        case "PENALTY GOAL":
                        case "DROP GOAL":
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
            Log.e(MainActivity.RRW_LOG_TAG, "TabReport.getScore Exception: " + e.getMessage());
            Toast.makeText(getContext(), R.string.fail_show_match, Toast.LENGTH_SHORT).show();
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
                ReportEventEdit ree = (ReportEventEdit) llEvents.getChildAt(i);
                JSONObject event = ree.toJson();
                if(event.getInt("id") == event_id){
                    llEvents.removeViewAt(i);
                    return;
                }
            }
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "TabReport.bDelClick Exception: " + e.getMessage());
            Toast.makeText(getContext(), R.string.fail_delete, Toast.LENGTH_SHORT).show();
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
            int home_pen_tries = 0;
            int home_goals = 0;
            int home_pen_goals = 0;
            int home_drop_goals = 0;
            int home_pens = 0;
            int away_tries = 0;
            int away_cons = 0;
            int away_pen_tries = 0;
            int away_goals = 0;
            int away_pen_goals = 0;
            int away_drop_goals = 0;
            int away_pens = 0;
            int score_home = 0;
            int score_away = 0;

            JSONArray events = new JSONArray();
            for(int i=0; i<llEvents.getChildCount(); i++){
                ReportEventEdit ree = (ReportEventEdit)llEvents.getChildAt(i);
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
                    case "PENALTY TRY":
                        if(event.getString("team").equals("home")){
                            home_pen_tries++;
                            score_home += points_try + points_con;
                        }else{
                            away_pen_tries++;
                            score_away += points_try + points_con;
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
                    case "PENALTY GOAL":
                        if(event.getString("team").equals("home")){
                            home_pen_goals++;
                            score_home += points_goal;
                        }else{
                            away_pen_goals++;
                            score_away += points_goal;
                        }
                        break;
                    case "DROP GOAL":
                        if(event.getString("team").equals("home")){
                            home_drop_goals++;
                            score_home += points_goal;
                        }else{
                            away_drop_goals++;
                            score_away += points_goal;
                        }
                        break;
                    case "PENALTY":
                        if(event.getString("team").equals("home")){
                            home_pens++;
                        }else{
                            away_pens++;
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
            home.put("pen_tries", home_pen_tries);
            home.put("goals", home_goals);
            home.put("pen_goals", home_pen_goals);
            home.put("drop_goals", home_drop_goals);
            home.put("pens", home_pens);
            match.put("home", home);

            JSONObject away = match.getJSONObject("away");
            String away_team = ((EditText)findViewById(R.id.etAwayName)).getText().toString();
            away.put("team", away_team);
            away.put("tot", score_away);
            away.put("tries", away_tries);
            away.put("cons", away_cons);
            away.put("pen_tries", away_pen_tries);
            away.put("goals", away_goals);
            away.put("pen_goals", away_pen_goals);
            away.put("drop_goals", away_drop_goals);
            away.put("pens", away_pens);
            match.put("away", away);

            Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
            intent.putExtra("intent_type", "updateMatch");
            intent.putExtra("match", match.toString());
            getContext().sendBroadcast(intent);
            loadMatch(match);
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "TabReport.bSaveClick Exception: " + e.getMessage());
            Toast.makeText(getContext(), R.string.fail_save, Toast.LENGTH_SHORT).show();
        }
    }
    public void bShareClick(){
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, getShareSubject());
        intent.putExtra(Intent.EXTRA_TEXT, getShareBody());
        try{
            getContext().startActivity(Intent.createChooser(intent, getContext().getString(R.string.share_report)));
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "TabReport.bShareClick Exception: " + e.getMessage());
            Toast.makeText(getContext(), R.string.fail_share, Toast.LENGTH_SHORT).show();
        }
    }
    private String getShareSubject(){
        String shareSubject = getContext().getString(R.string.match_report);
        try{
            Date match_date_d = new Date(match_id);
            String match_date_s = new SimpleDateFormat("E dd-MM-yyyy HH:mm", Locale.getDefault()).format(match_date_d);
            shareSubject += " " + match_date_s;

            JSONObject home = match.getJSONObject("home");
            shareSubject += " " + MainActivity.getTeamName(getContext(), home);
            shareSubject += " " + home.getString("tot");

            shareSubject += " v";

            JSONObject away = match.getJSONObject("away");
            shareSubject += " " + MainActivity.getTeamName(getContext(), away);
            shareSubject += " " + away.getString("tot");
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "TabReport.getShareSubject Exception: " + e.getMessage());
            Toast.makeText(getContext(), R.string.fail_share, Toast.LENGTH_SHORT).show();
        }
        return shareSubject;
    }
    private String getShareBody(){//TODO: also allow to export in english
        StringBuilder shareBody = new StringBuilder();
        try{
            shareBody.append(getShareSubject()).append("\n\n");

            JSONObject home = match.getJSONObject("home");
            JSONObject away = match.getJSONObject("away");

            boolean show_cons = home.has("cons") && away.has("cons") && (home.getInt("cons") > 0 || away.getInt("cons") > 0);
            boolean show_pen_tries = home.has("pen_tries") && away.has("pen_tries") && (home.getInt("pen_tries") > 0 || away.getInt("pen_tries") > 0);
            boolean show_goals = home.has("goals") && away.has("goals") && (home.getInt("goals") > 0 || away.getInt("goals") > 0);
            boolean show_pen_goals = home.has("pen_goals") && away.has("pen_goals") && (home.getInt("pen_goals") > 0 || away.getInt("pen_goals") > 0);
            boolean show_drop_goals = home.has("drop_goals") && away.has("drop_goals") && (home.getInt("drop_goals") > 0 || away.getInt("drop_goals") > 0);
            boolean show_pens = home.has("pens") && away.has("pens") && (home.getInt("pens") > 0 || away.getInt("pens") > 0);
            shareBody.append(MainActivity.getTeamName(getContext(), home)).append("\n");
            shareBody.append("  ").append(getContext().getString(R.string.tries)).append(": ").append(home.getString("tries")).append("\n");
            if(show_cons){
                shareBody.append("  ").append(getContext().getString(R.string.conversions)).append(": ").append(home.getString("cons")).append("\n");
            }
            if(show_pen_tries){
                shareBody.append("  ").append(getContext().getString(R.string.pen_tries)).append(": ").append(home.getString("pen_tries")).append("\n");
            }
            if(show_goals){
                shareBody.append("  ").append(getContext().getString(R.string.goals)).append(": ").append(home.getString("goals")).append("\n");
            }
            if(show_pen_goals){
                shareBody.append("  ").append(getContext().getString(R.string.pen_goals)).append(": ").append(home.getString("pen_goals")).append("\n");
            }
            if(show_drop_goals){
                shareBody.append("  ").append(getContext().getString(R.string.drop_goals)).append(": ").append(home.getString("drop_goals")).append("\n");
            }
            if(show_pens){
                shareBody.append("  ").append(getContext().getString(R.string.penalties)).append(": ").append(home.getString("pens")).append("\n");
            }
            shareBody.append("  ").append(getContext().getString(R.string.total)).append(": ").append(home.getString("tot")).append("\n");
            shareBody.append("\n");

            shareBody.append(MainActivity.getTeamName(getContext(), away)).append("\n");
            shareBody.append("  ").append(getContext().getString(R.string.tries)).append(": ").append(away.getString("tries")).append("\n");
            if(show_cons){
                shareBody.append("  ").append(getContext().getString(R.string.conversions)).append(": ").append(away.getString("cons")).append("\n");
            }
            if(show_pen_tries){
                shareBody.append("  ").append(getContext().getString(R.string.pen_tries)).append(": ").append(away.getString("pen_tries")).append("\n");
            }
            if(show_goals){
                shareBody.append("  ").append(getContext().getString(R.string.goals)).append(": ").append(away.getString("goals")).append("\n");
            }
            if(show_pen_goals){
                shareBody.append("  ").append(getContext().getString(R.string.pen_goals)).append(": ").append(away.getString("pen_goals")).append("\n");
            }
            if(show_drop_goals){
                shareBody.append("  ").append(getContext().getString(R.string.drop_goals)).append(": ").append(away.getString("drop_goals")).append("\n");
            }
            if(show_pens){
                shareBody.append("  ").append(getContext().getString(R.string.penalties)).append(": ").append(away.getString("pens")).append("\n");
            }
            shareBody.append("  ").append(getContext().getString(R.string.total)).append(": ").append(away.getString("tot")).append("\n");
            shareBody.append("\n");

            JSONArray events = match.getJSONArray("events");
            for(int i = 0; i < events.length(); i++){
                JSONObject event = events.getJSONObject(i);
                shareBody.append(event.getString("time"));
                shareBody.append("    ").append(event.getString("timer"));
                if(event.has("score")){
                    shareBody.append("    ").append(event.getString("score"));
                }
                shareBody.append("    ").append(translator.getEventTypeLocal(getContext(), event.getString("what")));
                if(event.has("team")){
                    if(event.getString("team").equals("home")){
                        shareBody.append(" ").append(MainActivity.getTeamName(getContext(), home));
                    }else{
                        shareBody.append(" ").append(MainActivity.getTeamName(getContext(), away));
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
            Log.e(MainActivity.RRW_LOG_TAG, "TabReport.getShareBody Exception: " + e.getMessage());
            Toast.makeText(getContext(), R.string.fail_share, Toast.LENGTH_SHORT).show();
        }
        return shareBody.toString();
    }
}

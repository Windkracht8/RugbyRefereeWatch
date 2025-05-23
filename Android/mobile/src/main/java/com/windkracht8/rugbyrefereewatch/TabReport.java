/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TabReport extends LinearLayout{
    private Main main;
    private final LinearLayout llEvents;

    private JSONObject match;
    private long match_id;

    private int view_type = 0;

    static int width_score;
    static int width_timer;
    static int width_time;
    static int width_team;

    private static final int SHARE_TIME = 0;
    private static final int SHARE_PENS = 1;
    private static final int SHARE_CLOCK = 2;

    public TabReport(Context context, AttributeSet attrs){
        super(context, attrs);
        ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.tab_report, this, true);
        llEvents = findViewById(R.id.llEvents);

        findViewById(R.id.bView).setOnClickListener(v->bViewClick());
        findViewById(R.id.bEdit).setOnClickListener(v->bEditClick());
        findViewById(R.id.bCancel).setOnClickListener(v->bCancelClick());
        findViewById(R.id.bSave).setOnClickListener(v->bSaveClick());
        findViewById(R.id.bShare).setOnClickListener(v->bShareClick());
    }

    void onCreateMain(Main main){
        findViewById(R.id.svReport).setOnTouchListener(main::onTouchEventScrollViews);
    }
    void loadMatch(Main main, JSONObject match){
        this.main = main;
        this.match = match;
        view_type = 0;
        try{
            match_id = match.getLong("matchid");
            JSONObject settings = match.getJSONObject("settings");
            JSONObject home = match.getJSONObject(Main.HOME_ID);
            JSONObject away = match.getJSONObject(Main.AWAY_ID);

            ((TextView)findViewById(R.id.tvHomeName)).setText(main.getTeamName(home));
            ((EditText)findViewById(R.id.etHomeName)).setText(main.getTeamName(home));
            ((TextView)findViewById(R.id.tvAwayName)).setText(main.getTeamName(away));
            ((EditText)findViewById(R.id.etAwayName)).setText(main.getTeamName(away));
            ((TextView)findViewById(R.id.tvHomeTries)).setText(home.getString("tries"));
            ((TextView)findViewById(R.id.tvAwayTries)).setText(away.getString("tries"));

            if(shouldShow("points_con", settings, "cons", home, away)){
                ((TextView)findViewById(R.id.tvHomeCons)).setText(home.getString("cons"));
                ((TextView)findViewById(R.id.tvAwayCons)).setText(away.getString("cons"));
                findViewById(R.id.trCons).setVisibility(View.VISIBLE);
            }else{
                findViewById(R.id.trCons).setVisibility(View.GONE);
            }

            if(shouldShow("pen_tries", home, away)){
                ((TextView)findViewById(R.id.tvHomePenTries)).setText(home.getString("pen_tries"));
                ((TextView)findViewById(R.id.tvAwayPenTries)).setText(away.getString("pen_tries"));
                findViewById(R.id.trPenTries).setVisibility(View.VISIBLE);
            }else{
                findViewById(R.id.trPenTries).setVisibility(View.GONE);
            }

            if(shouldShow("points_goal", settings, "goals", home, away)){
                ((TextView)findViewById(R.id.tvHomeGoals)).setText(home.getString("goals"));
                ((TextView)findViewById(R.id.tvAwayGoals)).setText(away.getString("goals"));
                findViewById(R.id.trGoals).setVisibility(View.VISIBLE);
            }else{
                findViewById(R.id.trGoals).setVisibility(View.GONE);
            }

            if(shouldShow("points_goal", settings, "pen_goals", home, away)){
                ((TextView)findViewById(R.id.tvHomePenGoals)).setText(home.getString("pen_goals"));
                ((TextView)findViewById(R.id.tvAwayPenGoals)).setText(away.getString("pen_goals"));
                findViewById(R.id.trPenGoals).setVisibility(View.VISIBLE);
            }else{
                findViewById(R.id.trPenGoals).setVisibility(View.GONE);
            }

            if(shouldShow("points_goal", settings, "drop_goals", home, away)){
                ((TextView)findViewById(R.id.tvHomeDropGoals)).setText(home.getString("drop_goals"));
                ((TextView)findViewById(R.id.tvAwayDropGoals)).setText(away.getString("drop_goals"));
                findViewById(R.id.trDropGoals).setVisibility(View.VISIBLE);
            }else{
                findViewById(R.id.trDropGoals).setVisibility(View.GONE);
            }

            if(shouldShow("yellow_cards", home, away)){
                ((TextView)findViewById(R.id.tvHomeYellowCards)).setText(home.getString("yellow_cards"));
                ((TextView)findViewById(R.id.tvAwayYellowCards)).setText(away.getString("yellow_cards"));
                findViewById(R.id.trYellowCards).setVisibility(View.VISIBLE);
            }else{
                findViewById(R.id.trYellowCards).setVisibility(View.GONE);
            }
            if(shouldShow("red_cards", home, away)){
                ((TextView)findViewById(R.id.tvHomeRedCards)).setText(home.getString("red_cards"));
                ((TextView)findViewById(R.id.tvAwayRedCards)).setText(away.getString("red_cards"));
                findViewById(R.id.trRedCards).setVisibility(View.VISIBLE);
            }else{
                findViewById(R.id.trRedCards).setVisibility(View.GONE);
            }
            if(shouldShow("pens", home, away)){
                ((TextView)findViewById(R.id.tvHomePens)).setText(home.getString("pens"));
                ((TextView)findViewById(R.id.tvAwayPens)).setText(away.getString("pens"));
                findViewById(R.id.trPens).setVisibility(View.VISIBLE);
            }else{
                findViewById(R.id.trPens).setVisibility(View.GONE);
            }

            ((TextView)findViewById(R.id.tvHomeTot)).setText(home.getString("tot"));
            ((TextView)findViewById(R.id.tvAwayTot)).setText(away.getString("tot"));

            showEvents();
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabReport.loadMatch Exception: " + e.getMessage());
            main.toast(R.string.fail_show_match);
        }

        findViewById(R.id.bView).setVisibility(VISIBLE);
        findViewById(R.id.bCancel).setVisibility(GONE);
        findViewById(R.id.bSave).setVisibility(GONE);
        findViewById(R.id.bShare).setVisibility(VISIBLE);
        findViewById(R.id.table).setVisibility(VISIBLE);
        findViewById(R.id.edit_team_names).setVisibility(GONE);
        findViewById(R.id.bEdit).setVisibility(VISIBLE);
    }
    private void showEvents(){
        llEvents.removeAllViews();
        try{
            JSONArray events = match.getJSONArray("events");
            JSONObject settings = match.getJSONObject("settings");
            int period_count = settings.getInt("period_count");
            int period_time = settings.getInt("period_time");
            int points_try = settings.getInt("points_try");
            int points_con = settings.getInt("points_con");
            int points_goal = settings.getInt("points_goal");
            int[] score = {0, 0};

            for(int i=0; i<events.length(); i++){
                JSONObject event = events.getJSONObject(i);
                switch(view_type){
                    case 0:
                        if(event.has("team")) calcScore(event.getString("what"), event.getString("team"), points_try, points_con, points_goal, score);
                        llEvents.addView(new ReportEventStandard(main, event, period_count, period_time, score));
                        break;
                    case 1:
                        llEvents.addView(new ReportEventFull(main, event, match, period_count, period_time));
                        break;
                    case 2:
                        llEvents.addView(new ReportEventEdit(main, event));
                        break;
                }
            }
            llEvents.getViewTreeObserver().addOnGlobalLayoutListener(resizeFields);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabReport.showEvents Exception: " + e.getMessage());
            main.toast(R.string.fail_show_match);
        }
    }
    private final ViewTreeObserver.OnGlobalLayoutListener resizeFields = new ViewTreeObserver.OnGlobalLayoutListener(){
        @Override public void onGlobalLayout(){
            //Resize the fields in llEvents so that it looks like a neat table
            width_score = 0;
            width_timer = 0;
            width_time = 0;
            width_team = 0;
            for(int i=0; i<llEvents.getChildCount(); i++){
                View view = llEvents.getChildAt(i);
                if(view instanceof ReportEvent)
                    ((ReportEvent)view).getFieldWidths();
            }
            if(width_timer > 0){
                llEvents.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                for(int i=0; i<llEvents.getChildCount(); i++){
                    View view = llEvents.getChildAt(i);
                    if(view instanceof ReportEvent)
                        ((ReportEvent)view).setFieldWidths();
                }
            }
        }
    };

    private static void calcScore(String what, String team, int points_try, int points_con, int points_goal, int[] score){
        switch(what){
            case "TRY":
                score[team.equals(Main.HOME_ID) ? 0 : 1] += points_try;
                break;
            case "CONVERSION":
                score[team.equals(Main.HOME_ID) ? 0 : 1] += points_con;
                break;
            case "PENALTY TRY":
                score[team.equals(Main.HOME_ID) ? 0 : 1] += points_try + points_con;
                break;
            case "GOAL":
            case "PENALTY GOAL":
            case "DROP GOAL":
                score[team.equals(Main.HOME_ID) ? 0 : 1] += points_goal;
                break;
        }
    }

    private void bCancelClick(){
        InputMethodManager inputMethodManager = (InputMethodManager)main.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(getApplicationWindowToken(),0);
        bViewClick();
    }
    private void bViewClick(){
        view_type = view_type == 0 ? 1 : 0;
        findViewById(R.id.bView).setVisibility(VISIBLE);
        findViewById(R.id.bEdit).setVisibility(VISIBLE);
        findViewById(R.id.bCancel).setVisibility(GONE);
        findViewById(R.id.bSave).setVisibility(GONE);
        findViewById(R.id.bShare).setVisibility(VISIBLE);
        findViewById(R.id.table).setVisibility(VISIBLE);
        findViewById(R.id.edit_team_names).setVisibility(GONE);
        showEvents();
    }
    private void bEditClick(){
        view_type = 2;
        findViewById(R.id.bView).setVisibility(GONE);
        findViewById(R.id.bEdit).setVisibility(GONE);
        findViewById(R.id.bCancel).setVisibility(VISIBLE);
        findViewById(R.id.bSave).setVisibility(VISIBLE);
        findViewById(R.id.bShare).setVisibility(GONE);
        findViewById(R.id.table).setVisibility(GONE);
        findViewById(R.id.edit_team_names).setVisibility(VISIBLE);
        showEvents();
    }

    void bDelClick(int event_id){
        try{
            for(int i=0; i<llEvents.getChildCount(); i++){
                ReportEventEdit ree = (ReportEventEdit) llEvents.getChildAt(i);
                JSONObject event = ree.toJson();
                if(event.getInt("id") == event_id){
                    llEvents.removeViewAt(i);
                    return;
                }
            }
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabReport.bDelClick Exception: " + e.getMessage());
            main.toast(R.string.fail_delete);
        }
    }
    private void bSaveClick(){
        InputMethodManager inputMethodManager = (InputMethodManager)main.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(getApplicationWindowToken(),0);
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
            int home_yellow_cards = 0;
            int home_red_cards = 0;
            int home_pens = 0;
            int away_tries = 0;
            int away_cons = 0;
            int away_pen_tries = 0;
            int away_goals = 0;
            int away_pen_goals = 0;
            int away_drop_goals = 0;
            int away_yellow_cards = 0;
            int away_red_cards = 0;
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
                        if(event.getString("team").equals(Main.HOME_ID)){
                            home_tries++;
                            score_home += points_try;
                        }else{
                            away_tries++;
                            score_away += points_try;
                        }
                        break;
                    case "CONVERSION":
                        if(event.getString("team").equals(Main.HOME_ID)){
                            home_cons++;
                            score_home += points_con;
                        }else{
                            away_cons++;
                            score_away += points_con;
                        }
                        break;
                    case "PENALTY TRY":
                        if(event.getString("team").equals(Main.HOME_ID)){
                            home_pen_tries++;
                            score_home += points_try + points_con;
                        }else{
                            away_pen_tries++;
                            score_away += points_try + points_con;
                        }
                        break;
                    case "GOAL":
                        if(event.getString("team").equals(Main.HOME_ID)){
                            home_goals++;
                            score_home += points_goal;
                        }else{
                            away_goals++;
                            score_away += points_goal;
                        }
                        break;
                    case "PENALTY GOAL":
                        if(event.getString("team").equals(Main.HOME_ID)){
                            home_pen_goals++;
                            score_home += points_goal;
                        }else{
                            away_pen_goals++;
                            score_away += points_goal;
                        }
                        break;
                    case "DROP GOAL":
                        if(event.getString("team").equals(Main.HOME_ID)){
                            home_drop_goals++;
                            score_home += points_goal;
                        }else{
                            away_drop_goals++;
                            score_away += points_goal;
                        }
                        break;
                    case "YELLOW CARD":
                        if(event.getString("team").equals(Main.HOME_ID)){
                            home_yellow_cards++;
                        }else{
                            away_yellow_cards++;
                        }
                        break;
                    case "RED CARD":
                        if(event.getString("team").equals(Main.HOME_ID)){
                            home_red_cards++;
                        }else{
                            away_red_cards++;
                        }
                        break;
                    case "PENALTY":
                        if(event.getString("team").equals(Main.HOME_ID)){
                            home_pens++;
                        }else{
                            away_pens++;
                        }
                        break;
                    case "END":
                        event.put("score", score);
                        break;
                }
                events.put(event);
            }
            match.put("events", events);

            JSONObject home = match.getJSONObject(Main.HOME_ID);
            String home_team = ((EditText)findViewById(R.id.etHomeName)).getText().toString();
            home.put("team", home_team);
            home.put("tot", score_home);
            home.put("tries", home_tries);
            home.put("cons", home_cons);
            home.put("pen_tries", home_pen_tries);
            home.put("goals", home_goals);
            home.put("pen_goals", home_pen_goals);
            home.put("drop_goals", home_drop_goals);
            home.put("yellow_cards", home_yellow_cards);
            home.put("red_cards", home_red_cards);
            home.put("pens", home_pens);
            match.put(Main.HOME_ID, home);

            JSONObject away = match.getJSONObject(Main.AWAY_ID);
            String away_team = ((EditText)findViewById(R.id.etAwayName)).getText().toString();
            away.put("team", away_team);
            away.put("tot", score_away);
            away.put("tries", away_tries);
            away.put("cons", away_cons);
            away.put("pen_tries", away_pen_tries);
            away.put("goals", away_goals);
            away.put("pen_goals", away_pen_goals);
            away.put("drop_goals", away_drop_goals);
            away.put("yellow_cards", away_yellow_cards);
            away.put("red_cards", away_red_cards);
            away.put("pens", away_pens);
            match.put(Main.AWAY_ID, away);

            main.tabHistory.updateMatch(match);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabReport.bSaveClick Exception: " + e.getMessage());
            main.toast(R.string.fail_save);
        }
        loadMatch(main, match);
    }
    private void bShareClick(){
        boolean[] eventTypes = {false, false, false};
        View view = View.inflate(main, R.layout.dialog_share, null);
        ((CheckBox)view.findViewById(R.id.dialog_share_time)).setOnCheckedChangeListener(
                (v, c)->eventTypes[SHARE_TIME] = c
        );
        ((CheckBox)view.findViewById(R.id.dialog_share_pens)).setOnCheckedChangeListener(
                (v, c)->eventTypes[SHARE_PENS] = c
        );
        ((CheckBox)view.findViewById(R.id.dialog_share_clock)).setOnCheckedChangeListener(
                (v, c)->eventTypes[SHARE_CLOCK] = c
        );
        new AlertDialog.Builder(main)
                .setMessage(R.string.dialog_share_message)
                .setView(view)
                .setPositiveButton(R.string.share, (d, w)->share(eventTypes))
                .setNegativeButton(R.string.cancel, (d, w)->d.dismiss())
                .create()
                .show();
    }
    private void share(boolean[] eventTypes){
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, getShareSubject());
        intent.putExtra(Intent.EXTRA_TEXT, getShareBody(eventTypes));
        try{
            main.startActivity(Intent.createChooser(intent, main.getString(R.string.share_report)));
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabReport.bShareClick Exception: " + e.getMessage());
            main.toast(R.string.fail_share);
        }
    }
    private String getShareSubject(){
        String shareSubject = main.getString(R.string.match_report);
        try{
            Date match_date_d = new Date(match_id);
            String match_date_s = new SimpleDateFormat("E dd-MM-yyyy HH:mm", Locale.getDefault()).format(match_date_d);
            shareSubject += " " + match_date_s;

            JSONObject home = match.getJSONObject(Main.HOME_ID);
            shareSubject += " " + main.getTeamName(home);
            shareSubject += " " + home.getString("tot");

            shareSubject += " v";

            JSONObject away = match.getJSONObject(Main.AWAY_ID);
            shareSubject += " " + main.getTeamName(away);
            shareSubject += " " + away.getString("tot");
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabReport.getShareSubject Exception: " + e.getMessage());
            main.toast(R.string.fail_share);
        }
        return shareSubject;
    }
    private String getShareBody(boolean[] eventTypes){
        StringBuilder shareBody = new StringBuilder();
        try{
            shareBody.append(getShareSubject()).append("\n\n");

            JSONObject home = match.getJSONObject(Main.HOME_ID);
            StringBuilder scoreHome = new StringBuilder();
            scoreHome.append(main.getTeamName(home)).append("\n")
                    .append(scoreLine(R.string.tries, home.getString("tries")));

            JSONObject away = match.getJSONObject(Main.AWAY_ID);
            StringBuilder scoreAway = new StringBuilder();
            scoreAway.append(main.getTeamName(away)).append("\n")
                    .append(scoreLine(R.string.tries, away.getString("tries")));

            if(shouldShow("cons", home, away)){
                scoreHome.append(scoreLine(R.string.conversions, home.getString("cons")));
                scoreAway.append(scoreLine(R.string.conversions, away.getString("cons")));
            }
            if(shouldShow("pen_tries", home, away)){
                scoreHome.append(scoreLine(R.string.pen_tries, home.getString("pen_tries")));
                scoreAway.append(scoreLine(R.string.pen_tries, away.getString("pen_tries")));
            }
            if(shouldShow("goals", home, away)){
                scoreHome.append(scoreLine(R.string.goals, home.getString("goals")));
                scoreAway.append(scoreLine(R.string.goals, away.getString("goals")));
            }
            if(shouldShow("pen_goals", home, away)){
                scoreHome.append(scoreLine(R.string.pen_goals, home.getString("pen_goals")));
                scoreAway.append(scoreLine(R.string.pen_goals, away.getString("pen_goals")));
            }
            if(shouldShow("drop_goals", home, away)){
                scoreHome.append(scoreLine(R.string.drop_goals, home.getString("drop_goals")));
                scoreAway.append(scoreLine(R.string.drop_goals, away.getString("drop_goals")));
            }
            if(shouldShow("yellow_cards", home, away)){
                scoreHome.append(scoreLine(R.string.yellow_cards, home.getString("yellow_cards")));
                scoreAway.append(scoreLine(R.string.yellow_cards, away.getString("yellow_cards")));
            }
            if(shouldShow("red_cards", home, away)){
                scoreHome.append(scoreLine(R.string.red_cards, home.getString("red_cards")));
                scoreAway.append(scoreLine(R.string.red_cards, away.getString("red_cards")));
            }
            if(eventTypes[SHARE_PENS] && shouldShow("pens", home, away)){
                scoreHome.append(scoreLine(R.string.penalties, home.getString("pens")));
                scoreAway.append(scoreLine(R.string.penalties, away.getString("pens")));
            }
            scoreHome.append(scoreLine(R.string.total, home.getString("tot")));
            scoreAway.append(scoreLine(R.string.total, away.getString("tot")));

            shareBody.append(scoreHome).append("\n")
                    .append(scoreAway).append("\n");

            JSONObject settings = match.getJSONObject("settings");
            int period_count = settings.getInt("period_count");
            boolean doubleDigitTime = settings.getInt("period_time") >= 10;
            JSONArray events = match.getJSONArray("events");
            for(int i=0; i<events.length(); i++){
                JSONObject event = events.getJSONObject(i);
                String what = event.getString("what");
                if((!eventTypes[SHARE_TIME] && what.equals("TIME OFF")) ||
                        (!eventTypes[SHARE_TIME] && what.equals("RESUME")) ||
                        (!eventTypes[SHARE_PENS] && what.equals("PENALTY"))
                ) continue;

                if(eventTypes[SHARE_CLOCK])
                    shareBody.append(event.getString("time")).append("    ");

                String timer = ReportEventEdit.prettyTimer(event.getInt("timer"));
                if(doubleDigitTime && timer.length() == 4) shareBody.append("0");
                shareBody.append(timer)
                        .append("    ")
                        .append(Translator.getEventTypeLocal(main, what, event.getInt("period"), period_count));
                if(event.has("team")){
                    if(event.getString("team").equals(Main.HOME_ID)){
                        shareBody.append(" ").append(main.getTeamName(home));
                    }else{
                        shareBody.append(" ").append(main.getTeamName(away));
                    }
                }
                if(event.has("who"))
                    shareBody.append(" ").append(event.getString("who"));
                if(event.has("score"))
                    shareBody.append(" ").append(event.getString("score"));
                if(event.has("reason"))
                    shareBody.append("\n      ").append(event.getString("reason"));
                shareBody.append("\n");
                if(what.equals("END"))
                    shareBody.append("\n");
            }

        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabReport.getShareBody Exception: " + e.getMessage());
            main.toast(R.string.fail_share);
        }
        return shareBody.toString();
    }
    private boolean shouldShow(String setting, JSONObject settings, String what, JSONObject home, JSONObject away) throws JSONException{
        return settings.has(setting) && settings.getInt(setting) > 0 && shouldShow(what, home, away);
    }
    private boolean shouldShow(String what, JSONObject home, JSONObject away) throws JSONException{
        return home.has(what) && away.has(what) && (home.getInt(what) > 0 || away.getInt(what) > 0);
    }
    private String scoreLine(int name, String value){
        return String.format("  %s: %s\n", main.getString(name), value);
    }
}

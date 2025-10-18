/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.windkracht8.rugbyrefereewatch;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Report extends Activity{
    private LinearLayout list;
    public static MatchData match;
    @Override public void onCreate(Bundle ignored){
        super.onCreate(null);
        setContentView(R.layout.scroll_screen);
        ((TextView) findViewById(R.id.label)).setText(R.string.report_title);
        list = findViewById(R.id.list);
        if(Main.isScreenRound) list.setPadding(Main._10dp, Main._10dp, Main._10dp, Main.vh25);
        findViewById(R.id.scrollView).requestFocus();

        if(match == null){
            finishAndRemoveTask();
            return;
        }
        addItem(match.home.tries + " tries " + match.away.tries);

        if(match.home.pen_tries > 0 || match.away.pen_tries > 0)
            addItem(match.home.pen_tries + " pen. tries " + match.away.pen_tries);
        if(match.points_con > 0 && (match.home.cons > 0 || match.away.cons > 0))
            addItem(match.home.cons + " conv. " + match.away.cons);
        if(match.points_goal > 0 && (match.home.drop_goals > 0 || match.away.drop_goals > 0))
            addItem(match.home.drop_goals + " drop goals " + match.away.drop_goals);
        if(match.points_goal > 0 && (match.home.pen_goals > 0 || match.away.pen_goals > 0))
            addItem(match.home.pen_goals + " pen. goals " + match.away.pen_goals);
        if(match.points_goal > 0 && (match.home.goals > 0 || match.away.goals > 0))
            addItem(match.home.goals + " goals " + match.away.goals);
        addItem(match.home.tot + " score " + match.away.tot);
        addItem("");

        if(match.home.yellow_cards > 0 || match.away.yellow_cards > 0)
            addItem(match.home.yellow_cards + " yellow cards " + match.away.yellow_cards);
        if(match.home.red_cards > 0 || match.away.red_cards > 0)
            addItem(match.home.red_cards + " red cards " + match.away.red_cards);
        if(Main.record_pens && (match.home.pens > 0 || match.away.pens > 0))
            addItem(match.home.pens + " penalties " + match.away.pens);
        addItem("");

        for(MatchData.Event event : match.events){
            if(event.what.equals("RESUME") || event.what.equals("TIME OFF") || event.deleted){
                continue;
            }
            String text = Utils.prettyTimer(event.timer) + " ";
            switch(event.what){
                case "START":
                    text += getString(R.string.Start) + " " + Main.getPeriodName(this, event.period, match.period_count);
                    break;
                case "END":
                    text += getString(R.string.Result) + " " + Main.getPeriodName(this, event.period, match.period_count);
                    if(event.score != null) text += " " + event.score;
                    break;
                case "REPLACEMENT":
                    if(event.team != null){
                        text += Translator.getTeamLocal(this, event.team)
                            + Utils.replacementString(this, event);
                    }
                    break;
                default:
                    text += Translator.getEventTypeLocal(this, event.what);
                    if(event.team != null){
                        text += " " + Translator.getTeamLocal(this, event.team);
                        if(event.who > 0) text += ' ' + Integer.toString(event.who);
                        text += Utils.replacementString(this, event);
                    }
                    break;
            }
            addItem(text);
        }
    }
    @Override public void onDestroy(){
        super.onDestroy();
        match = null;
    }

    private void addItem(String text){
        TextView item = new TextView(this, null, 0, R.style.textView_report);
        item.setText(text);
        list.addView(item);
    }
}
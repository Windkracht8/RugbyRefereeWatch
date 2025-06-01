/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.windkracht8.rugbyrefereewatch;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MatchLog extends ScrollScreen{
    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        label.setText(R.string.match_log_title);
        try{
            JSONArray matches = FileStore.readMatches(this);
            for(int i = matches.length() - 1; i >= 0; i--){
                MatchData match = new MatchData(matches.getJSONObject(i));
                addItem(match);
            }
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "MatchLog.show Exception: " + e.getMessage());
            Toast.makeText(this, R.string.fail_show_log, Toast.LENGTH_SHORT).show();
        }
    }

    private void addItem(MatchData match){
        TextView item = new TextView(this, null, 0, R.style.textView_item);
        Date match_date_d = new Date(match.match_id);
        String text = new SimpleDateFormat("E dd MMM HH:mm", Locale.getDefault()).format(match_date_d);

        text += "\n";
        text += match.home.team.equals(match.home.id) ? match.home.color : match.home.team;
        text += " : ";
        text += match.away.team.equals(match.away.id) ? match.away.color : match.away.team;

        text += "\n";
        text += match.home.tot;
        text += " : ";
        text += match.away.tot;

        item.setText(text);
        item.setOnClickListener(v->{
            Report.match = match;
            startActivity(new Intent(this, Report.class));
        });
        list.addView(item);
    }
}
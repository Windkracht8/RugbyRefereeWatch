package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;

import androidx.appcompat.content.res.AppCompatResources;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MatchLogMatch extends androidx.appcompat.widget.AppCompatTextView {
    public MatchLogMatch(Context context){
        super(context);
    }
    public MatchLogMatch(Context context, MatchData match, Report report){
        super(context);
        Date match_date_d = new Date(match.match_id);
        String item = new SimpleDateFormat("E dd MMM HH:mm", Locale.getDefault()).format(match_date_d);

        item += "\n";
        item += match.home.team.equals(match.home.id) ? match.home.color : match.home.team;
        item += " : ";
        item += match.away.team.equals(match.away.id) ? match.away.color : match.away.team;

        item += "\n";
        item += match.home.tot;
        item += " : ";
        item += match.away.tot;

        this.setText(item);
        this.setGravity(Gravity.CENTER);
        this.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        this.setBackground(AppCompatResources.getDrawable(context, R.drawable.menu_item_bg));
        this.setOnClickListener(v -> report.show(match));
    }
}
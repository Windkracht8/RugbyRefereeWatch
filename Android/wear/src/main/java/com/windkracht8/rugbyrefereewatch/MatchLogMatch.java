package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MatchLogMatch extends TextView{
    public MatchLogMatch(Context context){
        super(context);
    }
    public MatchLogMatch(Main main, MatchData match, Report report){
        super(main);
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

        setText(item);
        setGravity(Gravity.CENTER);
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        setBackgroundResource(R.drawable.conf_item_bg);
        setOnClickListener(v -> report.show(main, match));
    }
}

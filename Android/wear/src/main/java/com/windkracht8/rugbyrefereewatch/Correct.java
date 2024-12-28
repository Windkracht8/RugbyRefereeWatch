package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class Correct extends ScrollView{
    private final LinearLayout llCorrectItems;

    public Correct(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        inflater.inflate(R.layout.correct, this, true);
        llCorrectItems = findViewById(R.id.llCorrectItems);
    }
    void show(Main main){
        for(int i = llCorrectItems.getChildCount(); i > 0; i--){
            llCorrectItems.removeViewAt(i-1);
        }
        for(int i = Main.match.events.size(); i > 0; i--){
            MatchData.event event_data = Main.match.events.get(i-1);
            if(!event_data.what.equals("TRY") &&
                    !event_data.what.equals("CONVERSION") &&
                    !event_data.what.equals("PENALTY TRY") &&
                    !event_data.what.equals("PENALTY") &&
                    !event_data.what.equals("GOAL") &&
                    !event_data.what.equals("YELLOW CARD") &&
                    !event_data.what.equals("RED CARD")
            ){
                continue;
            }
            addNewItem(main, event_data);
        }

        fullScroll(View.FOCUS_UP);
        setVisibility(View.VISIBLE);
        animate().x(0).scaleX(1f).scaleY(1f).setDuration(0).start();
        requestFocus();
    }
    void onCreateMain(Main main){
        if(Main.isScreenRound){
            main.si_addLayout(this, llCorrectItems);
            llCorrectItems.setPadding(Main._10dp, 0, Main._10dp, Main.vh25);
            TextView label = findViewById(R.id.correctLabel);
            label.getLayoutParams().height = Main.vh30;
            label.setPadding(Main.vh10, Main.vh10, Main.vh10, 0);
        }
    }

    private void addNewItem(Main main, MatchData.event event){
        TextView item = new TextView(main, null, 0, R.style.textView_item);
        String text = Main.prettyTimer(event.timer) + " " + Translator.getEventTypeLocal(main, event.what);
        if(event.team != null){
            text += " " + Translator.getTeamLocal(main, event.team);
            if(event.who > 0){
                text += " " + event.who;
            }
        }
        item.setText(text);

        item.setOnClickListener(v->{
            if(Main.draggingEnded+100 > Main.getCurrentTimestamp()) return;
            Main.match.removeEvent(event);
            setVisibility(View.GONE);
            performClick();
        });
        llCorrectItems.addView(item);
        main.addOnTouch(item);
    }
}

/*
 * Copyright 2020-2026 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class Correct extends ScrollView{
    private final LinearLayout llCorrectItems;

    private static float scalePerPixel;
    private static float bottom_quarter;
    private static float below_screen;

    public Correct(Context context, AttributeSet attrs){
        super(context, attrs);
        ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.correct, this, true);
        llCorrectItems = findViewById(R.id.llCorrectItems);
    }
    void show(Main main){
        llCorrectItems.removeAllViews();
        for(int i = Main.match.events.size(); i > 0; i--){
            MatchData.Event event_data = Main.match.events.get(i-1);
            if(event_data.what.equals("TIME OFF") ||
                    event_data.what.equals("RESUME") ||
                    event_data.what.equals("END")//Wouldn't it be nice (Beach boys tune)
            ){
                continue;
            }
            addItem(main, event_data);
        }

        if(!fullScroll(View.FOCUS_UP)){
            getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
                @Override public void onGlobalLayout(){//yes, this seems excessive, scaling only works after it is initially rendered
                    if(llCorrectItems.getChildCount() > 0 && llCorrectItems.getChildAt(0).getHeight() > 0){
                        getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        scaleItems(0);
                    }
                }
            });
        }
        setVisibility(View.VISIBLE);
        animate().x(0).scaleX(1f).scaleY(1f).setDuration(0).start();
        requestFocus();
    }
    void onCreateMain(){
        if(!Main.isScreenRound) return;
        int item_height = getResources().getDimensionPixelSize(R.dimen.item_height);
        bottom_quarter = Main.vh75 - item_height;
        below_screen = Main.heightPixels - item_height;
        scalePerPixel = 0.2f / Main.vh25;

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
            @Override public void onGlobalLayout(){
                if(llCorrectItems.getChildCount() > 0 && llCorrectItems.getChildAt(0).getHeight() > 0){
                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    scaleItems(0);
                    setOnScrollChangeListener((v, sx, sy, osx, osy)->scaleItems(sy));
                }
            }
        });

        llCorrectItems.setPadding(Main._10dp, 0, Main._10dp, Main.vh25);
        findViewById(R.id.correctLabel).setPadding(Main.vh10, Main.vh10, Main.vh10, 0);
    }

    private void addItem(Main main, MatchData.Event event){
        TextView item = new TextView(main, null, 0, R.style.textView_item_single);
        if(event.what.equals("START")){
            String text = Main.getPeriodName(main, event.period, Main.match.period_count) + " " +
                            event.time;
            item.setText(text);
            item.setTextColor(getContext().getColor(R.color.hint));
            llCorrectItems.addView(item);
            main.addOnTouch(item);
            return;
        }
        String text = Utils.prettyTimer(event.timer) + " " + Translator.getEventTypeLocal(main, event.what);
        if(event.team != null){
            if(event.what.equals("REPLACEMENT")){
                text = Utils.prettyTimer(event.timer) + " "
                        + Translator.getTeamLocal(main, event.team)
                        + Utils.replacementString(main, event);
            }else{
                text += " " + Translator.getTeamLocal(main, event.team);
                if(event.who > 0) text += " " + event.who;
            }
        }
        item.setText(text);

        item.setOnClickListener(v->{
            if(Main.draggingEnded+100 > System.currentTimeMillis()) return;
            if(event.deleted){
                Main.match.undeleteEvent(event);
            }else{
                Main.match.removeEvent(event);
                if(Main.match.events.indexOf(event) == Main.match.events.size()-1){
                    if(event.team.equals(MatchData.HOME_ID)){
                        main.kickClockHomeHide();
                    }else{
                        main.kickClockAwayHide();
                    }
                }
            }
            setVisibility(View.GONE);
            performClick();
        });
        if(event.deleted) item.setPaintFlags(item.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        llCorrectItems.addView(item);
        main.addOnTouch(item);
    }
    private void scaleItems(int scrollY){
        for(int i = 0; i < llCorrectItems.getChildCount(); i++){
            View item = llCorrectItems.getChildAt(i);
            float top = (llCorrectItems.getY() + item.getY()) - scrollY;
            float scale = 1.0f;
            if(top < 0){
                scale = 0.8f;
            }else if(top < Main.vh25){
                scale = 0.8f + (scalePerPixel * top);
            }else if(top > below_screen){
                scale = 0.8f;
            }else if(top > bottom_quarter){
                scale = 1.0f - (scalePerPixel * (top - bottom_quarter));
            }
            item.setScaleX(scale);
            item.setScaleY(scale);
        }
    }
}
package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.Log;

class Translator{
    static String getTeamColorLocal(Context context, String teamColor_system){
        String[] teamColors = context.getResources().getStringArray(R.array.teamColors);
        String[] teamColors_system = context.getResources().getStringArray(R.array.teamColors_system);
        for(int i=0; i<teamColors_system.length; i++){
            if(teamColors_system[i].equals(teamColor_system)){
                return teamColors[i];
            }
        }
        Log.e(Main.LOG_TAG, "translator.getTeamColorLocal not found: " + teamColor_system);
        return teamColors[0];
    }
    static String getEventTypeLocal(Context context, String eventType_system){
        String[] eventTypesLocal = context.getResources().getStringArray(R.array.eventTypes);
        String[] eventTypes_system = context.getResources().getStringArray(R.array.eventTypes_system);
        for(int i=0; i<eventTypes_system.length; i++){
            if(eventTypes_system[i].equals(eventType_system)){
                return eventTypesLocal[i];
            }
        }
        return "";
    }
    static String getTeamLocal(Context context, String team_system){
        if(team_system.equals(MatchData.HOME_ID)) return context.getString(R.string.home);
        if(team_system.equals(MatchData.AWAY_ID)) return context.getString(R.string.away);
        return team_system;
    }
}

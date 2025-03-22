package com.windkracht8.rugbyrefereewatch;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import java.util.List;

public class ConfActivity extends FragmentActivity{
    private GestureDetector gestureDetector;
    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        gestureDetector = new GestureDetector(this, simpleOnGestureListener, new Handler(Looper.getMainLooper()));
        setContentView(R.layout.fragment_container);
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.addOnBackStackChangedListener(()->{
            List<Fragment> fragments = fragmentManager.getFragments();
            if(fragments.get(fragments.size()-1) instanceof ConfScreen confScreen){
                //This is needed because ConfScreen.onResume is not called when pop-ing the stack
                confScreen.onResume();
            }
        });
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new Conf()).commit();
    }
    @Override public void onStop(){
        super.onStop();
        Main.runInBackground(()->FileStore.storeSettings(this));
    }
    void openConfCustomScreen(){
        getSupportFragmentManager().popBackStack();
        openConfScreen(new ConfCustom());
    }
    void openConfScreen(ConfScreen confScreen){
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, confScreen, confScreen.getClass().getSimpleName())
                .addToBackStack(confScreen.getClass().getSimpleName())
                .commit();
    }
    @Override public boolean onTouchEvent(MotionEvent event){return gestureDetector.onTouchEvent(event);}
    private final GestureDetector.SimpleOnGestureListener simpleOnGestureListener = new GestureDetector.SimpleOnGestureListener(){
        @Override public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY){
            if(Math.abs(velocityX) < Math.abs(velocityY)) return false;
            if(velocityX > 0) goBack();
            return true;
        }
    };
    @SuppressLint("ClickableViewAccessibility")
    void addOnTouch(View view){view.setOnTouchListener((v, e)->gestureDetector.onTouchEvent(e));}
    void goBack(){
        if(getSupportFragmentManager().getBackStackEntryCount() == 0){
            finish();
        }else{
            getSupportFragmentManager().popBackStack();
        }
    }
}

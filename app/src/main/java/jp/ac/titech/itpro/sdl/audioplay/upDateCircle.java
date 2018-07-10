package jp.ac.titech.itpro.sdl.audioplay;

import android.os.Handler;

public class upDateCircle implements Runnable {

    private boolean stopRun = false;
    private final Handler handler = new Handler();
    private final DrawCircle mCircle;

    private int period = 3;


    upDateCircle(DrawCircle c){
        mCircle = c;
    }

    protected void startRun() {
        mCircle.setDraw();
        Thread thread;
        stopRun = false;
        thread = new Thread(this);
        thread.start();
    }

    protected void stopRun() {
        stopRun = true;
    }

    public void run() {
        while (!stopRun) {
            // sleep: period msec
            try {
                Thread.sleep(period);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
                stopRun = true;
            }
            handler.post(new Runnable() {
                @Override
                public void run() {
                    stopRun = mCircle.updateAngle();
                }
            });
        }
    }
}

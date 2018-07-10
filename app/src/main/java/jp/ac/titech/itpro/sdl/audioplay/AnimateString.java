package jp.ac.titech.itpro.sdl.audioplay;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.content.Context;
import android.view.View;
import android.util.AttributeSet;
import android.os.Handler;


public class AnimateString extends View{

    private float sx = 0.1f;
    private float sy = 0.2f;

    public AnimateString(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Paint mPaint = new Paint();
        mPaint.setColor(Color.BLUE);
        mPaint.setTextSize(100f);
        canvas.skew(sx, sy);
        canvas.drawText("東", 50, 120, mPaint);
    }

    protected void updateSkew() {
        sx += 0.02f;
        if(sx > 0.9f) sx = 0.1f;
        sy += 0.01f;
        if(sy > 0.9f) sy = 0.1f;
        invalidate();
    }

    protected void startRun() {
        Thread thread;
        thread = new Thread(new transform(this));
        thread.start();
    }

    private class transform implements Runnable {

        private AnimateString AS;
        private boolean stopRun = false;
        private int period = 10;
        private final Handler handler = new Handler();

        public transform(AnimateString as){
            AS = as;
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
                        AS.updateSkew();
                    }
                });
            }
        }
    }

}

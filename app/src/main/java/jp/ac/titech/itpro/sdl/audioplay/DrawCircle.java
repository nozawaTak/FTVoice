package jp.ac.titech.itpro.sdl.audioplay;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.content.Context;
import android.view.View;
import android.util.AttributeSet;

public class DrawCircle extends View {

    private int Angle = 0;
    private boolean draw = true;

    DrawCircle(Context context, AttributeSet attrs){
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (draw) {
            Paint mPaint = new Paint();
            mPaint.setColor(Color.argb(255, 109, 247, 156));
            super.onDraw(canvas);
            RectF rectf = new RectF(20.0f, 20.0f, 220.0f, 220.0f);
            canvas.drawArc(rectf, 270, Angle, true, mPaint);
        }
    }

    protected boolean updateAngle() {
        Angle++;
        if(Angle >= 360) {
            draw = false;
            invalidate();
            return true;
        }
        invalidate();
        return false;
    }

    protected void setDraw() {
        draw = true;
        Angle = 0;
    }
}

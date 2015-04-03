package gov.adlnet.xapi.beaconfinder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

public class MapView extends View {
    MapActivity map_act;

    Drawable beacon1;
    Drawable beacon2;
    Drawable beacon3;
    Drawable dot;

    Rect windowRect = new Rect();

    Rect beaconBounds;
    Rect b1Loc;
    Rect b2Loc;
    Rect b3Loc;
    Rect dotLoc;

    int[] colors;

    public MapView(Context c) {
        super(c);
        map_act = (MapActivity)c;

        beacon1 = getResources().getDrawable(R.drawable.beacon);
        beacon2 = getResources().getDrawable(R.drawable.beacon);
        beacon3 = getResources().getDrawable(R.drawable.beacon);
        dot = getResources().getDrawable(R.drawable.red_pog);

        colors = new int[]{getResources().getColor(R.color.strong_signal),
                getResources().getColor(R.color.med_signal),
                getResources().getColor(R.color.weak_signal)};

        getWindowVisibleDisplayFrame(windowRect);

        beaconBounds = new Rect(0,0,scalew(.0167f), scaley(.0278f));
        b1Loc = getBounds(new Point(scalew(.016f), scaley(.85f)));
        b2Loc = getBounds(new Point(scalew(.94f), scaley(.7f)));
        b3Loc = getBounds(new Point(scalew(.83f), scaley(.28f)));
        dotLoc = getBounds(new Point((Math.abs(b1Loc.centerX() - b2Loc.centerX())/2) + b1Loc.centerX(),
                                     (Math.abs(b1Loc.centerY() - b3Loc.centerY())/2) + b3Loc.centerY()));
    }

    @Override
    protected void onDraw(Canvas canvas) {

        beacon1.setBounds(b1Loc);
        beacon2.setBounds(b2Loc);
        beacon3.setBounds(b3Loc);
        dot.setBounds(dotLoc);

        drawCircles(canvas);

        beacon1.draw(canvas);
        beacon2.draw(canvas);
        beacon3.draw(canvas);
        invalidate();
    }

    private void drawCircles(Canvas canvas) {
        for (MyBeaconInfo b : MainActivity.beaconArray) {
            Signal s = getCircleInfo(b.rssi);
            Paint paint = new Paint();
            paint.setColor(s.color);
            paint.setAlpha(s.alpha);
            paint.setStyle(Paint.Style.FILL);

            if ("RM6M-JW4ZE".equals(b.beacon.getIdentifier())){
                canvas.drawCircle(b1Loc.centerX(), b1Loc.centerY(), scalew(s.radius), paint);
            }
            else if ("Y5U2-CT5YG".equals(b.beacon.getIdentifier())){
                canvas.drawCircle(b2Loc.centerX(), b2Loc.centerY(), scalew(s.radius), paint);
            }
            else if ("U1F4-ERM4X".equals(b.beacon.getIdentifier())){
                canvas.drawCircle(b3Loc.centerX(), b3Loc.centerY(), scalew(s.radius), paint);
            }
        }
    }

    private Signal getCircleInfo(int rssi) {
        if (rssi > -75)
            return Signal.STRONG;
        else if (rssi > -90)
            return Signal.MEDIUM;
        else
            return Signal.WEAK;
    }

    private int scaley(float in) {
        return Math.round(in * windowRect.height());
    }

    private int scalew(float in) {
        return Math.round(in * windowRect.width());
    }

    private Rect getBounds(Point loc) {
        int wadj = loc.x - Math.round(beaconBounds.width()/2);
        int hadj = loc.y - Math.round(beaconBounds.height()/2);

        return new Rect(beaconBounds.left + wadj,
                beaconBounds.top + hadj,
                beaconBounds.right + wadj,
                beaconBounds.bottom + hadj);
    }

    private enum Signal {
        STRONG(0x007f00, .1f, 70),
        MEDIUM(0xff7f00, .25f, 50),
        WEAK(0x7f0000, .5f, 30);
        int color;
        float radius;
        int alpha;
        Signal(int c, float r, int a) {
            this.color = c;
            this.radius = r;
            this.alpha = a;
        }
    }
}

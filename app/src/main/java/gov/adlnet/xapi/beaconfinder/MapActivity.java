package gov.adlnet.xapi.beaconfinder;

import android.app.Activity;
import android.os.Bundle;
import android.widget.FrameLayout;

public class MapActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_layout);
        FrameLayout map_frame = (FrameLayout)findViewById(R.id.map_view_frame);
        map_frame.addView(new MapView(this));
    }
}

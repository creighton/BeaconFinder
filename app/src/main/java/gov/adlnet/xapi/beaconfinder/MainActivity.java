package gov.adlnet.xapi.beaconfinder;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.gimbal.android.Beacon;
import com.gimbal.android.BeaconEventListener;
import com.gimbal.android.BeaconManager;
import com.gimbal.android.BeaconSighting;
import com.gimbal.android.Communication;
import com.gimbal.android.CommunicationListener;
import com.gimbal.android.CommunicationManager;
import com.gimbal.android.Gimbal;
import com.gimbal.android.Place;
import com.gimbal.android.PlaceEventListener;
import com.gimbal.android.PlaceManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends ActionBarActivity {
    private BeaconEventListener beaconEventListener;
    private PlaceEventListener placeEventListener;
    private CommunicationListener communicationListener;
    private BeaconListAdapter beaconListAdapter;
    public static ArrayList<MyBeaconInfo> beaconArray = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Gimbal.setApiKey(this.getApplication(), getString(R.string.api_key));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = getSharedPreferences(getString(R.string.preferences_key), MODE_PRIVATE);
        String tmpName = prefs.getString(getString(R.string.preferences_name_key), null);
        String tmpEmail = prefs.getString(getString(R.string.preferences_email_key), null);

        if(tmpName == null || tmpEmail == null)
        {
            //launchSettings();
        }
        else
        {
//            _actor_email = tmpEmail;
//            _actor_name = tmpName;
        }

        beaconListAdapter = new BeaconListAdapter(this, beaconArray);

        ListView list = (ListView)findViewById(R.id.beacon_list);
        list.setAdapter(beaconListAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MyBeaconInfo b = beaconArray.get(position);
                if (b.communications != null && b.communications.size() ==1) {
                    Communication c = b.communications.toArray(new Communication[b.communications.size()])[0];
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(c.getURL()));
                    startActivity(i);
                }
            }
        });
        beaconEventListener = new BeaconEventListener() {
            @Override
            public void onBeaconSighting(BeaconSighting beaconSighting) {
                addBeacon(new MyBeaconInfo(beaconSighting.getBeacon(), beaconSighting.getRSSI(), beaconSighting.getTimeInMillis()));
            }
        };
        BeaconManager.getInstance().addListener(beaconEventListener);

        placeEventListener = new PlaceEventListener() {
            @Override
            public void onEntry(Place place, long timestamp) {

            }

            @Override
            public void onExit(Place place, long entryTimestamp, long exitTimestamp) {
                Beacon b = new Beacon();
                b.setIdentifier(place.getIdentifier());
                removeBeacon(new MyBeaconInfo(b, 0, exitTimestamp));
            }
        };
        PlaceManager.getInstance().addPlaceEventListener(placeEventListener);

        communicationListener = new CommunicationListener() {
            @Override
            public void communicationsOnPlaceEntry(Collection<Communication> communications, Place place, long l) {
                addMessages(communications, place);
            }
        };
        CommunicationManager.getInstance().addListener(communicationListener);

        PlaceManager.getInstance().startMonitoring();
        BeaconManager.getInstance().startMonitoring();
        runBeaconTimer();
    }

    @Override
    protected void onDestroy() {
        PlaceManager.getInstance().stopMonitoring();
        BeaconManager.getInstance().stopMonitoring();

        PlaceManager.getInstance().removePlaceEventListener(placeEventListener);
        BeaconManager.getInstance().removeListener(beaconEventListener);
        CommunicationManager.getInstance().addListener(communicationListener);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void showMap(View view) {
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);
    }

    private void addBeacon(MyBeaconInfo b) {
        if (! beaconArray.contains(b)) {
            beaconArray.add(b);
        }
        else {
            if(updateBeacon(b, beaconArray)) {
                beaconListAdapter.notifyDataSetChanged();
            }
        }
    }

    private boolean updateBeacon(MyBeaconInfo b, ArrayList<MyBeaconInfo> beacons) {
        for ( MyBeaconInfo i : beacons) {
            if (i.equals(b)) {
                i.update(b);
                return true;
            }
        }
        return false;
    }

    private void removeBeacon(MyBeaconInfo b) {
        beaconArray.remove(b);
    }

    private void addMessages(Collection<Communication> coms, Place place) {
        for (MyBeaconInfo b : beaconArray) {
            if (b.beacon.getName().equals(place.getName())) {
                b.communications = coms;
                return;
            }
        }
    }

    public void runBeaconTimer() {
        final Handler handler = new Handler();
        Timer timer = new Timer();
        TimerTask checkBeacons = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        for(MyBeaconInfo b : beaconArray.toArray(new MyBeaconInfo[beaconArray.size()])) {
                            long t = Calendar.getInstance().getTimeInMillis();
                            if (t - b.timems >= 30000) {
                                removeBeacon(b);
                            }
                        }
                    }
                });
            }
        };
        timer.schedule(checkBeacons, 30000, 30000); //execute in every 30000 ms
    }

    private class BeaconListAdapter extends ArrayAdapter<MyBeaconInfo> {
        private Activity activity;
        private ArrayList<MyBeaconInfo> mybeacons;
        public BeaconListAdapter(Activity activity, ArrayList<MyBeaconInfo> myBeaconInfos) {
            super(activity.getApplicationContext(), R.layout.beacon_list_item, myBeaconInfos);
            this.activity = activity;
            this.mybeacons = myBeaconInfos;
        }

        public View getView(int position, View view, ViewGroup parent) {
            ViewHolder v;
            if (view != null){
                v = (ViewHolder)view.getTag();
            }
            else {
                view = activity.getLayoutInflater().inflate(R.layout.beacon_list_item, parent, false);

                v = new ViewHolder();
                v.name_text = (TextView)view.findViewById(R.id.name_text);
                v.rssi = (TextView)view.findViewById(R.id.rssi);
                v.date_text = (TextView)view.findViewById(R.id.date_text);
                v.temp_view = (TextView)view.findViewById(R.id.temp_view);
                v.beacon_icon = (ImageView)view.findViewById(R.id.beacon_icon);
                v.messages_icon = (ImageView)view.findViewById(R.id.messages_icon);
                view.setTag(v);
            }

            MyBeaconInfo b = mybeacons.get(position);
            v.messages_icon.setImageResource(R.drawable.no_message_icon);

            v.beacon_icon.setImageResource(R.drawable.beacon);
            v.beacon_icon.setColorFilter(null);
            if (b.rssi > -75)
                v.beacon_icon.setColorFilter(getResources().getColor(R.color.strong_signal));
            else if (b.rssi > -90)
                v.beacon_icon.setColorFilter(getResources().getColor(R.color.med_signal));
            else
                v.beacon_icon.setColorFilter(getResources().getColor(R.color.weak_signal));

            // this is set up only to expect 1 message, which i have configured to be
            // a url to the glass app for setting up oculus rift
            if (b.communications != null && b.communications.size() ==1) {
                v.messages_icon.setImageResource(R.drawable.message_icon);
            }

            v.name_text.setText(b.beacon.getName());
            v.rssi.setText(String.valueOf(b.rssi));
            v.date_text.setText(String.format("%1$tb %1$td %1$tY %1$tr",new Date(mybeacons.get(position).timems)));
            v.temp_view.setText(String.valueOf(b.beacon.getTemperature()) + "\u00B0");
            return view;
        }
    }

    static class ViewHolder {
        TextView name_text;
        TextView rssi;
        TextView date_text;
        TextView temp_view;
        ImageView beacon_icon;
        ImageView messages_icon;
    }
}

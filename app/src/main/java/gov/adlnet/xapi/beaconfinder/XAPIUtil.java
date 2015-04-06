package gov.adlnet.xapi.beaconfinder;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.gimbal.android.Place;

import org.joda.time.Interval;
import org.joda.time.PeriodType;
import org.joda.time.format.ISOPeriodFormat;
import org.joda.time.format.PeriodFormatter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import gov.adlnet.xapi.client.StatementClient;
import gov.adlnet.xapi.model.Activity;
import gov.adlnet.xapi.model.ActivityDefinition;
import gov.adlnet.xapi.model.Actor;
import gov.adlnet.xapi.model.Agent;
import gov.adlnet.xapi.model.ContextActivities;
import gov.adlnet.xapi.model.IStatementObject;
import gov.adlnet.xapi.model.Result;
import gov.adlnet.xapi.model.Statement;
import gov.adlnet.xapi.model.Verb;


public class XAPIUtil {
    private static final String TAG = "XAPIUtil";

    private Verb ENTERED;
    private Verb EXITED;

    private StatementClient xapiclient;

    private Context ctx;

    public XAPIUtil(Context ctx) {
        this.ctx = ctx;

        ENTERED = new Verb();
        ENTERED.setId(ctx.getString(R.string.verb_iri_entered));
        HashMap<String, String> entereddisplay = new HashMap<>();
        entereddisplay.put("en-US", ctx.getString(R.string.verb_en_display_entered));
        ENTERED.setDisplay(entereddisplay);

        EXITED = new Verb();
        EXITED.setId(ctx.getString(R.string.verb_iri_exited));
        HashMap<String, String> exiteddisplay = new HashMap<>();
        exiteddisplay.put("en-US", ctx.getString(R.string.verb_en_display_exited));
        EXITED.setDisplay(exiteddisplay);

        try {
            xapiclient = new StatementClient(ctx.getString(R.string.lrs_endpoint),
                    ctx.getString(R.string.lrs_user), ctx.getString(R.string.lrs_password));
        }
        catch (MalformedURLException e) {
            Log.d(TAG, "MalformedURLException: " + e.getLocalizedMessage());
        }
    }

    public boolean sendStatements() {
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getString(R.string.preferences_key), Context.MODE_PRIVATE);
        return sp.getBoolean(ctx.getString(R.string.preferences_send_xapi_key), false) && xapiclient != null;
    }

    public void sendEntered(Place place, long timestamp) {
        Statement s = new Statement();
        s.setActor(getActor());
        s.setVerb(ENTERED);
        s.setObject(getActivity(place));
        s.setTimestamp(convertToISO8601(timestamp));
        s.setContext(getContext(place));
        new SendTask().execute(s);
    }

    public void sendExited(Place place, long entryTimestamp, long exitTimestamp) {
        Statement s = new Statement();
        s.setActor(getActor());
        s.setVerb(EXITED);
        s.setObject(getActivity(place));
        s.setTimestamp(convertToISO8601(exitTimestamp));
        s.setResult(getResultDuration(entryTimestamp, exitTimestamp));
        s.setContext(getContext(place));
        new SendTask().execute(s);
    }

    private gov.adlnet.xapi.model.Context getContext(Place place) {
        gov.adlnet.xapi.model.Context c = new gov.adlnet.xapi.model.Context();
        ContextActivities ctx_acts = new ContextActivities();
        ArrayList<Activity> a = new ArrayList<>();
        a.add(new Activity(ctx.getString(R.string.context_category)));
        ctx_acts.setCategory(a);
        c.setContextActivities(ctx_acts);
        return c;
    }

    private Result getResultDuration(long entryTimestamp, long exitTimestamp) {
        Result r = new Result();
        Interval iv = new Interval(entryTimestamp, exitTimestamp);
        PeriodFormatter fpt = ISOPeriodFormat.standard();
        r.setDuration(fpt.print(iv.toPeriod(PeriodType.time())));
        return r;
    }

    private IStatementObject getActivity(Place place) {
        ActivityDefinition def = new ActivityDefinition();
        def.setName(new HashMap<String, String>());
        def.getName().put("en-US", place.getName());

        return new Activity(ctx.getString(R.string.beacon_baseiri) + place.getIdentifier(), def);
    }

    private Actor getActor() {
        SharedPreferences sp = ctx.getSharedPreferences(ctx.getString(R.string.preferences_key), Context.MODE_PRIVATE);
        String user = sp.getString(ctx.getString(R.string.preferences_name_key), ctx.getString(R.string.anon_user_name));
        String email = sp.getString(ctx.getString(R.string.preferences_email_key), ctx.getString(R.string.anon_user_email));
        return new Agent(user, (email.startsWith("mailto:") ? email : "mailto:" + email));
    }

    private String convertToISO8601(long timestamp) {
        TimeZone tz = TimeZone.getDefault();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        df.setTimeZone(tz);
        return df.format(timestamp);
    }

    private class SendTask extends AsyncTask<Statement, Void, String> {

        @Override
        protected String doInBackground(Statement... params) {
            String response;

            if (! sendStatements()) {
                Log.d(TAG, "sending is off");
                return "sending is off";
            }

            try {
                response = xapiclient.publishStatement(params[0]);
            }
            catch (UnsupportedEncodingException e) {
                Log.d(TAG, "unsupported encoding: " + e.getLocalizedMessage());
                response = "unsupported encoding: " + e.getLocalizedMessage();
            }
            catch (IOException e) {
                Log.d(TAG, "io exception: " + e.getLocalizedMessage());
                response = "io exception: " + e.getLocalizedMessage();
            }
            return response;
        }

    }
}

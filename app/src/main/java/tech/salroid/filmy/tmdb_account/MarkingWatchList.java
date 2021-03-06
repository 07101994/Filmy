package tech.salroid.filmy.tmdb_account;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import tech.salroid.filmy.BuildConfig;
import tech.salroid.filmy.R;
import tech.salroid.filmy.customs.CustomToast;
import tech.salroid.filmy.network_stuff.TmdbVolleySingleton;

/*
 * Filmy Application for Android
 * Copyright (c) 2016 Ramankit Singh (http://github.com/webianks).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class MarkingWatchList {

    private String api_key = BuildConfig.API_KEY;
    private String SESSION_PREF = "SESSION_PREFERENCE";
    private TmdbVolleySingleton tmdbVolleySingleton = TmdbVolleySingleton.getInstance();
    private RequestQueue tmdbrequestQueue = tmdbVolleySingleton.getRequestQueue();
    private Context context;

    private int NOTIFICATION_ID;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;


    public void addToWatchList(Context context,String media_id){

        this.context = context;
        SharedPreferences sp = context.getSharedPreferences(SESSION_PREF,Context.MODE_PRIVATE);
        String session_id = sp.getString("session"," ");

        //issue notification to show indeterminate progress

        Random random = new Random();
        NOTIFICATION_ID = random.nextInt(10000);

        mNotifyManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(context);
        mBuilder.setContentTitle("Filmy")
                .setContentText("Adding to watchlist.")
                .setSmallIcon(R.drawable.ic_stat_status);
        mBuilder.setProgress(0, 0, true);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());

        getProfile(session_id,media_id);

    }


    private void getProfile(final String session_id, final String media_id) {

        String PROFILE_URI = "https://api.themoviedb.org/3/account?api_key="+api_key+"&session_id=" + session_id;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, PROFILE_URI, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        parseOutput(response,session_id,Integer.valueOf(media_id));
                    }

                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Log.e("webi", "Volley Error: " + error.getCause());

            }
        });

        tmdbrequestQueue.add(jsonObjectRequest);
    }

    private void parseOutput(JSONObject response,String session_id,int media_id) {

        try {

            String account_id = response.getString("id");
            if (account_id != null) {

                String query = "https://api.themoviedb.org/3/account/"+account_id+"/watchlist?api_key="+
                        api_key+"&session_id="+session_id;

                addWatchFinal(query,media_id);



            }else{
                CustomToast.show(context,"You are not logged in. Please login from account.",false);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            CustomToast.show(context,"You are not logged in. Please login from account.",false);
        }
    }

    private void addWatchFinal(String query,int media_id) {

        JSONObject jsonBody = new JSONObject();
        try {

            jsonBody.put("media_type", "movie");
            jsonBody.put("media_id", media_id);
            jsonBody.put("watchlist", true);

        } catch (JSONException e) {

            e.printStackTrace();
        }
        final String mRequestBody = jsonBody.toString();


        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, query, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        parseMarkedResponse(response);
                    }

                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Log.e("webi", "Volley Error: " + error.getCause());

            }
        }){

            @Override
            public byte[] getBody() {
                try {
                    return mRequestBody == null ? null : mRequestBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", mRequestBody, "utf-8");
                    return null;
                }
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("content-type", "application/json;charset=utf-8");
                return headers;
            }
        };

        tmdbrequestQueue.add(jsonObjectRequest);

    }


    private void parseMarkedResponse(JSONObject response) {

        try {
            int status_code = response.getInt("status_code");

            if (status_code == 1){
                CustomToast.show(context,"Movie added to the watchlist.",false);

                mBuilder.setContentText("Movie added to the watchlist.")
                        // Removes the progress bar
                        .setProgress(0,0,false);
                mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
                mNotifyManager.cancel(NOTIFICATION_ID);


            }else if(status_code == 12){

                CustomToast.show(context,"Watchlist updated.",false);

                mBuilder.setContentText("Watchlist updated.")
                        // Removes the progress bar
                        .setProgress(0,0,false);
                mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
                mNotifyManager.cancel(NOTIFICATION_ID);

            }


        } catch (JSONException e) {

            Log.d("webi",e.getCause().toString());
            CustomToast.show(context,"Can't add to watch list.",false);

            mBuilder.setContentText("Can't add to watch list.")
                    // Removes the progress bar
                    .setProgress(0,0,false);
            mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
            mNotifyManager.cancel(NOTIFICATION_ID);
        }


    }

}

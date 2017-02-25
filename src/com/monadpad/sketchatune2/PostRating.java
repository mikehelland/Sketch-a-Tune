package com.monadpad.sketchatune2;

import android.accounts.*;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PostRating {

    Context context;
    String url = "http://cloudmusiccompany.appspot.com";

    DefaultHttpClient httpClient = new DefaultHttpClient();

    float rating;
    long grooveId;


    public PostRating(Context context, float rating, long grooveId) {
        this.context = context;
        this.rating = rating;
        this.grooveId = grooveId;
        String userId = "";

        final AccountManager amgr = AccountManager.get(context);
        Account[] accounts = amgr.getAccountsByType("com.google");
        if (accounts.length > 0) {

            // ask the user if they want to login with google
            // if yes, login with google
            Account account = accounts[0];
            amgr.getAuthToken(account, "ah", false, new GetAuthTokenCallback(), null);


        }
        else {

        }

        if (userId.length() == 0) {

        }


        // else get a temp user account from cloudmusiccompany

        // post the rating
        new PostIt().execute(Float.toString(rating), Long.toString(grooveId), userId);

    }

    private class GetAuthTokenCallback implements AccountManagerCallback {
        public void run(AccountManagerFuture result) {
            Bundle bundle;
            try {
                bundle = (Bundle)result.getResult();
                Intent intent = (Intent)bundle.get(AccountManager.KEY_INTENT);
                if (intent != null) {
                    context.startActivity(intent);
                }
                else {
                    onGetAuthToken(bundle);
                }
            }
            catch (OperationCanceledException e) {
                e.printStackTrace();
            }
            catch (AuthenticatorException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    protected void onGetAuthToken(Bundle bundle) {
        String authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
        new GetCookieTask().execute(authToken);
    }

    private class GetCookieTask extends AsyncTask<String, Void, Boolean> {
        protected Boolean doInBackground(String... tokens) {

            try {
                httpClient.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);
                HttpGet httpGet = new HttpGet(url + "/_ah/login?continue=http://localhost/&auth=" + tokens[0]);

                HttpResponse response = httpClient.execute(httpGet);
                if (response.getStatusLine().getStatusCode() != 302) {
                    return false;
                }

                for (Cookie cookie : httpClient.getCookieStore().getCookies()) {
                    if (cookie.getName().equals("ACSID")) {
                        return true;
                    }
                }

            }
            catch (ClientProtocolException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                httpClient.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, true);
            }


            return false;
        }

        protected void onPostExecute(Boolean result) {
//            new AuthenticatedRequestTask().execute(url + "/admin");
            // post the rating
            Log.d("MGH", "onPostExecute of getCookieTask");
            if (result)
                new PostIt().execute(Float.toString(rating), Long.toString(grooveId), "");

        }
    }


    class PostIt extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {

            HttpClient httpclientup = new DefaultHttpClient();
            try {
                HttpPost hPost = new HttpPost(url + "/rating");
                List<NameValuePair> postParams = new ArrayList<NameValuePair>();
                postParams.add(new BasicNameValuePair("rating", params[0]));
                postParams.add(new BasicNameValuePair("id", params[1]));
                postParams.add(new BasicNameValuePair("userid", params[2]));
                hPost.setEntity(new UrlEncodedFormEntity(postParams));

                HttpResponse response = httpclientup.execute(hPost);
                StatusLine statusLine = response.getStatusLine();
                if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    out.close();
                    String responseString = out.toString();
                    // responseString,
                } else {

                    // "Something went wrong.",

                }

            } catch (ClientProtocolException ee) {
                // TODO Auto-generated catch block
                ee.printStackTrace();

            } catch (IOException ee) {
                // TODO Auto-generated catch block
                ee.printStackTrace();
            }

            return null;
        }
    }
}

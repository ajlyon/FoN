package com.gimranov.hon;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.BaseRequestListener;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.facebook.android.LoginButton;
import com.facebook.android.R;
import com.facebook.android.SessionEvents;
import com.facebook.android.SessionEvents.AuthListener;
import com.facebook.android.SessionEvents.LogoutListener;
import com.facebook.android.SessionStore;
import com.facebook.android.Util;

public class HoNActivity extends Activity {
	
	private static final String TAG = "com.gimranov.hon.HoNActivity";
	
	private ImageButton mImageButton1;
	private ImageButton mImageButton2;
	private LoginButton mLoginButton;
	private TextView mPrompt;
	private TextView mInstructions;
	
	private JSONObject person1;
	private JSONObject person2;
	
	private ArrayList<JSONObject> friends;
	private ArrayList<JSONObject> scores;
		
    private Facebook mFacebook;
    private AsyncFacebookRunner mAsyncRunner;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        friends = new ArrayList<JSONObject>();
        scores = new ArrayList<JSONObject>();
        
		mImageButton1 = (ImageButton) findViewById(R.id.imageButton1);
		mImageButton2 = (ImageButton) findViewById(R.id.imageButton2);
        mLoginButton = (LoginButton) findViewById(R.id.login);
        
        mPrompt = (TextView) HoNActivity.this.findViewById(R.id.prompt);
        mInstructions = (TextView) HoNActivity.this.findViewById(R.id.instructions);
        
       	mFacebook = new Facebook(Credentials.APP_ID);
       	mAsyncRunner = new AsyncFacebookRunner(mFacebook);

        SessionStore.restore(mFacebook, this);
        SessionEvents.addAuthListener(new HoNAuthListener());
        SessionEvents.addLogoutListener(new HoNLogoutListener());
        mLoginButton.init(this, mFacebook);
        
        // get friends and show pics
        if (mFacebook.isSessionValid()) {
        	mAsyncRunner.request("me/friends", new HoNRequestListener());
        }
        
        mImageButton1.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	Log.d(TAG, "Button one clicked");
            	scores.add(person1);
            	mInstructions.setText(person1.optString("name")+ " is more fun?");
            	mAsyncRunner.request("me", new HoNRequestListener());
            }
        });
        
        mImageButton2.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	Log.d(TAG, "Button two clicked");
            	scores.add(person2);
            	mInstructions.setText(person2.optString("name")+ " -- no contest.");
            	mAsyncRunner.request("me", new HoNRequestListener());
            }
        });

        mImageButton1.setVisibility(mFacebook.isSessionValid() ?
                View.VISIBLE :
                View.INVISIBLE);
        
        mImageButton2.setVisibility(mFacebook.isSessionValid() ?
                View.VISIBLE :
                View.INVISIBLE);
    }


    
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        mFacebook.authorizeCallback(requestCode, resultCode, data);
    }

    public class HoNAuthListener implements AuthListener {

        public void onAuthSucceed() {
        	mInstructions.setText("You have logged in! ");
        	mAsyncRunner.request("me/friends", new HoNRequestListener());
            mImageButton1.setVisibility(View.VISIBLE);
            mImageButton2.setVisibility(View.VISIBLE);
        }

        public void onAuthFail(String error) {
        	mInstructions.setText("Login Failed: " + error);
        }
    }

    public class HoNLogoutListener implements LogoutListener {
        public void onLogoutBegin() {
            mInstructions.setText("Logging out...");
        }

        public void onLogoutFinish() {
        	mInstructions.setText("You have logged out! ");
            mImageButton1.setVisibility(View.INVISIBLE);
            mImageButton1.setVisibility(View.INVISIBLE);
        }
    }

    public class HoNRequestListener extends BaseRequestListener {

        public void onComplete(final String response, final Object state) {
            try {
                /**
                 * Gets a set number of friends to start -- gets all, then picks that number
                 * at random, and fetches their images and names.
                 */
            	
                // process the response here: executed in background thread
                Log.d(TAG, "Response: " + response.toString());
                
                if (friends == null || friends.size() == 0) {
                	JSONObject json = Util.parseJson(response);
                    
                    JSONArray friendsJSON = json.getJSONArray("data");
                   
                    friends = new ArrayList<JSONObject>();
                    
                    for (int i = 0; i < friendsJSON.length(); i++) {
                    	friends.add(friendsJSON.getJSONObject(i));
                    }
                    
                    // Choose forty friends at random
                    Collections.shuffle(friends);
                    
                    HoNActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                        	mInstructions.setText("Checking up on your friends...");
                        }
                    });	
                }

                try {
                	InputStream is1 = fetchFriend(0);
                	InputStream is2 = fetchFriend(1);
                	                	
                	final Bitmap bm1 = BitmapFactory.decodeStream(is1);
                	final Bitmap bm2 = BitmapFactory.decodeStream(is2);
                	
                	person1 = friends.remove(0);
                	person2 = friends.remove(0);
                	
                	HoNActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                        	mInstructions.setText(scores.size() + " of 10 done already...");
                        	mImageButton1.setImageBitmap(bm1);
                        	mImageButton2.setImageBitmap(bm2);
                        }
                    });
                } catch (Exception e) {
                	Log.e(TAG, "Issues getting images", e);
                }
                
            } catch (JSONException e) {
                Log.w(TAG, "JSON Error in response");
            } catch (FacebookError e) {
                Log.w(TAG, "Facebook Error: " + e.getMessage());
            }
        }
    }
    
    public InputStream fetchFriend(int j) throws JSONException, IOException {
		URL url = new URL("https://graph.facebook.com/"+friends.get(j).optString("id")+"/picture?type=large");
        URLConnection ucon = url.openConnection();
        ucon.setRequestProperty("User-Agent","Mozilla/5.0 ( compatible ) ");


        InputStream is = ucon.getInputStream();
        return is;
    }
}
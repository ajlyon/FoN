package com.gimranov.hon;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Credentials {

    public static final String APP_ID = "216895011730323";
	
	public static boolean check(Context c) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		if (settings.getString("user_id", null) != null
				&& settings.getString("user_key", null) != null
				&& !settings.getString("user_id", null).equals("")
				&& !settings.getString("user_key", null).equals(""))
			return true;
		else return false;
	}
}

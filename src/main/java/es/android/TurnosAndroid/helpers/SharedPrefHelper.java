/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package es.android.TurnosAndroid.helpers;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * A class containing utility methods related to Calendar apps.
 * <p/>
 * This class is expected to move into the app framework eventually.
 */
public class SharedPrefHelper {
  private static final String  TAG   = SharedPrefHelper.class.getSimpleName();

  /**
   * A helper method for writing a String value to the preferences
   * asynchronously.
   *
   * @param key   The preference to write to
   * @param value The value to write
   */
  public static void setSharedPreference(SharedPreferences prefs, String key, String value) {
//            SharedPreferences prefs = getSharedPreferences(context);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString(key, value);
    editor.apply();
  }

  /**
   * A helper method for writing a boolean value to the preferences
   * asynchronously.
   *
   * @param key   The preference to write to
   * @param value The value to write
   */
  public static void setSharedPreference(SharedPreferences prefs, String key, boolean value) {
//            SharedPreferences prefs = getSharedPreferences(context, prefsName);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean(key, value);
    editor.apply();
  }

  /**
   * Return a properly configured SharedPreferences instance
   */
  public static SharedPreferences getSharedPreferences(Context context, String prefsName) {
    return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
  }
}

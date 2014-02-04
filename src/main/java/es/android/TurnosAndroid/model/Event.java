/*
 * Copyright (C) 2007 The Android Open Source Project
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

package es.android.TurnosAndroid.model;

import android.database.Cursor;
import android.graphics.Color;
import es.android.TurnosAndroid.database.DBConstants;
import es.android.TurnosAndroid.helpers.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;

// TODO: should Event be Parcelable so it can be passed via Intents?
public class Event {
  // The coordinates of the event rectangle drawn on the screen.
  public  float  left;
  public  float  right;
  public  float  top;
  public  float  bottom;
  private int    id;
  private String name;
  private String description;
  private int    startTime;      // Start and end time are in minutes since midnight
  private int    duration;
  private String location;
  private int    color;

  public Event() {
    id = 0;
    name = "";
    description = "";
    startTime = 0;
    duration = 0;
    location = null;
    color = Color.WHITE;
  }

  public Event(Event event) {
    id = event.getId();
    name = event.getName();
    description = event.getDescription();
    startTime = event.getStartTime();
    duration = event.getDuration();
    location = event.getLocation();
    color = event.getColor();
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public int getStartTime() {
    return startTime;
  }

  public void setStartTime(int startTime) {
    this.startTime = startTime;
  }

  public int getDuration() {
    return duration;
  }

  public void setDuration(int duration) {
    this.duration = duration;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public int getColor() {
    return color;
  }

  public void setColor(int color) {
    this.color = color;
  }
}

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

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

public class Event implements Parcelable {
  public static final Parcelable.Creator<Event> CREATOR = new Parcelable.Creator<Event>() {
    @Override
    public Event createFromParcel(Parcel in) {
      return new Event(in);
    }

    @Override
    public Event[] newArray(int size) {
      return new Event[size];
    }
  };

  private int    id;
  private String name;
  private String description;
  private int    startTime;
  private int    duration;
  private String location;
  private int    color;
  private long   creationTime;

  public Event() {
    id = 0;
    name = "";
    description = "";
    startTime = 0;
    duration = 0;
    location = null;
    color = Color.WHITE;
    creationTime = System.currentTimeMillis();
  }

  public Event(Event event) {
    id = event.getId();
    name = event.getName();
    description = event.getDescription();
    startTime = event.getStartTime();
    duration = event.getDuration();
    location = event.getLocation();
    color = event.getColor();
    creationTime = event.getCreationTime();
  }

  private Event(Parcel in) {
    id = in.readInt();
    name = in.readString();
    description = in.readString();
    startTime = in.readInt();
    duration = in.readInt();
    location = in.readString();
    color = in.readInt();
    creationTime = in.readLong();
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

  public long getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(long creationTime) {
    this.creationTime = creationTime;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(id);
    dest.writeString(name);
    dest.writeString(description);
    dest.writeInt(startTime);
    dest.writeInt(duration);
    dest.writeString(location);
    dest.writeInt(color);
    dest.writeLong(creationTime);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Event)) {
      return false;
    }

    Event event = (Event) o;

    return id == event.id;
  }

  @Override
  public int hashCode() {
    return id;
  }
}

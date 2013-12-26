package es.android.TurnosAndroid.requests;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import es.android.TurnosAndroid.database.CalendarProvider;
import es.android.TurnosAndroid.database.DBConstants;

public class ImportEntries extends AsyncTask<Void, Void, Void> {

  private Context context;

  public ImportEntries(Context context) {
    this.context = context;
  }

  @Override
  protected Void doInBackground(Void... arg0) {
    importEntries();
    return null;
  }

  private void importEntries() {
//		Long[] times = new Long[2];
//		if(add){
//			long time = System.currentTimeMillis();
    String[] eventHolder = {DBConstants.ID, DBConstants.NAME, DBConstants.DESCRIPTION, DBConstants.START, DBConstants.DURATION, DBConstants.LOCATION, DBConstants.COLOR};
    Cursor cursor = context.getContentResolver().query(CalendarProvider.CONTENT_URI, eventHolder, null, null, null);

//    if (cursor != null && cursor.moveToFirst()) {
//      do {
//        long eventID = cursor.getLong(0);
//        String calID = cursor.getString(1);
//        String dbEvent = cursor.getString(2);
//        String dblocation = cursor.getString(3);
//        String dbdesc = cursor.getString(4);
//		            times = checkTimes(eventID);
//        Cursor e = context.getContentResolver().query(CalendarProvider.CONTENT_URI, eventHolder, DBConstants.EVENT_ID + "=?",
//                                                      new String[] {String.valueOf(eventID)}, null);
//        if (e == null || !e.moveToFirst()) {
//          ContentValues values = new ContentValues();
//          Calendar cal = Calendar.getInstance();
//          TimeZone tz = TimeZone.getDefault();
//          cal.setTimeZone(tz);
//          long offset2 = tz.getRawOffset();
//          long offset3 = (tz.getOffset(cursor.getLong(5)) / 1000) % 60;
//          long offset = 3600000 * 4;
//          cal.setTimeInMillis(cursor.getLong(5));
//
//          long offset4 = TimeUnit.MILLISECONDS.toSeconds(tz.getOffset(cursor.getLong(5)));
//
//          int hours = cal.get(Calendar.HOUR_OF_DAY);
//          int min = cal.get(Calendar.MINUTE);
//          int startDay = Time.getJulianDay(cursor.getLong(5), offset4);
//          int endDay = Time.getJulianDay(cursor.getLong(6), TimeUnit.MILLISECONDS.toSeconds(tz.getOffset(cursor.getLong(6))));
//          int startMin = (cal.get(Calendar.HOUR_OF_DAY) * 60) + cal.get(Calendar.MINUTE);
//          cal = Calendar.getInstance();
//          cal.setTimeInMillis(cursor.getLong(6));
//          int endMin = (cal.get(Calendar.HOUR_OF_DAY) * 60) + cal.get(Calendar.MINUTE);
//          values.put(DBConstants.DESCRIPTION, dbdesc);
//          values.put(DBConstants.NAME, dbEvent);
//          values.put(DBConstants.LOCATION, dblocation);
//          values.put(DBConstants.START_DAY, startDay);
//          values.put(DBConstants.END_DAY, endDay);
//          values.put(DBConstants.START, startMin);
//          values.put(DBConstants.DURATION, endMin);
//          context.getContentResolver().insert(CalendarProvider.CONTENT_URI, values);
//        }
//        if (e != null) {
//          e.close();
//        }
//      } while (cursor.moveToNext());
//
//    }
    if (cursor != null) {
      cursor.close();
    }
//		}else{
//			String[] eventHolder = {Events._ID,Events.CALENDAR_ID,Events.NAME,Events.EVENT_LOCATION,Events.DESCRIPTION,Events.DTSTART,Events.DTEND};
//			Cursor cursor = getContentResolver().query(Events.CONTENT_URI,eventHolder,null,null,null);
//			int value = cursor.getCount();
//			int count = 0;
//			if(cursor != null && cursor.moveToFirst()){
//				do{
//					count++;
//                	long eventID = cursor.getLong(0);
//                	String calID = cursor.getString(1);
//                    String dbEvent = cursor.getString(2);
//                    String dblocation = cursor.getString(3);
//                    String dbdesc = cursor.getString(4);
//                    String reminders = getReminders(eventID);
//                    if(reminders == null){
//                    	reminders = "0";
//                    }
//                    times = checkTimes(eventID,reminders);
//                    
//                    if(times[0] != null && times[1] != null){
//                    	long alert = Long.parseLong(reminders) * (60*1000);
//        	            long trueStart = times[0] - alert;
//                    	Cursor e = getContentResolver().query(CalendarProvider.CONTENT_URI, new String[] {CalendarProvider.ID},
//        	            		CalendarProvider.EVENT_ID+"=?",new String[] {String.valueOf(eventID)},null);
//                    	if(e != null && e.moveToFirst()){
//                    		ContentValues values = new ContentValues();
//        	    			values.put(CalendarProvider.DESCRIPTION, dbdesc);
//        	    			values.put(CalendarProvider.END, times[1]);
//        	    			values.put(CalendarProvider.START, times[0]);
//        	    			values.put(CalendarProvider.NAME, dbEvent);
//        	    			values.put(CalendarProvider.EVENT_ID, eventID);
//        	    			values.put(CalendarProvider.LOCATION, dblocation);
//        	    			values.put(CalendarProvider.CALENDAR_ID, calID);
//        	    			values.put(CalendarProvider.REMINDERS, reminders);
//        	    			values.put(CalendarProvider.START_WITH_REMINDER, trueStart);
//        	    			getContentResolver().update(CalendarProvider.CONTENT_URI, values,
//        	    					CalendarProvider.EVENT_ID+"=?", new String[] {String.valueOf(eventID)});
//                    	}else{
//                    		ContentValues values = new ContentValues();
//                    		values.put(CalendarProvider.BLINK, "Default");
//        	    			values.put(CalendarProvider.COLOR, "Default");
//        	    			values.put(CalendarProvider.DESCRIPTION, dbdesc);
//        	    			values.put(CalendarProvider.END, times[1]);
//        	    			values.put(CalendarProvider.START, times[0]);
//        	    			values.put(CalendarProvider.NAME, dbEvent);
//        	    			values.put(CalendarProvider.EVENT_ID, eventID);
//        	    			values.put(CalendarProvider.ICON, "Default");
//        	    			values.put(CalendarProvider.VIBRATE, "Normal");
//        	    			values.put(CalendarProvider.LOCATION, dblocation);
//        	    			values.put(CalendarProvider.CALENDAR_ID, calID);
//        	    			values.put(CalendarProvider.REMINDERS, reminders);
//        	    			values.put(CalendarProvider.START_WITH_REMINDER, trueStart);
//        	            	getContentResolver().insert(CalendarProvider.CONTENT_URI, values);
//                    	}
//                    	e.close();
//                    }
//				}while(cursor.moveToNext());
//			}
//			cursor.close();
//		}

  }

}

package org.opennms.android.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import org.opennms.android.R;
import org.opennms.android.communication.alarms.AlarmsServerCommunication;
import org.opennms.android.communication.alarms.AlarmsServerCommunicationImpl;
import org.opennms.android.dao.Columns.AlarmColumns;
import org.opennms.android.dao.alarms.Alarm;
import org.opennms.android.dao.alarms.AlarmsListProvider;
import org.opennms.android.ui.MainActivity;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class AlarmsSyncService extends IntentService {

    private static final String TAG = "AlarmsSyncService";
    private static final int ALARM_NOTIFICATION_ID = 1;

    public AlarmsSyncService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ContentResolver contentResolver = getContentResolver();
        AlarmsServerCommunication alarmsServer = new AlarmsServerCommunicationImpl(getApplicationContext());
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int latestShownAlarmId = sharedPref.getInt("latest_shown_alarm_id", 0);
        int newAlarmsCount = 0, maxId = 0;
        Log.d(TAG, "Synchronizing alarms...");
        try {
            List<Alarm> alarms = alarmsServer.getAlarms("alarms");
            for (Alarm alarm : alarms) {
                insertAlarm(contentResolver, alarm);
                if (alarm.getId() > latestShownAlarmId) newAlarmsCount++;
                if (alarm.getId() > maxId) maxId = alarm.getId();
            }
        } catch (UnknownHostException e) {
            Log.i(TAG, e.getMessage());
            contentResolver.delete(AlarmsListProvider.CONTENT_URI, null, null);
        } catch (InterruptedException e) {
            Log.i(TAG, e.getMessage());
            contentResolver.delete(AlarmsListProvider.CONTENT_URI, null, null);
        } catch (ExecutionException e) {
            Log.i(TAG, e.getMessage());
            contentResolver.delete(AlarmsListProvider.CONTENT_URI, null, null);
        } catch (IOException e) {
            Log.i(TAG, e.getMessage());
            contentResolver.delete(AlarmsListProvider.CONTENT_URI, null, null);
        }
        Log.d(TAG, "Done!");

        if (latestShownAlarmId != maxId) sharedPref.edit().putInt("latest_shown_alarm_id", maxId).commit();
        if (newAlarmsCount > 0) issueNewAlarmsNotification(newAlarmsCount);
    }

    private Uri insertAlarm(ContentResolver contentResolver, Alarm alarm) {
        ContentValues values = new ContentValues();
        values.put(AlarmColumns.COL_ALARM_ID, alarm.getId());
        values.put(AlarmColumns.COL_SEVERITY, alarm.getSeverity());
        values.put(AlarmColumns.COL_DESCRIPTION, alarm.getDescription());
        values.put(AlarmColumns.COL_LOG_MESSAGE, alarm.getLogMessage());
        return contentResolver.insert(AlarmsListProvider.CONTENT_URI, values);
    }

    private void issueNewAlarmsNotification(int newAlarmsCount) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Constructs the Builder object.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle(getString(R.string.alarms_notification_title))
                .setDefaults(Notification.DEFAULT_ALL); // requires VIBRATE permission

        if (newAlarmsCount == 1) builder.setContentText(getString(R.string.alarms_notification_text_singular));
        else builder.setContentText(String.format(getString(R.string.alarms_notification_text_plural), newAlarmsCount));

        // Clicking the notification itself displays MainActivity.
        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        /*
         * Because clicking the notification opens a new ("special") activity,
         * there's no need to create an artificial back stack.
         */
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(resultPendingIntent);

        notificationManager.notify(ALARM_NOTIFICATION_ID, builder.build());
    }

}
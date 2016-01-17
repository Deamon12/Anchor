package com.sos.saveourstudents.supportclasses;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.sos.saveourstudents.MemberWantsToJoinActivity;
import com.sos.saveourstudents.R;

/**
 * Initiates broadcast of notifications from Google Cloud Messenger pushes
 */
public class GcmIntentService extends IntentService {
    private final String ADD_MEMBER = "1", ADD_TUTOR = "2", ACCEPT_USER = "3", DELETE_USER = "4";
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    private String message = "";

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);

        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle

            if(extras.getString("type").equalsIgnoreCase(ADD_MEMBER)){
                message = "You have a request from someone to join your group!";
            }
            else if(extras.getString("type").equalsIgnoreCase(ADD_TUTOR)){
                message = "You have a request from a tutor to join your group!";
            }
            else if(extras.getString("type").equalsIgnoreCase(ACCEPT_USER)){
                message = "You have been accepted to join a group!";
            }
            else if(extras.getString("type").equalsIgnoreCase(DELETE_USER)){
                message = "You have been removed from your group";
            }
            else{
                message = "Have fun using SaveOurStudents app!";
            }


            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                sendNotification("Send error: " + extras.toString(), extras);
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                sendNotification("Deleted messages on server: " + extras.toString(), extras);
                // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                sendNotification(message, extras);
            }else
                sendNotification(message, extras);
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    private void sendNotification(String msg, Bundle extras) {


        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent newIntent = new Intent(this, MemberWantsToJoinActivity.class);

        PendingIntent contentIntent = null;

        newIntent.putExtra("type", extras.getString("type"));
        newIntent.putExtra("userId", extras.getString("userId"));
        newIntent.putExtra("questionId", extras.getString("questionId"));
        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if(extras.getString("type").equalsIgnoreCase(ADD_TUTOR) || extras.getString("type").equalsIgnoreCase(ADD_MEMBER)) {
            contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, newIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        }
        else{
            Intent intent = new Intent();
            intent.setAction("com.sos.saveourstudents.CUSTOM_INTENT");
            sendBroadcast(intent);
            contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), 0);
        }

        
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.sos_icon)
                        .setAutoCancel(true)
                        .setContentTitle("SaveOurStudents")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setTicker(message)
                                .addExtras(extras)
                        .setContentText(msg);

        mBuilder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE);
        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }


}
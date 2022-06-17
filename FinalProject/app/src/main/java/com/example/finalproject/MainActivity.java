package com.example.finalproject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {

    long prevTime = 0;
    long nextTime = 0;
    String idNum;
    int validTime = 0;
    int timeLeft = 0;

    Handler handler = new Handler();
    Runnable refresh = new Runnable() {
        @Override
        public void run() {
            autoRefresh();
            handler.postDelayed(this, 3600000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        createNotificationChannel();

        Button updateBtn = findViewById(R.id.updateBtn);
        Button healthCode = findViewById(R.id.healthCode);
        Button nearPos = findViewById(R.id.nearPos);
        Button setting = findViewById(R.id.setting);

        updateBtn.setOnClickListener(view -> autoRefresh());

        healthCode.setOnClickListener(view -> {
            Uri uri = Uri.parse("https://h5.dingtalk.com/healthAct/index.html");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        });

        nearPos.setOnClickListener(view -> {
            Uri uri = Uri.parse("https://info.autonavi.com/activity/2020CommonLanding/index.html?id=default&local=1&schema=amapuri%3A%2F%2Fajx_template_map%2Findex%3FmapType%3Dnucleic&gd_from=ajx_share&logId=&auto=0");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        });

        setting.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, com.example.finalproject.setting.class);
            startActivity(intent);
        });

        refresh.run();
    }

    public void autoRefresh() {
        String datas;
        if(!(datas = readFile()).equals("")) {
            String[] args = datas.split("\n");
            idNum = args[1];
            validTime = Integer.parseInt(args[2]);
            String urlStr = "https://passcode.zju.edu.cn/pass_code/hs?cardNo=" + idNum;
            try {
                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(3000);
                connection.setReadTimeout(3000);
                if(connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
                    byte[] bytes = new byte[1024];
                    int length;
                    while ((length = inputStream.read(bytes)) != -1) {
                        arrayOutputStream.write(bytes, 0, length);
                        arrayOutputStream.flush();
                    }
                    String responseStr = arrayOutputStream.toString();
                    Log.d("response", responseStr);
                    JSONObject jObject = new JSONObject(responseStr);
                    String lastAcidTime = jObject.getString("checktime");
                    @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
                    prevTime = Objects.requireNonNull(sdf.parse(lastAcidTime, new ParsePosition(0))).getTime();
                    nextTime = prevTime + (long) validTime * 3600 * 1000;
                    refreshDisp();
                    NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(MainActivity.this);
                    if(timeLeft <= 24) {
                        NotificationCompat.Builder builder;
                        if(timeLeft >= 0) {
                            builder = new NotificationCompat.Builder(MainActivity.this, "NUCLEAR_ACID")
                                    .setSmallIcon(R.mipmap.icon)
                                    .setContentTitle("核酸提醒🔈")
                                    .setContentText("上次核酸还有" + timeLeft + "小时过期")
                                    .setPriority(NotificationCompat.PRIORITY_MIN);
                        } else {
                            builder = new NotificationCompat.Builder(MainActivity.this, "NUCLEAR_ACID")
                                    .setSmallIcon(R.mipmap.icon)
                                    .setContentTitle("核酸提醒🔈")
                                    .setContentText("核酸已经过期了耶……")
                                    .setPriority(NotificationCompat.PRIORITY_MIN);
                        }
                        Notification notification = builder.build();
                        notification.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
                        notificationManagerCompat.notify(0, notification);
                    } else {
                        notificationManagerCompat.cancel(0);
                    }
                }
            } catch(Exception e) {
                Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void refreshDisp() {
        CalendarView calendar = findViewById(R.id.calendarView);
        TextView prevDateDisp = findViewById(R.id.prevDate);
        TextView nextDateDisp = findViewById(R.id.nextDate);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String prevStr = sdf.format(prevTime);
        String nextStr = sdf.format(nextTime);
        prevDateDisp.setText(prevStr);
        nextDateDisp.setText(nextStr);
        calendar.setDate(nextTime);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        RemoteViews remoteViews = new RemoteViews(this.getPackageName(), R.layout.desktop_widget);
        timeLeft = (int)((nextTime-Calendar.getInstance().getTimeInMillis())/3600000);
        if(timeLeft < 0) {
            remoteViews.setTextViewText(R.id.daysLeft, "0");
        } else {
            remoteViews.setTextViewText(R.id.daysLeft, ""+timeLeft);
        }
        appWidgetManager.updateAppWidget(appWidgetManager.getAppWidgetIds(new ComponentName(this.getPackageName(), DesktopWidget.class.getName())), remoteViews);
        Toast.makeText(this, "已更新", Toast.LENGTH_SHORT).show();
    }
    private void createNotificationChannel() {
        CharSequence name = "NuclearAcidNotification";
        String description = "notification";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel("NUCLEAR_ACID", name, importance);
        channel.setDescription(description);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    public String readFile() {
        Context context = MainActivity.this;
        String ret = "";
        try {
            InputStream is = context.openFileInput("RenyiNuclearAcid.txt");
            if(is != null) {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String recvStr;
                StringBuilder strBuilder = new StringBuilder();
                while ((recvStr = br.readLine()) != null) {
                    strBuilder.append("\n").append(recvStr);
                }
                is.close();
                ret = strBuilder.toString();
            }
        } catch (Exception e) {
            Log.e("ReadError", e.toString());
        }
        return ret;
    }
}
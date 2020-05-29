package com.clevertap.templateapp;

import android.app.NotificationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.pushtemplates.TemplateRenderer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    Button sendBasicPush, sendCarouselPush, sendRatingPush, sendProductDisplayNotification, sendCTANotification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final CleverTapAPI cleverTapAPI = CleverTapAPI.getDefaultInstance(getApplicationContext());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            CleverTapAPI.createNotificationChannel(this, "Test", "Push Template App Channel", "Channel for Push Template App", NotificationManager.IMPORTANCE_HIGH, true);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            CleverTapAPI.createNotificationChannel(this, "PTTesting", "Push Template App Channel", "Channel for Push Template App", NotificationManager.IMPORTANCE_HIGH, true);
        }
        HashMap<String, Object> profileUpdate = new HashMap<String, Object>();
        profileUpdate.put("Email", "test1@clevertap.com");
        if (cleverTapAPI != null) {
            cleverTapAPI.onUserLogin(profileUpdate);
        }

        sendBasicPush = findViewById(R.id.basicPush);
        sendBasicPush.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*if (cleverTapAPI != null) {
                    cleverTapAPI.pushEvent("Send Basic Push");
                }*/
                Bundle extras = new Bundle();

                try {
                    JSONObject payload = new JSONObject("{\"wzrk_cid\":\"PTTesting\",\n" +
                            "\"pt_id\":\"pt_basic\",\n" +
                            "\"pt_msg\":\"Hurry! Purchase {items[0].name | items } till stocks last!\",\n" +
                            "\"pt_msg_summary\":\"Hurry! Purchase {items[0].name | items } till stocks last .\",\n" +
                            "\"pt_title\":\"You have {items.length} items waiting in your cart.\",\n" +
                            "\"pt_big_img\":\"{items[0].imgUrl}\",\n" +
                            "\"pt_msg_clr\":\"#91B75F\",\n" +
                            "\"pt_title_clr\":\"#CD5748\",\n" +
                            "\"pt_ico\":\"https://i.pinimg.com/originals/49/3e/de/493ede620ab04894295105635d73f77d.png\",\n" +
                            "\"pt_api_endpoint\":\"https://satyajeetjadhav.github.io/ct-audit/cart.json\",\n" +
                            "\"pt_api_method\":\"GET\",\n" +
                            "\"pt_api_param_user\":\"satya\",\n" +
                            "\"pt_api_headers\": \"{\n" +
                            "            'X-CleverTap-Account-Id': actId,\n" +
                            "            'X-CleverTap-Passcode': passcode,\n" +
                            "            'Content-Type': 'application/json; charset=utf-8'\n" +
                            "        }\",\n" +
                            "\"pt_dl1\":\"https://google.com\"}");

                            /*"\"pt_big_img\":\"https://www.bmmagazine.co.uk/wp-content/uploads/2020/03/marvel-avengers-not-ended-disney-ceo-teases-mcu-fans.jpg\",\n" +*/
                    JSONArray keys = payload.names();
                    for (int i = 0; i < keys.length(); ++i) {
                        String key = keys.getString(i);
                        String value = payload.getString(key);
                        extras.putString(key, value);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                TemplateRenderer.createNotification(getApplicationContext(), extras);
            }
        });

        sendCarouselPush = findViewById(R.id.carouselPush);
        sendCarouselPush.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cleverTapAPI != null) {
                    cleverTapAPI.pushEvent("Send Carousel Push");
                }
            }
        });

        sendRatingPush = findViewById(R.id.ratingPush);
        sendRatingPush.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cleverTapAPI != null) {
                    cleverTapAPI.pushEvent("Send Rating Push");
                }
            }
        });

        sendProductDisplayNotification = findViewById(R.id.productDisplay);
        sendProductDisplayNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cleverTapAPI != null) {
                    cleverTapAPI.pushEvent("Send Product Display Notification");
                }
            }
        });

        sendCTANotification = findViewById(R.id.cta);
        sendCTANotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cleverTapAPI != null) {
                    cleverTapAPI.pushEvent("Send CTA Notification");
                }
            }
        });
    }
}
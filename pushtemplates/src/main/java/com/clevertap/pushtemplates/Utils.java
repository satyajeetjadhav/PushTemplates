package com.clevertap.pushtemplates;

import android.app.Notification;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.RemoteViews;


import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.NotificationTarget;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("WeakerAccess")
public class Utils {

    public static boolean isPNFromCleverTap(Bundle extras) {
        if (extras == null) return false;

        boolean fromCleverTap = extras.containsKey(Constants.NOTIF_TAG);
        boolean shouldRender = fromCleverTap && extras.containsKey("nm");
        return fromCleverTap && shouldRender;
    }


    static Bitmap getNotificationBitmap(String icoPath, boolean fallbackToAppIcon, final Context context)
            throws NullPointerException {
        // If the icon path is not specified
        if (icoPath == null || icoPath.equals("")) {
            return fallbackToAppIcon ? getAppIcon(context) : null;
        }
        // Simply stream the bitmap
        if (!icoPath.startsWith("http")) {
            icoPath = Constants.ICON_BASE_URL + "/" + icoPath;
        }
        Bitmap ic = getBitmapFromURL(icoPath);
        //noinspection ConstantConditions
        return (ic != null) ? ic : ((fallbackToAppIcon) ? getAppIcon(context) : null);
    }

    private static Bitmap getAppIcon(final Context context) throws NullPointerException {
        // Try to get the app logo first
        try {
            Drawable logo = context.getPackageManager().getApplicationLogo(context.getApplicationInfo());
            if (logo == null)
                throw new Exception("Logo is null");
            return drawableToBitmap(logo);
        } catch (Exception e) {
            // Try to get the app icon now
            // No error handling here - handle upstream
            return drawableToBitmap(context.getPackageManager().getApplicationIcon(context.getApplicationInfo()));
        }
    }

    private static Bitmap drawableToBitmap(Drawable drawable)
            throws NullPointerException {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    private static Bitmap getBitmapFromURL(String srcUrl) {
        // Safe bet, won't have more than three /s
        srcUrl = srcUrl.replace("///", "/");
        srcUrl = srcUrl.replace("//", "/");
        srcUrl = srcUrl.replace("http:/", "http://");
        srcUrl = srcUrl.replace("https:/", "https://");
        HttpURLConnection connection = null;
        try {
            URL url = new URL(srcUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (IOException e) {

            PTLog.verbose("Couldn't download the notification icon. URL was: " + srcUrl);
            return null;
        } finally {
            try {
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Throwable t) {
                PTLog.verbose("Couldn't close connection!", t);
            }
        }
    }

    static JSONObject getLinkedContent(String srcUrl, String method, Bundle headers) {
        // Safe bet, won't have more than three /s
        srcUrl = srcUrl.replace("///", "/");
        srcUrl = srcUrl.replace("//", "/");
        srcUrl = srcUrl.replace("http:/", "http://");
        srcUrl = srcUrl.replace("https:/", "https://");
        HttpURLConnection connection = null;
        try {
            /*// add parameters to the query
            StringBuilder queries = new StringBuilder();
            for(HashMap.Entry<String, String> query: urlParams.entrySet()) {
                queries.append( "&" + query.getKey()+"="+query.getValue());
            }
            srcUrl += queries.toString();
*/
            URL url = new URL(srcUrl);

            connection = (HttpURLConnection) url.openConnection();

            //set request method POST, GET, etc.
            connection.setRequestMethod(method);

            // set headers
            for (String key : headers.keySet()) {
                connection.setRequestProperty(key, headers.getString(key));
            }

            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(input));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = in.readLine()) != null) {
                response.append(line);
            }

            return new JSONObject(response.toString());


        } catch (IOException e) {

            PTLog.verbose("Couldn't get dynamic data " + srcUrl);
            return null;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Throwable t) {
                PTLog.verbose("Couldn't close connection!", t);
            }
        }
    }

    static String replaceLiquidTags(String pt_string, JSONObject liquidContent) {
        try {
            Pattern p = Pattern.compile("\\{(.*?)\\}");
            Matcher m = p.matcher(pt_string);

            while (m.find()) {
                String match = m.group(1).trim();
                String[] matchArr = match.split("\\|");
                String liquidKey = matchArr[0].trim();
                String defaultVal = "";
                if (matchArr.length > 1) {
                    defaultVal = matchArr[1].trim();
                }
                int sepPos = liquidKey.lastIndexOf(".");
                if (sepPos > -1) {
                    if (liquidKey.substring(sepPos + 1).equals("length")) {
                        try {
                            Integer size = ((JSONArray) liquidContent.get(liquidKey.substring(0, sepPos))).length();
                            pt_string = pt_string.replace("{" + m.group(1) + "}", String.valueOf(size));
                        } catch (Exception e) {
                            e.printStackTrace();
                            pt_string = pt_string.replace("{" + m.group(1) + "}", defaultVal);
                        }
                    } else {
                        try {
                            if (liquidKey.contains(".")) {
                                String[] liquidKeyArr = liquidKey.split("\\.");
                                if (liquidKeyArr[0].contains("[")) {
                                    Pattern pattern = Pattern.compile("\\[(.*?)\\]");
                                    Matcher matcher = pattern.matcher(liquidKeyArr[0]);
                                    Integer index = 0;
                                    while (matcher.find()) {
                                        index = Integer.valueOf(matcher.group(1));
                                    }

                                    JSONObject item = (JSONObject) ((JSONArray) liquidContent.get(liquidKeyArr[0].substring(0,liquidKeyArr[0].indexOf("[")))).get(index);
                                    String result = item.getString(liquidKeyArr[1]).trim();
                                    pt_string = pt_string.replace("{" + m.group(1) + "}", result);
                                } else {
                                    JSONObject item = (JSONObject) liquidContent.get(liquidKeyArr[0]);
                                    String result = item.getString(liquidKeyArr[1]).trim();
                                    pt_string = pt_string.replace("{" + m.group(1) + "}", result);
                                }
                            } else {
                                pt_string = pt_string.replace("{" + m.group(1) + "}", String.valueOf(liquidContent.get(liquidKey)));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            pt_string = pt_string.replace("{" + m.group(1) + "}", defaultVal);
                        }
                    }
                } else {
                    try {
                        pt_string = pt_string.replace("{" + m.group(1) + "}", String.valueOf(liquidContent.get(liquidKey)));
                    } catch (Exception e) {
                        e.printStackTrace();
                        pt_string = pt_string.replace("{" + m.group(1) + "}", defaultVal);
                    }
                }
            }
            return pt_string;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static String _getManifestStringValueForKey(Bundle manifest, String name) {
        try {
            Object o = manifest.get(name);
            return (o != null) ? o.toString() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    static int getAppIconAsIntId(final Context context) {
        ApplicationInfo ai = context.getApplicationInfo();
        return ai.icon;
    }

    static ArrayList<String> getImageListFromExtras(Bundle extras) {
        ArrayList<String> imageList = new ArrayList<>();
        for (String key : extras.keySet()) {
            if (key.contains("pt_img")) {
                imageList.add(extras.getString(key));
            }
        }
        return imageList;
    }

    static ArrayList<String> getCTAListFromExtras(Bundle extras) {
        ArrayList<String> ctaList = new ArrayList<>();
        for (String key : extras.keySet()) {
            if (key.contains("pt_cta")) {
                ctaList.add(extras.getString(key));
            }
        }
        return ctaList;
    }

    static ArrayList<String> getDeepLinkListFromExtras(Bundle extras) {
        ArrayList<String> dlList = new ArrayList<>();
        for (String key : extras.keySet()) {
            if (key.contains("pt_dl")) {
                dlList.add(extras.getString(key));
            }
        }
        return dlList;
    }

    static ArrayList<String> getBigTextFromExtras(Bundle extras) {
        ArrayList<String> btList = new ArrayList<>();
        for (String key : extras.keySet()) {
            if (key.contains("pt_bt")) {
                btList.add(extras.getString(key));
            }
        }
        return btList;
    }

    static ArrayList<String> getSmallTextFromExtras(Bundle extras) {
        ArrayList<String> stList = new ArrayList<>();
        for (String key : extras.keySet()) {
            if (key.contains("pt_st")) {
                stList.add(extras.getString(key));
            }
        }
        return stList;
    }

    static ArrayList<String> getPriceFromExtras(Bundle extras) {
        ArrayList<String> stList = new ArrayList<>();
        for (String key : extras.keySet()) {
            if (key.contains("pt_price")) {
                stList.add(extras.getString(key));
            }
        }
        return stList;
    }

    static void loadIntoGlide(Context context, int imageResource, String imageURL, RemoteViews remoteViews, Notification notification, int notificationId) {
        NotificationTarget bigNotifTarget = new NotificationTarget(
                context,
                imageResource,
                remoteViews,
                notification,
                notificationId);
        Glide
                .with(context.getApplicationContext())
                .asBitmap()
                .load(imageURL)
                .centerCrop()
                .into(bigNotifTarget);
    }

    static void loadIntoGlide(Context context, int imageResource, int identifier, RemoteViews remoteViews, Notification notification, int notificationId) {
        NotificationTarget bigNotifTarget = new NotificationTarget(
                context,
                imageResource,
                remoteViews,
                notification,
                notificationId);
        Glide
                .with(context.getApplicationContext())
                .asBitmap()
                .load(identifier)
                .centerCrop()
                .into(bigNotifTarget);
    }

    static String getTimeStamp(Context context) {
        return DateUtils.formatDateTime(context, System.currentTimeMillis(), DateUtils.FORMAT_SHOW_TIME);
    }

    static String getApplicationName(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        int stringId = applicationInfo.labelRes;
        return stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : context.getString(stringId);
    }

    static Bundle fromJson(JSONObject s) {
        Bundle bundle = new Bundle();

        for (Iterator<String> it = s.keys(); it.hasNext(); ) {
            String key = it.next();
            JSONArray arr = s.optJSONArray(key);
            String str = s.optString(key);

            if (arr != null && arr.length() <= 0)
                bundle.putStringArray(key, new String[]{});

            else if (arr != null && arr.optString(0) != null) {
                String[] newarr = new String[arr.length()];
                for (int i = 0; i < arr.length(); i++)
                    newarr[i] = arr.optString(i);
                bundle.putStringArray(key, newarr);
            } else if (str != null)
                bundle.putString(key, str);

            else
                System.err.println("unable to transform json to bundle " + key);
        }

        return bundle;
    }

    static String bundleToJSON(Bundle extras) {
        JSONObject json = new JSONObject();
        Set<String> keys = extras.keySet();
        for (String key : keys) {
            try {
                json.put(key, extras.get(key));
            } catch (JSONException e) {
                //Handle exception here
            }
        }
        return json.toString();
    }
}

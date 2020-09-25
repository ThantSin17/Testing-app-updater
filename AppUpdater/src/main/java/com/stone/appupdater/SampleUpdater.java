package com.stone.appupdater;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.StringTokenizer;

import androidx.core.content.FileProvider;


public class SampleUpdater {
    private Activity activity;
    private int requestCode = 111;
    private DownloadManager mDownloadManager;
    private String downloadPath = "";
    private String appName = "myApp";
    private String url = "";
    private String serverRes = "";
    private String downloadLink = "";
    private String versionName = null;
    private int versionCode=0;
    private boolean isPlayStore=false;
    private long mDownloadedFileID;
    private DownloadManager.Request mRequest;
    private String messages=null;

    public SampleUpdater(Activity activity) {
        this.activity = activity;
        // this.requestCode = requestCode;
        mDownloadManager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/";
        appName = activity.getApplicationInfo().loadLabel(activity.getPackageManager()).toString();

    }

    public void check(String link) {
        url = link;
        File newFile = new File(downloadPath);
        if (!newFile.exists()) {
            newFile.mkdirs();
        }
        if (isOnline()) {
            checkUpdate();
        } else {
            Toast.makeText(activity, "No Internet Connection!!!!", Toast.LENGTH_LONG).show();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void checkUpdate() {
        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... voids) {
                HttpURLConnection httpConn = null;
                URL myUrl;
                String result;
                try {
                    myUrl = new URL(url);
                    httpConn = (HttpURLConnection) myUrl.openConnection();
                    httpConn.setUseCaches(false);
                    httpConn.setRequestMethod("GET");
                    InputStream is = httpConn.getInputStream();
                    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                    String line;
                    StringBuffer response = new StringBuffer();
                    while ((line = rd.readLine()) != null) {
                        response.append(line);
                        response.append('\n');
                    }
                    rd.close();
                    result = response.toString().trim();

                    return result;
                } catch (Exception e) {
                    e.printStackTrace();
                    serverRes = e.toString();
                    return null;
                } finally {
                    if (httpConn != null) {
                        httpConn.disconnect();
                    }
                }
            }
            @Override
            protected void onPostExecute(String response)
            {
                super.onPostExecute(response);

                if (response != null)
                {
                    PackageManager manager = activity.getPackageManager();
                    PackageInfo info;
                    int currentVersion = 0;
                    try
                    {
                        info = manager.getPackageInfo(activity.getPackageName(), 0);
                        currentVersion = info.versionCode;
                        JSONObject jo=new JSONObject(response);
                        versionCode = jo.getInt("versionCode");
                        versionName = jo.getString("latestVersion");
                        isPlayStore = jo.getBoolean("playStore");
                        messages=jo.getString("releaseNotes");

                        if (!isPlayStore)
                        {
                            downloadLink = jo.getString("link");
                        }
                        if (versionCode > currentVersion)
                        {

                            //listener.onUpdateAvailable(versionName);
                            showConfirmDialog();
                        }
                        else
                        {
                            Toast.makeText(activity, "This app is up-to-date", Toast.LENGTH_SHORT).show();
                        }
                    }
                    catch (Exception e)
                    {
                        Toast.makeText(activity, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
                else
                {
                    Toast.makeText(activity, serverRes, Toast.LENGTH_SHORT).show();

                }
            }
        }.execute();
    }
    public void showConfirmDialog()
    {
        showConfirmDialog("Update Apk", "New update available. (" + versionName + ")", "Update", "Cancel");
    }
    public void update()
    {
        if (isPlayStore)
        {
            try
            {
                activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + activity.getPackageName())));
            }
            catch (ActivityNotFoundException anfe)
            {
                activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + activity.getPackageName())));
            }
        }
        else
        {
            if (Build.VERSION.SDK_INT >= 23)
            {

                if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED || activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
                {
                    activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE}, requestCode);
                }
                else
                {
                    downloadFile();
                }
            }
            else
            {
                downloadFile();
            }
        }
    }
    private void downloadFile()
    {
        String fileName=appName + "_" + versionName + ".apk";
        try
        {
            String mBaseFolderPath = downloadPath;
            if (!new File(mBaseFolderPath).exists())
            {
                new File(mBaseFolderPath).mkdir();
            }
            File myFile = new File(mBaseFolderPath ,fileName);
            if (!myFile.exists())
            {
                String mFilePath ="file://" +mBaseFolderPath + fileName;
                Uri downloadUri = Uri.parse(downloadLink);
                mRequest = new DownloadManager.Request(downloadUri);
                mRequest.setDestinationUri(Uri.parse(mFilePath));
                mRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                mDownloadedFileID = mDownloadManager.enqueue(mRequest);
                IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
                activity.registerReceiver(downloadReceiver, filter);
                Toast.makeText(activity, "Download started: " + fileName, Toast.LENGTH_SHORT).show();
            }
            else
            {
                installApk(myFile.getAbsolutePath());
            }
        }
        catch (Exception e)
        {
            Toast.makeText(activity, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void installApk(String path)
    {
        try
        {
            Uri uri=null;
            Intent intent = new Intent(Intent.ACTION_VIEW);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            {
                uri = FileProvider.getUriForFile(activity, activity.getPackageName()+".provider", new File(path));
            }
            else
            {
                uri = Uri.parse("file://" + path);
            }
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            activity.startActivity(intent);
        }
        catch (Exception e)
        {
            Toast.makeText(activity, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            final Uri uri = mDownloadManager.getUriForDownloadedFile(mDownloadedFileID);
            final String apkPath = getRealPathFromURI(uri);
            installApk(apkPath);
            activity.unregisterReceiver(this);
        }
    };
    private String getRealPathFromURI(Uri contentUri)
    {
        String path = null;
        String[] proj = { MediaStore.MediaColumns.DATA };
        Cursor cursor = activity.getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor.moveToFirst())
        {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            path = cursor.getString(column_index);
        }
        cursor.close();
        return path;
    }
    public void showConfirmDialog(String title, String msg, String yes, String no)
    {
        final AlertDialog ad=new AlertDialog.Builder(activity).create();
        ad.setTitle(title);
        ad.setMessage(msg);
        if (messages!=null){
            String str="";
            StringTokenizer tokenizer=new StringTokenizer(messages,",");
            while (tokenizer.hasMoreTokens())
            {
                str+=tokenizer.nextToken()+"\n";
            }
            ad.setMessage(msg+"\n"+str);

        }
        //final AlertDialog ad=builder.create();
        ad.setButton(AlertDialog.BUTTON_POSITIVE, yes,
                new DialogInterface.OnClickListener(){

                    @Override
                    public void onClick(DialogInterface p1, int p2)
                    {
                        update();
                    }
                });

        ad.setButton(AlertDialog.BUTTON_NEGATIVE, no,
                new DialogInterface.OnClickListener(){

                    @Override
                    public void onClick(DialogInterface p1, int p2)
                    {
                        ad.dismiss();
                    }
                });
        ad.show();
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        if (requestCode == requestCode)
        {
            boolean ok=false;
            if (grantResults.length >0)
            {
                for(int i=0;i<grantResults.length;i++){
                    if (grantResults[i] == 0){
                        ok=true;
                    }else{
                        ok=false;


                        break;
                    }
                }
                if (!ok)
                {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (activity.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) && activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE))
                        {
                            Toast.makeText(activity, "Permission enable", Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            Toast.makeText(activity, "Permission denied", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                else
                {
                    downloadFile();
                }
            }
        }

    }
    protected boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        assert cm != null;
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        } else {
            return false;
        }
    }

}

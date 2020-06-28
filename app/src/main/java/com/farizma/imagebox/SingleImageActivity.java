package com.farizma.imagebox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.DownloadManager;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static com.farizma.imagebox.FavoriteActivity.SHARED_PREF;

public class SingleImageActivity extends AppCompatActivity {

    private static final String PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    private ImageView imageView;
    private TextView textView;
    private ImageButton downloadButton, heartButton, shareButton;

    private String id, username, name, url, downloadLocation;
    private Bitmap bitmap;
    private boolean isHearted, isLoaded;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private JSONArray favouriteList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme2);
        setContentView(R.layout.activity_single_image);

        sharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        editor = sharedPreferences.edit();

        statusBarConfig(findViewById(R.id.rootView));

        ActivityCompat.requestPermissions(SingleImageActivity.this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 10);

        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);
        downloadButton = findViewById(R.id.downloadButton);
        heartButton = findViewById(R.id.heartButton);
        shareButton = findViewById(R.id.shareButton);

        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!permissionGranted()) showToast(getString(R.string.permission_download));
                else getDownloadLink();
            }
        });

        heartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isFav()) removeFromFav();
                else addToFav();
            }
        });

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!permissionGranted()) showToast(getString(R.string.permission_share));
                else {
                    if(isLoaded) shareImage();
                    else showToast(getString(R.string.loading_image));
                }
            }
        });

        Intent intent = getIntent();
        id = intent.getStringExtra("ID");
        username = intent.getStringExtra("USERNAME");
        name = intent.getStringExtra("NAME");
        url = intent.getStringExtra("URL");
        downloadLocation = intent.getStringExtra("DOWNLOAD_LOCATION");

        setText();

        isHearted = isFav();
        if(isHearted) heartButton.setImageResource(R.drawable.ic_heart_fill);
        else heartButton.setImageResource(R.drawable.ic_heart_empty);
        displayImage();
    }

    private static class ClickableString extends ClickableSpan {
        private View.OnClickListener mListener;

        public ClickableString(View.OnClickListener onClickListener) {
            mListener = onClickListener;
        }

        @Override
        public void onClick(@NonNull View view) {
            mListener.onClick(view);
        }
    }

    private void makeLinksFocusable(TextView tv) {
        MovementMethod m = tv.getMovementMethod();
        if ((m == null) || !(m instanceof LinkMovementMethod)) {
            if (tv.getLinksClickable()) {
                tv.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
    }

    private SpannableString makeLineSpan(String text, View.OnClickListener onClickListener) {
        SpannableString link = new SpannableString(text);
        link.setSpan(new ClickableString(onClickListener), 0, text.length(), SpannableString.SPAN_INCLUSIVE_EXCLUSIVE);
        return link;
    }

    private void setText() {
        textView.setText(getString(R.string.photo_by));
        textView.setTypeface(Typeface.SERIF, Typeface.ITALIC);
        textView.setTextSize(10);

        SpannableString link_username = makeLineSpan(name, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = getString(R.string.link_user_start) + username +getString(R.string.link_user_end);
                openUrl(url);
            }
        });
        SpannableString link_unsplash = makeLineSpan(getString(R.string.unsplash), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = getString(R.string.link_unsplash);
                openUrl(url);
            }
        });

        textView.append(link_username);
        textView.append(" on ");
        textView.append(link_unsplash);
        makeLinksFocusable(textView);
    }

    private void openUrl(String url) {
        Intent linkIntent = new Intent(Intent.ACTION_VIEW);
        linkIntent.setData(Uri.parse(url));
        startActivity(linkIntent);
    }

    private void displayImage() {
        Glide.with(this)
                .asBitmap()
                .load(url)
                .override(1000, 1000)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        imageView.setImageBitmap(resource);
                        bitmap = resource;
                        isLoaded = true;
                    }
                });
    }

    private void shareImage() {
        // share image without saving it to gallery using cache directory
        Uri contentUri = getImageUri(bitmap);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/jpeg");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent.setDataAndType(contentUri, getApplicationContext().getContentResolver().getType(contentUri));
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text));
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_with)));
    }

    private Uri getImageUri(Bitmap bmp) {
        File cachePath = new File(getApplicationContext().getCacheDir(), "images");
        if(!cachePath.exists())
            cachePath.mkdirs();
        try {
            FileOutputStream out = new FileOutputStream(cachePath + "/image.jpg");
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }  catch (IOException e) {
            e.printStackTrace();
        }
        File imagePath = new File(getApplicationContext().getCacheDir(), "images");
        File newFile = new File(imagePath, "image.jpg");
        Uri uri = FileProvider.getUriForFile(getApplicationContext(), BuildConfig.APPLICATION_ID + ".fileprovider", newFile);
        return uri;
    }

    private void downloadImage(String link) {
        String title = getTitleName();

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(link));
        request.allowScanningByMediaScanner();
        request.setTitle(title);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir("/ImageBox", title);

        DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        downloadManager.enqueue(request);
        showToast(getString(R.string.downloading));

    }

    private void getDownloadLink() {
        StringRequest downloadRequest = new StringRequest(Request.Method.GET, downloadLocation,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject downloadObject = new JSONObject(response);
                            downloadImage(downloadObject.getString("url"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                Log.d("Volley", "ERROR: " + error);
            }
        });
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(downloadRequest);
    }

    private String getTitleName() {
        String timeStamp = new SimpleDateFormat("yyMMdd_HHmmss").format(new Date());
        return "ImageBox_" + timeStamp + ".jpeg";
    }

    private void showToast(String string) {
        Toast.makeText(getApplicationContext(), string, Toast.LENGTH_SHORT).show();
    }

    private void addToFav() {
        // add image to favourites
        heartButton.setImageResource(R.drawable.ic_heart_fill);
        isHearted = true;
        JSONObject item = new JSONObject();
        try {
            item.put("id", id);
            item.put("username", username);
            item.put("name", name);
            item.put("url", url);
            item.put("download_location", downloadLocation);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if(!sharedPreferences.contains("FavouriteList")) {
            favouriteList = new JSONArray();
        } else {
            try {
                favouriteList = new JSONArray(sharedPreferences.getString("FavouriteList", null));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        favouriteList.put(item);
        editor.putString("FavouriteList", favouriteList.toString());
        editor.apply();
        showToast(getString(R.string.added));
    }

    private void removeFromFav() {
        // ic_clear image from favourites
        heartButton.setImageResource(R.drawable.ic_heart_empty);
        isHearted = false;
        for (int i=0; i<favouriteList.length(); i++) {
            try {
                JSONObject currentItem = new JSONObject(favouriteList.getJSONObject(i).toString());
                if(currentItem.getString("id").compareTo(id) == 0) {
                    favouriteList.remove(i);
                    break;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        editor.putString("FavouriteList", favouriteList.toString());
        editor.apply();
        showToast(getString(R.string.removed));
    }

    private boolean isFav() {
        if(sharedPreferences.contains("FavouriteList")) {
            try {
                favouriteList = new JSONArray(sharedPreferences.getString("FavouriteList", null));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            for (int i=0; i<favouriteList.length(); i++) {
                try {
                    JSONObject currentItem = new JSONObject(favouriteList.getJSONObject(i).toString());
                    if(currentItem.getString("id").compareTo(id) == 0)
                        return true;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    private void statusBarConfig(View view) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int flags = view.getSystemUiVisibility();
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            view.setSystemUiVisibility(flags);
            this.getWindow().setStatusBarColor(Color.WHITE);
        }
    }

    private boolean permissionGranted() {
        return (ContextCompat.checkSelfPermission(this, PERMISSION) == PackageManager.PERMISSION_GRANTED);
    }
}
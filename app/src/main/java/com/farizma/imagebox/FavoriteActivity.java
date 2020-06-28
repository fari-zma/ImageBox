package com.farizma.imagebox;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class FavoriteActivity extends AppCompatActivity {

    protected static final String SHARED_PREF = "mysharedpref";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private ArrayList<Item> itemList = new ArrayList<>();

    private Toolbar toolbar;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme2);
        setContentView(R.layout.activity_favorite);

        toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_fav);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(Color.WHITE);

        // Enable the Up button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        imageView = findViewById(R.id.imageView);
        sharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_fav, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear: if(imageView.getVisibility() == View.INVISIBLE) showDialog(); break;
            case R.id.home: onBackPressed(); break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        start();
    }

    private void start() {
        itemList.clear();
        showFavorites();
    }

    private void showDialog() {
        // show dialog, if yes -> clearFavList then showFav ; else do nothings
        final Dialog dialog = new Dialog(this);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.dailog);

        dialog.findViewById(R.id.clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                clearFavList();
                Toast.makeText(getApplicationContext(), getString(R.string.deleted), Toast.LENGTH_SHORT).show();
                start();
            }
        });

        dialog.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });
        dialog.show();
    }

    private void clearFavList() {
        // remove data from sharedPreference
        if(sharedPreferences.contains("FavouriteList")) {
            editor.putString("FavouriteList", (new JSONArray()).toString());
            editor.apply();
        }
    }

    private void showFavorites() {
        if(sharedPreferences.contains("FavouriteList")) {
            try {
                JSONArray favouriteList = new JSONArray(sharedPreferences.getString("FavouriteList", null));
                if(!favouriteList.isNull(0)) imageView.setVisibility(View.INVISIBLE);
                else imageView.setVisibility(View.VISIBLE);
                getList();
                recyclerViewConfig();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void getList() {
        try {
            JSONArray favouriteList = new JSONArray(sharedPreferences.getString("FavouriteList", null));
            for(int i=0; i<favouriteList.length(); i++) {
                JSONObject item = new JSONObject(favouriteList.getJSONObject(i).toString());
                insertData(item.getString("id"), item.getString("username"), item.getString("name"),
                        item.getString("url"), item.getString("download_location"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void insertData(String id, String username, String name, String raw, String download_location) {
        itemList.add(new Item(id, username, name, raw, download_location));
    }

    private void recyclerViewConfig() {
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(30);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        recyclerView.setLayoutManager(new GridLayoutManager(this, getResources().getInteger(R.integer.columnSpan)));
        adapter = new Adapter(itemList);
        recyclerView.setAdapter(adapter);
    }
}
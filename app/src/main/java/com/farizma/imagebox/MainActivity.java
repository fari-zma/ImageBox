package com.farizma.imagebox;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuItemCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SearchView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public static final String URL = "https://api.unsplash.com/";
    public static final String ACCESS_KEY = "Paste Unsplash APi's ACCESS KEY here";

    private ProgressBar progressBar;
    private Button loadButton;

    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;

    private ArrayList<Item> itemList = new ArrayList<>();
    private int count = 1;

    private String mQuery;
    private boolean isSearch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme1);
        setContentView(R.layout.activity_main);

        loadButton = findViewById(R.id.loadMore);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        recyclerViewConfig();
        fetchData();

        loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ++count;
                fetchData();
            }
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("showButton"));
    }

    public BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get data included in the Intent
            boolean isLast = intent.getBooleanExtra("value", false);
            if (isLast) loadButton.setVisibility(View.VISIBLE);
            else loadButton.setVisibility(View.INVISIBLE);
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem search = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) search.getActionView();

        // Associate searchable configuration with the SearchView
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                reset(true, query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mQuery = newText;
                return false;
            }
        });

        MenuItemCompat.setOnActionExpandListener(search, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                reset(false, null);
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.fav:
                startActivity(new Intent(this, FavoriteActivity.class));
                break;
        }
        return true;
    }

    private void fetchData() {
        progressBar.setVisibility(View.VISIBLE);
        String url = isSearch ? getSearchUrl(mQuery) : getRandomUrl();

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        progressBar.setVisibility(View.INVISIBLE);
                        try {
                            JSONArray jsonArray;
                            if(isSearch) {
                                // response is in JSONObject format
                            JSONObject jsonObject1 = new JSONObject(response);
                            jsonArray = jsonObject1.getJSONArray("results");
                            }
                            // response is in JSONArray format
                            else jsonArray = new JSONArray(response);

                            for(int i=0; i<jsonArray.length(); i++) {
                                // fetch one item at a time
                                JSONObject jsonObject = new JSONObject(jsonArray.getJSONObject(i).toString());
                                // get ID
                                String id = jsonObject.getString("id");
                                // get image link
                                JSONObject urls = new JSONObject(jsonObject.getJSONObject("urls").toString());
                                String raw = urls.getString("raw");
                                // get user's username & name
                                JSONObject user = new JSONObject(jsonObject.getJSONObject("user").toString());
                                // get username
                                String username = user.getString("username");
                                // get name
                                String name = user.getString("name");
                                // get download link
                                JSONObject links = new JSONObject(jsonObject.getJSONObject("links").toString());
                                // set download location
                                String download_loc = links.getString("download_location")+"?client_id="+ACCESS_KEY;
                                // insert all information into ArrayList
                                insertData(id, username, name, raw, download_loc);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.d("Volley", "JSONException: " + e);
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
        requestQueue.add(stringRequest);
    }

    private String getRandomUrl() {
        // url for getting the images as jSONArray
        String url = URL + "photos?page=" + count + "&per_page=30&client_id=" + ACCESS_KEY;
        return url;
    }

    private String getSearchUrl(String query) {
        // url for getting the images for query as jSONObject
        String url = URL + "search/photos?page=" + count + "&per_page=30&query=" + query + "&client_id=" + ACCESS_KEY;
        return url;
    }

    private void insertData(String id, String username, String name, String raw, String download_location) {
        itemList.add(new Item(id, username, name, raw, download_location));
        adapter.notifyDataSetChanged();
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

    private void reset(boolean isSearch, String query) {
        count = 1;
        this.isSearch = isSearch;
        mQuery = query;
        itemList.clear();
        adapter.notifyDataSetChanged();
        fetchData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // release itemList
        itemList.clear();
        itemList = null;
        recyclerView.getRecycledViewPool().clear();
    }
}
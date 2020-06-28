package com.farizma.imagebox;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;

public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

    private ArrayList<Item> arrayList;
    private Context context;

    public Adapter(ArrayList<Item> arrayList) {
        this.arrayList = arrayList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Item currentItem = arrayList.get(position);
        Glide.with(context)
                .load(currentItem.getUrl())
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .thumbnail(0.05f)
                .dontAnimate()
                .centerCrop()
                .placeholder(R.drawable.loading)
                .into(holder.imageView);

        holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, SingleImageActivity.class);
                intent.putExtra("ID", currentItem.getId());
                intent.putExtra("USERNAME", currentItem.getUsername());
                intent.putExtra("NAME", currentItem.getName());
                intent.putExtra("URL", currentItem.getUrl());
                intent.putExtra("DOWNLOAD_LOCATION", currentItem.getDownloadLocation());
                context.startActivity(intent);
            }
        });

        Intent intent = new Intent("showButton");
        if(position == (arrayList.size()-1)) intent.putExtra("value", true);
        else intent.putExtra("value", false);
         LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageView;

        ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }
}

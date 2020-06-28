package com.farizma.imagebox;

public class Item {
    private String mId;
    private String mUsername;
    private String mName;
    private String mUrl;
    private String mDownloadLocation;

    public Item(String id, String username, String name, String url, String downloadLoc) {
        mId = id;
        mUsername = username;
        mName = name;
        mUrl = url;
        mDownloadLocation = downloadLoc;
    }

    public String getId() {
        return mId;
    }

    public String getUsername() {
        return mUsername;
    }

    public String getName() {
        return mName;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getDownloadLocation() { return mDownloadLocation; }
}

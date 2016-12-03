package com.dropbox.android.sample;


import android.content.Context;
import android.widget.ListView;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;

import java.io.File;

public class DropboxAccess {

    private Context mContext;
    DropboxAPI<AndroidAuthSession> mApi;

    public DropboxAccess(Context context, DropboxAPI<AndroidAuthSession> api) {
        mContext = context;
        mApi = api;
    }

    public void DownloadFile(String remoteDir, String localDir, String filename, ListView displayList){
        File targetDir = new File(localDir);
        new DownloadFile(mContext,mApi,remoteDir,targetDir,filename,displayList).execute();
    }

    public void UploadFile(String remoteDir,File file,ListView displayList){
        new UploadFile(mContext,mApi,remoteDir,file,displayList).execute();
    }

    public void ListLocalFile(String path,ListView displayList){
        new ListFile(mContext,null,path,displayList).execute();
    }

    public void ListRemoteFile(String path,ListView displayList){
        new ListFile(mContext,mApi,path,displayList).execute();
    }
}

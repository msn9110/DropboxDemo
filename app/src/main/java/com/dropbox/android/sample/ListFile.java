package com.dropbox.android.sample;


import java.io.File;
import java.util.ArrayList;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxParseException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;

/**
 * Here we show getting metadata for a directory and downloading a file in a
 * background thread, trying to show typical exception handling and flow of
 * control for an app that downloads a file from Dropbox.
 */

public class ListFile extends AsyncTask<Void, Void, Boolean> {


    private Context mContext;
    private DropboxAPI<?> mApi;
    private String mPath;
    private ListView mList;

    private ArrayList<String> files;

    private String mErrorMsg;
    private String mExt;

    public ListFile(Context context, DropboxAPI<?> api,
                                 String Path, ListView view, String ext) {
        // We set the context this way so we don't accidentally leak activities
        mContext = context;
        mApi = api;
        mPath = Path;
        mList = view;
        mExt = ext;
        files = new ArrayList<>();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        if(mApi != null){
            try {
                // Get the metadata for a directory
                Entry dirent = mApi.metadata(mPath, 1000, null, true, null);

                if (!dirent.isDir || dirent.contents == null) {
                    // It's not a directory, or there's nothing in it
                    mErrorMsg = "File or empty directory";
                    return false;
                }

                //===============================================================================
                for (Entry ent : dirent.contents) {
                    String filename = ent.fileName();
                    if (filename.endsWith(mExt)){
                        files.add(filename);
                    } else if(mExt == null || mExt.equalsIgnoreCase("*")){
                        files.add(filename);
                    }

                }
                //===============================================================================


                return true;

            } catch (DropboxUnlinkedException e) {
                // The AuthSession wasn't properly authenticated or user unlinked.
            } catch (DropboxPartialFileException e) {
                // We canceled the operation
                mErrorMsg = "Download canceled";
            } catch (DropboxServerException e) {
                // This gets the Dropbox error, translated into the user's language
                mErrorMsg = e.body.userError;
                if (mErrorMsg == null) {
                    mErrorMsg = e.body.error;
                }
            } catch (DropboxIOException e) {
                // Happens all the time, probably want to retry automatically.
                mErrorMsg = "Network error.  Try again.";
            } catch (DropboxParseException e) {
                // Probably due to Dropbox server restarting, should retry
                mErrorMsg = "Dropbox error.  Try again.";
            } catch (DropboxException e) {
                // Unknown error
                mErrorMsg = "Unknown error.  Try again.";
            }
            return false;
        } else {
            File dir = new File(mPath);
            if(!dir.exists())
                return false;
            File[] myfiles = dir.listFiles();
            for (File f : myfiles){
                if (f.isFile()){
                    String filename = f.getName();
                    if (filename.endsWith(mExt)){
                        files.add(filename);
                    } else if(mExt == null || mExt.equalsIgnoreCase("*")){
                        files.add(filename);
                    }
                }
            }

            return (files.size() > 0);
        }
    }

    @Override
    protected void onProgressUpdate(Void... progress) {
        super.onProgressUpdate(progress);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        if (result) {
            ArrayAdapter<String> listAdapter=new ArrayAdapter<>(mContext,android.R.layout.simple_list_item_1,files);
            mList.setAdapter(listAdapter);
        } else {
            // Couldn't download it, so show an error
            showToast(mErrorMsg);
        }
    }

    private void showToast(String msg) {
        Toast error = Toast.makeText(mContext, msg, Toast.LENGTH_LONG);
        error.show();
    }
}
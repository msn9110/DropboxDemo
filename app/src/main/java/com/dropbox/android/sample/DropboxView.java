package com.dropbox.android.sample;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.android.AuthActivity;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;

import java.io.File;



public class DropboxView extends Fragment {

    Context context;
    private static final String TAG = "##DropboxView";

    ///////////////////////////////////////////////////////////////////////////
    //                      Your app-specific settings.                      //
    ///////////////////////////////////////////////////////////////////////////

    // Replace this with your app key and secret assigned by Dropbox.
    // Note that this is a really insecure way to do this, and you shouldn't
    // ship code which contains your key & secret in such an obvious way.
    // Obfuscation is good.
    private String MyDropbox_DIR = "/Backup/";//<<<===============dropbox directory
    private File downloadDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"DBDownload");//<<<========local directory

    private static String APP_KEY = "4llmgy33myaizyn"; //<<<============replace app key
    private static String APP_SECRET = "naoed5xmpmlh55l"; //<<<=============replace app secret

    ///////////////////////////////////////////////////////////////////////////
    //                      End app-specific settings.                       //
    ///////////////////////////////////////////////////////////////////////////

    // You don't need to change these, leave them alone.
    private static final String ACCOUNT_PREFS_NAME = "prefs";
    private static final String ACCESS_KEY_NAME = "ACCESS_KEY";
    private static final String ACCESS_SECRET_NAME = "ACCESS_SECRET";

    private static final boolean USE_OAUTH1 = false;

    DropboxAPI<AndroidAuthSession> mApi;
    DropboxAccess operations;
    ///////////////////////////////////////////////////////////////////////////
    private boolean mLoggedIn;

    // Android widgets
    private Button mSubmit;
    private LinearLayout mDisplay;
    private ListView mList, downloadList;

    private static final int CHOOSE_FILE = 2;

    public static DropboxView newInstance(String appKey, String appSecret, String dropboxDir,
                                          String localPath) {
        DropboxView newFragment = new DropboxView();
        Bundle bundle = new Bundle();
        bundle.putString("APP_KEY", appKey);
        bundle.putString("APP_SECRET", appSecret);
        bundle.putString("MyDropbox_Dir", dropboxDir);
        bundle.putString("localDir",localPath);
        newFragment.setArguments(bundle);
        return newFragment;

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View mContentView = inflater.inflate(R.layout.dropbox_view, container, false);

        Bundle args = getArguments();
        if (args != null) {
            APP_KEY = args.getString("APP_KEY");
            APP_SECRET = args.getString("APP_SECRET");
            MyDropbox_DIR = args.getString("MyDropbox_DIR");
            String localDir = args.getString("localDir");
            if (localDir != null)
                downloadDir = new File(localDir);
        }

        Log.d(TAG,"TEST");
        //======================== 初始化=======================
        // We create a new AuthSession so that we can use the Dropbox API.
        AndroidAuthSession session = buildSession();
        mApi = new DropboxAPI<>(session);
        checkAppKeySetup();
        operations = new DropboxAccess(context,mApi);
        //======================================================
        Log.d(TAG,"TEST");

        // Basic Android widgets
        String path = downloadDir.getPath();
        TextView displayPath = (TextView) mContentView.findViewById(R.id.localPath);
        displayPath.setText(path);
        path = "Dropbox:" + MyDropbox_DIR;
        displayPath = (TextView) mContentView.findViewById(R.id.dropboxPath);
        displayPath.setText(path);

        mSubmit = (Button) mContentView.findViewById(R.id.auth_button);

        mSubmit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // This logs you out if you're logged in, or vice versa
                if (mLoggedIn) {
                    logOut();
                } else {
                    // Start the remote authentication
                    if (USE_OAUTH1) {
                        mApi.getSession().startAuthentication(context);
                    } else {
                        mApi.getSession().startOAuth2Authentication(context);
                    }
                }
            }
        });

        mDisplay = (LinearLayout) mContentView.findViewById(R.id.logged_in_display);

        // This is the button to upload file
        Button mUpload = (Button) mContentView.findViewById(R.id.photo_button);

        mUpload.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final String mimeType = "application/x-sqlite3";
                final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType(mimeType);
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY,true);
                startActivityForResult(intent, CHOOSE_FILE);
            }
        });

        Button mShowDropboxFile=(Button) mContentView.findViewById(R.id.list_button);
        mList = (ListView) mContentView.findViewById(R.id.listView_dropbox);
        mShowDropboxFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                operations.ListRemoteFile(MyDropbox_DIR, mList);
            }
        });

        downloadList = (ListView) mContentView.findViewById(R.id.listView_download);
        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String select=((TextView) view).getText().toString();
                operations.DownloadFile(MyDropbox_DIR,downloadDir.getAbsolutePath(),select,downloadList);
            }
        });
        // Display the proper UI state if logged in or not
        setLoggedIn(mApi.getSession().isLinked());//,<<<==============================================

        return mContentView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
    }


    @Override
    public void onResume() {
        super.onResume();
        AndroidAuthSession session = mApi.getSession();

        // The next part must be inserted in the onResume() method of the
        // activity from which session.startAuthentication() was called, so
        // that Dropbox authentication completes properly.
        if (session.authenticationSuccessful()) {
            try {
                // Mandatory call to complete the auth
                session.finishAuthentication();

                // Store it locally in our app for later use
                storeAuth(session);
                setLoggedIn(true);
            } catch (IllegalStateException e) {
                showToast("Couldn't authenticate with Dropbox:" + e.getLocalizedMessage());
                Log.i(TAG, "Error authenticating", e);
            }
        }
    }

    //fragment runing

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void logOut() {
        // Remove credentials from the session
        mApi.getSession().unlink();

        // Clear our stored keys
        clearKeys();
        // Change UI state to display logged out version
        setLoggedIn(false);
    }

    /**
     * Convenience function to change UI state based on being logged in
     */
    private void setLoggedIn(boolean loggedIn) {
        mLoggedIn = loggedIn;
        String buttonText;
        if (loggedIn) {
            buttonText = "Unlink from Dropbox";
            mDisplay.setVisibility(View.VISIBLE);
            operations.ListRemoteFile(MyDropbox_DIR, mList);
            operations.ListLocalFile(downloadDir.getPath(), downloadList);
        } else {
            buttonText = "Link with Dropbox";
            mDisplay.setVisibility(View.GONE);
        }

        mSubmit.setText(buttonText);
    }

    private void checkAppKeySetup() {
        // Check to make sure that we have a valid app key
        if (APP_KEY.startsWith("CHANGE") ||
                APP_SECRET.startsWith("CHANGE")) {
            showToast("You must apply for an app key and secret from developers.dropbox.com, and add them to the DBRoulette ap before trying it.");
            return;
        }

        // Check if the app has set up its manifest properly.
        Intent testIntent = new Intent(Intent.ACTION_VIEW);
        String scheme = "db-" + APP_KEY;
        String uri = scheme + "://" + AuthActivity.AUTH_VERSION + "/test";
        testIntent.setData(Uri.parse(uri));
        PackageManager pm = context.getPackageManager();
        if (0 == pm.queryIntentActivities(testIntent, 0).size()) {
            showToast("URL scheme in your app's " +
                    "manifest is not set up correctly. You should have a " +
                    "com.dropbox.client2.android.AuthActivity with the " +
                    "scheme: " + scheme);
        }
    }

    private void showToast(String msg) {
        Toast error = Toast.makeText(context, msg, Toast.LENGTH_LONG);
        error.show();
    }

    /**
     * Shows keeping the access keys returned from Trusted Authenticator in a local
     * store, rather than storing user name & password, and re-authenticating each
     * time (which is not to be done, ever).
     */
    private void loadAuth(AndroidAuthSession session) {
        SharedPreferences prefs = context.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        String key = prefs.getString(ACCESS_KEY_NAME, null);
        String secret = prefs.getString(ACCESS_SECRET_NAME, null);
        if (key == null || secret == null || key.length() == 0 || secret.length() == 0) return;

        if (key.equals("oauth2:")) {
            // If the key is set to "oauth2:", then we can assume the token is for OAuth 2.
            session.setOAuth2AccessToken(secret);
        } else {
            // Still support using old OAuth 1 tokens.
            session.setAccessTokenPair(new AccessTokenPair(key, secret));
        }
    }

    /**
     * Shows keeping the access keys returned from Trusted Authenticator in a local
     * store, rather than storing user name & password, and re-authenticating each
     * time (which is not to be done, ever).
     */
    private void storeAuth(AndroidAuthSession session) {
        // Store the OAuth 2 access token, if there is one.
        String oauth2AccessToken = session.getOAuth2AccessToken();
        if (oauth2AccessToken != null) {
            SharedPreferences prefs = context.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString(ACCESS_KEY_NAME, "oauth2:");
            edit.putString(ACCESS_SECRET_NAME, oauth2AccessToken);
            edit.apply();
            return;
        }
        // Store the OAuth 1 access token, if there is one.  This is only necessary if
        // you're still using OAuth 1.
        AccessTokenPair oauth1AccessToken = session.getAccessTokenPair();
        if (oauth1AccessToken != null) {
            SharedPreferences prefs = context.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString(ACCESS_KEY_NAME, oauth1AccessToken.key);
            edit.putString(ACCESS_SECRET_NAME, oauth1AccessToken.secret);
            edit.apply();
        }
    }

    private void clearKeys() {
        SharedPreferences prefs = context.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        SharedPreferences.Editor edit = prefs.edit();
        edit.clear();
        edit.apply();
    }

    private AndroidAuthSession buildSession() {
        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);

        AndroidAuthSession session = new AndroidAuthSession(appKeyPair);
        loadAuth(session);
        return session;
    }

    // This is what gets called on finishing a media piece to import
    //處理選擇檔案上傳
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CHOOSE_FILE) {
            // return from file upload
            if (resultCode == Activity.RESULT_OK) {
                Uri uri = null;
                if (data != null) {
                    uri = data.getData();
                }

                if (uri != null) {
                    String path=uri.getPath();
                    System.out.println(uri.getLastPathSegment());
                    if(path.startsWith("/file"))
                        path=path.replace("/file","");
                    operations.UploadFile(MyDropbox_DIR,new File(path),mList);
                }
            } else {
                Log.w(TAG, "Unknown Activity Result from mediaImport: "+ resultCode);
            }
        }
    }
}

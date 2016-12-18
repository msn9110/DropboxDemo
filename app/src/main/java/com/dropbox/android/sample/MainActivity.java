package com.dropbox.android.sample;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import java.io.File;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    private final String MyDropbox_DIR = "/Backup/";//<<<===============dropbox directory
    private File downloadDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"DBDownload");//<<<========local directory

    private static final String APP_KEY = "4llmgy33myaizyn"; //<<<============replace app key
    private static final String APP_SECRET = "naoed5xmpmlh55l"; //<<<=============replace app secret

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        if (Build.VERSION.SDK_INT >= 23){
            //api 23 若target sdk version為api 23不含以下 就不用
            int permission = ActivityCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 無權限，向使用者請求
                ActivityCompat.requestPermissions(this,
                        new String[] {WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE},
                        REQUEST_EXTERNAL_STORAGE);
            }else{
                //已有權限，執行儲存程式

                //加载Fragment
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                DropboxView dropboxView = DropboxView.newInstance(APP_KEY, APP_SECRET, MyDropbox_DIR, downloadDir.getAbsolutePath());
                transaction.add(R.id.frag_dropbox_view, dropboxView);
                transaction.commit();
            }
        } else {
            //已有權限，執行儲存程式
            //加载Fragment
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            DropboxView dropboxView = DropboxView.newInstance(APP_KEY, APP_SECRET, MyDropbox_DIR, downloadDir.getAbsolutePath());
            transaction.add(R.id.frag_dropbox_view, dropboxView);
            transaction.commit();
        }

    }

    //api 23 permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case REQUEST_EXTERNAL_STORAGE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //取得權限，進行檔案存取
                    //加载Fragment
                    FragmentManager fragmentManager = getFragmentManager();
                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                    DropboxView dropboxView = DropboxView.newInstance(APP_KEY, APP_SECRET, MyDropbox_DIR, downloadDir.getAbsolutePath());
                    transaction.add(R.id.frag_dropbox_view, dropboxView);
                    transaction.commit();
                } else {
                    //使用者拒絕權限，停用檔案存取功能
                    this.finish();
                }
        }
    }
}

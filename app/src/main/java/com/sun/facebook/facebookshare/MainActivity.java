package com.sun.facebook.facebookshare;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.AccessTokenManager;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareButton;
import com.facebook.share.widget.ShareDialog;

import java.io.FileNotFoundException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private final int CHOOSE_PHOTO = 1001;
    private static String facebookPkgName = "com.facebook.katana";
    private CallbackManager mCallbackManager;
    private ShareDialog mShareDialog;
    private Bitmap publish_bitmap;
    private SharePhotoContent mPhotoContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        initPermission();
        initFacebook();

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choosePhoto();
            }
        });

    }

    private void initFacebook() {
        mCallbackManager = CallbackManager.Factory.create();
        mShareDialog = new ShareDialog(this);
        mShareDialog.registerCallback(mCallbackManager, new FacebookCallback<Sharer.Result>() {
            @Override
            public void onSuccess(Sharer.Result result) {

            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException error) {

            }
        });

        LoginManager.getInstance().registerCallback(mCallbackManager,new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                showFacebookDialog(mPhotoContent);
            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException error) {

            }
        });
    }

    private void initPermission() {
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            String[] requestPermission = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
            ActivityCompat.requestPermissions(this, requestPermission, 1002);
        }
    }

    private void choosePhoto() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent, CHOOSE_PHOTO);
    }

    private Bitmap getBitmap(String path) {
        if (path == null) {
            Toast.makeText(this, "图片未找到!", Toast.LENGTH_SHORT).show();
            return null;
        }
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        return bitmap;
    }

    private void handleImageBeforeKitKat(Intent data) {
        Uri uri = data.getData();
        String imagePath = getImagePath(uri, null);
//        getBitmap(imagePath);
        shareTofb(getBitmap(imagePath));
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void handleImageAfterKitKat(Intent data) {
        String imagePath = null;
        Uri uri = data.getData();
        if (DocumentsContract.isDocumentUri(this,uri)) {
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            imagePath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            imagePath = uri.getPath();
        }
//        getBitmap(imagePath);
        shareTofb(getBitmap(imagePath));
    }

    private String getImagePath(Uri uri, String selection) {
        String path = null;
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    private void shareTofb(Bitmap bitmap) {
        SharePhoto photo = new SharePhoto.Builder().setBitmap(bitmap).build();
        mPhotoContent = new SharePhotoContent.Builder().addPhoto(photo).build();

        if (!checkApkExist(this, facebookPkgName)) {
            boolean loggedIn = AccessToken.getCurrentAccessToken() == null;
            if (loggedIn) {
//            LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile"));
                LoginManager.getInstance().logInWithPublishPermissions(this, Arrays.asList("publish_actions"));
            }
        }

        showFacebookDialog(mPhotoContent);

//        if (mShareDialog.canShow(ShareLinkContent.class)) {
////            ShareLinkContent linkContent = new ShareLinkContent.Builder().setContentUrl(
////                    (Uri.parse("http://developers.facebook.com/android"))
////            ).build();
//            mShareDialog.show(this,content);
//        }
//        mShareButton.setShareContent(content);

//        LoginManager.getInstance().logInWithPublishPermissions(this, Arrays.asList("publish_actions"));

    }

    private void showFacebookDialog(SharePhotoContent content) {
        if (mShareDialog.canShow(ShareLinkContent.class)) {
            mShareDialog.show(this,content);
        }
    }

    public static boolean checkApkExist(Context context, String packageName){
        if (TextUtils.isEmpty(packageName))
            return false;
        try {
            ApplicationInfo info = context.getPackageManager()
                    .getApplicationInfo(packageName,
                            PackageManager.GET_UNINSTALLED_PACKAGES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHOOSE_PHOTO && resultCode == RESULT_OK) {
            if (Build.VERSION.SDK_INT >= 19) {
                handleImageAfterKitKat(data);
            } else {
                handleImageBeforeKitKat(data);
            }
        } else {
            mCallbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }
}

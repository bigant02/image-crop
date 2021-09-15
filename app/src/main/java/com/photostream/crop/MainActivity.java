package com.photostream.crop;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public final String APP_TAG = "crop";
    public final static int CAPTURE_PHOTO_CODE = 1034;
    public final static int PICK_PHOTO_CODE = 1046;
    public final static int CROP_PHOTO_CODE = 1052;

    public String intermediateName = "1.jpg";
    public String resultName = "2.jpg";

    Uri intermediateProvider;
    Uri resultProvider;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonCamera = findViewById(R.id.buttonCamera);
        buttonCamera.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onLaunchCamera();
            }
        });

        Button buttonGallery = findViewById(R.id.buttonGallery);
        buttonGallery.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onPickPhoto();
            }
        });
    }

    public void onLaunchCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = getPhotoFileUri(intermediateName);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            intermediateProvider = FileProvider.getUriForFile(MainActivity.this, "com.photostream.crop.fileprovider", photoFile);
        else
            intermediateProvider = Uri.fromFile(photoFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, intermediateProvider);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, CAPTURE_PHOTO_CODE);
        }
    }

    // Trigger gallery selection for a photo
    public void onPickPhoto() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, PICK_PHOTO_CODE);
        }
    }

    private void onCropImage() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            grantUriPermission("com.android.camera", intermediateProvider, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent intent = new Intent("com.android.camera.action.CROP");
            intent.setDataAndType(intermediateProvider, "image/*");

            List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, 0);

            int size = 0;

            if(list != null) {
                grantUriPermission(list.get(0).activityInfo.packageName, intermediateProvider, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                size = list.size();
            }

            if (size == 0) {
                Toast.makeText(this, "Error, wasn't taken image!", Toast.LENGTH_SHORT).show();
            } else {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                intent.putExtra("crop", "true");

                intent.putExtra("scale", true);

                File photoFile = getPhotoFileUri(resultName);
                // wrap File object into a content provider
                // required for API >= 24
                // See https://guides.codepath.com/android/Sharing-Content-with-Intents#sharing-files-with-api-24-or-higher
                resultProvider = FileProvider.getUriForFile(MainActivity.this, "com.photostream.crop.fileprovider", photoFile);

                intent.putExtra("return-data", false);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, resultProvider);
                intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());

                Intent cropIntent = new Intent(intent);
                ResolveInfo res = list.get(0);
                cropIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                cropIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                grantUriPermission(res.activityInfo.packageName, resultProvider, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

                cropIntent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
                startActivityForResult(cropIntent, CROP_PHOTO_CODE);
            }
        } else {
            File photoFile = getPhotoFileUri(resultName);
            resultProvider = Uri.fromFile(photoFile);

            Intent intentCrop = new Intent("com.android.camera.action.CROP");
            intentCrop.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intentCrop.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intentCrop.setDataAndType(intermediateProvider, "image/*");
            intentCrop.putExtra("crop", "true");
            intentCrop.putExtra("scale", true);
            intentCrop.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
            intentCrop.putExtra("noFaceDetection", true);
            intentCrop.putExtra("return-data", false);
            intentCrop.putExtra(MediaStore.EXTRA_OUTPUT, resultProvider);
            startActivityForResult(intentCrop, CROP_PHOTO_CODE);
        }
    }

    // Returns the File for a photo stored on disk given the fileName
    public File getPhotoFileUri(String fileName) {
        File mediaStorageDir = new File(getExternalFilesDir(""), APP_TAG);
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()){
            Log.d(APP_TAG, "failed to create directory");
        }
        File file = new File(mediaStorageDir.getPath() + File.separator + fileName);
        return file;
    }

    public Bitmap loadFromUri(Uri photoUri) {
        Bitmap image = null;
        try {
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1){
                // on newer versions of Android, use the new decodeBitmap method
                ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(), photoUri);
                image = ImageDecoder.decodeBitmap(source);
            } else {
                // support older versions of Android by using getBitmap
                image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }

    private void saveBitmapFileToIntermediate(Uri sourceUri) {
        try {
            Bitmap bitmap =  loadFromUri(sourceUri);

            File imageFile = getPhotoFileUri(intermediateName);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                intermediateProvider = FileProvider.getUriForFile(MainActivity.this, "com.photostream.crop.fileprovider", imageFile);
            else
                intermediateProvider = Uri.fromFile(imageFile);

            OutputStream out = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAPTURE_PHOTO_CODE) {
            if (resultCode == RESULT_OK) {
                Bitmap takenImage = loadFromUri(intermediateProvider);
                ImageView ivPreview = findViewById(R.id.originView);
                ivPreview.setImageBitmap(takenImage);
                onCropImage();
            } else {
                Toast.makeText(this, "Picture wasn't taken!", Toast.LENGTH_SHORT).show();
            }
        }

        if ((data != null) && requestCode == PICK_PHOTO_CODE) {
            saveBitmapFileToIntermediate(data.getData());
            Bitmap selectedImage = loadFromUri(intermediateProvider);
            ImageView ivPreview = findViewById(R.id.originView);
            ivPreview.setImageBitmap(selectedImage);
            onCropImage();
        }

        if(requestCode == CROP_PHOTO_CODE) {
            if (resultCode == RESULT_OK) {
                Bitmap cropImage = loadFromUri(resultProvider);
                ImageView ivPreview = findViewById(R.id.resultView);
                ivPreview.setImageBitmap(cropImage);
            }
        }
    }
}
package com.rs.photoshopfacedetector;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {

    //private static final int REQUEST_IMAGE_GET = 9;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
    }

    //GET IMAGE FROM GALLERY
    public static final int GET_FROM_GALLERY = 3;
    public boolean running = false;
    public void getimagebutton(View v) {
        //selectImage();
        if (!running) {
            running = true;
            startActivityForResult(new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI), GET_FROM_GALLERY);
        }else{
            Toast toast = Toast.makeText(this,"Already processing a photo, please wait!",Toast.LENGTH_LONG);
            toast.show();
        }

    }
    public String picturePath;
    public TextView txtbox;
    public ProgressBar loadingbar;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Detects request codes
        if(requestCode==GET_FROM_GALLERY && resultCode == Activity.RESULT_OK) {
            Uri selectedImage = data.getData();
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                final byte[] byteArray = stream.toByteArray();
                bitmap.recycle();
                picturePath = getPath( this.getApplicationContext( ), selectedImage);

                txtbox = findViewById(R.id.resultview);
                loadingbar = findViewById(R.id.loadingsymbol);
                loadingbar.setVisibility(View.VISIBLE);
                new Thread(new Runnable() {
                    public void run() {
                        txtbox.setText("Please wait, your photo is being processed...");
                        Log.d("Threading", "user selected picture, starting processing!");
                        //processimage(picturePath);
                        processimage(byteArray);
                        }
                }).start();
                Log.d("timing","task is supposedly done, or does it run next step at the same time?");

            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                Toast exceptiontoast = Toast.makeText(this,"File not found!",Toast.LENGTH_SHORT);
                exceptiontoast.show();
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    //gets path of URI
    public static String getPath(Context context, Uri uri ) {
        String result = null;
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = context.getContentResolver( ).query( uri, proj, null, null, null );
        if(cursor != null){
            if ( cursor.moveToFirst( ) ) {
                int column_index = cursor.getColumnIndexOrThrow( proj[0] );
                result = cursor.getString( column_index );
            }
            cursor.close( );
        }
        if(result == null) {
            result = "Not found";
        }
        return result;
    }

    //processes image
    public void processimage(byte[] path_to_picture){
        Python py = Python.getInstance();
        PyObject pyfile = py.getModule("global_classifier");
        String result = pyfile.callAttr("classify_fake","weights/global.pth",path_to_picture).toString();
        TextView txtbox = findViewById(R.id.resultview);
        txtbox.setText("Probability of being Photoshopped: " + result.substring(0,5) + "%");
        Log.d("Result",result);
        loadingbar.post(new Runnable() {
            public void run() {
                loadingbar.setVisibility(View.INVISIBLE);
            }
        });
        running = false;
    }

}

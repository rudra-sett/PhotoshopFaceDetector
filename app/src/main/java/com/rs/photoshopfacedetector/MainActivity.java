package com.rs.photoshopfacedetector;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
//import com.android.vending.expansion.zipfile;
import com.chaquo.python.android.AndroidPlatform;
//import zip_file;
import com.android.vending.expansion.zipfile.*;

import java.io.*;
import java.util.Vector;
import java.util.zip.ZipFile;

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

    public byte[] readglobald(){
        try {
            ZipResourceFile weights = APKExpansionSupport.getAPKExpansionZipFile(this,3,0);
            /*String[] weights2 = getAPKExpansionFiles(this,1,0);
            Log.d("expansiontag",weights2[0]);*/
            if (weights!= null) {
                InputStream strem = weights.getInputStream("global.pth");
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[82546530]; //this is the exact size, in bytes, of the model
                /*while ((nRead = strem.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                byte[] globalweight = buffer.toByteArray();*/
                //This implementation requires much less RAM
                int offset = 0;
                int numRead = 0;
                while (offset < data.length
                        && (numRead=strem.read(data, offset, data.length-offset)) >= 0) {
                    offset += numRead;
                }
                strem.close();
                return data;
            }else{
                //Toast toast = Toast.makeText(this, "Please reinstall this app from the Play Store", Toast.LENGTH_LONG);
                //toast.show();
                return new byte[0];
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast toast = Toast.makeText(this, "Please reinstall this app from the Play Store", Toast.LENGTH_LONG);
            toast.show();
            return new byte[0];
        }

    }
    public byte[] readlocald(){
        try {
            ZipResourceFile weights = APKExpansionSupport.getAPKExpansionZipFile(this,3,0);
            /*String[] weights2 = getAPKExpansionFiles(this,1,0);
            Log.d("expansiontag",weights2[0]);*/
            if (weights!= null) {
                InputStream strem = weights.getInputStream("local.pth");
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                //Log.d("size",buffer.size());
                Log.d("size",Integer.toString(strem.available()));
                int nRead;
                byte[] data = new byte[247495840]; //this is the exact size, in bytes, of the model
                Log.d("checking","finished allocating the bytearray");
                int offset = 0;
                int numRead = 0;
                while (offset < data.length
                        && (numRead=strem.read(data, offset, data.length-offset)) >= 0) {
                    offset += numRead;
                }
                //byte [] globalweight  = strem.
                /*while ((nRead = strem.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                byte[] globalweight = buffer.toByteArray();*/

                strem.close();
                return data;
                //return globalweight;
            }else{
                //Toast toast = Toast.makeText(this, "Please reinstall this app from the Play Store", Toast.LENGTH_LONG);
                //toast.show();
                return new byte[0];
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast toast = Toast.makeText(this, "Please reinstall this app from the Play Store", Toast.LENGTH_LONG);
            toast.show();
            return new byte[0];
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
                RadioButton heat = findViewById(R.id.heatmaprb);
                RadioButton prob = findViewById(R.id.probabilityrb);
                boolean heaton = false;
                if (heat.isChecked()){
                    heaton = true;
                }
                final boolean finalHeaton = heaton;
                new Thread(new Runnable() {
                    public void run() {
                        txtbox.setText("Please wait, your photo is being processed...");
                        Log.d("Threading", "user selected picture, starting processing!");
                        if (finalHeaton){
                            classifylocal(byteArray);
                        }
                        if (!finalHeaton){
                            classifyglobal(byteArray);
                        }
                        //processimage(picturePath);
                        //classifyglobal(byteArray);
                        //classifylocal(byteArray);
                        }
                }).start();
                //Log.d("timing","task is supposedly done, or does it run next step at the same time?");

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
    public void classifyglobal(byte[] path_to_picture){
        Python py = Python.getInstance();
        PyObject pyfile = py.getModule("global_classifier");
        //String result = pyfile.callAttr("classify_fake","weights/global.pth",path_to_picture).toString();
        byte[] mod= readglobald();
        //byte[] mod = readlocald();
        if (mod.length < 100){
            //Looper.prepare();
            Log.d("expf","expansion is missing!");
            //Toast toast = Toast.makeText(this, "Please reinstall this app from the Play Store", Toast.LENGTH_LONG);
            //toast.show();
            return;
        }
        final String result = pyfile.callAttr("classify_fake",mod,path_to_picture).toString();

        loadingbar.post(new Runnable() {
            public void run() {
                loadingbar.setVisibility(View.INVISIBLE);
                TextView txtbox = findViewById(R.id.resultview);
                txtbox.setText("Probability of being Photoshopped: " + result.substring(0,5) + "%");
                Log.d("Result",result);
            }
        });
        running = false;
    }
    public void classifylocal(byte[] path_to_picture){
        Python py = Python.getInstance();
        PyObject pyfile = py.getModule("local_detector");
        //String result = pyfile.callAttr("classify_fake","weights/global.pth",path_to_picture).toString();
        byte[] mod = readlocald();
        if (mod.length < 100){
            //Looper.prepare();
            Log.d("expf","expansion is missing!");
            //Toast toast = Toast.makeText(this, "Please reinstall this app from the Play Store", Toast.LENGTH_LONG);
            //toast.show();
            return;
        }
        byte[] result = pyfile.callAttr("detect_changes",mod,path_to_picture).toJava(byte[].class);
        //result.
        final TextView txtbox = findViewById(R.id.resultview);
        //txtbox.setText("Probability of being Photoshopped: " + result.substring(0,5) + "%");
        //Log.d("Result",result);
        final ImageView pic = findViewById(R.id.pic);
        final Bitmap returned = BitmapFactory.decodeByteArray(result,0,result.length);
        loadingbar.post(new Runnable() {
            public void run() {
                loadingbar.setVisibility(View.INVISIBLE);
                pic.setImageBitmap(returned);
                txtbox.setText("");
            }
        });
        running = false;
    }

}

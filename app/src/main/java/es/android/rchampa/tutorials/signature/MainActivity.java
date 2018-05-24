package es.android.rchampa.tutorials.signature;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String SHARED_PROVIDER_AUTHORITY = BuildConfig.APPLICATION_ID + ".myfileprovider";
    private static final String PDF_FOLDER = "pdf";
    private static final String IMAGE_FOLDER = "images";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= 23) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        final SignatureView myCanvasView = findViewById(R.id.signature_view);
        // Request the full available screen for layout.
        myCanvasView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);

        Toolbar toolbar = findViewById(R.id.toolbar);

        toolbar.findViewById(R.id.bt_picture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveBitmapToGallery(myCanvasView.getBitmap());
            }
        });

        toolbar.findViewById(R.id.bt_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myCanvasView.clear();
            }
        });

        toolbar.findViewById(R.id.bt_pdf).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Bitmap bitmap = myCanvasView.getBitmap();

                PdfDocument document = new PdfDocument();
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), 1).create();
                PdfDocument.Page page = document.startPage(pageInfo);
                Canvas canvas = page.getCanvas();


                Paint paint = new Paint();
                paint.setColor(Color.parseColor("#ffffff"));
                canvas.drawPaint(paint);

                canvas.drawBitmap(bitmap, 0, 0 , null);
                document.finishPage(page);

                // write the document content

                File file = null;
                try {
                    file = createFile(PDF_FOLDER,".pdf");
                    document.writeTo(new FileOutputStream(file));
                } catch (IOException e) {
                    e.printStackTrace();
                    showToast("Something wrong: " + e.toString());
                }

                // close the document
                document.close();


                Uri uri = getUriFrom(file);

                Intent target = new Intent(Intent.ACTION_VIEW);
                target.setDataAndType(uri,"application/pdf");
                target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                target.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);


                Intent intent = Intent.createChooser(target, "Open File");
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    // Instruct the user to install a PDF reader here, or something
                }

            }
        });
    }

    @NonNull
    private Uri getUriFrom(File file){
        return FileProvider.getUriForFile(getApplicationContext(), SHARED_PROVIDER_AUTHORITY, file);
    }

    @NonNull
    private File createFile(String path, String extension) throws IOException {

        final File sharedFolder = new File(getFilesDir(), path);
        sharedFolder.mkdirs();

        final File sharedFile = new File(sharedFolder,getFilename()+extension);
        boolean created = sharedFile.createNewFile();

        return sharedFile;

    }

    private void saveBitmapToGallery(Bitmap bitmap){

        if(!StorageHelper.isExternalStorageReadableAndWritable()){
            showToast(R.string.error_gallery_not_available);
            return;
        }

        String filename = getFilename()+".png";
        String url = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, filename, "Firma creada desde Signature app");

        if(url==null){
            showToast(R.string.error_save_gallery);
            return;
        }

        Intent target = new Intent(Intent.ACTION_VIEW);
        target.setDataAndType(Uri.parse(url),"image/*");
        target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

        Intent intent = Intent.createChooser(target, "Open File");
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // Instruct the user to install a PDF reader here, or something
        }
    }

    private void showToast(String message){
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showToast(@StringRes int message_id){
        Toast.makeText(this, message_id, Toast.LENGTH_LONG).show();
    }

    private String getFilename(){
        return "firma-"+ System.currentTimeMillis();
    }

}

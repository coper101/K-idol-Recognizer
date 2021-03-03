package com.daryl.kidolrecognizer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.ImageAnalysisConfig;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.common.util.concurrent.ListenableFuture;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {

    private static String TAG = "MainActivity";

    private ExecutorService cameraExecutor;
    private Python python;

    private PreviewView viewFinder;
    private TextView textView, textView2;
    private ImageView ivBitmap;


    // Check OpenCV
    static {
        if (OpenCVLoader.initDebug())
            Log.d(TAG, "OpenCV installed successfully");
        else
            Log.d(TAG, "OpenCV not installed");
    }

    private final int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{
            "android.permission.CAMERA"
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request Camera Permission If Not Granted
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        cameraExecutor = Executors.newSingleThreadExecutor();

        // Views
        viewFinder = findViewById(R.id.viewFinder);
        textView = findViewById(R.id.textView);
        textView2 = findViewById(R.id.textView2);
        ivBitmap = findViewById(R.id.ivBitmap);

        // Check Python is Started
        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        python = Python.getInstance();

        // Access to Module
        final PyObject pyObject = python.getModule("myScript");

        PyObject objData = pyObject.callAttr("create_recognizer_and_labels");
        Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>> Recognizer and Labels Created?: " + objData.toString());


    } // -- end --

    private void startCamera() {

        // (1) Request Camera Provider
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            // (2) Check Camera Provider Availability
            ProcessCameraProvider cameraProvider = null;
            try {
                cameraProvider = cameraProviderFuture.get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // (3) Select Camera Bind Lifecycle and Uses Cases
            Preview preview = new Preview.Builder().build();
            CameraSelector cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
            preview.setSurfaceProvider(viewFinder.createSurfaceProvider());

            // Image Analyzer
            ImageAnalysis imageAnalyzer = new ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // non-blocking mode
                    .build();


            imageAnalyzer.setAnalyzer(cameraExecutor, image -> {
                Log.d(TAG, "Image Info: " + image.getImageInfo());

                // convert from bitmap to Matrix Frame
                Bitmap bitmapFrame = viewFinder.getBitmap();

                final PyObject pytObject = python.getModule("myScript");

//                PyObject imageShapeObj = pytObject.callAttr("image_shape", encodeBitmapImage(bitmapFrame));
//                Log.d(TAG, "type of bitmap ======================================== " + imageShapeObj.toString());

                PyObject detectFacesObj = pytObject.callAttr("recognize_face", encodeBitmapImage(bitmapFrame));
                setText2(detectFacesObj.toString());

                PyObject pathObj = pytObject.callAttr("create_path");
                Log.d(TAG, ">>>>>>>>>=================>>>>>>>>>>>>>>>>>>> File Path: " + pathObj.toString());

                Mat matFrame4 = new Mat();
                Mat matFrame3 = new Mat();

                if (bitmapFrame == null)
                    return;

                Utils.bitmapToMat(bitmapFrame, matFrame4);

                Imgproc.cvtColor(matFrame4, matFrame3, Imgproc.COLOR_BGRA2BGR);

                Mat detections = faceDetector(matFrame3);

                Log.d(TAG, "Mat: " + matFrame3.channels());

                // Extract faces detected matrix
                int detectionsCount = (int) (detections.total() / 7);
                Mat faceDetections = detections.reshape(1, detectionsCount);

                Log.d(TAG, "--------- Face Detections -------------" + matFrame3.channels());
                Log.d(TAG, "cols: " + faceDetections.cols());
                Log.d(TAG, "rows: " + faceDetections.rows());

                int width = matFrame3.cols();
                int height = matFrame3.rows();
                int fill = Core.LINE_4;
                Scalar color = new Scalar(0, 255, 0);
                // Draw box for each face detected
                boolean isDetectedHighConf = false;
                for (int i = 0; i < faceDetections.rows(); i++) {
                    // confidence
                    double confidence = faceDetections.get(i, 2)[0];
                    if (confidence > 0.50) {
                        // box axis
                        int x1 = (int) (faceDetections.get(i, 3)[0] * width);
                        int y1 = (int) (faceDetections.get(i, 4)[0] * height);
                        int x2 = (int) (faceDetections.get(i, 5)[0] * width);
                        int y2 = (int) (faceDetections.get(i, 6)[0] * height);
                        // draw box around face detected
                        // rectangle with color
                        Point pos1 = new Point(x1, y1);
                        Point pos2 = new Point(x2, y2);
                        Imgproc.rectangle(matFrame3, pos1, pos2, color, fill);
                        Log.d(TAG, ">>>>>>>>>>> x1y1: " + x1 + " " + y1);
                        Log.d(TAG, ">>>>>>>>>>> x1y1: " + x2 + " " + y2);

                        int[] points = {x1, y1, x2, y2};

                        final PyObject pyObject = python.getModule("myScript");
                        PyObject obj = pyObject.callAttr("box_axis", points);
                        setText("Face Detected\n" + obj.toString());

                        isDetectedHighConf = true;
                    }
                }
                if (!isDetectedHighConf) {
                    setText("No Face Detected");
                }


                Utils.matToBitmap(matFrame3, bitmapFrame);
                Log.d(TAG, "-------------------------New bitmap: " + bitmapFrame);

//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        ivBitmap.setImageBitmap(bitmapFrame);
//                    }
//                });

                image.close();
            });

            try {
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer);
            } catch (Exception exc) {
                Log.e(TAG, "Use case binding failed", exc);
            }
        }, ContextCompat.getMainExecutor(this));

    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            // start camera when granted
            if (allPermissionsGranted()) {
                startCamera();
                // exit app when not granted
            } else {
                Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        // image analysis is called again
        if (allPermissionsGranted()) {
            startCamera();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    private Mat faceDetector(Mat imgMatrix) {

        String prototxt = getPath("deploy.prototxt", this);
        String weights = getPath("res10_300x300_ssd_iter_140000.caffemodel", this);

        // Load Neural Network Model
        Net net = Dnn.readNetFromCaffe(prototxt, weights);

        // Resize Image
        Mat resizeImage = new Mat();
        Size scaleSize = new Size(300, 300);
        Imgproc.resize(imgMatrix, resizeImage, scaleSize , 0, 0, Imgproc.INTER_AREA);

        // Blob
        Size size = new Size(300, 300);
        Scalar mean = new Scalar(104.0, 177.0, 123.0);
        Mat blobImage = Dnn.blobFromImage(resizeImage, 1.0, size, mean);

        // Pass Blob to network
        net.setInput(blobImage);
        Mat detections = net.forward();

        return detections;
    }

    private static String getPath(String file, Context context) {
        AssetManager assetManager = context.getAssets();
        BufferedInputStream inputStream = null;
        try {
            // Read data from assets.
            inputStream = new BufferedInputStream(assetManager.open(file));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
            // Create copy file in storage.
            File outFile = new File(context.getFilesDir(), file);
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
            // Return a path to file which may be read in common way.
            return outFile.getAbsolutePath();
        } catch (IOException ex) {
            Log.i(TAG, "Failed to upload a file");
        }
        return "";
    }

    // Set the Text of View outside the main
    private void setText(String value){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(value);
            }
        });
    }

    private void setText2(String value){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView2.setText(value);
            }
        });
    }


    private String encodeBitmapImage(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        return android.util.Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }
}
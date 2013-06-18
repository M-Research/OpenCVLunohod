package org.opencv.samples.facedetect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.objdetect.CascadeClassifier;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

public class FdActivity extends Activity implements CvCameraViewListener2,
	OnPreparedListener,OnCompletionListener {

    private static final String    TAG                 = "OCVSample::Activity";
    private static final Scalar    FACE_RECT_COLOR     = new Scalar(0, 255, 0, 255);
    public static final int        JAVA_DETECTOR       = 0;
    public static final int        NATIVE_DETECTOR     = 1;

    private MenuItem               mItemFace50;
    private MenuItem               mItemFace40;
    private MenuItem               mItemFace30;
    private MenuItem               mItemFace20;
    private MenuItem               mItemType;

    private Mat                    mRgba;
    private Mat                    mGray,mGrayPrev,mGrayMotion;
    private File                   mCascadeFile;
    private CascadeClassifier      mJavaDetector;
    private DetectionBasedTracker  mNativeDetector;

    private int                    mDetectorType       = JAVA_DETECTOR;
    private String[]               mDetectorName;

    private float                  mRelativeFaceSize   = 0.2f;
    private int                    mAbsoluteFaceSize   = 0;

    private CameraBridgeViewBase   mOpenCvCameraView;
    MediaPlayer m = new MediaPlayer();
    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // Load native library after(!) OpenCV initialization
                    System.loadLibrary("mixed_sample");

                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

                        mNativeDetector = new DetectionBasedTracker(mCascadeFile.getAbsolutePath(), 0);

                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public FdActivity() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";
        mDetectorName[NATIVE_DETECTOR] = "Native (tracking)";

        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.face_detect_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
       // mOpenCvCameraView.setMaxFrameSize(1280, 400);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mGrayPrev = new Mat();
        mGrayMotion = new Mat();
        mRgba = new Mat();
    }

    public void onCameraViewStopped() {
        mGray.release();
        mGrayPrev.release();
        mGrayMotion.release();
        mRgba.release();
    }
    private int frameLRdecision = 0;
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();
        if(mGrayPrev.empty()) mGrayPrev = inputFrame.gray();
        mGray = inputFrame.gray();
        //mGrayMotion = inputFrame.gray();
        
       Core.absdiff(mGray, mGrayPrev, mGrayMotion);
       mGrayPrev = mGray.clone();
       
//       int pixel_count = 1;
//       int sum_x = 0;
//       int sum_y = 0;
//       
//       for ( int i = 0; i < mGrayMotion.width(); i++){
//           for (int u = 0; u < mGrayMotion.height(); u++)
//           {
//               // gets pixel color (gray-scale values ranging from 0 to 255 (=white)); 
//               // see above B
//        	   double[] vals = mGrayMotion.get(u, i);
//               // 100 is threshold; if color value is higher than 100, 
//               //pixel will be used for calculations
//               if( vals[0] > 20){
//                   pixel_count++;
//                   // sums up x and y coordinates of pixels identified
//                   sum_x += i;
//                   sum_y += u;
//               }
//           }
//       }
//       double mean_x = sum_x/pixel_count;
//       double mean_y = sum_y/pixel_count;
//      
//       if (mean_x < mGrayMotion.width() && mean_y < mGrayMotion.height())
//           // cvCircle(IplImage,CvPoint,int radius,CvColor,int thickness, int line_type, int shift);
//    	   Core.circle(mGray, new Point(mean_x,mean_y ),4,new Scalar(0,255,0,0), 2,8,0);
//       
       //  Mat mask = new Mat(mGrayMotion.rows(), mGrayMotion.cols(), CvType.CV_8U);
      //  Imgproc.cvtColor(mGrayMotion, mask, Imgproc.); //your conversion specifier may vary
      //  Imgproc.threshold(mask, mask, 0, 255, 1);
        
        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
            mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
        }

        MatOfRect faces = new MatOfRect();

        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector != null)
                mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
            	
        }
        else if (mDetectorType == NATIVE_DETECTOR) {
            if (mNativeDetector != null)
                mNativeDetector.detect(mGray, faces);
             	//mNativeDetector.diff(mGray, mGrayPrev, mGrayMotion);
        	
        }
        else {
            Log.e(TAG, "Detection method is not selected!");
        }

//        Rect[] facesArray = faces.toArray();
//        for (int i = 0; i < facesArray.length; i++)
//            Core.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);

        
        //FindFeatures(mGrayMotion.getNativeObjAddr(), mGray.getNativeObjAddr());
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.threshold(mGrayMotion, mGrayMotion, 100, 255, 0);
        Imgproc.findContours(mGrayMotion, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        Core.fillPoly(mRgba, contours, FACE_RECT_COLOR);
        
      double max_square = 2500;
      int conts_count = 0;
      Point center_full = new Point();
      MatOfPoint mt_max = null;
      Rect max_obj_rect = null;
        for(MatOfPoint mt:contours){
          Rect obj_rect = Imgproc.boundingRect(mt);
          double curr_ar = obj_rect.area();
          
          if(curr_ar > max_square) {
        	  max_square = curr_ar;
        	  mt_max = mt;
        	  max_obj_rect = obj_rect;
        	  center_full.x = center_full.x+obj_rect.x+obj_rect.width/2;
              center_full.y = center_full.y+obj_rect.y+obj_rect.height/2;
              conts_count++;
          }
          Core.rectangle(mRgba, obj_rect.tl(), obj_rect.br(), new Scalar(0, 0, 255, 0), 1);
        }
        Point center = null;
        if(max_obj_rect!= null)
        {
        	center = new Point();
        	center.x = max_obj_rect.x+max_obj_rect.width/2;
        	center.y = max_obj_rect.y+max_obj_rect.height/2;
        	
        	//Core.rectangle(mRgba, max_obj_rect.tl(), max_obj_rect.br(), new Scalar(0, 255, 0, 0), 3);
        	Core.circle(mRgba, center, 50, new Scalar(255,0 , 0, 0),-10);
        }
        if(conts_count!=0){
        	
        	center_full.x = center_full.x/conts_count;
        	center_full.y = center_full.y/conts_count;
        	Core.circle(mRgba, center_full, 40, new Scalar(200,100 , 100, 0),-20);
        }
        //Imgproc.ca
        if(center!=null && center_full!=null){
        	double xmean = (center_full.x+center.x)/2;
           // double ymean = (center_full.y+center.y)/2;
            if(xmean < mOpenCvCameraView.getWidth()/2){
            	if(frameLRdecision > -10)frameLRdecision--;
            }
            if(xmean > mOpenCvCameraView.getWidth()/2){
            	if(frameLRdecision < 10)frameLRdecision++;
            }
        }
        if(frameLRdecision == -10){
        	navigate(true);
        	frameLRdecision=0;
        }
        if(frameLRdecision == 10){
        	navigate(false);
        	frameLRdecision=0;
        }
        if(frameLRdecision>0){
         	Core.putText(mRgba, "I turn left", new Point(mOpenCvCameraView.getWidth()/5,mOpenCvCameraView.getHeight()*2/5), Core.FONT_HERSHEY_DUPLEX, 4, FACE_RECT_COLOR);
        }else{
        	Core.putText(mRgba, "I turn right", new Point(mOpenCvCameraView.getWidth()/5,mOpenCvCameraView.getHeight()*2/5), Core.FONT_HERSHEY_DUPLEX, 4, FACE_RECT_COLOR);
        }
        return mRgba;
    }
    public void navigate(final boolean left){
    			try{
    				
    				m.stop();
	 		        m.release();
    				m = new MediaPlayer();    
        		        AssetFileDescriptor descriptor1  = getAssets().openFd("rec2.wav");
        		        AssetFileDescriptor descriptor2  = getAssets().openFd("rec1.wav");
        		        
        		        if(left)
            	//	        m.setDataSource(Environment.getExternalStorageDirectory().getPath()+"left2.mp3");
  //      		        	descriptor = getAssets().openFd("left2.mp3");
        		        	m.setDataSource(descriptor1.getFileDescriptor(), descriptor1.getStartOffset(), descriptor1.getLength());
             		     else 
            		//        m.setDataSource(Environment.getExternalStorageDirectory().getPath()+"right2.mp3");
//        		        	descriptor = getAssets().openFd("right2.mp3");
             		    	m.setDataSource(descriptor2.getFileDescriptor(), descriptor2.getStartOffset(), descriptor2.getLength());
              		       
         		       ///m.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
        		        descriptor1.close();
        		        descriptor2.close();
        		        
        		        m.setOnPreparedListener(this);
        		        m.prepare();
        		        m.setVolume(1f, 1f);
        		        m.setLooping(false);
        		        m.setOnCompletionListener(this);
        		       
        		    } catch (Exception e) {
        		    	e.printStackTrace();
        		    }
    }

  
    	
    
    public native void FindFeatures(long matAddrGr, long matAddrRgba);
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemFace50 = menu.add("Face size 50%");
        mItemFace40 = menu.add("Face size 40%");
        mItemFace30 = menu.add("Face size 30%");
        mItemFace20 = menu.add("Face size 20%");
        mItemType   = menu.add(mDetectorName[mDetectorType]);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item == mItemFace50)
            setMinFaceSize(0.5f);
        else if (item == mItemFace40)
            setMinFaceSize(0.4f);
        else if (item == mItemFace30)
            setMinFaceSize(0.3f);
        else if (item == mItemFace20)
            setMinFaceSize(0.2f);
        else if (item == mItemType) {
            mDetectorType = (mDetectorType + 1) % mDetectorName.length;
            item.setTitle(mDetectorName[mDetectorType]);
            setDetectorType(mDetectorType);
        }
        return true;
    }

    private void setMinFaceSize(float faceSize) {
        mRelativeFaceSize = faceSize;
        mAbsoluteFaceSize = 0;
    }

    private void setDetectorType(int type) {
        if (mDetectorType != type) {
            mDetectorType = type;

            if (type == NATIVE_DETECTOR) {
                Log.i(TAG, "Detection Based Tracker enabled");
                mNativeDetector.start();
            } else {
                Log.i(TAG, "Cascade detector enabled");
                mNativeDetector.stop();
            }
        }
    }

	@Override
	public void onPrepared(MediaPlayer paramMediaPlayer) {
		//paramMediaPlayer.start();
		m.start();
	}

	@Override
	public void onCompletion(MediaPlayer paramMediaPlayer) {
//		m.stop();
//		m.release();
	}
}

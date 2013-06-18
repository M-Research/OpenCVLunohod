package org.opencv.samples.facedetect;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;

public class DetectionBasedTracker
{
    public DetectionBasedTracker(String cascadeName, int minFaceSize) {
     //   mNativeObj = nativeCreateObject(cascadeName, minFaceSize);
    }

    public void start() {
     //   nativeStart(mNativeObj);
    }

    public void stop() {
       // nativeStop(mNativeObj);
    }

    public void setMinFaceSize(int size) {
       // nativeSetFaceSize(mNativeObj, size);
    }

    public void detect(Mat imageGray, MatOfRect faces) {
      //  nativeDetect(mNativeObj, imageGray.getNativeObjAddr(), faces.getNativeObjAddr());
    }
    public void diff(Mat imageGray1, Mat imageGray2,Mat imageGrayRes) {
      //  absdiff(imageGray1.getNativeObjAddr(),imageGray2.getNativeObjAddr(),imageGrayRes.getNativeObjAddr());
    }

    public void release() {
       // nativeDestroyObject(mNativeObj);
        mNativeObj = 0;
    }

    private long mNativeObj = 0;

//    private static native long nativeCreateObject(String cascadeName, int minFaceSize);
//    private static native void nativeDestroyObject(long thiz);
//    private static native void nativeStart(long thiz);
//    private static native void nativeStop(long thiz);
//    private static native void nativeSetFaceSize(long thiz, int size);
//    private static native void nativeDetect(long thiz, long inputImage, long faces);
//    private static native void absdiff(long inputImage1, long inputImage2, long resultImg);
//    
}

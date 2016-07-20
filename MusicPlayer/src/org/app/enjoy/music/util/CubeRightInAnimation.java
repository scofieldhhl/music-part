package org.app.enjoy.music.util;

import android.graphics.Camera;
import android.graphics.Matrix;
import android.view.animation.Animation;
import android.view.animation.Transformation;

/**
 * Created by Administrator on 2016/5/10.
 */
public class CubeRightInAnimation extends Animation {
    private Camera mCamera;
    private Matrix mMatrix;
    private int mWidth;
    private int mHeight;
    private static final int sFinalDegree = 80;

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        super.initialize(width, height, parentWidth, parentHeight);
        mCamera = new Camera();
        mMatrix = new Matrix();
        mWidth = width;
        mHeight = height;
    }


    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        super.applyTransformation(interpolatedTime, t);

        float rotate = (-sFinalDegree * interpolatedTime);
        mCamera.save();
//        mCamera.translate(interpolatedTime * mWidth / 2, 0, 0);
		mCamera.translate(interpolatedTime * mWidth / 3, 0, 0);
        mCamera.rotateY(rotate / 2);
        mCamera.getMatrix(mMatrix);
        mCamera.restore();

        mMatrix.postTranslate(0, mHeight / 2);
        mMatrix.preTranslate(-mWidth, -mHeight / 2);

        t.getMatrix().postConcat(mMatrix);
    }
}
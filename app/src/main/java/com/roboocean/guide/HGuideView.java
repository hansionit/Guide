package com.roboocean.guide;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import java.util.ArrayList;

/**
 * Description：
 * Author: Hansion
 * Time: 2017/6/13 10:17
 */
public class HGuideView extends View {

    //高亮形状：矩形、圆形、椭圆形
    public static final int VIEWSTYLE_RECT = 0;
    public static final int VIEWSTYLE_CIRCLE = 1;
    public static final int VIEWSTYLE_OVAL = 2;


    private Activity mActivity;
    // 屏幕宽高
    private int screenW, screenH;
    //高亮目标view集合
    private ArrayList<View> targetViews;
    //activity的contentview
    private View rootView;
    //蒙版Bitmap
    private Bitmap fgBitmap;
    //蒙版画布
    private Canvas mCanvas;
    //蒙版画笔
    private Paint mPaint;
    //引导层关闭的监听
    private OnDismissListener onDismissListener;
    //圆形高亮区域的半径
    private int radius;

    //-------------------------------- 可定义属性 ---------------------------------------------
    // 高亮边缘阴影扩散值
    private int blurRadius = 20;
    // 蒙版颜色
    private int maskColor = 0x99000000;
    //外部点击是否可关闭
    private boolean touchOutsideCancel = true;
    //高亮形状
    private int highLightShape = VIEWSTYLE_CIRCLE;
    // 高亮区域padding
    private int highLisghtPadding = 0;
    // 高亮区域边框宽度
    private int borderWitdh = 10;
    private RectF mRectf;

    //-------------------------------- 公开方法 ---------------------------------------------
    public static HGuideView builder(Activity activity) {
        return new HGuideView(activity);
    }


    /**
     * 设置需要高亮的View
     * @param targetView
     */
    public HGuideView addHighLightGuidView(View targetView) {
        try {
            targetViews.add(targetView);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }


    /**
     * 设置高亮边缘阴影扩散值
     *
     * @param blurRadius
     */
    public HGuideView setBlurRadius(int blurRadius) {
        this.blurRadius = blurRadius;
        return this;
    }


    /**
     * 设置蒙版颜色
     *
     * @param bgColor
     */
    public HGuideView setMaskColor(int bgColor) {
        try {
            this.maskColor = ContextCompat.getColor(getContext(), bgColor);
            // 重新绘制蒙版
            mCanvas.drawColor(maskColor);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }


    /**
     * 设置外部是否关闭，默认关闭
     *
     * @param cancel
     */
    public HGuideView setTouchOutsideDismiss(boolean cancel) {
        this.touchOutsideCancel = cancel;
        return this;
    }


    /**
     * 设置引导层关闭监听
     *
     * @param listener
     */
    public HGuideView setOnDismissListener(OnDismissListener listener) {
        this.onDismissListener = listener;
        return this;
    }

    /**
     * 设置边框宽度
     * @param borderWidth
     */
    public HGuideView setBorderWidth(int borderWidth) {
        this.borderWitdh = borderWidth;
        return this;
    }

    /**
     * 设置高亮区域padding 默认为0
     * @param highLisghtPadding
     */
    public HGuideView setHighLisghtPadding(int highLisghtPadding) {
        this.highLisghtPadding = highLisghtPadding;
        return this;
    }


    /**
     * 设置高亮区域形状
     * @param style
     */
    public HGuideView setHighLightStyle(int style) {
        this.highLightShape = style;
        return this;
    }



    /**
     * 显示引导层
     */
    public void show() {
        if (rootView != null) {
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams
                    (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            ((ViewGroup) rootView).addView(this, ((ViewGroup) rootView).getChildCount(), lp);
        }
    }





    //-------------------------------- 私有方法 ---------------------------------------------

    /**
     * 构造方法：
     * 根据传入的activity对象，获取屏幕尺寸
     *
     * @param activity
     */
    private HGuideView(Activity activity) {
        super(activity);
        mActivity = activity;
        getScreenSize();
        initAll();
    }

    /**
     * 获取屏幕尺寸
     */
    private void getScreenSize() {
        DisplayMetrics metrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int[] screenSize = new int[]{metrics.widthPixels, metrics.heightPixels};

        // 获取屏幕宽高
        screenW = screenSize[0];
        screenH = screenSize[1];
    }

    private void initAll() {
        mRectf = new RectF();
        targetViews = new ArrayList<>();
        rootView = ((Activity) getContext()).findViewById(android.R.id.content);

        // 实例化画笔并开启其抗锯齿和抗抖动
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        // 设置画笔透明度为0,视觉上达到“高亮”的效果
        mPaint.setARGB(0, 255, 0, 0);
        // 设置混合模式为DST_IN
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        mPaint.setMaskFilter(new BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.SOLID));
        // 生成前景图Bitmap
        fgBitmap = Bitmap.createBitmap(screenW, screenH, Bitmap.Config.ARGB_4444);
        // 将其注入画布
        mCanvas = new Canvas(fgBitmap);
        // 绘制前景画布颜色
        mCanvas.drawColor(maskColor);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //如果没设置目标View,不做任何绘制操作
        if (targetViews == null || targetViews.size() < 1) {
            return;
        }

        //从(0,0)坐标开始,绘制蒙版
        canvas.drawBitmap(fgBitmap, 0, 0, null);

        //遍历目标View集合,将其高亮显示。之所以遍历是为了满足同时设置多个目标View的需求
        //我们需要根据目标View的坐标和宽高来绘制高亮区域
        for (int i = 0; i < targetViews.size(); i++) {
            //获取获取高亮View坐标
            int left = 0;
            int top = 0;
            int right = 0;
            int bottom = 0;

            try {
                Rect rtLocation = getLocationInView(((ViewGroup) mActivity.findViewById(Window.ID_ANDROID_CONTENT)).getChildAt(0), targetViews.get(i));
                left = rtLocation.left;
                top = rtLocation.top;
                right = rtLocation.right;
                bottom = rtLocation.bottom;
            } catch (Exception e) {
                e.printStackTrace();
            }

            //获取到目标View宽高
            int vWidth = targetViews.get(i).getWidth();
            int vHeight = targetViews.get(i).getHeight();

            //根据设置的高亮形状绘制高亮区域
            switch (highLightShape) {
                case VIEWSTYLE_OVAL:    //椭圆形
                    mRectf.set(
                            left - highLisghtPadding,
                            top - highLisghtPadding,
                            right + highLisghtPadding,
                            bottom + highLisghtPadding);
                    mCanvas.drawOval(mRectf, mPaint);
                    break;
                case VIEWSTYLE_RECT:    //矩形
                    mRectf.set(
                            left - borderWitdh - highLisghtPadding,
                            top - borderWitdh - highLisghtPadding,
                            right + borderWitdh + highLisghtPadding,
                            bottom + borderWitdh + highLisghtPadding);
                    mCanvas.drawRoundRect(mRectf, 20, 20, mPaint);
                    break;
                case VIEWSTYLE_CIRCLE:  //圆形(默认)
                default:
                    radius = vWidth > vHeight ? vWidth / 2 + highLisghtPadding / 2 : vHeight / 2 + highLisghtPadding / 2;
                    if (radius < 50) {
                        radius = 100;
                    }
                    mCanvas.drawCircle(left + vWidth / 2, top + vHeight / 2, radius, mPaint);
                    break;

            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP://
                if (touchOutsideCancel) {
                    this.setVisibility(View.GONE);
                    //移除view
                    if (rootView != null) {
                        ((ViewGroup) rootView).removeView(this);
                    }
                    //返回监听
                    if (this.onDismissListener != null) {
                        onDismissListener.onDismiss();
                    }
                    return true;
                }
                break;
        }
        return true;
    }

    private Rect getLocationInView(View parent, View child) {
        if (child == null || parent == null) {
            throw new IllegalArgumentException("parent and child can not be null .");
        }
        View decorView = null;
        Context context = child.getContext();
        if (context instanceof Activity) {
            decorView = ((Activity) context).getWindow().getDecorView();
        }
        Rect result = new Rect();
        Rect tmpRect = new Rect();

        View tmp = child;

        if (child == parent) {
            child.getHitRect(result);
            return result;
        }
        while (tmp != decorView && tmp != parent) {
            //找到控件占据的矩形区域的矩形坐标
            tmp.getHitRect(tmpRect);
            if (!tmp.getClass().equals("NoSaveStateFrameLayout")) {
                result.left += tmpRect.left;
                result.top += tmpRect.top;
            }
            tmp = (View) tmp.getParent();
        }
        result.right = result.left + child.getMeasuredWidth();
        result.bottom = result.top + child.getMeasuredHeight();
        return result;
    }


    public interface OnDismissListener {
        void onDismiss();
    }

}

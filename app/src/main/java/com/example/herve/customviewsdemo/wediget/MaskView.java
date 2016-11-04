package com.example.herve.customviewsdemo.wediget;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.view.Window;

import com.example.herve.customviewsdemo.R;

/**
 * 用于提示用户的遮罩，可以设置包裹的View 或者根据坐标位置来设置包裹区域。
 * <p>
 * 可以通过XML的自定义命名空间来设置属性，也可以通过代码来设置
 * <p>
 * 目前还不是非常的完善，须后继续完善多View包裹的队列和提示指示箭头
 * <p>
 * 注意：
 * values 目录下还有 attrs属性
 * <declare-styleable name="maskView">
 * <attr name="hollow_width" format="dimension"/>
 * <attr name="hollow_height" format="dimension"/>
 * <attr name="hollow_margin_left" format="dimension"/>
 * <attr name="hollow_margin_right" format="dimension"/>
 * <attr name="hollow_margin_top" format="dimension"/>
 * <attr name="hollow_src" format="reference"/>
 * <attr name="hollow_clear_square" format="boolean"/>
 * <attr name="hollow_alpha" format="float"/>
 * <attr name="hollow_mask_color" format="color"/>
 * <attr name="hollow_alignParentBottom" format="boolean"/>
 * <attr name="hollow_alignParentRight" format="boolean"/>
 * <attr name="hollow_alignParentCenter" format="boolean"/>
 * <attr name="hollow_centerVertical" format="boolean"/>
 * <attr name="hollow_centerHorizontal" format="boolean"/>
 * <attr name="hollow_mask_orientation">
 * <enum name="below" value="1"/>
 * <enum name="above" value="2"/>
 * </attr>
 * </declare-styleable>
 */

public class MaskView extends View {


    private final String TAG = getClass().getSimpleName();

    private Context mContext;

    /*背景绘制器*/
    Paint backGroundPaint;
    /*镂空绘制器*/
    Paint cleanSquarePaint;
    /*三角区域绘制器*/
    Paint clearDrawPaint;
    /*虚线绘制器*/
    Paint dashedLinePaint;

    private boolean showMask = true;

    /*默认资源图片*/
    int tipBitmapRes = -1;

    /*位置屬性值*/

    /*镂空部分的宽度*/
    private int childViewWidth = 0;
    /*镂空部分的高度*/
    private int childViewHeight = 0;
    /*镂空部分的左间距*/
    private float hollow_margin_left = 0;
    /*镂空部分的右间距*/
    private float hollow_margin_right = 0;
    /*镂空部分的上间距*/
    private float hollow_margin_top = 0;
    /*镂空的下间距*/
    private float hollow_margin_bottom = 0;
    /*是否清空镂空部分，布局预览效果*/
    private boolean hollow_clear_square = true;
    /*遮罩背景的透明值*/
    private float maskView_hollow_alpha = 0;
    /*遮罩背景颜色*/
    private int hollow_mask_color = 0;

    private boolean alignParentBottom = false;
    private boolean alignParentRight = false;

    private boolean centerVertical = false;
    private boolean centerHorizontal = false;

    /**
     * 绘制虚线区域
     */
    private RectF roundRect;

    private float roundRx = 10;
    private float roundRY = 10;

    /**
     * 指示箭头方向
     */
    private int hollow_mask_orientation = 1;


    public MaskView(Context context) {
        this(context, null);
    }

    public MaskView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);

    }

    public MaskView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.maskView);
        childViewWidth = typedArray.getDimensionPixelSize(R.styleable.maskView_hollow_width, -1);
        childViewHeight = typedArray.getDimensionPixelSize(R.styleable.maskView_hollow_height, -1);

        hollow_margin_top = typedArray.getDimension(R.styleable.maskView_hollow_margin_top, 10);
        hollow_margin_left = typedArray.getDimension(R.styleable.maskView_hollow_margin_left, 10);
        hollow_margin_right = typedArray.getDimension(R.styleable.maskView_hollow_margin_right, 10);

        maskView_hollow_alpha = typedArray.getFloat(R.styleable.maskView_hollow_alpha, 0.5f);
        hollow_mask_color = typedArray.getColor(R.styleable.maskView_hollow_mask_color, Color.BLACK);
        hollow_mask_orientation = typedArray.getInt(R.styleable.maskView_hollow_mask_orientation, 1);
        hollow_clear_square = typedArray.getBoolean(R.styleable.maskView_hollow_clear_square, true);
        tipBitmapRes = typedArray.getResourceId(R.styleable.maskView_hollow_src, R.mipmap.ic_launcher);

        /*控制位置的信息*/
        alignParentBottom = typedArray.getBoolean(R.styleable.maskView_hollow_alignParentBottom, false);
        alignParentRight = typedArray.getBoolean(R.styleable.maskView_hollow_alignParentRight, false);

        centerVertical = typedArray.getBoolean(R.styleable.maskView_hollow_centerVertical, false);
        centerHorizontal = typedArray.getBoolean(R.styleable.maskView_hollow_centerHorizontal, false);

        typedArray.recycle();

        init(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMySize(100, widthMeasureSpec);
        int height = getMySize(100, heightMeasureSpec);


        setMeasuredDimension(width, height);
    }

    private int getMySize(int defaultSize, int measureSpec) {
        int mySize = defaultSize;

        int mode = MeasureSpec.getMode(measureSpec);
        int size = MeasureSpec.getSize(measureSpec);
        switch (mode) {
            case MeasureSpec.UNSPECIFIED: {//如果没有指定大小，就设置为默认大小
                mySize = defaultSize;
                break;
            }
            case MeasureSpec.AT_MOST: {//如果测量模式是最大取值为size
                //我们将大小取最大值,你也可以取其他值
                mySize = size;
                break;
            }
            case MeasureSpec.EXACTLY: {//如果是固定的大小，那就不要去改变它
                mySize = size;
                break;
            }
        }
        return mySize;
    }


    public void init(Context context) {
        this.mContext = context;
        setWillNotDraw(false);
    }


    private Canvas mCanvas;


    private boolean drawOnlyDrawBackground = false;

    public void setDrawOnlyDrawBackground(boolean drawOnlyDrawBackground) {
        this.drawOnlyDrawBackground = drawOnlyDrawBackground;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!showMask) {
            return;
        }
        mCanvas = canvas;


        getViewWH();

        if (roundRect == null) {
            initRoundRectDraftData();
        }

        float rectHeight = roundRect.height();

        float rectWidth = roundRect.width();

        if (alignParentBottom) {
            alignParentBottom = false;
            roundRect.top = getHeight() - rectHeight + hollow_margin_top;
            roundRect.bottom = getHeight() + hollow_margin_top;
        }

        if (alignParentRight) {
            alignParentRight = false;
            roundRect.right = getWidth() + hollow_margin_left - hollow_margin_right;
            roundRect.left = roundRect.right - rectWidth;

        }

        if (centerHorizontal) {
            centerHorizontal = false;
            roundRect.left = getWidth() / 2 - rectWidth / 2 + hollow_margin_left - hollow_margin_right;
            roundRect.right = roundRect.left + rectWidth;
        }

        if (centerVertical) {
            centerVertical = false;
            roundRect.top = getHeight() / 2 - rectHeight / 2 + hollow_margin_top - hollow_margin_bottom;
            roundRect.bottom = roundRect.top + rectHeight;
        }

        //创建一个图层，在图层上演示图形混合后的效果
        int sc = canvas.saveLayer(0, 0, getWidth(), getHeight(), null, Canvas.MATRIX_SAVE_FLAG |
                Canvas.CLIP_SAVE_FLAG |
                Canvas.HAS_ALPHA_LAYER_SAVE_FLAG |
                Canvas.FULL_COLOR_LAYER_SAVE_FLAG |
                Canvas.CLIP_TO_LAYER_SAVE_FLAG);

        int save = canvas.save(Canvas.MATRIX_SAVE_FLAG |
                Canvas.CLIP_SAVE_FLAG |
                Canvas.HAS_ALPHA_LAYER_SAVE_FLAG |
                Canvas.FULL_COLOR_LAYER_SAVE_FLAG |
                Canvas.CLIP_TO_LAYER_SAVE_FLAG);

        // 创建背景
        drawBackground();

        if (drawOnlyDrawBackground) {
            return;
        }

        //擦拭除所需区域

        if (hollow_clear_square) {
            cleanSquare(roundRect);
        }
        /*画虚线*/
        drawdashedLine();

        /*指示箭头部分*/

        drawDeltaView();


        /*画Text说明*/
        drawDetailBitmap();

        // 还原画布
        canvas.restoreToCount(save);
    }


    private void drawDeltaView() {


        Path pathDr = new Path();
        float floatW = roundRect.width();
        float floatH = roundRect.height();

        float floatLeft = roundRect.left;
        float floatTop = roundRect.top;


        float x1 = floatW / 4 + floatLeft;
        float y1 = floatH + floatTop;


        float x2 = roundRect.left + floatW / 4 + px2dip(mContext, 80);
        float y2 = floatH + px2dip(mContext, 60) + floatTop;

        float x3 = floatW / 4 + floatH / 8 + floatLeft + px2dip(mContext, 80);
        float y3 = floatH + floatTop;

        if (hollow_mask_orientation == 2) {
            y1 = floatTop;
            y2 = floatTop - px2dip(mContext, 100);
            y3 = floatTop;
        }

        /*清除一部分虚线*/
        if (hollow_clear_square) {
            pathDr.moveTo(x1, y1);// 此点为多边形的起点
            pathDr.lineTo(x2, y2);
            pathDr.lineTo(x3, y3);
            cleanDraw(pathDr);
        }


        Path pathDr2 = new Path();
        pathDr2.moveTo(x1, y1);// 此点为多边形的起点
        pathDr2.lineTo(x2, y2);
        pathDr2.lineTo(x3, y3);

        /*说明箭头*/
        draView(pathDr2);


    }

    /**
     * 1 为TiP箭头朝下
     * 2 为TiP箭头朝上
     */
    public void setHollow_mask_orientation(int hollow_mask_orientation) {
        this.hollow_mask_orientation = hollow_mask_orientation;
    }

    /**
     * Bitmap缩小的方法
     */
    private static Bitmap small(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postScale(0.8f, 0.8f, bitmap.getWidth() / 2, bitmap.getHeight() / 2); //长和宽放大缩小的比例
        Bitmap resizeBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return resizeBmp;
    }


    private float bitmapMarginLeft = 0;
    private float bitmapMarginTop = 0;

    public void setBitmapMarginLeft(float bitmapMarginLeft) {
        this.bitmapMarginLeft = bitmapMarginLeft;
    }

    public void setBitmapMarginTop(float bitmapMarginTop) {
        this.bitmapMarginTop = bitmapMarginTop;
    }

    private void drawDetailBitmap() {


        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), tipBitmapRes);
//        bitmap = small(bitmap);

        float floatW = roundRect.width();
        float floatH = roundRect.height();
        float floatLeft = roundRect.left;
        float floatTop = roundRect.top;

        float x2 = 0;
        float left = 0;
        switch (explainDrawablgravity) {
            case GRAVITY_LEFT:

                left = floatLeft;

                break;
            case GRAVITY_RIGHT:

                left = roundRect.right - bitmap.getWidth();

                break;
            case GRAVITY_CENTER:
                x2 = floatW / 4 + px2dip(mContext, 80) + floatLeft;
                left = x2 - bitmap.getWidth() / 2;
                if (left < roundRect.left) {
                    left = roundRect.left;
                }
                break;
            default:
                x2 = floatW / 4 + px2dip(mContext, 80) + floatLeft;
                left = x2 - bitmap.getWidth() / 2;

                break;
        }


        float top = floatH + px2dip(mContext, 100) + floatTop + 10;

        if (hollow_mask_orientation == 2) {
            top = floatTop - px2dip(mContext, 100) - bitmap.getHeight();
        }

        mCanvas.drawBitmap(bitmap, left + bitmapMarginLeft, top + bitmapMarginTop, null);

    }


    public static final int GRAVITY_LEFT = 0;
    public static final int GRAVITY_RIGHT = 1;
    public static final int GRAVITY_CENTER = 2;
    public int explainDrawablgravity = 2;

    public void setExplainDrawablleAlign(int gravity) {
        explainDrawablgravity = gravity;

    }

    private void draView(Path pathDr) {
        mCanvas.drawPath(pathDr, dashedLinePaint);

    }

    private void cleanDraw(Path pathDr) {
        if (clearDrawPaint == null) {
            clearDrawPaint = new Paint();
            PorterDuffXfermode clearMode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
            clearDrawPaint.setXfermode(clearMode);
            clearDrawPaint.setStrokeWidth(4);//设置画笔宽度
            clearDrawPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            clearDrawPaint.setColor(Color.TRANSPARENT);
            clearDrawPaint.setAntiAlias(true); // 是否抗锯齿
        }

        pathDr.close();

        mCanvas.drawPath(pathDr, clearDrawPaint);

    }

    private void drawdashedLine() {
        if (dashedLinePaint == null) {
            dashedLinePaint = new Paint();
            dashedLinePaint.setStyle(Paint.Style.STROKE);//设置画笔style空心
            dashedLinePaint.setColor(Color.WHITE);
            dashedLinePaint.setStrokeWidth(4);//设置画笔宽度
            dashedLinePaint.setAntiAlias(true); // 是否抗锯齿

            PathEffect effect = new DashPathEffect(new float[]{30, 20, 30, 20}, 1);
            dashedLinePaint.setPathEffect(effect);
        }

        mCanvas.drawRoundRect(roundRect, roundRx, roundRY, dashedLinePaint);
    }

    private void cleanSquare(RectF roundRect) {
        /*
        PorterDuffXfermode  这是一个非常强大的转换模式，使用它，可以使用图像合成的16条Porter-Duff规则的任意一条来控制Paint如何与已有的Canvas图像进行交互。
        */

        if (cleanSquarePaint == null) {
            cleanSquarePaint = new Paint();
            PorterDuffXfermode mode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
            cleanSquarePaint.setXfermode(mode);
            cleanSquarePaint.setColor(Color.TRANSPARENT);
            cleanSquarePaint.setStyle(Paint.Style.FILL_AND_STROKE);
            //设置笔刷的样式，默认为BUTT，如果设置为ROUND(圆形),SQUARE(方形)，需要将填充类型Style设置为STROKE或者FILL_AND_STROKE
            cleanSquarePaint.setStrokeCap(Paint.Cap.SQUARE);
            //设置画笔的结合方式
            cleanSquarePaint.setStrokeJoin(Paint.Join.ROUND);
        }

        mCanvas.drawRoundRect(roundRect, 10, 10, cleanSquarePaint);
    }

    private void drawBackground() {

        if (backGroundPaint == null) {
            backGroundPaint = new Paint();
            backGroundPaint.setStyle(Paint.Style.FILL_AND_STROKE);//设置填满
            backGroundPaint.setAntiAlias(true); // 是否抗锯齿
        }

        backGroundPaint.setColor(hollow_mask_color);// 设置黑色

        backGroundPaint.setAlpha((int) (maskView_hollow_alpha * 255));

        RectF backRectF = new RectF(0, 0, getWidth(), getHeight());

        mCanvas.drawRect(backRectF, backGroundPaint);
    }

    private void initRoundRectDraftData() {

        roundRect = new RectF(hollow_margin_left - hollow_margin_right, hollow_margin_top, childViewWidth - hollow_margin_right + hollow_margin_left, childViewHeight + hollow_margin_top);
        roundRx = 10;
        roundRY = 10;
    }


    private void getViewWH() {
            /*获取默认宽高*/
        if (childViewWidth == -1) {
            childViewWidth = getWidth();
        }
        if (childViewHeight == -1) {
            childViewHeight = 500;
        }

    }

    /*包裹指定的View*/
    public void setFillView(@NonNull Activity activity, View fillView) {
        if (activity == null) {
            new RuntimeException("fillView must have  Activity Context");
        }
        childViewWidth = fillView.getWidth();
        childViewHeight = fillView.getHeight();

        if (roundRect == null) {
            roundRect = new RectF();
        }
        int stateBarHeight = getBarHeight(activity);

        Rect viewRect = new Rect();

        fillView.getGlobalVisibleRect(viewRect);


//        roundRect.left = viewRect.left + hollow_margin_left;
//        roundRect.top = viewRect.top + hollow_margin_top;
//        roundRect.right = viewRect.right - hollow_margin_right;
//        roundRect.bottom = viewRect.bottom - hollow_margin_bottom;
//
//
//
        roundRect.left = viewRect.left + hollow_margin_left;
        roundRect.top = viewRect.top + hollow_margin_top - stateBarHeight;
        roundRect.right = viewRect.right - hollow_margin_right;
        roundRect.bottom = viewRect.bottom - hollow_margin_bottom - stateBarHeight;


    }

    /*包裹指定的View*/
    public void setFillView(View fillView) {

        if (fillView.getContext() instanceof Activity) {
            setFillView((Activity) fillView.getContext(), fillView);
        } else {
            new RuntimeException("fillView.getContext() must instanceof Activity or you  can  use setFillView(Activity,fillView)");
        }

    }

    /*获取Activity界面的状态栏+标题栏的高度*/
    public int getBarHeight(Activity act) {
//        Rect frame = new Rect();
//        act.getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
//        状态栏高度
//        int statusBarHeight = frame.top;
//        Log.i(TAG, "getBarHeight: statusBarHeight=" + statusBarHeight);
//

//      *状态栏高度 + 标题栏高度
        Rect outRect2 = new Rect();
        act.getWindow().findViewById(Window.ID_ANDROID_CONTENT).getGlobalVisibleRect(outRect2);
        int titleBarHeight = outRect2.top;

        return titleBarHeight;
    }

    /**
     * @param width       矩形的宽信息
     * @param height      矩形的高位置信息
     * @param marginTop   圆角
     * @param marginLeft  圆角
     * @param marginRight 圆角
     */
    public void setRoundRect(int width, int height, float marginTop, float marginLeft, float marginRight) {


        hollow_margin_top = marginTop;
        hollow_margin_left = marginLeft;
        hollow_margin_right = marginRight;

        roundRect = new RectF();

        roundRect.left = hollow_margin_left;
        roundRect.top = hollow_margin_top;
        roundRect.right = width - hollow_margin_right;
        roundRect.bottom = height - hollow_margin_top;


    }

    public void setRoundRect(int width, int height, float left, float top, float right, float bottom) {

        childViewWidth = width;
        childViewHeight = height;

        if (roundRect == null) {
            roundRect = new RectF();
        }

        roundRect.left = left + hollow_margin_left;
        roundRect.top = top + hollow_margin_top;
        roundRect.right = right - hollow_margin_right;
        roundRect.bottom = bottom - hollow_margin_bottom;

    }

    public RectF getRoundRect() {
        return roundRect;
    }

    /**
     * @param width  矩形的宽信息
     * @param height 矩形的高位置信息
     */
    public void setRoundRect(int width, int height) {


        roundRect.right = width - hollow_margin_right;
        roundRect.bottom = height + hollow_margin_top;

        hollow_margin_left = roundRect.left;


    }

    public void setRoundRxy(float roundRx, float roundRY) {
        this.roundRx = roundRx;
        this.roundRY = roundRY;
    }

    public void setRoundRx(float roundRx) {
        this.roundRx = roundRx;
    }

    public void setRoundRy(float roundRY) {
        this.roundRY = roundRY;
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public boolean isShowMask() {
        return showMask;
    }


    public void showMask() {
        showMask = true;
        invalidate();
        setVisibility(VISIBLE);

    }

    /*用某个资源图片来显示遮罩*/
    public void showMask(@DrawableRes int tipBitmapRes) {

        this.tipBitmapRes = tipBitmapRes;
        showMask = true;
        invalidate();
        setVisibility(VISIBLE);

    }

    /*设置遮罩*/
    public void setTipBitmapRes(@DrawableRes int tipBitmapRes) {
        this.tipBitmapRes = tipBitmapRes;
    }

    public void hindMask() {
        showMask = false;
        invalidate();
        setVisibility(GONE);
    }


    @Override
    public void invalidate() {
        super.invalidate();
    }


    public float getHollow_margin_left() {
        return hollow_margin_left;
    }

    public void setHollow_margin_left(float hollow_margin_left) {
        this.hollow_margin_left = hollow_margin_left;
    }

    public float getHollow_margin_right() {
        return hollow_margin_right;
    }

    public void setHollow_margin_right(float hollow_margin_right) {
        this.hollow_margin_right = hollow_margin_right;
    }

    public float getHollow_margin_top() {
        return hollow_margin_top;
    }

    public void setHollow_margin_top(float hollow_margin_top) {
        this.hollow_margin_top = hollow_margin_top;
    }

    public float getHollow_margin_bottom() {
        return hollow_margin_bottom;
    }

    public void setHollow_margin_bottom(float hollow_margin_bottom) {
        this.hollow_margin_bottom = hollow_margin_bottom;
    }

    public boolean isHollow_clear_square() {
        return hollow_clear_square;
    }

    public void setHollow_clear_square(boolean hollow_clear_square) {
        this.hollow_clear_square = hollow_clear_square;
    }

    public float getMaskView_hollow_alpha() {
        return maskView_hollow_alpha;
    }

    public void setMaskView_hollow_alpha(float maskView_hollow_alpha) {
        this.maskView_hollow_alpha = maskView_hollow_alpha;
    }

    public boolean isAlignParentBottom() {
        return alignParentBottom;
    }

    public void setAlignParentBottom(boolean alignParentBottom) {
        this.alignParentBottom = alignParentBottom;
    }

    public boolean isAlignParentRight() {
        return alignParentRight;
    }

    public void setAlignParentRight(boolean alignParentRight) {
        this.alignParentRight = alignParentRight;
    }

    public boolean isCenterVertical() {
        return centerVertical;
    }

    public void setCenterVertical(boolean centerVertical) {
        this.centerVertical = centerVertical;
    }

    public boolean isCenterHorizontal() {
        return centerHorizontal;
    }

    public void setCenterHorizontal(boolean centerHorizontal) {
        this.centerHorizontal = centerHorizontal;
    }

    public int getHollow_mask_orientation() {
        return hollow_mask_orientation;
    }

    public void switchMask() {
        if (isShowMask()) {
            hindMask();
        } else {
            showMask();
        }
    }


}

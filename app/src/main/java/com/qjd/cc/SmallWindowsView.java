package com.qjd.cc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SmallWindowsView extends FrameLayout {

    private int mSlop;//触发移动事件的最小距离
    private float downX;//手指放下去的x坐标
    private float downY;//手指放下去的Y坐标
    /**
     * 下面四个数据都为像素
     */
    private int screenWidth;//屏幕宽度
    private int screenHeight;//屏幕高度
    private int viewWidth;//小窗的宽度
    private int viewHeight;//小窗的高度

    private WindowManager wm;//窗口管理器，用来把view添加进窗口层
    private WindowManager.LayoutParams wmParams;
    private Context ActContext = null;
    public SmallWindowsView(@NonNull Context context, Context ActContext) {
        super(context);
        this.ActContext = ActContext;
        init();
    }

    private void init() {
        ViewConfiguration vc = ViewConfiguration.get(getContext());
        mSlop = vc.getScaledTouchSlop();
        screenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        screenHeight = getContext().getResources().getDisplayMetrics().heightPixels;
        viewWidth = dp2px(getContext(), 130);
        viewHeight = dp2px(getContext(), 130);
        Toast.makeText(getContext(), "hello init", Toast.LENGTH_SHORT).show();
        //可以根据你的实际情况在这个FrameLayout里添加界面控件之类的，
        // 我之前是用的实时音视频，把相关业务代码去掉了，直接放一个图标
        // 实际上就是拿到一个View从WindowManager给addView进去
        //ImageView imageView = new ImageView(getContext());
        //imageView.setImageResource(R.mipmap.ic_launcher);
        InputStream hsm =null;
        Bitmap bitmap = null;
        try {
            hsm = ActContext.getAssets().open("image/hesuanma222.png");
            bitmap = BitmapFactory.decodeStream(hsm);
            viewWidth = bitmap.getWidth();
            viewHeight = bitmap.getHeight();
            String str = " viewWidth= " + viewWidth + ", viewHeight=" + viewHeight;
            Log.d("hello", str);
            Toast.makeText(getContext(), str, Toast.LENGTH_SHORT).show();
        }catch(IOException ex){
            ex.printStackTrace();
        }
        //Drawable drawb = getResources().getDrawable(R.drawable.hesuanma);
        //imageView.setImageDrawable(drawb);
        if(hsm == null) Log.d("hello", "Not find png");
        Drawable drable = new BitmapDrawable(bitmap);
        View fl_view = LayoutInflater.from(getContext()).inflate(R.layout.float_layout, null);
        LinearLayout layout = fl_view.findViewById(R.id.flayout);
        if(layout == null) {
            Log.d("hello", "layout is null!!!");
            return;
        }else {
            layout.setBackground(drable);
        }
        TextView tv_date = fl_view.findViewById(R.id.tvDate) ;
        SimpleDateFormat dFormat=new SimpleDateFormat("yyyy-MM-dd 06:52");
        Date now=new Date();
        tv_date.setText(dFormat.format(now));
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        addView(fl_view, params);
    }

    //dp转px
    public int dp2px(Context context, int dp) {
        return (int) (getDensity(context) * dp + 0.5);
    }
    public float getDensity(Context context) {
        return context.getResources().getDisplayMetrics().density;
    }

    public void show() {
        wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        wmParams = new WindowManager.LayoutParams(
                viewWidth, viewHeight,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,//8.0以上需要用这个权限
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        wmParams.gravity = Gravity.NO_GRAVITY;
        wmParams.x = screenWidth/2 - viewWidth/2;
        wmParams.y = screenHeight/2 - viewHeight/2;
        wm.addView(this, wmParams);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //本来想在这边直接设置宽高，但是有问题
//    setMeasuredDimension(QMUIDisplayHelper.dp2px(getContext(), 130), QMUIDisplayHelper.dp2px(getContext(), 130));
    }
    //拦截触摸事件自己消费
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }
    private long downTime;
    private float lastMoveX;
    private float lastMoveY;
    //消费触摸事件
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getRawX();
                downY = event.getRawY();
                lastMoveX = downX;
                lastMoveY = downY;
                downTime = System.currentTimeMillis();
                break;
            case MotionEvent.ACTION_MOVE:
                float moveX = event.getRawX();
                float moveY = event.getRawY();
                //就两个坐标算他们距离要大于触发移动事件的最小距离
                //这里也可以减去lastMoveX lastMoveY 但是移动会有卡顿感 因此这里使用的还是downX downY
                if (Math.pow(Math.abs(moveX - downX), 2) + Math.pow(Math.abs(moveY - downY), 2) > Math.pow(mSlop, 2)) {
                    updateViewPosition(moveX - lastMoveX, moveY - lastMoveY);
                    lastMoveX = moveX;
                    lastMoveY = moveY;
                }

                break;
            case MotionEvent.ACTION_UP:
                float upX = event.getRawX();
                float upY = event.getRawY();
                long upTime = System.currentTimeMillis();
                long time = upTime - downTime;
                //点击事件实现 点击小窗口消失
                //这里加了时间判断，是因为假如移动到原来的地方，也会触发成点击事件
                if (Math.pow(Math.abs(upX - downX), 2) + Math.pow(Math.abs(upY - downY), 2) < Math.pow(mSlop, 2) && time < 1000) {
                    showRtcVideo();
                } else {

                }
                break;
        }
        return true;
    }
    private void showRtcVideo() {
        dismiss();
//    Toast.makeText(getContext(), "aaaaaaaaa", Toast.LENGTH_SHORT).show();
    }

    public void dismiss() {
        wm.removeView(this);
    }

    private void updateViewPosition(float moveX, float moveY) {
        wmParams.gravity = Gravity.NO_GRAVITY;
        //更新浮动窗口位置参数
        //    Log.d("moveX, moveY", moveX + "--" + moveY);
        wmParams.x = (int) (wmParams.x + moveX);
        wmParams.y = (int) (wmParams.y + moveY);
        //    刷新显示
        wm.updateViewLayout(this, wmParams);

    }
}
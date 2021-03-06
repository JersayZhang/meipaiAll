package com.gzw.mp.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gzw.mp.R;
import com.gzw.mp.utils.TextDrawable;

/**
 * author Grrsun
 * 自定义顶部Tab导航
 */
public class HomeTabIndicator extends HorizontalScrollView {

    private LayoutInflater mLayoutInflater;
    private final PageListener pageListener = new PageListener();
    private ViewPager pager;
    private LinearLayout tabsContainer;
    private int tabCount;

    private int currentPosition = 0;
    private float currentPositionOffset = 0f;

    private Rect indicatorRect;

    private LinearLayout.LayoutParams defaultTabLayoutParams;

    private int scrollOffset = 10;
    private int lastScrollX = 0;

    private Drawable indicator;
    private TextDrawable[] drawables;
    private Drawable left_edge;
    private Drawable right_edge;

    /**
     * 构造方法1 2 3
     *
     * @param context
     */
    public HomeTabIndicator(Context context) {
        this(context, null);
    }

    public HomeTabIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HomeTabIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mLayoutInflater = LayoutInflater.from(context);
        drawables = new TextDrawable[3];
        int i = 0;
        while (i < drawables.length) {
            drawables[i] = new TextDrawable(getContext());
            i++;
        }

        indicatorRect = new Rect();

        setFillViewport(true);
        setWillNotDraw(false);

        tabsContainer = new LinearLayout(context);
        tabsContainer.setOrientation(LinearLayout.HORIZONTAL);
        tabsContainer.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(tabsContainer);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        scrollOffset = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, scrollOffset, dm);

        defaultTabLayoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);

        //tab LinearLayout的背景
        indicator = getResources().getDrawable(R.drawable.base_tabpager_indicator_selected);
        //左右阴影部分
        left_edge = getResources().getDrawable(R.color.black);
        right_edge = getResources().getDrawable(R.color.black);
    }

    /**
     * 设置当前ViewPager
     *
     * @param pager
     */
    public void setViewPager(ViewPager pager) {
        this.pager = pager;
        //如果此ViewPager没有设置Adapter则抛出以下异常
        if (pager.getAdapter() == null) {
            throw new IllegalStateException("ViewPager does not have adapter instance.");
        }
        //设置页面改变监听
        pager.setOnPageChangeListener(pageListener);
        //通知Tab改变数据
        notifyDataSetChanged();
    }

    /**
     * 此方法在与ViewPager绑定的Adapter数据发生改变时通知HomeTabIndicator更新UI
     */
    public void notifyDataSetChanged() {
        //改变前清除所有View ----->真是日了狗 一个自定义view写了半天
        tabsContainer.removeAllViews();
        //获取所有的Tab category
        tabCount = pager.getAdapter().getCount();
        //循环出所欲category的标题
        for (int i = 0; i < tabCount; i++) {
            addTab(i, pager.getAdapter().getPageTitle(i).toString());
        }
    }

    /**
     * 将category添加到Tab的LinearLayout中
     *
     * @param position
     * @param title
     */
    private void addTab(final int position, String title) {
        //获取View
        ViewGroup tab = (ViewGroup) mLayoutInflater.inflate(R.layout.category_tab, this, false);
        //获取TextView
        TextView category_text = (TextView) tab.findViewById(R.id.category_text);
        //设置Text属性
        category_text.setText(title);//标题
        category_text.setGravity(Gravity.CENTER);//居中
        category_text.setSingleLine();//单行
        category_text.setFocusable(true);//获取焦点
        category_text.setTextColor(getResources().getColor(R.color.category_tab_text));//字体颜色
        //设置点击事件监听
        tab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //设为当前ViewPager
                pager.setCurrentItem(position);
            }
        });
        //添加到View
        tabsContainer.addView(tab, position, defaultTabLayoutParams);
    }

    //计算选中category的高亮区域的宽高
    private void calculateIndicatorRect(Rect rect) {
        ViewGroup currentTab = (ViewGroup) tabsContainer.getChildAt(currentPosition);
        TextView category_text = (TextView) currentTab.findViewById(R.id.category_text);

        float left = (float) (currentTab.getLeft() + category_text.getLeft());
        float width = ((float) category_text.getWidth()) + left;

        if (currentPositionOffset > 0f && currentPosition < tabCount - 1) {
            ViewGroup nextTab = (ViewGroup) tabsContainer.getChildAt(currentPosition + 1);
            TextView next_category_text = (TextView) nextTab.findViewById(R.id.category_text);

            float next_left = (float) (nextTab.getLeft() + next_category_text.getLeft());
            left = left * (1.0f - currentPositionOffset) + next_left * currentPositionOffset;
            width = width * (1.0f - currentPositionOffset) + currentPositionOffset * (((float) next_category_text.getWidth()) + next_left);
        }

        rect.set(((int) left) + getPaddingLeft(), getPaddingTop() + currentTab.getTop() + category_text.getTop(),
                ((int) width) + getPaddingLeft(), currentTab.getTop() + getPaddingTop() + category_text.getTop() + category_text.getHeight());

    }

    /**
     * 计算滚动区域
     *
     * @return
     */
    private int getScrollRange() {
        return getChildCount() > 0 ? Math.max(0, getChildAt(0).getWidth() - getWidth() + getPaddingLeft() + getPaddingRight()) : 0;
    }

    private void scrollToChild(int position, int offset) {

        if (tabCount == 0) {
            return;
        }

        calculateIndicatorRect(indicatorRect);

        int newScrollX = lastScrollX;
        if (indicatorRect.left < getScrollX() + scrollOffset) {
            newScrollX = indicatorRect.left - scrollOffset;
        } else if (indicatorRect.right > getScrollX() + getWidth() - scrollOffset) {
            newScrollX = indicatorRect.right - getWidth() + scrollOffset;
        }
        if (newScrollX != lastScrollX) {
            lastScrollX = newScrollX;
            scrollTo(newScrollX, 0);
        }

    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        calculateIndicatorRect(indicatorRect);

        if (indicator != null) {
            indicator.setBounds(indicatorRect);
            indicator.draw(canvas);
        }

        int i = 0;
        while (i < tabsContainer.getChildCount()) {
            if (i < currentPosition - 1 || i > currentPosition + 1) {
                i++;
            } else {
                ViewGroup tab = (ViewGroup) tabsContainer.getChildAt(i);
                TextView category_text = (TextView) tab.findViewById(R.id.category_text);
                if (category_text != null) {
                    TextDrawable textDrawable = drawables[i - currentPosition + 1];
                    int save = canvas.save();
                    calculateIndicatorRect(indicatorRect);
                    canvas.clipRect(indicatorRect);
                    textDrawable.setText(category_text.getText());
                    textDrawable.setTextSize(0, category_text.getTextSize());
                    textDrawable.setTextColor(getResources().getColor(R.color.deeppink));
                    int left = tab.getLeft() + category_text.getLeft() + (category_text.getWidth() - textDrawable.getIntrinsicWidth()) / 2 + getPaddingLeft();
                    int top = tab.getTop() + category_text.getTop() + (category_text.getHeight() - textDrawable.getIntrinsicHeight()) / 2 + getPaddingTop();
                    textDrawable.setBounds(left, top, textDrawable.getIntrinsicWidth() + left, textDrawable.getIntrinsicHeight() + top);
                    textDrawable.draw(canvas);
                    canvas.restoreToCount(save);
                }
                i++;
            }
        }

        i = canvas.save();
        int top = getScrollX();
        int height = getHeight();
        int width = getWidth();
        canvas.translate((float) top, 0.0f);
        if (left_edge == null || top <= 0) {
            if (right_edge == null || top >= getScrollRange()) {
                canvas.restoreToCount(i);
            }
            right_edge.setBounds(width - right_edge.getIntrinsicWidth(), 0, width, height);
            right_edge.draw(canvas);
            canvas.restoreToCount(i);
        }
        left_edge.setBounds(0, 0, left_edge.getIntrinsicWidth(), height);
        left_edge.draw(canvas);
        if (right_edge == null || top >= getScrollRange()) {
            canvas.restoreToCount(i);
        }
        right_edge.setBounds(width - right_edge.getIntrinsicWidth(), 0, width, height);
        right_edge.draw(canvas);
        canvas.restoreToCount(i);
    }

    private class PageListener implements OnPageChangeListener {

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            currentPosition = position;
            currentPositionOffset = positionOffset;

            scrollToChild(position, (int) (positionOffset * tabsContainer.getChildAt(position).getWidth()));

            invalidate();

        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (state == ViewPager.SCROLL_STATE_IDLE) {
                if (pager.getCurrentItem() == 0) {

                    scrollTo(0, 0);
                } else if (pager.getCurrentItem() == tabCount - 1) {
                    scrollTo(getScrollRange(), 0);
                } else {
                    scrollToChild(pager.getCurrentItem(), 0);
                }
            }
        }

        @Override
        public void onPageSelected(int position) {

        }

    }

}

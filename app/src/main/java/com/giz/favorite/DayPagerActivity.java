package com.giz.favorite;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.transition.Slide;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import database.FavoriteItemLib;
import datatool.FavoriteItem;
import datatool.FavoriteTool;
import datatool.SourceApp;
import utility.CommonAdapter;
import utility.CommonUtil;
import utility.CommonViewHolder;
import utility.CoverFlowEffectTransformer;
import utility.RoundedImageView;

import static com.giz.favorite.MainActivity.REQUEST_DATE_PICKER_FRAGMENT;

public class DayPagerActivity extends AppCompatActivity {

    private static final String TAG = "DayPagerActivity";

    private SectionsPagerAdapter mSectionsPagerAdapter;

    private ViewPager mContainViewPager;
    private ViewPager mCalendarViewPager;

    List<Calendar> mCalendarList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_day_pager);
        Slide slide = new Slide(Gravity.TOP);
        getWindow().setEnterTransition(slide);

        initDateList();
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mContainViewPager = findViewById(R.id.day_pager_container);
        mContainViewPager.setAdapter(mSectionsPagerAdapter);
        mContainViewPager.setCurrentItem(mCalendarList.size() - 1);
        mContainViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {
                mCalendarViewPager.setCurrentItem(i);
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

        mCalendarViewPager = findViewById(R.id.day_pager_calendar);
        mCalendarViewPager.setAdapter(new DateAdapter());
        mCalendarViewPager.setCurrentItem(mCalendarList.size() - 1);
        mCalendarViewPager.setOffscreenPageLimit(5);
        mCalendarViewPager.setPageTransformer(false, new CoverFlowEffectTransformer(this));
        mCalendarViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {
                mContainViewPager.setCurrentItem(i);
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

        findViewById(R.id.day_pager_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        findViewById(R.id.day_pager_date_picker).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DayPagerDatePickerFragment fragment = DayPagerDatePickerFragment.newInstance(mCalendarList.get(mCalendarViewPager.getCurrentItem()).getTime());
                fragment.show(getSupportFragmentManager(), "DatePicker");
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            Log.d(TAG, "onActivityResult: ok");
            if(requestCode == REQUEST_DATE_PICKER_FRAGMENT && data != null){
                Date date = (Date)data.getSerializableExtra(DatePickerFragment.EXTRA_DATE);

                for(int i = mCalendarList.size() - 1; i >= 0; i--){
                    if(mCalendarList.get(i).getTime().getTime() == date.getTime()){
                        mCalendarViewPager.setCurrentItem(i);
                        break;
                    }
                }
            }
        }
    }

    private void initDateList() {
        mCalendarList = new ArrayList<>();
        Date today = new Date();
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.set(2019, 0, 1, 0,0,0);
        calendar.set(Calendar.MILLISECOND, 0);
        while (calendar.getTime().getTime() <= today.getTime()){
            Calendar calendar1 = Calendar.getInstance(Locale.getDefault());
            calendar1.setTime(calendar.getTime());
            mCalendarList.add(calendar1);

            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_day_pager, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        private static final String ARG_DATE = "argDate";

        public PlaceholderFragment() {
        }

        public static PlaceholderFragment newInstance(Calendar date) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putSerializable(ARG_DATE, date);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_day_pager, container, false);
            RecyclerView recyclerView = rootView.findViewById(R.id.fragment_day_pager_rv);
            Calendar calendar = (Calendar)getArguments().getSerializable(ARG_DATE);

            List<FavoriteItem> itemList = FavoriteItemLib.get(getContext()).getFavoriteItemListByDate(calendar.getTime());
            Collections.reverse(itemList);
            if(itemList.size() == 0){
                rootView.findViewById(R.id.fragment_day_pager_noTv).setVisibility(View.VISIBLE);
            }else{
                rootView.findViewById(R.id.fragment_day_pager_noTv).setVisibility(View.GONE);
                CommonAdapter commonAdapter = new CommonAdapter<FavoriteItem>(getContext(), itemList, R.layout.item_favorite) {
                    @Override
                    public void bindData(CommonViewHolder viewHolder, final FavoriteItem data, int pos) {
                        View itemView = viewHolder.itemView;
                        ((TextView)itemView.findViewById(R.id.item_favorite_title)).setText(data.getTitle());
                        ((TextView)itemView.findViewById(R.id.item_favorite_date))
                                .setText(CommonUtil.getDateBriefDescription(data.getDate()));
                        ((TextView)itemView.findViewById(R.id.item_favorite_source)).setText(SourceApp.getSourceText(data.getSource()));

                        TextView archiveTv = itemView.findViewById(R.id.item_favorite_archive);
                        if(data.getArchive() != null){
                            archiveTv.setVisibility(View.VISIBLE);
                            archiveTv.setText(data.getArchive());
                        }
                        else
                            archiveTv.setVisibility(View.GONE);

                        Drawable drawable = CommonUtil.getAppIcon(getContext(), SourceApp.getAppPackageName(data.getSource()));
                        ((RoundedImageView)itemView.findViewById(R.id.item_favorite_icon))
                                .setImageDrawable(drawable == null ? getContext().getResources().getDrawable(R.mipmap.ic_launcher, getContext().getTheme()) : drawable);
                        Bitmap itemImg = FavoriteTool.getItemImage(data.getImagePath());
                        if(itemImg != null){
                            ((ImageView)itemView.findViewById(R.id.item_favorite_image)).setImageBitmap(itemImg);
                            itemView.findViewById(R.id.item_favorite_image).setVisibility(View.VISIBLE);
                        }
                        else
                            itemView.findViewById(R.id.item_favorite_image).setVisibility(View.GONE);
                        // 是否星标
                        itemView.findViewById(R.id.item_favorite_star).setVisibility(data.isStarred() ? View.VISIBLE : View.GONE);

                        itemView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = RichContentActivity.newIntent(getContext(), data.getUUID().toString());
                                getContext().startActivity(intent);
                            }
                        });
                    }
                };
                recyclerView.setAdapter(commonAdapter);
                recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
            }

            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(mCalendarList.get(position));
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return mCalendarList.size();
        }
    }

    public class DateAdapter extends PagerAdapter{

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, final int position) {
            View rootView = getLayoutInflater().inflate(R.layout.pager_item_pager_calendar, null);
            TextView textView = rootView.findViewById(R.id.pipc_tv);
            textView.setText(CommonUtil.getDateDescription(mCalendarList.get(position).getTime()));
            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCalendarViewPager.setCurrentItem(position);
                }
            });

            container.addView(rootView);
            return rootView;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View)object);
        }

        @Override
        public int getCount() {
            return mCalendarList.size();
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
            return view == o;
        }
    }
}

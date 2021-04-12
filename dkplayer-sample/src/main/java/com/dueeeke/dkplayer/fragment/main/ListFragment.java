package com.dueeeke.dkplayer.fragment.main;

import androidx.viewpager.widget.ViewPager;

import com.dueeeke.dkplayer.R;
import com.dueeeke.dkplayer.adapter.ListPagerAdapter;
import com.dueeeke.dkplayer.fragment.BaseFragment;
import com.dueeeke.dkplayer.util.Tag;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Note: I implemented the RecyclerView demo by inheritance,
 *   * ${@link com.dueeeke.dkplayer.fragment.list.RecyclerViewPortraitFragment} I even used triple inheritance 😂,
 *   * This is not necessary in actual development.
 *   * I do this only for code reuse and easy maintenance
 */
public class ListFragment extends BaseFragment {

    @Override
    protected int getLayoutResId() {
        return R.layout.fragment_list;
    }

    @Override
    protected void initView() {
        super.initView();

        ViewPager viewPager = findViewById(R.id.view_pager);

        List<String> titles = new ArrayList<>();
        titles.add(getString(R.string.str_list_view));
        titles.add(getString(R.string.str_recycler_view));
        titles.add(getString(R.string.str_auto_play_recycler_view));
        titles.add(getString(R.string.str_tiktok));
        titles.add(getString(R.string.str_seamless_play));
        titles.add(getString(R.string.str_portrait_when_fullscreen));

        viewPager.setAdapter(new ListPagerAdapter(getChildFragmentManager(), titles));

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getVideoViewManager().releaseByTag(Tag.LIST);
        getVideoViewManager().releaseByTag(Tag.SEAMLESS);
    }
}

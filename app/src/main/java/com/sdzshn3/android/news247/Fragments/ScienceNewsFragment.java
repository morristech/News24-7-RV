package com.sdzshn3.android.news247.Fragments;

import android.app.SearchManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.sdzshn3.android.news247.Activities.MainActivity;
import com.sdzshn3.android.news247.Adapters.ArticleAdapter;
import com.sdzshn3.android.news247.R;
import com.sdzshn3.android.news247.Retrofit.Article;
import com.sdzshn3.android.news247.SupportClasses.DataHolder;
import com.sdzshn3.android.news247.SupportClasses.ItemClickSupport;
import com.sdzshn3.android.news247.SupportClasses.Utils;
import com.sdzshn3.android.news247.SupportClasses.WeatherIcon;
import com.sdzshn3.android.news247.ViewModel.ScienceViewModel;
import com.sdzshn3.android.news247.ViewModel.WeatherViewModel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindView;
import butterknife.ButterKnife;

public class ScienceNewsFragment extends BaseFragment {

    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout mSwipeRefreshLayout;
    @BindView(R.id.recycler_view_list)
    RecyclerView newsRecyclerView;
    @BindView(R.id.loading_circle)
    ProgressBar progressBar;
    @BindView(R.id.no_data_found)
    TextView mEmptyStateTextView;
    @BindView(R.id.weather_temp)
    TextView weatherTemp;
    @BindView(R.id.weather_icon)
    ImageView weatherIcon;

    public static String URL;
    private Context mContext;
    private String mSearchQuery;
    private ScienceViewModel scienceViewModel;
    private WeatherViewModel weatherViewModel;
    private ArticleAdapter mAdapter;

    public ScienceNewsFragment() {
        //Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.list, container, false);
        mContext = getContext();
        setHasOptionsMenu(true);
        setRetainInstance(true);

        mAdapter = new ArticleAdapter();

        ButterKnife.bind(this, rootView);

        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            mSwipeRefreshLayout.setRefreshing(true);
            if (Utils.isConnected(mContext)) {
                scienceViewModel.refresh();
                weatherViewModel.refresh();
            } else {
                Snackbar.make(newsRecyclerView, "Internet connection not available", Snackbar.LENGTH_LONG).show();
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });

        URL = Utils.setUpUrl(mContext, mSearchQuery, progressBar, DataHolder.science);

        Utils.setUpRecyclerView(mContext, newsRecyclerView);
        newsRecyclerView.setAdapter(mAdapter);
        newsRecyclerView.setNestedScrollingEnabled(false);

        scienceViewModel = ViewModelProviders.of(ScienceNewsFragment.this).get(ScienceViewModel.class);
        scienceViewModel.getData().observe(ScienceNewsFragment.this, articles -> {
            if (articles != null && !articles.isEmpty()) {
                mAdapter.submitList(articles);
                mEmptyStateTextView.setVisibility(View.GONE);
            } else {
                if (Utils.isConnected(mContext)) {
                    if (mSearchQuery != null) {
                        mAdapter.submitList(articles);
                    }
                    mEmptyStateTextView.setVisibility(View.VISIBLE);
                } else {
                    Snackbar.make(newsRecyclerView, "Internet connection not available", Snackbar.LENGTH_LONG).show();
                }
            }
            progressBar.setVisibility(View.GONE);
            mSwipeRefreshLayout.setRefreshing(false);
        });

        weatherViewModel = ViewModelProviders.of(ScienceNewsFragment.this).get(WeatherViewModel.class);
        weatherViewModel.getData().observe(ScienceNewsFragment.this, weatherModel -> {
            if (weatherModel != null) {
                String temp = String.valueOf(weatherModel.getMain().getTemp()).split("\\.", 2)[0];
                weatherTemp.setText(getString(R.string.weather_temperature_concatenate, temp, weatherModel.getName()));

                String iconId = weatherModel.getWeather().get(0).getIcon();
                weatherIcon.setImageResource(WeatherIcon.getWeatherIcon(iconId));
            } else {
                if (Utils.isConnected(mContext)) {
                    weatherTemp.setText("Unable to load");
                    weatherIcon.setImageResource(R.drawable.unknown);
                }
            }
        });

        ItemClickSupport.addTo(newsRecyclerView).setOnItemClickListener((recyclerView, position, v) -> {
            Article currentArticle = mAdapter.getItem(position);
            Uri newsUri = Uri.parse(currentArticle.getUrl());
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            CustomTabsIntent customTabsIntent = builder.build();
            builder.setToolbarColor(getResources().getColor(R.color.colorPrimary));
            customTabsIntent.launchUrl(mContext, newsUri);
        });
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchManager searchManager = (SearchManager) mContext.getSystemService(Context.SEARCH_SERVICE);
        androidx.appcompat.widget.SearchView searchView = null;
        if (searchItem != null) {
            searchView = (androidx.appcompat.widget.SearchView) searchItem.getActionView();
        }
        if (searchView != null) {
            MainActivity mainActivity = new MainActivity();
            if (searchManager != null) {
                searchView.setSearchableInfo(searchManager.getSearchableInfo(mainActivity.getComponentName()));
            }
            searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    mSearchQuery = query;
                    Utils.setUpUrl(mContext, mSearchQuery, progressBar, DataHolder.science);
                    scienceViewModel.refresh();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    return false;
                }
            });
            searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    mSearchQuery = null;
                    Utils.setUpUrl(mContext, null, progressBar, DataHolder.science);
                    scienceViewModel.refresh();
                    return true;
                }
            });
        }
        super.onCreateOptionsMenu(menu, inflater);
    }
}

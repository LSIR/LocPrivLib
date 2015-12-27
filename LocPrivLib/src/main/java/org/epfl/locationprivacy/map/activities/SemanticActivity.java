package org.epfl.locationprivacy.map.activities;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;

import org.epfl.locationprivacy.R;
import org.epfl.locationprivacy.map.adapters.SemanticAdapter;


public class SemanticActivity extends ActionBarActivity implements android.support.v7.app.ActionBar.TabListener {

	private String[] tabs = {"Map"};
	private ViewPager viewPager;
	private SemanticAdapter semanticAdapter;
	private android.support.v7.app.ActionBar actionBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_semantic);

		// Initialization
		viewPager = (ViewPager) findViewById(R.id.title_activity_semantic);
		actionBar = getSupportActionBar();
		semanticAdapter = new SemanticAdapter(getSupportFragmentManager(), this);

		viewPager.setAdapter(semanticAdapter);
		//		actionBar.setHomeButtonEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// Adding Tabs
		for (String tab_name : tabs) {
			actionBar.addTab(actionBar.newTab().setText(tab_name).setTabListener(this));
		}

		// on swiping the viewpager make respective tab selected
		viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

			@Override
			public void onPageSelected(int position) {
				// on changing the page
				// make respected tab selected
				actionBar.setSelectedNavigationItem(position);
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
			}
		});
	}

	// Tab Methods
	@Override
	public void onTabSelected(android.support.v7.app.ActionBar.Tab tab, android.support.v4.app.FragmentTransaction fragmentTransaction) {
		viewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(android.support.v7.app.ActionBar.Tab tab, android.support.v4.app.FragmentTransaction fragmentTransaction) {

	}

	@Override
	public void onTabReselected(android.support.v7.app.ActionBar.Tab tab, android.support.v4.app.FragmentTransaction fragmentTransaction) {

	}
}
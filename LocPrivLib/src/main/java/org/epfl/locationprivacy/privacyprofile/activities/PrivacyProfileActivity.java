package org.epfl.locationprivacy.privacyprofile.activities;

import org.epfl.locationprivacy.R;
import org.epfl.locationprivacy.privacyprofile.adapters.PrivacyProfileTabsAdapter;
import org.epfl.locationprivacy.privacyprofile.databases.SemanticLocationsDataSource;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;

public class PrivacyProfileActivity extends ActionBarActivity implements android.support.v7.app.ActionBar.TabListener {

	private String[] tabs = { "Semantics", "Locations" };
	private ViewPager viewPager;
	private PrivacyProfileTabsAdapter privacyProfileTabsAdapter;
	private android.support.v7.app.ActionBar actionBar;
	SemanticLocationsDataSource semanticLocationsDataSource;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_privacyprofile);

		// Initialization
		viewPager = (ViewPager) findViewById(R.id.privacyprofilepager);
		actionBar = getSupportActionBar();
		privacyProfileTabsAdapter = new PrivacyProfileTabsAdapter(getSupportFragmentManager(), this);

		viewPager.setAdapter(privacyProfileTabsAdapter);
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

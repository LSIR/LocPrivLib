package org.epfl.locationprivacy.privacyprofile.adapters;

import org.epfl.locationprivacy.privacyprofile.activities.PrivacyProfileMapFragment;
import org.epfl.locationprivacy.privacyprofile.activities.PrivacyProfileSemanticsFragment;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.SparseArray;
import android.view.ViewGroup;

public class PrivacyProfileTabsAdapter extends FragmentPagerAdapter {

	SparseArray<Fragment> registeredFragments = new SparseArray<Fragment>();
	Context context;

	public PrivacyProfileTabsAdapter(FragmentManager fm, Context context) {
		super(fm);
		this.context = context;
	}

	@Override
	public Fragment getItem(int index) {
		switch (index) {
		case 0:
			return new PrivacyProfileSemanticsFragment();
		case 1:
			return new PrivacyProfileMapFragment();
		}
		return null;
	}

	@Override
	public int getCount() {
		return 2;
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		Fragment fragment = (Fragment) super.instantiateItem(container, position);
		registeredFragments.put(position, fragment);
		return fragment;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		registeredFragments.remove(position);
		super.destroyItem(container, position, object);
	}

	public Fragment getRegisteredFragment(int position) {
		return registeredFragments.get(position);
	}

}

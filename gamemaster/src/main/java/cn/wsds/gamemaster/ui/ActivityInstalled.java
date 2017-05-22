package cn.wsds.gamemaster.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.subao.common.Misc;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ViewFlipper;
import cn.wsds.gamemaster.R;

public class ActivityInstalled extends ActivityBase {

	private RadioGroup radioGroup;
	private ViewFlipper viewFlipper;
	private ListView listUser, listSys;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setDisplayHomeArrow("已安装应用");
		this.setContentView(R.layout.activity_installed);
		viewFlipper = (ViewFlipper)findViewById(R.id.installed_view_flipper);
		radioGroup = (RadioGroup) findViewById(R.id.installed_radio_group);
		radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if (checkedId == R.id.installed_radio_sys) {
					viewFlipper.setDisplayedChild(1);
				} else {
					viewFlipper.setDisplayedChild(0);
				}
			}
		});
		listUser = (ListView) findViewById(R.id.installed_list_user);
		listSys = (ListView) findViewById(R.id.installed_list_sys);
		refreshListView();
	}

	private void refreshListView() {
		PackageManager pm = getPackageManager();
		List<ApplicationInfo> list = pm.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
		if (list == null || list.size() == 0) {
			return;
		}
		List<Item> itemListUser = new ArrayList<Item>(50);
		List<Item> itemListSys = new ArrayList<Item>(50);	
		String myPackageName = getPackageName();
		for (ApplicationInfo app : list) {
			// 我自己，忽略
			if (myPackageName.equals(app.packageName)) {
				continue;
			}
			//
			String label = app.loadLabel(pm).toString();
			Item item = new Item(app.packageName, label, app.uid);
			if (!Misc.isApplicationsUID(app.uid) || (app.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
				itemListSys.add(item);
			} else {
				itemListUser.add(item);
			}
		}
		//
		ItemComparator comparator = new ItemComparator();
		Collections.sort(itemListUser, comparator);
		Collections.sort(itemListSys, comparator);
		listUser.setAdapter(new MyAdapter(this, itemListUser));
		listSys.setAdapter(new MyAdapter(this, itemListSys));
	}
	
	private static class ItemComparator implements Comparator<Item> {

		@Override
		public int compare(Item lhs, Item rhs) {
			int r = lhs.appLabel.compareTo(rhs.appLabel);
			if (r == 0) {
				r = lhs.packageName.compareTo(rhs.packageName);
				if (r == 0) {
					r = lhs.uid - rhs.uid;
				}
			}
			return r;
		}
		
	}

	private static class Item {
		public final String packageName;
		public final String appLabel;
		public final int uid;

		public Item(String packageName, String appLabel, int uid) {
			this.packageName = packageName;
			this.appLabel = appLabel;
			this.uid = uid;
		}
	}

	private static class MyAdapter extends BaseAdapter {

		private final LayoutInflater layoutInflater;
		private final List<Item> itemList;

		public MyAdapter(Context context, List<Item> itemList) {
			this.layoutInflater = LayoutInflater.from(context);
			this.itemList = itemList;
		}

		@Override
		public int getCount() {
			return itemList.size();
		}

		@Override
		public Object getItem(int position) {
			return itemList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			Holder holder;
			if (view == null) {
				view = layoutInflater.inflate(R.layout.item_installed, parent, false);
				holder = new Holder(view);
				view.setTag(holder);
			} else {
				holder = (Holder)view.getTag();
			}
			Item item = itemList.get(position);
			holder.textAppLabel.setText(item.appLabel);
			holder.textPackageName.setText(item.packageName);
			holder.textUID.setText(Integer.toString(item.uid));
			return view;
		}
		
		private static class Holder {
			public final TextView textAppLabel;
			public final TextView textPackageName;
			public final TextView textUID;
			public Holder(View parent) {
				textAppLabel = (TextView)parent.findViewById(R.id.text_app_label);
				textPackageName = (TextView)parent.findViewById(R.id.text_package_name);
				textUID = (TextView)parent.findViewById(R.id.text_uid);
			}
		}
	}
}

package cn.wsds.gamemaster.ui;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.data.ProcessCleanRecords;
import cn.wsds.gamemaster.data.ProcessCleanRecords.AppFlag;
import cn.wsds.gamemaster.data.ProcessCleanRecords.Record;
import cn.wsds.gamemaster.ui.view.Switch;

/**
 * 进程清理列表界面
 */
public class ActivityProccesClean extends ActivityRecords {
    private RadioGroup autoRadioGroup;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		View footerView = getFooterView();
		listRecords.addFooterView(footerView);
		setDisplayHomeArrow(R.string.process_clean_setting);
		ProcessCleanRecords.getInstance().filterUninstallApp();
		List<Record> allRecord = ProcessCleanRecords.getInstance().getAllRecord();
		Collections.sort(allRecord,new ComparatorRecords());
		MyAdapter adapter = new MyAdapter(allRecord);
		listRecords.setAdapter(adapter);
        initProcessClean();
	}

    private void initProcessClean() {
        processCleanCheckedChange();
    }

    private int getGroupCheckId() {
        int checkId;
        int index = ConfigManager.getInstance().getAutoCleanProcessInternal();
        switch (index) {
            case 2:
                checkId = R.id.ten_process_clean;
                break;
            case 3:
                checkId = R.id.fifteen_process_clean;
                break;
            default:
                checkId = R.id.five_process_clean;
                break;
        }

        return checkId;
    }
    private void setAutoCleanIndex(int checkId) {
        int index;
        switch (checkId) {
            case R.id.ten_process_clean:
                index = 2;
                break;
            case R.id.fifteen_process_clean:
                index = 3;
                break;
            default:
                index = 1;
                break;
        }
        ConfigManager.getInstance().setAutoCleanProcessInternal(index);
    }
    private void processCleanCheckedChange() {
        boolean isAutoClean = ConfigManager.getInstance().getAutoProcessClean();
        autoRadioGroup =(RadioGroup)findViewById(R.id.process_setting_group);
        setRadioGroup(isAutoClean);
        autoRadioGroup.check(getGroupCheckId());
        Switch sw = (Switch) findViewById(R.id.auto_process_clean);
        sw.setChecked(isAutoClean);
        sw.setOnChangedListener(new Switch.OnChangedListener() {
            @Override
            public void onCheckedChanged(Switch checkSwitch, boolean checked) {
                setRadioGroup(checked);
                ConfigManager.getInstance().setAutoProcessClean(checked);
            }
        });
        autoRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                setAutoCleanIndex(checkedId);
            }
        });

    }

    private void setRadioGroup(boolean checked) {
        int visibility = checked ? View.VISIBLE : View.GONE;
        autoRadioGroup.setVisibility(visibility);
    }

    @Override
    protected int getContentViewId() {
        return R.layout.activity_process_clean;
    }

    private View getFooterView() {
		TextView footerView = new TextView(getApplicationContext());
		footerView.setText("注：内存清理不涉及系统预装应用");
		footerView.setBackgroundColor(getResources().getColor(R.color.color_game_1));
        footerView.setTextColor(getResources().getColor(R.color.color_game_6));
        footerView.setTextSize(16);
        footerView.setGravity(Gravity.CENTER);
		int height = getResources().getDimensionPixelSize(R.dimen.height_item_background_intercept);
        footerView.setHeight(height);
		return footerView;
	}

	/**
	 * 排序比较器
	 */
	private static class ComparatorRecords implements Comparator<Record> {

		@Override
		public int compare(Record lhs, Record rhs) {
			AppFlag lhsFlag = lhs.getFlag();
			AppFlag rhsFlag = rhs.getFlag();
			if(lhsFlag.equals(rhsFlag)){
				return 0;
			}else{
				if(AppFlag.suggestClean == lhsFlag){
					return -1;
				}else{
					return 1;
				}
			}
		}

	}

	@Override
	protected void onStop() {
		super.onStop();
		//保存数据
		ProcessCleanRecords.getInstance().save();
	}

	/**
	 * 数据适配器
	 */
	private final class MyAdapter extends BaseAdapter{

		private final Item[] items;
		private final LayoutInflater inflate;
		private final PackageManager packageManager;

		public MyAdapter(List<Record> allRecord) {
			packageManager = getPackageManager();
			inflate = LayoutInflater.from(getApplicationContext());
			this.items = new Item[allRecord.size()];
			for (int i = 0; i < allRecord.size(); ++i) {
				Record rec = allRecord.get(i);
				this.items[i] = new Item(rec);
			}
		}

		@Override
		public int getCount() {
			return items.length;
		}

		@Override
		public Object getItem(int position) {
			Item item = items[position];
			if (item.icon == null) {
				item.icon = getIcon(item.record.getPackageName());
			}
			return item;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			ViewHolder holder = null;
			if(view==null){
				view = inflate.inflate(R.layout.item_procces_clean, parent,false);
				holder = new ViewHolder();
				holder.init(view);
				view.setTag(holder);
			}else{
				holder = (ViewHolder) view.getTag();
			}

			bindView(holder,(Item)getItem(position));
			return view;
		}

		private final class Item{
			public Drawable icon;
			public final Record record;
			private Item(Record record) {
				this.record = record;
			}
		}

		/**
		 * 将数据绑定到组件上
		 * @param holder
		 * @param record
		 */
		private void bindView(ViewHolder holder, final Item item) {
			final Record record = item.record;
			holder.textFlag.setText(record.getFlag().getDesc());
			holder.textLabel.setText(record.getLabel());
			holder.imageIcon.setImageDrawable(item.icon);
			holder.checkbox.setOnCheckedChangeListener(null);
			holder.checkbox.setChecked(record.clean());
			holder.checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					String packageName = record.getPackageName();
					if(isChecked){
						ProcessCleanRecords.getInstance().addBlacklist(packageName);
					}else{
						ProcessCleanRecords.getInstance().addWhitelist(packageName,record.getLabel());
					}
					record.setClean(isChecked);
				}
			});
		}

		private Drawable getIcon(String packageName){
			try {
				ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
				Drawable icon = applicationInfo.loadIcon(packageManager);
				return icon;
			} catch (NameNotFoundException e) {
				e.printStackTrace();
				return UIUtils.loadAppDefaultIcon(getApplicationContext());
			}
		}

		/**
		 * 组件集
		 */
		private class ViewHolder{
			private TextView textLabel,textFlag;
			private CheckBox checkbox;
			private ImageView imageIcon;
			public void init(View view) {
				imageIcon = (ImageView) view.findViewById(R.id.image_icon);
				checkbox = (CheckBox) view.findViewById(R.id.check_clean);
				textLabel = (TextView) view.findViewById(R.id.text_app_label);
				textFlag = (TextView) view.findViewById(R.id.text_app_flag);
			}
		}

	}

}

package cn.wsds.gamemaster.ui;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.ConfigManager;

/**
 * 帮助界面
 */
public class ActivityQA extends ActivityBase {

	private ExpandableListView listView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setDisplayHomeArrow(R.string.main_menu_qa);
		setContentView(R.layout.activity_qa);
		listView = (ExpandableListView) findViewById(R.id.list_qa);
		listView.addHeaderView(View.inflate(this, R.layout.qa_list_header, null), null, false);
		listView.setAdapter(new MyAdapter(this));
		ConfigManager.getInstance().setMenuQAClicked_1_4_8();
	}

	private static class MyAdapter extends BaseExpandableListAdapter {

		private static int[] PICTURE_ID_LIST = {
			R.drawable.question_pic_1, R.drawable.question_pic_2, R.drawable.question_pic_3,
		};

		private static int[] TEXT_ID_LIST = {
			R.array.qa_answer_0, R.array.qa_answer_1, R.array.qa_answer_2, R.array.qa_answer_3, R.array.qa_answer_4,
		};

		private final LayoutInflater layoutInfater;
		private final String[] questions;

		private static class HolderQuestion {
			public final TextView text;
			public final ImageView arrow;

			public HolderQuestion(TextView text, ImageView arrow) {
				this.text = text;
				this.arrow = arrow;
			}
		}

		public MyAdapter(Context context) {
			layoutInfater = LayoutInflater.from(context);
			questions = context.getResources().getStringArray(R.array.qa_questions);
		}

		@Override
		public int getGroupCount() {
			return questions.length;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return 1;
		}

		@Override
		public Object getGroup(int groupPosition) {
			return null;
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			return null;
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			HolderQuestion holder;
			View view = convertView;
			if (view == null) {
				view = layoutInfater.inflate(R.layout.item_qa_question, parent, false);
				holder = new HolderQuestion((TextView) view.findViewById(R.id.item_qa_question), (ImageView) view.findViewById(R.id.item_qa_question_arrow));
				view.setTag(holder);
			} else {
				holder = (HolderQuestion) view.getTag();
			}
			holder.text.setText(questions[groupPosition]);
			holder.arrow.setImageResource(isExpanded ? R.drawable.game_detailed_open : R.drawable.game_detailed);
			return view;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
			View view = convertView;
			ChildViewHolder childViewHolder;
			if (view == null) {
				view = layoutInfater.inflate(R.layout.item_qa_answer, parent, false);
				childViewHolder = new ChildViewHolder(view);
				view.setTag(childViewHolder);
			} else {
				childViewHolder = (ChildViewHolder) view.getTag();
			}
			if (groupPosition >= 0 && groupPosition < PICTURE_ID_LIST.length) {
				childViewHolder.typicalDrawing.setVisibility(View.VISIBLE);
				childViewHolder.typicalDrawing.setImageResource(PICTURE_ID_LIST[groupPosition]);
			}else{
				childViewHolder.typicalDrawing.setVisibility(View.GONE);
			}
			setSpannableText(childViewHolder.desc, groupPosition);
			return view;
		}
		
		private static final class ChildViewHolder {
			public final TextView desc;
			public final ImageView typicalDrawing;
			public ChildViewHolder(View groupView) {
				this.desc = (TextView) groupView.findViewById(R.id.item_qa_answer_text);
				this.typicalDrawing = (ImageView) groupView.findViewById(R.id.item_qa_answer_typical_drawing);
			}
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return false;
		}

		private void setSpannableText(TextView text, int groupPosition) {
			if (groupPosition < 0 || groupPosition >= TEXT_ID_LIST.length) {
				text.setText(null);
				return;
			}
			Resources res = text.getContext().getResources();
			int color0 = res.getColor(R.color.color_game_7);
			int color1 = res.getColor(R.color.color_game_11);
			String[] texts = text.getContext().getResources().getStringArray(TEXT_ID_LIST[groupPosition]);
			SpannableStringBuilder ssb = new SpannableStringBuilder();
			for (int i = 0; i < texts.length; ++i) {
				int start = ssb.length();
				int color = ((i & 1) == 0) ? color0 : color1;
				String field = texts[i];
				int startBold = field.indexOf('【');
				if (startBold >= 0) {
					ssb.append(field, 0, startBold);
					int startBoldSpan = ssb.length();
					int endBold = field.indexOf('】', startBold);
					if (endBold < 0) {
						endBold = field.length();
					}
					ssb.append(field, startBold + 1, endBold);
					AbsoluteSizeSpan ass = new AbsoluteSizeSpan(16, true);
					ssb.setSpan(ass, startBoldSpan, ssb.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
					ssb.append(field, endBold + 1, field.length());
				} else {
					ssb.append(field);
				}
				ForegroundColorSpan fcs = new ForegroundColorSpan(color);
				ssb.setSpan(fcs, start, ssb.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);

			}
			text.setText(ssb);
		}
	}
}

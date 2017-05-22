package cn.wsds.gamemaster.ui.floatwindow;


import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.ConfigManager;

@SuppressLint("InflateParams") public class NewFunctionPrompt extends FloatWindow {
	
	private int xBox, yBox;
	private int boxWidth, earHeight;
	private boolean left;
	private static NewFunctionPrompt instance;
	
	public static NewFunctionPrompt createInstance(Context context, int xBox, int yBox, int boxWidth, int earHeight, boolean left){
		if (instance == null){
			LayoutInflater inflater = LayoutInflater.from(context);
			View view = inflater.inflate(R.layout.new_function_prompt, null);
			instance = new NewFunctionPrompt(context, xBox, yBox, boxWidth, earHeight, left);
			instance.addView(Type.DIALOG, view, 0, 0);
		}
		return instance;
	}
	
	
	
	public static void destoryInstance(){
		ConfigManager.getInstance().setNeverShowNewFunctionPrompt(false);
		if (instance != null){
			NewFunctionPrompt prompt = instance;
			instance = null;
			prompt.destroy();
		}
	}
	
	
	private NewFunctionPrompt(Context context, int xBox, int yBox, int boxWidth, int earHeight, boolean left) {
		super(context);
		this.xBox = xBox;
		this.yBox = yBox;
		this.boxWidth = boxWidth;
		this.earHeight = earHeight;
		this.left = left;
	}

	@Override
	protected void onViewAdded(View view) {
		int xCenter;
		int yCenter;
		int width = view.getMeasuredWidth();
		int height = view.getMeasuredHeight();
		ImageView promptImage = (ImageView) view.findViewById(R.id.prompt_image);
		promptImage.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				NewFunctionPrompt.destoryInstance();
			}
		});
		if (this.left){
			promptImage.setImageResource(R.drawable.suspension_tips_new_text_right);
			xCenter = this.xBox + this.boxWidth - width / 2;
		}else{
			promptImage.setImageResource(R.drawable.suspension_tips_new_text_left);
			xCenter = this.xBox + width / 2;
			
		}
		yCenter = this.yBox + this.earHeight + height / 2;
		
		this.setCenterPosition(xCenter, yCenter);
	}

	@Override
	protected boolean canDrag() {
		return false;
	}
	
	@Override
	protected void onTouchOutside() {
		destoryInstance();
	}

}

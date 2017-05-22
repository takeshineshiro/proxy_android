package cn.wsds.gamemaster.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import cn.wsds.gamemaster.R;

/**
 * 引导进入用户中心
 */
abstract public class BasePormpt extends Dialog{

	public BasePormpt(Context context, View menuView) {
		super(context, R.style.MainHelpUIDialog);
		RelativeLayout contentView = new RelativeLayout(context);
		// 添加一个遮罩
		MaskView maskView = createMask(context, menuView);
		contentView.addView(maskView);
		
		// 添加引导view
		View view = createPormpt(context);
		LayoutParams params = createPormptParams(context, menuView);
		contentView.addView(view, params);
		
		// 点击即消失
		contentView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

		setContentView(contentView);
	}

	abstract protected LayoutParams createPormptParams(Context context, View menuView);


	abstract public View createPormpt(Context context);

	private MaskView createMask(Context context, View menuView) {
        int[] menuLocation = new int[2];
		menuView.getLocationOnScreen(menuLocation);
        int startY = getStartY(menuView, menuLocation[1]);
        Rect rectMenu = new Rect(menuLocation[0], startY, menuLocation[0]+menuView.getWidth(), startY + menuView.getHeight());
		MaskView maskView = MaskView.createMask(context, rectMenu);
		return maskView;
	}

    protected int getStartY(View menuView, int i) {
        int height = getStatusHeight(menuView);
        return i - height;
    }

    private int getStatusHeight(View menuView) {
        Rect rect = new Rect();
        menuView.getWindowVisibleDisplayFrame(rect);
        return rect.top;
    }

    //遮罩
	private static final class MaskView extends View {
		
		private Rect[] highlightRects;
		private final Paint paint = new Paint();

        private MaskView(Context context) {
            super(context);
        }

		private MaskView(Context context, Rect[] highlightRects) {
			super(context);
			 
			this.highlightRects = highlightRects;
			paint.setColor(context.getResources().getColor(R.color.help_mask));
			
			// 因为用了clipRect()，所以关闭硬件加速，否则在某些低版本手机下会崩溃
			this.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		}
		
		// 创建一个遮罩
		public static MaskView createMask(Context context,Rect... highlightRects){
			return new MaskView(context,highlightRects);
		}
		
		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			for (Rect rect : highlightRects) {
				canvas.clipRect(rect, Region.Op.XOR);
			}
			canvas.drawPaint(paint);
		}
	}

}
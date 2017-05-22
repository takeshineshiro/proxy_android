package cn.wsds.gamemaster.ui.store;

import android.content.Context;
import android.graphics.Paint;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.pay.model.ProductDetail;
import cn.wsds.gamemaster.pay.model.Store;
import cn.wsds.gamemaster.ui.user.Identify;
import cn.wsds.gamemaster.ui.user.Identify.VIPStatus;

public class AdapterProductList extends BaseAdapter{

	private final int[] Flags = new int[]{Store.FLAG_SEASON,
			Store.FLAG_MONTH,Store.FLAG_FREE};

	private final List<ProductDetail> products = new ArrayList<>(3);
	private OnProductSelectedListener listener ;

	public interface OnProductSelectedListener{
		void onProductSelected(ProductDetail product) ;
	}

	public AdapterProductList(OnProductSelectedListener listener){
		this.listener = listener ;
	}

	public void setData(List<ProductDetail> productList){
		if((productList==null)||(productList.isEmpty())){
			return ;
		}

		inflateDatas(productList);
		notifyDataSetChanged();
	}

	private void inflateDatas(List<ProductDetail> productList){

		//products.addAll(productList);

		for (ProductDetail product : productList){
			Store.ProductPresent present =
					Store.getProductPresent(product.getFlag());

			product.setProductName(present.name);
			product.setDescription(present.desc);
		}

		reIndex(productList);
	}

	private void reIndex(List<ProductDetail> productList){
		products.clear();
		for(int flag : Flags){
			for (ProductDetail product :productList){
				if (product.getFlag()==flag){
					products.add(product);
					break;
				}
			}
		}
	}

	public void updateView(){
		notifyDataSetInvalidated();
	}

	@Override
	public int getCount() {
		return products.size();
	}

	@Override
	public Object getItem(int position) {
		return products.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		if((products.isEmpty())||(position>=products.size())){
			return null ;
		}

		ProductDetail product = products.get(position);
		if(product ==null){
			return null ;
		}

		ViewHolder holder ;
		if(convertView==null){
			convertView = LayoutInflater.from(parent.getContext())
					.inflate(R.layout.item_product, parent,false);

			holder = new ViewHolder();
			convertView.setTag(holder);
		}else{
			holder = (ViewHolder) convertView.getTag();
		}

		holder.bindView(convertView,position,product);

		return convertView;
	}

	private class ViewHolder{

		ViewHolder(){
		}

		private void bindView(View convertView ,int position ,
							  ProductDetail product){
			if((product==null)||(convertView==null)){
				return ;
			}

			ProductBinder binder = getBinder(convertView,position,product);
			if (binder==null){
				return;
			}

			binder.bindView();
		}

		private ProductBinder getBinder(View convertView ,int position ,
										ProductDetail product){
			switch (product.getFlag()){
				case Store.FLAG_SEASON:
					return new SeasonCard(convertView,position,product);
				case Store.FLAG_MONTH:
					return new MonthCard(convertView,position,product);
				case Store.FLAG_FREE:
					return new FreeTrial(convertView,position,product);
				default:
					return null;
			}
		}
	}

	private abstract class ProductBinder{
		protected final View convertView ;
		protected final ProductDetail product;
		protected final int position ;
		protected final ImageView icon ;
		protected final TextView name;
		protected final TextView desc ;
		protected final TextView renew;
		protected final TextView old_price ;
		protected final TextView price;
		protected final ImageView arrow ;

		protected final Context context = AppMain.getContext();

		private OnClickListener clickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(listener!=null){
					listener.onProductSelected( products.get(position));
				}
			}
		};

		ProductBinder(View convertView,int position , ProductDetail product){
			this.convertView = convertView ;
			this.product = product ;
			this.position = position ;
			icon = (ImageView) convertView.findViewById(R.id.icon_product);
			name = (TextView)convertView.findViewById(R.id.text_name);
			desc = (TextView)convertView.findViewById(R.id.text_desc);
			renew = (TextView)convertView.findViewById(R.id.text_renew) ;
			old_price = (TextView)convertView.findViewById(R.id.text_old_price) ;
			price = (TextView)convertView.findViewById(R.id.text_price);
			arrow = (ImageView)convertView.findViewById(R.id.icon_arrow);
		}

		protected void bindView(){

			setTextContent();
			setTextColor();
			setRes();

			addClickEvent();
		}

		private  void setTextContent(){
			name.setText(product.getProductName());
			desc.setText(product.getDescription());

			price.setText("");
			float price = product.getPrice();
			if (price>0){
				appendPriceUnit();
			}

			appendPrice(price);
		}

		private  void setTextColor(){
			int color = getColor();
			name.setTextColor(color);
			renew.setTextColor(color);
		}

		private void appendPriceUnit(){
			String strUnit = "￥" ;
			doTextAppend(strUnit,R.style.price_unit);
		}

		private void appendPrice(float productPrice){
			String strPrice = (productPrice>0)?String.valueOf(productPrice):"免费";
			doTextAppend(strPrice,R.style.text_price);
		}

		private void doTextAppend(String content , int style){
			SpannableString textPrice = new SpannableString(content);
			textPrice.setSpan(new TextAppearanceSpan(AppMain.getContext(), style),
					0, content.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

			price.append(textPrice);
		}

		protected  VIPStatus  getStatus(){
			return Identify.getInstance().getVIPStatus();
		}

		protected void addValidClickEnvent(){
			convertView.setOnClickListener(clickListener);
		}

		protected void setRenewVisibility(){
			int visibility = (needShowRenewIcon()?View.VISIBLE:View.INVISIBLE);
			renew.setVisibility(visibility);
		}

		private boolean needShowRenewIcon(){
			VIPStatus status = getStatus();
			if (status==null){
				return false ;
			}

			switch (status){
				case VIP_VALID:
				case VIP_EXPIRED:
					return true;
				default:
					return false;
			}
		}

		protected void hideOldPrice(){
			old_price.setVisibility(View.GONE);
		}

		abstract void setRes();
		abstract int getColor();
		abstract void addClickEvent();
	}

	private class SeasonCard extends ProductBinder{

		SeasonCard(View convertView, int position, ProductDetail product) {
			super(convertView, position, product);
		}

		@Override
		void setRes() {
			icon.setImageResource(R.drawable.purchase_first_vip_90_pic);
			renew.setBackgroundResource(R.drawable.bg_text_renew_season);
			setRenewVisibility();
			old_price.setVisibility(View.VISIBLE);
			old_price.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG);
		}

		@Override
		void addClickEvent() {
			addValidClickEnvent();
		}

		@Override
		int getColor() {
			return context.getResources().getColor(R.color.color_game_17);
		}
	}

	private class MonthCard extends ProductBinder{

		MonthCard(View convertView, int position, ProductDetail product) {
			super(convertView, position, product);
		}

		@Override
		void setRes() {
			icon.setImageResource(R.drawable.purchase_first_vip_30_pic);
			renew.setBackgroundResource(R.drawable.bg_text_renew_month);
			setRenewVisibility();
			hideOldPrice();
		}

		@Override
		void addClickEvent() {
			addValidClickEnvent();
		}

		@Override
		int getColor() {
			return context.getResources().getColor(R.color.color_game_11);
		}
	}

	private class FreeTrial extends ProductBinder{

		FreeTrial(View convertView, int position, ProductDetail product) {
			super(convertView, position, product);
		}

		@Override
		void setRes() {
			if(freeTrialEnable()){
				setEnableRes();
			}else{
				setDisableRes();
			}

			renew.setVisibility(View.INVISIBLE);
			int color = getColor();
			desc.setTextColor(color);
			price.setTextColor(color);
			hideOldPrice();
		}

		@Override
		void addClickEvent() {
			if (freeTrialEnable()){
				addValidClickEnvent();
			}else{
				convertView.setOnClickListener(null);
			}
		}

		@Override
		int getColor() {
			if (freeTrialEnable()){
				return context.getResources().getColor(R.color.color_game_7);
			}else{
				return context.getResources().getColor(R.color.color_game_32);
			}
		}

		private  boolean freeTrialEnable(){
			VIPStatus status = getStatus();
			return (VIPStatus.VIP_NO_ACTIVATED.equals(status)||
				(VIPStatus.USER_NOT_LOGIN.equals(status))) ;
		}

		private void setEnableRes(){
			icon.setImageResource(R.drawable.purchase_first_vip_free_pic);
			arrow.setImageResource(R.drawable.purchase_first_next_icon);
			convertView.setBackgroundResource(R.drawable.about_page_list_bg);
		}

		private void setDisableRes(){
			icon.setImageResource(R.drawable.purchase_first_vip_free_pic_2);
			arrow.setImageResource(R.drawable.purchase_first_next_icon_32);
			convertView.setBackgroundResource(R.drawable.free_product_disable_bg);
		}
	}

}

package cn.wsds.gamemaster.pay.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * model目录下面的类用来进行序列化和反序列化的，字段名不能修改，也不能混淆
 */
public class ProductDetail implements Parcelable{

	private String productId;
	private String productName;
	private String description;
	private float price;
	private int accelDays;
	private int flag ;
	
	public ProductDetail(String productId,String productName,String description,
			float price , int accelDays , int flag){
		this.productId = productId;
		this.productName = productName;
		this.description = description;
		this.price = price;
		this.accelDays = accelDays;
		this.flag = flag ;
	}
	
	public String getProductId() {
		return productId;
	}
	
	public void setProductId(String productId) {
		this.productId = productId;
	}
	
	public String getProductName() {		
		return productName;
	}
	
	public void setProductName(String productName) {
		this.productName = productName;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public float getPrice() {
		return price;
	}
	
	public void setPrice(float price) {
		this.price = price;
	}
	
	public int getAccelDays() {
		return accelDays;
	}
	
	public void setAccelDays(int accelDays) {
		this.accelDays = accelDays;
	}
	
	public int getFlag() {
		return flag;
	}
	
	public void setFlag(int flag) {
		this.flag = flag;
	}
	
	public static ProductDetail deSerialer(String jsonStr) {
		try {
			return new Gson().fromJson(jsonStr, ProductDetail.class);
		} catch (JsonSyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(productId);
		dest.writeString(productName);
		dest.writeString(description);
		dest.writeFloat(price);
		dest.writeInt(accelDays);
		dest.writeInt(flag);
	}
	
	public static final Parcelable.Creator<ProductDetail> CREATOR =
			new Parcelable.Creator<ProductDetail>() {

		@Override
		public ProductDetail createFromParcel(Parcel in) {	
			return new ProductDetail(in);
		}

		@Override
		public ProductDetail[] newArray(int size) {		
			return new ProductDetail[size];
		}
	};
	
	private ProductDetail(Parcel in){	 
		productId = in.readString();
		productName = in.readString();
		description = in.readString();
		price = in.readFloat();
		accelDays = in.readInt();
		flag = in.readInt();
	}

}

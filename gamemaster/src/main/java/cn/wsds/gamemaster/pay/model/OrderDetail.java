package cn.wsds.gamemaster.pay.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import cn.wsds.gamemaster.pay.vault.PayApiService;

/**
 * model目录下面的类用来进行序列化和反序列化的，字段名不能修改，也不能混淆
 * Created by hujd on 16-8-4.
 */
public class OrderDetail implements Parcelable{

	/**
	 * orderId : 00000000-858a-eb92-ebd0-811a554fd200
	 * tradeNo : 2013082244524842
	 * productId : 11111100-858a-eb92-ebd0-811a554fd200
	 * productName : 包月
	 * description : 30天游戏加速
	 * totalFee : 20
	 * payType : 1
	 * status : 1
	 * createTime : 2016-04-20 20:28:00
	 * paidTime : 2016-04-20 20:30:00
	 * dealedTime : 2016-04-22 20:28:00
	 * refundTime : 2016-04-22 21:28:00
	 */

	private final String orderId;
	private final String tradeNo;
	private final String productId;
	private final String productName;
	private final String description;
	private final float totalFee;
	private final int freeDays;
	private final int payType;
	private final int status;
	private final String createTime;
	private final String paidTime;
	private final String dealedTime;

	public OrderDetail(String orderId, String tradeNo, String productId,
					   String productName, String description,
					   float totalFee, int freeDays, int payType,
					   int status, String createTime,
					   String paidTime, String dealedTime, String refundTime) {
		this.orderId = orderId;
		this.tradeNo = tradeNo;
		this.productId = productId;
		this.productName = productName;
		this.description = description;
		this.totalFee = totalFee;
		this.freeDays = freeDays ;
		this.payType = payType;
		this.status = status;
		this.createTime = createTime;
		this.paidTime = paidTime;
		this.dealedTime = dealedTime;
		this.refundTime = refundTime;
	}

	private final String refundTime;

	public String getOrderId() {
		return orderId;
	}

	public String getTradeNo() {
		return tradeNo;
	}

	public String getProductId() {
		return productId;
	}

	public String getProductName() {
		return productName;
	}

	public String getDescription() {
		return description;
	}

	public float getTotalFee() {
		return totalFee;
	}

	public int getFreeDays() {
		return freeDays;
	}

	public int getPayType() {
		return payType;
	}

	public int getStatus() {
		return status;
	}

	public String getCreateTime() {
		return createTime;
	}

	public String getPaidTime() {
		return paidTime;
	}

	public String getDealedTime() {
		return dealedTime;
	}

	public String getRefundTime() {
		return refundTime;
	}

	public static OrderDetail deSerialer(String jsonStr) {
		try {
			return new Gson().fromJson(jsonStr, OrderDetail.class);
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
		dest.writeString(orderId);
		dest.writeString(tradeNo);
		dest.writeString(productId);
		dest.writeString(productName);
		dest.writeString(description);
		dest.writeFloat(totalFee);
		dest.writeInt(freeDays);
		dest.writeInt(payType);
		dest.writeInt(status);
		dest.writeString(createTime);
		dest.writeString(paidTime);
		dest.writeString(dealedTime);
		dest.writeString(refundTime);
	}
	
	public static final Parcelable.Creator<OrderDetail> CREATOR =
			new Parcelable.Creator<OrderDetail>() {
		@Override
		public OrderDetail createFromParcel(Parcel in) {	
			return new OrderDetail(in);
		}

		@Override
		public OrderDetail[] newArray(int size) {		
			return new OrderDetail[size];
		}
	};
	
	private OrderDetail(Parcel in){	 
		
		orderId = in.readString();
		tradeNo = in.readString();
		productId = in.readString();
		productName = in.readString();
		description = in.readString();
		totalFee = in.readFloat();
		freeDays = in.readInt();
		payType = in.readInt();
		status = in.readInt();
		createTime = in.readString();
		paidTime = in.readString();
		dealedTime = in.readString();
		refundTime = in.readString();
	}
	
	public static String orderState(int state){
		String strState = "失败";
		
		switch(state){
		case PayApiService.PayStatus.PAY_STATUS_CREATED:
			strState = "订单已创建";
			break;
		case PayApiService.PayStatus.PAY_STATUS_PAYING:
			strState = "支付中";
			break;
		case PayApiService.PayStatus.PAY_STATUS_PAY_SUCCESS:
			strState = "成功";
			break;
		case PayApiService.PayStatus.PAY_STATUS_PAY_FAILED:
			strState = "失败";
			break;
		case PayApiService.PayStatus.PAY_STATUS_REFUNDING:
			strState = "退款中";
			break;
		case PayApiService.PayStatus.PAY_STATUS_REFUND_SUCCESS:
			strState = "退款成功" ;
			break;
		case PayApiService.PayStatus.PAY_STATUS_REFUND_FAILED:
			strState = "退款失败" ;
			break;
		case PayApiService.PayStatus.PAY_STATUS_USER_CANCEL:
			strState = "已取消" ;
			break;
		default:
			break;
		}
		
		return strState;
	}
	
	public static final String payType(int type){
		String payType = "";
		if(type==1){
			payType = "支付宝";
		}else if(type==4){
			payType = "微信";
		}
		
		return payType;
	}
}

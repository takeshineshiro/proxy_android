package cn.wsds.gamemaster.pay.model;

import com.subao.utils.UrlConfig;

import java.util.ArrayList;
import java.util.List;

public class Store {

	private static final String PRODUCT_ID_MONTH_TEST =
			"63d8234abc5a49a2bbb011a724845388";
	private static final String PRODUCT_ID_SEASON_TEST =
			"8cd6e60b8f6c46779d3c2771e044b4a8";
	private static final String PRODUCT_ID_FREE_TEST =
			"9e25ee7ca52c463e9c8942427ab6f93e" ;

	private static final String PRODUCT_ID_MONTH =
			"8845275b717749f08ccf3dcc9bb6dc43";
	private static final String PRODUCT_ID_SEASON =
			"766a1cd0afa144adb0bc80a6e0b91053";
	private static final String PRODUCT_ID_FREE  =
			"294421026b694ac9bf59b7a8b9fe08c4" ;

	private static final String NAME_MONTH = "VIP月卡";
	private static final String NAME_SEASON = "VIP季卡";
	private static final String NAME_FREE = "VIP试用";

	private static final String DESCRIPTION_MONTH =
			"享受所有服务持续30天";
	private static final String DESCRIPTION_SEASON =
			"享受所有服务持续90天";
	private static final String DESCRIPTION_FREE =
			"享受所有服务持续7天";

	private static final float PRICE_MONTH = 15.0f;
	private static final float PRICE_SEASON = 40.0f;
	private static final float PRICE_FREE = 0.0f;

	private static final int ACCEL_MONTH = 30;
	private static final int ACCEL_SEASON = 90 ;
	private static final int ACCEL_FREE = 7 ;

	private static final int FLAG = 1;

	public static final int FLAG_MONTH = 1;
	public static final int FLAG_SEASON = 2 ;
	public static final int FLAG_FREE = 3;

	private static final Store instance = new Store();
	private static final List<ProductDetail> products = new ArrayList<>(2);

	private enum ServiceType{
		SERVICE_TYPE_SEASON,
		SERVICE_TYPE_MONTH,
		SERVICE_TYPE_FREE
	}

	public static class ProductPresent{
		public final String name ;
		public final String desc ;

		ProductPresent(String name , String desc){
			this.name = name ;
			this.desc = desc ;
		}
	}
	
	public static Store getInstance(){
		return instance ;
	}
	
	public List<ProductDetail> getPoducts(){
		if(products.isEmpty()){

			products.add(getProduct(ServiceType.SERVICE_TYPE_SEASON,
					isTestServer()));
			products.add(getProduct(ServiceType.SERVICE_TYPE_MONTH,
					isTestServer()));
			products.add(getProduct(ServiceType.SERVICE_TYPE_FREE,
					isTestServer()));
		}
		
		return products ;
	}

	private static boolean isTestServer(){
		return (UrlConfig.instance.getServerType() ==
				UrlConfig.ServerType.TEST) ;
	}

	private static ProductDetail getProduct(ServiceType serviceType ,
											boolean isTest){
		switch (serviceType){
			case SERVICE_TYPE_SEASON:
				return new ProductDetail(getSeasonCardId(isTest),
						NAME_SEASON, DESCRIPTION_SEASON,
						PRICE_SEASON, ACCEL_SEASON , FLAG);
			case SERVICE_TYPE_MONTH:
			    return new ProductDetail(getMonthCardId(isTest),
						NAME_MONTH, DESCRIPTION_MONTH,
						PRICE_MONTH, ACCEL_MONTH , FLAG);
			case SERVICE_TYPE_FREE:
			    return new ProductDetail(getFreeTrialId(isTest),
						NAME_FREE, DESCRIPTION_FREE,
						PRICE_FREE, ACCEL_FREE , FLAG);
			default:
				return null ;
		}
	}

	private static String getSeasonCardId(boolean isTest){
		return isTest?PRODUCT_ID_SEASON_TEST:PRODUCT_ID_SEASON ;
	}

	private static String getMonthCardId(boolean isTest){
		return isTest?PRODUCT_ID_MONTH_TEST:PRODUCT_ID_MONTH;
	}

	private static String getFreeTrialId(boolean isTest){
		return isTest?PRODUCT_ID_FREE_TEST:PRODUCT_ID_FREE;
	}

	public static ProductPresent getProductPresent(int flag){
		switch (flag){
			case FLAG_SEASON:
				return new ProductPresent(NAME_SEASON,DESCRIPTION_SEASON);
			case FLAG_MONTH:
				return new ProductPresent(NAME_MONTH,DESCRIPTION_MONTH);
			case FLAG_FREE:
				return new ProductPresent(NAME_FREE,DESCRIPTION_FREE);
			default:
				return null;
		}
	}

}

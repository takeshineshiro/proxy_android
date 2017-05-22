package cn.wsds.gamemaster.service.aidl;

import android.os.Parcel;
import android.os.Parcelable;

public class JNIKeyValue implements Parcelable{

	private Integer key ;
	private Integer value ;
	
	public JNIKeyValue(int key , int value){
		this.key = key ;
		this.value = value ;
	}
	
	private JNIKeyValue(Parcel in){
		key = in.readInt();
		value = in.readInt();
	}
	
	@Override
	public int describeContents() {
		 
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(key);
		dest.writeInt(value);		
	}
	
	public static final Parcelable.Creator<JNIKeyValue> CREATOR = new Parcelable.Creator<JNIKeyValue>() {

		@Override
		public JNIKeyValue createFromParcel(Parcel in) {	
			return new JNIKeyValue(in);
		}

		@Override
		public JNIKeyValue[] newArray(int size) {		
			return new JNIKeyValue[size];
		}
	};

	public Integer getKey() {
		return key;
	}

	public void setKey(Integer key) {
		this.key = key;
	}

	public Integer getValue() {
		return value;
	}

	public void setValue(Integer value) {
		this.value = value;
	}

	
}

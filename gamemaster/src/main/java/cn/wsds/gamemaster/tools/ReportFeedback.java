package cn.wsds.gamemaster.tools;

import java.io.IOException;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;
import cn.wsds.gamemaster.GlobalDefines;
import cn.wsds.gamemaster.data.UserFeedback;
import cn.wsds.gamemaster.pb.Proto.UserFeedback.Builder;
import cn.wsds.gamemaster.statistic.Statistic;

import com.subao.common.net.Http;
import com.subao.common.utils.InfoUtils;
import com.subao.utils.SubaoHttp;
import com.subao.utils.UrlConfig;

/**
 * 上报反馈消息
 */
public class ReportFeedback extends AsyncTask<Object, String, Boolean> {

	private final String baseUrl;
	private final byte[] postData;
	private final Callback callback;

	public interface Callback {
		public void onEnd(boolean result);
	}

	public ReportFeedback(byte[] postData, Callback callback) {
		this.baseUrl = UrlConfig.instance.getDomainOfFeedBack() + "/feedbackAdd";
		this.postData = postData;
		this.callback = callback;
	}

	@Override
	protected Boolean doInBackground(Object... params) {
		try {
			com.subao.common.net.Http.Response response = SubaoHttp.createHttp().doPost(
				SubaoHttp.createURL(SubaoHttp.InterfaceType.HAS_TIMESTAMP_KEY, GlobalDefines.APP_NAME_FOR_HTTP_REQUEST, null, baseUrl, null),
				postData,
				Http.ContentType.PROTOBUF.str);
			return response.code == 200;
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	protected void onPostExecute(Boolean succeeded) {
		Callback cb = callback;
		if (cb != null) {
			boolean result = (succeeded != null) ? succeeded.booleanValue() : false;
			cb.onEnd(result);
		}
	}

	public static byte[] buildProtoHaveDeviceInfo(UserFeedback userFeedback, Context context, String phoneModel) {
		Builder builder = userFeedback.buildProtobuf();
		builder.setUserId(Statistic.getDeviceId());
		builder.setVersion(InfoUtils.getVersionName(context));
		builder.setMobileType(TextUtils.isEmpty(phoneModel) ? Build.MODEL : phoneModel);
		builder.setOs(Build.VERSION.RELEASE);
		return builder.build().toByteArray();
	}

	public static byte[] buildProtoDefault(UserFeedback userFeedback) {
		Builder builder = userFeedback.buildProtobuf();
		return builder.build().toByteArray();
	}

}

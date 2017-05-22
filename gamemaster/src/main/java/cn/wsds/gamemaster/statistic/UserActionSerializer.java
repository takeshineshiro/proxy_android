package cn.wsds.gamemaster.statistic;

import grpc.client.AppOuterClass.AppType;
import grpc.client.EventOuterClass.Event;
import grpc.client.EventOuterClass.EventMsg;
import grpc.client.EventOuterClass.EventMsgList;
import grpc.client.EventOuterClass.EventPara;
import grpc.client.Id.SubaoId;
import grpc.client.Id.SubaoId.Builder;

import java.util.ArrayList;
import java.util.List;

import android.text.TextUtils;
import cn.wsds.gamemaster.useraction.UserAction;
import cn.wsds.gamemaster.useraction.UserActionList;
import cn.wsds.gamemaster.useraction.VersionInfo;

import com.google.protobuf.InvalidProtocolBufferException;

public class UserActionSerializer implements cn.wsds.gamemaster.useraction.UserActionManager.Serializer {

	private static grpc.client.Version.VersionInfo serializeVersionInfo(VersionInfo versionInfo) {
		grpc.client.Version.VersionInfo.Builder builder = grpc.client.Version.VersionInfo.newBuilder();
		builder.setNumber(versionInfo.number);
		builder.setChannel(versionInfo.channel);
		builder.setOsVersion(versionInfo.osVersion);
		builder.setAndroidVersion(versionInfo.androidVersion);
		return builder.build();
	}

	private static VersionInfo unserizlizeVersionInfo(grpc.client.Version.VersionInfo vi) {
		return new VersionInfo(vi.getNumber(), vi.getChannel(), vi.getOsVersion(), vi.getAndroidVersion());
	}

	private static grpc.client.Id.SubaoId serizlizeSubaoId(String subaoId, String userId) {
		Builder builder = grpc.client.Id.SubaoId.newBuilder();
		if (subaoId != null) {
			builder.setId(subaoId);
		}
		if (userId != null) {
			builder.setUserId(userId);
		}
		return builder.build();
	}

	private static Event serizlizeEvent(UserAction ua, Event.Builder eventBuilder, EventPara.Builder paramBuilder) {
		eventBuilder.clear();
		eventBuilder.setId(ua.name);
		eventBuilder.setTime(ua.timeOfUTCSeconds);
		if (!TextUtils.isEmpty(ua.param)) {
			paramBuilder.clear();
			paramBuilder.setValue(ua.param);
			eventBuilder.addParas(paramBuilder.build());
		}
		return eventBuilder.build();
	}

	private static UserAction unserizlizeEvent(Event event) {
		String param = null;
		if (event.getParasCount() > 0) {
			EventPara ep = event.getParas(0);
			param = ep.getValue();
		}
		return new UserAction(event.getTime(), event.getId(), param);
	}

	private static EventMsg doSerializeSingle(UserActionList ual) {
		EventMsg.Builder builder = EventMsg.newBuilder();
		builder.setId(serizlizeSubaoId(ual.subaoId, ual.userId));
		builder.setType(AppType.ANDROID_APP);
		builder.setVersion(serializeVersionInfo(ual.versionInfo));
		//
		Event.Builder eventBuilder = Event.newBuilder();
		EventPara.Builder paramBuilder = EventPara.newBuilder();
		for (UserAction ua : ual) {
			builder.addEvents(serizlizeEvent(ua, eventBuilder, paramBuilder));
		}
		return builder.build();
	}

	private static UserActionList doUnserializeSingle(EventMsg eventMsg) {
		SubaoId objId = eventMsg.getId();
		UserActionList ual = new UserActionList(objId.getId(), objId.getUserId(), unserizlizeVersionInfo(eventMsg.getVersion()));
		for (Event event : eventMsg.getEventsList()) {
			ual.offer(unserizlizeEvent(event));
		}
		return ual;
	}

	@Override
	public byte[] serializeSingle(UserActionList ual) {
		return doSerializeSingle(ual).toByteArray();
	}

	@Override
	public UserActionList unserializeSingle(byte[] data) {
		try {
			EventMsg eventMsg = EventMsg.parseFrom(data);
			return doUnserializeSingle(eventMsg);
		} catch (InvalidProtocolBufferException e) {}
		return null;
	}

	@Override
	public byte[] serializeList(Iterable<UserActionList> list) {
		EventMsgList.Builder builder = EventMsgList.newBuilder();
		for (UserActionList ual : list) {
			builder.addAll(doSerializeSingle(ual));
		}
		return builder.build().toByteArray();
	}

	@Override
	public List<UserActionList> unserializeList(byte[] data, boolean merge) {
		try {
			EventMsgList eml = EventMsgList.parseFrom(data);
			List<UserActionList> result = new ArrayList<UserActionList>(eml.getAllCount());
			for (EventMsg em : eml.getAllList()) {
				UserActionList ual = doUnserializeSingle(em);
				if (!merge || !mergeUserActionListToListTail(result, ual)) {
					result.add(ual);
				}
			}
			return result;
		} catch (InvalidProtocolBufferException e) {}
		return null;
	}

	private static boolean mergeUserActionListToListTail(List<UserActionList> list, UserActionList ual) {
		if (list.isEmpty()) {
			return false;
		}
		UserActionList target = list.get(list.size() - 1);
		if (target.size() + ual.size() > (UserActionList.MAX_CAPACITY * 3 / 2)) {
			return false;
		}
		return target.merge(ual);
	}
}

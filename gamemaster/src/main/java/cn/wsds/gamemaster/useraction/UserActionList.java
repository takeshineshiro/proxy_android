package cn.wsds.gamemaster.useraction;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * 相同{@link }和{@link VersionInfo}的UserAction的列表
 */
public class UserActionList implements Iterable<UserAction> {
	
	/** 因为UserActionList要上传到服务器，所以定义一个推荐的最大容量，以避免单次上传数据量过大 */
	public static final int MAX_CAPACITY = 128;

	public final String subaoId;
	public final String userId;
	public final VersionInfo versionInfo;
	private final Queue<UserAction> queue = new LinkedList<UserAction>();

	public UserActionList(String subaoId, String userId, VersionInfo versionInfo) {
		this.subaoId = subaoId;
		this.userId = userId;
		this.versionInfo = versionInfo;
	}

	public void offer(UserAction userAction) {
		this.queue.offer(userAction);
	}

	public int size() {
		return this.queue.size();
	}

	public boolean isEmpty() {
		return this.queue.isEmpty();
	}

	@Override
	public Iterator<UserAction> iterator() {
		return this.queue.iterator();
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (o == this) {
			return true;
		}
		if (!(o instanceof UserActionList)) {
			return false;
		}
		UserActionList other = (UserActionList) o;
		if (this.queue.size() != other.queue.size()) {
			return false;
		}
		if (!com.subao.common.utils.StringUtils.isStringEqual(this.subaoId, other.subaoId)) {
			return false;
		}
		if (!this.versionInfo.equals(other.versionInfo)) {
			return false;
		}
		//
		Iterator<UserAction> it1 = this.queue.iterator();
		Iterator<UserAction> it2 = other.queue.iterator();
		while (it1.hasNext()) {
			if (!it2.hasNext()) {
				return false;
			}
			UserAction ua1 = it1.next();
			UserAction ua2 = it2.next();
			if (!ua1.equals(ua2)) {
				return false;
			}
		}
		if (it2.hasNext()) {
			return false;
		}
		return true;
	}

	/**
	 * 如果另一个UserActionList的SubaoId和Version，与自己的相等，则将另一个的数据合并到自己
	 * 
	 * @return true表示成功合并，false表示不能合并
	 */
	public boolean merge(UserActionList other) {
		if (other == null || this == other) {
			return false;
		}
		if (this.subaoId.equals(other.subaoId) && this.versionInfo.equals(other.versionInfo)) {
			this.queue.addAll(other.queue);
			return true;
		}
		return false;

	}
}

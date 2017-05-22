package cn.wsds.gamemaster.data;


public class RecommandGameInfo implements Comparable<RecommandGameInfo>{

	private int sequenceNum;
	private String gameId;
	private String name;
	private String description;
	private String iconUrl;
	private String packageUrl ;
	private String reason;

	public int getSequenceNum() {
		return sequenceNum;
	}
	
	public void setSequenceNum(int sequenceNum) {
		this.sequenceNum = sequenceNum;
	}
	
	public String getGameId() {
		return gameId;
	}
	
	public void setGameId(String gameId) {
		this.gameId = gameId;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getIconUrl() {
		return iconUrl;
	}
	
	public void setIconUrl(String iconUrl) {
		this.iconUrl = iconUrl;
	}
	
	public String getPackageUrl() {
		return packageUrl;
	}
	
	public void setPackageUrl(String packageUrl) {
		this.packageUrl = packageUrl;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	@Override
	public int compareTo(RecommandGameInfo another) {
		if(another==null){
			return -1;
		}
		return this.sequenceNum-another.sequenceNum;
	}
}

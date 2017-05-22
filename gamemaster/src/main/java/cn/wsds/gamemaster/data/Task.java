package cn.wsds.gamemaster.data;

/**
 * 任务 
 */
public class Task {
	public final String id;
	public final String name;
	public final Object data;
	public Task(String id, String name, Object data) {
		super();
		this.id = id;
		this.name = name;
		this.data = data;
	}
	
}

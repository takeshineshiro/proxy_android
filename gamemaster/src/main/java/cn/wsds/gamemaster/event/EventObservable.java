package cn.wsds.gamemaster.event;

public interface EventObservable {

	void addObserver(EventObserver o);
	void addObserver(int location, EventObserver o);
	void deleteObserver(EventObserver o);
}

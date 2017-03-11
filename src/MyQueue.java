import java.util.LinkedList;

public class MyQueue {
	private LinkedList<Integer> list;
	
	public MyQueue() {
		list = new LinkedList<Integer>();
	}
	
	public boolean isEmpty() {
		return (list.size() == 0);
	}
	
	public void enqueue(int pos) {
		list.add(pos);
	}
	
	public int dequeue() {
		int pos = list.get(0);
		list.remove(0);
		
		return pos;
	}
}

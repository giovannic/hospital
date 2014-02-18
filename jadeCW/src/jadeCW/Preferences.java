package jadeCW;


import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Preferences {
	
	List<Set<Integer>> preferences;
	Iterator<Set<Integer>> group;
	Iterator<Integer> current;
	private int next;
	
	//factory method for preferences
	public static Preferences parsePreferences(String input){
		List<Set<Integer>> preferences = new LinkedList<Set<Integer>>();
		for (String level : input.split("-")) {
			Set<Integer> levelSet = new HashSet<Integer>();
			for (String preference : level.split(" ")) {
				levelSet.add(Integer.parseInt(preference));
			}
			preferences.add(levelSet);
		}
		
		Preferences p = new Preferences();
		p.preferences = preferences;
		p.group = p.preferences.iterator();
		if(p.group.hasNext()){
			p.current = p.group.next().iterator();
		} else {
			//pre: always preferences
		}
		return p;
	}
	
	public Set<Integer> getBestPreferences(){
		return preferences.iterator().next();
	}
	
	public int getNextPreference(){
		if (current.hasNext()){
			next = current.next();
		} else {
			if(group.hasNext()){
				current = group.next().iterator();
				next = getNextPreference();
			} else {
				return -1;
			}
		}
		System.err.println(next);
		return next;
	}
	
	public void resetIterators(){
		group = preferences.iterator();
		if(group.hasNext()){
			current = group.next().iterator();
		} else {
			//pre: always preferences
		}
	}
	/**
	 * 
	 * @param compare integer to compare
	 * @param owned the integer owned 
	 * @return true iff @param{compare} is at least equally as desirable
	 * as the second argument 
	 */
	public boolean isPreferable(Integer compare, Integer owned) {
		for(Set<Integer> layer : preferences) {
			if(layer.contains(compare)) {
				return true;
			} if(layer.contains(owned)) {
				return false;
			}
		}
		//neither compare nor owned is in preferred
		return false;
		
	}

	public boolean worseThan(int whatWeHave) {
		for(Set<Integer> g : preferences){
			if (g.contains(whatWeHave)){
				return true;
			} else if (g.contains(next)) {
				return false;
			}
		}
		//shouldn't get to the end
		return true;
	}
}

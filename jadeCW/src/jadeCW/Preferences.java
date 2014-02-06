package jadeCW;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Preferences {
	
	List<Set<Integer>> preferences;
	
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
		return p;
	}
	
	public Set<Integer> getBestPreferences(){
		return preferences.iterator().next();
	}
}

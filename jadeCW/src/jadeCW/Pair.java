package jadeCW;

public class Pair {
	
	private int a;
	private int b;
	
	public Pair(int a, int b){
		this.a = Math.min(a, b);
		this.b = Math.max(a, b);
	}
	
	public int getB() {
		return b;
	}
	
	public int getA() {
		return a;
	}
	
	@Override
	public boolean equals(Object p) {
		if(!(p instanceof Pair)){
			return false;
		}
		else{
			return ((Pair) p).a == a && ((Pair) p).b == b; 
		}
		
	}
	
	public int hashCode() {
        return (a*2)+(b*3);
    }
	
	

}

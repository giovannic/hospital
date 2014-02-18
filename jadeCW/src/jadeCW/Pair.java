package jadeCW;

public class Pair {
	
	private int a;
	private int b;
	
	public Pair(int a, int b){
		this.a = a;
		this.b = b;
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
			return ((((Pair) p).getA() == this.a) && (((Pair) p).getB() == this.b))
					|| ((((Pair) p).getA() == this.b) && (((Pair) p).getB() == this.a));
		}
		
	}
	
	

}

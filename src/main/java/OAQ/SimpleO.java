package OAQ;

public class SimpleO {
	private String s1;
	private String s2;
	
	
	public SimpleO(String s1, String s2) {
		super();
		this.s1 = s1;
		this.s2 = s2;
	}
	public SimpleO() {
		super();
	}
	public String getS1() {
		return s1;
	}
	public void setS1(String s1) {
		this.s1 = s1;
	}
	public String getS2() {
		return s2;
	}
	public void setS2(String s2) {
		this.s2 = s2;
	}
	@Override
	public String toString() {
		return "SimpleO [s1=" + s1 + ", s2=" + s2 + "]";
	}
	
	
}

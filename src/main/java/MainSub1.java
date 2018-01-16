import javax.jms.JMSException;

public class MainSub1 {
	public static void main(String[] args) throws JMSException, InterruptedException {
		Subscriber sub_1 = new Subscriber();
		sub_1.create("sub-1", "pub.t");
		synchronized (sub_1) {
			sub_1.wait();
		}
		sub_1.closeConnection();
	}
}

import javax.jms.JMSException;

public class MainSub2 {

	public static void main(String[] args) throws JMSException, InterruptedException {
		Subscriber sub_2 = new Subscriber();
		sub_2.create("sub-2", "pub.t");
		synchronized (sub_2) {
			sub_2.wait();
		}
		sub_2.closeConnection();

	}

}

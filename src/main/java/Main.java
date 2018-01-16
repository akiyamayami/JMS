import javax.jms.JMSException;

public class Main {
	private static Publisher pub_1, publisherMultipleConsumers, publisherNonDurableSubscriber;
	private static Subscriber subscriberPublishSubscribe, subscriber1MultipleConsumers, subscriber2MultipleConsumers,
			subscriber1NonDurableSubscriber, subscriber2NonDurableSubscriber;

	public static void main(String[] args) throws JMSException, InterruptedException {
		pub_1 = new Publisher();
		pub_1.create("pub-1", "pub.t");
		int i = 0;
		while (true) {
			Thread.sleep(1000);
			i++;
			pub_1.sendName("Peregrin" + i, "Took" + i);
		}
	}

}

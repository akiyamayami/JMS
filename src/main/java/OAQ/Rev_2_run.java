package OAQ;

import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;

public class Rev_2_run implements Runnable {

	public void run() {
		rev_12 rv1 = new rev_12();
		try {
			rv1.init();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		List<MessageConsumer> listConsumer = new ArrayList<MessageConsumer>();
		String[] listQueueAddress = {"REQUESTS_MSG_QUEUE_2","REQUESTS_MSG_QUEUE_3"};
		for (int i = 0; i < listQueueAddress.length; i++) {
			try {
				listConsumer.add(rv1.createConsume(rev_12.session, "aq_admin", listQueueAddress[i]));
			} catch (JMSException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
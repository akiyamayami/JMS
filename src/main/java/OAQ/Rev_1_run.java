package OAQ;

import java.util.List;
import java.util.Scanner;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;

public class Rev_1_run implements Runnable {
	private List<MessageConsumer> listconsume;

	public Rev_1_run(List<MessageConsumer> listconsume) {
		super();
		this.listconsume = listconsume;
	}

	public void run() {
		Scanner scanner = new Scanner(System.in);
		//boolean flag = false;
		while (true) {
			String s = scanner.next();
			if (s != null && !s.isEmpty()) {
				int number = Integer.parseInt(s);
				try {
					listconsume.get(number).close();
				} catch (JMSException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}

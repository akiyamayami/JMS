package test2;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

public class ConsumerMessageListener implements MessageListener {
	private String consumerName;

	public ConsumerMessageListener(String consumerName) {
		this.consumerName = consumerName;
	}

	synchronized public void onMessage(Message message) {
		//TextMessage textMessage = (TextMessage) message;
		try {
			BytesMessage bytesXMLMessage = ((BytesMessage) message);
			byte[] b = new byte[(int) bytesXMLMessage.getBodyLength()];
			bytesXMLMessage.readBytes(b);
			//Print Message received as String
			System.out.println("Message received: "+new String(b));
			//Get JMS type of message
			System.out.println(bytesXMLMessage.getJMSType());
			//message.acknowledge();			
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

}
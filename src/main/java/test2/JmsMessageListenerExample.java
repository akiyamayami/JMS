package test2;

import java.net.URI;
import java.util.Properties;
import java.util.Scanner;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;

public class JmsMessageListenerExample {
	public static void main(String[] args) throws Exception {
		
		BrokerService broker = BrokerFactory.createBroker(new URI("broker:(tcp://localhost:61618)"));
		broker.start();
		Connection connection = null;
		try {
			ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61618");
			connection = connectionFactory.createConnection();
			Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Queue queue = session.createQueue("customerQueue");
			//String payload = "Important Task2";
			//Scanner sc = new Scanner(System.in);
			int i = 0;
			while(true) {
				Thread.sleep(1000);
				Message msg = session.createTextMessage("Message " + i);
				MessageProducer producer = session.createProducer(queue);
				System.out.println("Sending text '" + "Message " + i + "'");
				producer.send(msg);
				i++;
			}
			

//			// Consumer
//			MessageConsumer consumer = session.createConsumer(queue);
//			consumer.setMessageListener(new ConsumerMessageListener("Consumer"));
//			connection.start();
//			Thread.sleep(1000);
//			session.close();
		} finally {
			if (connection != null) {
				connection.close();
			}
		}
	}
}

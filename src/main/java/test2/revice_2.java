package test2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;

public class revice_2 {

	private ConnectionFactory connectionFactory;
	private Session session;
	private Queue queue;
	private static Connection connection = null;
	public static boolean quit = false;
	private void init() throws JMSException {
		connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61618");
		connection = connectionFactory.createConnection();
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		queue = session.createQueue("customerQueue");
		MessageConsumer consumer = session.createConsumer(queue);
		consumer.setMessageListener(new ConsumerMessageListener("Consumer2"));
	}
	private void close() throws JMSException {
		connection.stop();
		session.close();
	}
	public static void main(String[] args) {
		
		try {
			revice_2 rv = new revice_2();
			rv.init();
			connection.start();
			synchronized (rv) {
				rv.wait();
			}
			System.out.println("quit");
			rv.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	

}

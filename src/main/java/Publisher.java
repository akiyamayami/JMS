import java.util.Properties;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

public class Publisher {
	private static String clientId;
	private Connection connection;
	private static Session session;
	private static MessageProducer messageProducer;



	public void create(String clientId, String topicName) throws JMSException {
		this.clientId = clientId;

		// create a Connection Factory
		ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(ActiveMQConnection.DEFAULT_BROKER_URL);

		// create a Connection
		connection = connectionFactory.createConnection();
		connection.setClientID(clientId);

		// create a Session
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

		// create the Topic to which messages will be sent
		Topic topic = session.createTopic(topicName);

		// create a MessageProducer for sending messages
		messageProducer = session.createProducer(topic);
	}

	public void closeConnection() throws JMSException {
		connection.close();
	}

	public static void sendName(String firstName, String lastName) throws JMSException {
		String text = firstName + " " + lastName;

		// create a JMS TextMessage
		TextMessage textMessage = session.createTextMessage(text);

		// send the message to the topic destination
		messageProducer.send(textMessage);
		System.out.println(clientId + ": sent message with text=" + text );
	}
	

}

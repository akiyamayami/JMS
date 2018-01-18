package OAQ;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.jms.BytesMessage;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;

import oracle.AQ.AQQueueTable;
import oracle.AQ.AQQueueTableProperty;
import oracle.jdbc.pool.OracleDataSource;
import oracle.jms.AQjmsDestination;
import oracle.jms.AQjmsDestinationProperty;
import oracle.jms.AQjmsFactory;
import oracle.jms.AQjmsQueueSender;
import oracle.jms.AQjmsSession;
import oracle.xdb.XMLType;
import test2.ConsumerMessageListener;

public class Send_2 {

	public static QueueConnection getConnection() {
		QueueConnectionFactory QFac = null;
		QueueConnection QCon = null;
		try {
			QFac = AQjmsFactory.getQueueConnectionFactory(getOracleDataSource());
			QCon = QFac.createQueueConnection();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return QCon;
	}
	
	public static OracleDataSource getOracleDataSource() throws SQLException {
		OracleDataSource ds = new OracleDataSource();
		ds.setDriverType("thin");
		ds.setServerName("172.16.13.10");
		ds.setPortNumber(1521);
		ds.setServiceName("MOMOBUSINESSDEV");
		// ds.setDatabaseName("xe"); // sid
		ds.setUser("aq_admin");
		ds.setPassword("123456");

		return ds;
	}


	public static void sendMessage(String user, String queueName) throws SQLException, IOException, InstantiationException, IllegalAccessException {

		try {
			QueueConnection QCon = getConnection();
			QueueSession session = QCon.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);
			QCon.start();
			Queue queue = ((AQjmsSession) session).getQueue(user, queueName);
			MessageProducer producer = session.createProducer(queue);
			SimpleO x = new SimpleO("3 Send","3 Send");
			BytesMessage bs = session.createBytesMessage();
			bs.writeObject(x.toString().getBytes());
			producer.send(bs);
			
			session.close();
			producer.close();
			QCon.close();

		} catch (JMSException e) {
			e.printStackTrace();
			return;
		}
	}

	public static void main(String args[]) throws SQLException, InterruptedException, IOException, InstantiationException, IllegalAccessException {
		String userName = "aq_admin";
		String queue = "REQUESTS_MSG_QUEUE_3";
		while(true) {
			//System.out.println("3 Send");
			Thread.sleep(1500);
			sendMessage(userName, queue);
		}
		
		
		
	}
	
}



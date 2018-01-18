package OAQ;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.springframework.core.annotation.Order;

import oracle.AQ.AQDriverManager;
import oracle.AQ.AQEnqueueOption;
import oracle.AQ.AQMessage;
import oracle.AQ.AQMessageProperty;
import oracle.AQ.AQObjectPayload;
import oracle.AQ.AQOracleRawPayload;
import oracle.AQ.AQQueue;
import oracle.AQ.AQQueueProperty;
import oracle.AQ.AQQueueTable;
import oracle.AQ.AQRawPayload;
import oracle.AQ.AQSession;
import oracle.jdbc.driver.OracleConnection;
import oracle.jdbc.pool.OracleDataSource;
import oracle.jms.AQjmsFactory;
import oracle.jms.AQjmsSession;
import oracle.jpub.runtime.MutableStruct;
import oracle.sql.CustomDatum;
import oracle.sql.Datum;
import oracle.sql.ORAData;
import oracle.sql.STRUCT;
import oracle.sql.StructDescriptor;
import oracle.xdb.XMLType;
import test2.ConsumerMessageListener;
import oracle.jdbc.oracore.OracleTypeADT;

public class rev_12 {
	public static boolean quit = false;
	static Session session;
	MutableStruct _struct;
	private static QueueConnection QCon;

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

	public static QueueConnection getConnection() {
		QueueConnectionFactory QFac = null;
		QueueConnection QCon = null;
		try {

			QFac = AQjmsFactory.getQueueConnectionFactory(getOracleDataSource());
			// create connection
			QCon = QFac.createQueueConnection();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return QCon;
	}

	public static void init() throws InterruptedException {
		try {
			QueueConnection QCon = getConnection();
			session = QCon.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);
			QCon.start();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	public MessageConsumer createConsume(Session session, String user, String queueName) throws JMSException {
		Queue queue = ((AQjmsSession) session).getQueue(user, queueName);
		MessageConsumer consumer = session.createConsumer(queue);
		consumer.setMessageListener(new ConsumerMessageListener("Cus1"));
		return consumer;
	}

	public static void main(String[] args)
			throws InterruptedException, JMSException, SQLException, ClassNotFoundException {

		 rev_12 rv1 = new rev_12();
		 rv1.init();
		 List<MessageConsumer> listConsumer = new ArrayList<MessageConsumer>();
		 String[] listQueueAddress = {"REQUESTS_MSG_QUEUE_2","REQUESTS_MSG_QUEUE_3"};
		 for (int i = 0; i < listQueueAddress.length; i++) {
		 listConsumer.add(rv1.createConsume(rev_12.session, "aq_admin",
		 listQueueAddress[i]));
		 }
		 Thread t1 = new Thread(new Rev_1_run(listConsumer));
		 t1.start();
		 while(true) {
		 Thread.sleep(1000);
		 System.out.println("123");
		 }
//		Class.forName("oracle.AQ.AQOracleDriver");
//		Connection db_conn = getOracleDataSource().getConnection();
//		//createBackOrderQueues(db_conn);
//		init();
//		TextMessage tMsg = session.createTextMessage("mot hai ba");
//		System.out.println();
//		SimpleO x = new SimpleO("2 Send","2 Send");
//		BytesMessage bs = session.createBytesMessage();
//		bs.writeObject(x.toString().getBytes());
//		enqueue_WS_unfilled_order(db_conn, bs);
	}

	public static void createBackOrderQueues(Connection db_conn) {
		AQSession aq_sess;
		AQQueue backorders_q;
		AQQueue backorders_excp_q;
		AQQueueProperty q_prop;
		AQQueueProperty q_prop2;
		AQQueueTable mq_table;

		try {
			/* Create an AQ Session: */
			aq_sess = AQDriverManager.createAQSession(db_conn);
			// mq_table = aq_sess.getQueueTable("WS", "WS_orders_mqtab");
			mq_table = aq_sess.getQueueTable("aq_admin", "REQUESTS_QT_2");
			/*
			 * Create a back order queue in Western Region which allows a maximum of 5
			 * retries and 1 day delay between each retry.
			 */

			q_prop = new AQQueueProperty();
			q_prop.setMaxRetries(5);
			q_prop.setRetryInterval(60 * 24 * 24);

			backorders_q = aq_sess.createQueue(mq_table, "WS_backorders_que2", q_prop);

			backorders_q.start(true, true);

			/*
			 * Create an exception queue for the back order queue for Western Region.
			 */
			q_prop2 = new AQQueueProperty();
			q_prop2.setQueueType(AQQueueProperty.EXCEPTION_QUEUE);

			backorders_excp_q = aq_sess.createQueue(mq_table, "WS_backorders_excpt_que2", q_prop2);

		} catch (Exception ex) {
			System.out.println("Exception " + ex);
		}

	}

	public static void enqueue_WS_unfilled_order(Connection db_conn, BytesMessage back_order) {
		AQSession aq_sess;
		AQQueue back_order_q;
		AQEnqueueOption enq_option;
		AQMessageProperty m_property;
		AQMessage message;
		AQRawPayload rawpayload;
		AQObjectPayload obj_payload;
		byte[] enq_msg_id;

		try {
			/* Create an AQ Session: */
			aq_sess = AQDriverManager.createAQSession(db_conn);

			back_order_q = aq_sess.getQueue("aq_admin", "WS_backorders_que2");

			message = back_order_q.createMessage();

			/* Set exception queue name for this message: */
			m_property = message.getMessageProperty();

			m_property.setExceptionQueue("WS_backorders_excpt_que2");
			//rawpayload = message.getRawPayload();
			//SimpleO x = new SimpleO("2 Send","2 Send");
			//rawpayload.setStream(x.toString().getBytes(),x.toString().length());
			//obj_payload = message.getObjectPayload();
			CallableStatement cStmt = db_conn.prepareCall("{call TEXT(?)}");
			cStmt.setString(1, "123");
			cStmt.execute();
			//OracleTypeADT xxx = new OracleTypeADT();
//			obj_payload.setPayloadData(new ORAData() {
//		        public Datum toDatum(Connection c) throws SQLException {
//			        	StructDescriptor sd = StructDescriptor.createDescriptor("TEXT",c);
//	
//			    		Object[] attributes = { "qwe"};
//			    		//STRUCT();
//			    		Struct s = c.createStruct("TEXT", attributes);
//			    		return STRUCT;
//		        }
//		    });
			//obj_payload.setPayloadData(back_order);

			//enq_option = new AQEnqueueOption();

			/* Enqueue the message */
			//enq_msg_id = back_order_q.enqueue(enq_option,message);

			//db_conn.commit();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}


//	public Datum toDatum(Connection c) throws SQLException {
//		StructDescriptor sd = StructDescriptor.createDescriptor("REQUESTS_QT_3", c);
//
//		Object[] attributes = { "qwe"};
//
//		return new STRUCT(sd, c, attributes);
//	}
}

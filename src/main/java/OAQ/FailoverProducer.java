package OAQ;

import javax.jms.*;
import com.sun.messaging.ConnectionConfiguration;

import oracle.jdbc.pool.OracleDataSource;
import oracle.jms.AQjmsFactory;

import java.sql.SQLException;
import java.util.*;

public class FailoverProducer implements ExceptionListener {

	// connection factory
	private TopicConnectionFactory factory;
	// connection
	private TopicConnection pconn = null;
	// session
	private TopicSession psession = null;
	// publisher
	private TopicPublisher publisher  = null;
	// topic
	private Topic topic = null;
	// This flag indicates whether this test client is closed.
	private boolean isClosed = false;
	// auto reconnection flag
	private boolean autoReconnect = false;
	// destination name for this example.
	private static final String DURA_TEST_TOPIC = "DuraTestTopic";
	// the message counter property name
	public static final String MESSAGE_COUNTER = "MESSAGE_COUNTER";
	// the message in-doubt-bit property name
	public static final String MESSAGE_IN_DOUBT = "MESSAGE_IN_DOUBT";

	/**
	 * Constructor. Get imqReconnectEnabled property value from System property.
	 */
	public FailoverProducer() {

		try {
			autoReconnect = Boolean.getBoolean(ConnectionConfiguration.imqReconnectEnabled);
		} catch (Exception e) {
			this.printException(e);
		}

	}

	/**
	 * Connection is broken if this handler is called. If autoReconnect flag is
	 * true, this is called only if no more retries from MQ.
	 */
	public void onException(JMSException jmse) {
		this.printException(jmse);
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

	/**
	 * create MQ connection factory.
	 * 
	 * @throws JMSException
	 * @throws SQLException 
	 */
	private void initFactory() throws JMSException, SQLException {
		// get connection factory
		//factory = new com.sun.messaging.TopicConnectionFactory();
		factory = AQjmsFactory.getTopicConnectionFactory(getOracleDataSource());
	}

	/**
	 * JMS setup. Create a Connection,Session, and Producer.
	 * 
	 * If any of the JMS object creation fails (due to system failure), it retries
	 * until it succeeds.
	 *
	 */
	private void initProducer() {

		boolean isConnected = false;

		while (isClosed == false && isConnected == false) {

			try {
				println("producer client creating connection ...");

				// create connection
				pconn = factory.createTopicConnection();
				pconn.setClientID("pub_1");
				// set connection exception listener
				pconn.setExceptionListener(this);

				// create topic session
				psession = pconn.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

				// get destination
				topic = psession.createTopic(DURA_TEST_TOPIC);

				// publisher
				publisher = psession.createPublisher(topic);

				// set flag to true
				isConnected = true;

				println("producer ready.");
			} catch (Exception e) {

				println("*** connect failed ... sleep for 5 secs.");

				try {
					// close resources.
					if (pconn != null) {
						pconn.close();
					}
					// pause 5 secs.
					Thread.sleep(5000);

				} catch (Exception e1) {
					;
				}
			}
		}
	}

	/**
	 * Start test. This sends JMS messages in a loop (forever).
	 */
	public void run() {

		try {
			// create MQ connection factory.
			initFactory();

			// create JMS connection,session, and producer
			initProducer();

			// send messages forever.
			sendMessages();
		} catch (Exception e) {
			this.printException(e);
		}
	}

	/**
     * Send persistent messages to a topic forever.  This shows how
     * to handle failover for a message producer.
     */
    private void sendMessages() {
    	
    	//this is set to true if send failed.
        boolean messageInDoubt = false;
        
        //message to be sent
        TextMessage m = null;
        
        //msg counter
        long msgcount = 0;

        while (isClosed == false) {
        	
            try {
            	
            	/**
            	 * create a text message 
            	 */
                m = psession.createTextMessage();
               
                /**
                 * the MESSAGE_IN_DOUBT bit is set to true if 
                 * you get an exception for the last message.
                 */
                if ( messageInDoubt == true ) {
                    m.setBooleanProperty (MESSAGE_IN_DOUBT, true);
                    messageInDoubt = false;
                    
                    println("MESSAGE_IN_DOUBT bit is set to true  for msg: " + msgcount);
                } else {
                    m.setBooleanProperty (MESSAGE_IN_DOUBT, false);
                }
                
                //set message counter
                m.setLongProperty(MESSAGE_COUNTER, msgcount);
                
                //set message body
                m.setText("msg: " + msgcount);
                
                //send the msg
                //publisher.send(m, DeliveryMode.PERSISTENT, 4, 0);
                publisher.publish(m);
                println("sent msg: " + msgcount);
                
                /**
                 * reset counetr if reached max long value.
                 */
                if (msgcount == Long.MAX_VALUE) {
                	msgcount = 0;
                	
                	println ("Reset message counter to 0.");
                }
                
                //increase counter
                msgcount ++;
                
                Thread.sleep(1000);

            } catch (Exception e) {

                if ( isClosed == false ) {
                	
                    //set in doubt bit to true.
                    messageInDoubt = true;

                    this.printException(e);
                   
                    //init producer only if auto reconnect is false.
                    if ( autoReconnect == false ) {
                        this.initProducer();
                    }
                }
            }
        }
    }

	/**
	 * Close this example program.
	 */
	public synchronized void close() {

		try {
			isClosed = true;
			pconn.close();

			notifyAll();
		} catch (Exception e) {
			this.printException(e);
		}
	}

	/**
	 * print the specified exception.
	 * 
	 * @param e
	 *            the exception to be printed.
	 */
	private void printException(Exception e) {
		System.out.println(new Date().toString());
		e.printStackTrace();
	}

	/**
	 * print the specified message.
	 * 
	 * @param msg
	 *            the message to be printed.
	 */
	private void println(String msg) {
		System.out.println(new Date() + ": " + msg);
	}

	/**
	 * Main program to start this example.
	 */
	public static void main(String args[]) {
		FailoverProducer fp = new FailoverProducer();
		fp.run();
	}

}

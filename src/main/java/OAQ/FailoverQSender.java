package OAQ;

import java.sql.SQLException;
import java.util.Date;
import javax.jms.*;
import com.sun.messaging.jms.Connection;
import com.sun.messaging.jms.notification.*;

import oracle.jdbc.pool.OracleDataSource;
import oracle.jms.AQjmsFactory;

/**
 *
 * This sample program uses a transacted session to send messages. It is
 * designed to run with test.jmsclient.ha.FailoverQReceiver
 * 
 * @version 1.0
 */
public class FailoverQSender implements ExceptionListener, EventListener, Runnable {
	// constant - commit property name
	public static final String COMMIT_PROPERTY_NAME = "COMMIT_PROPERTY";
	// constant - message counter
	public static final String MESSAGE_COUNTER = "counter";
	// constant - destination name
	public static final String TEST_DEST_NAME = "REQUESTS_MSG_QUEUE_1";
	// queue connection
	QueueConnection conn = null;
	// session
	QueueSession session = null;
	// queue sender
	QueueSender sender = null;
	// queue destination
	Queue queue = null;

	// commmitted counter.
	private int commitCounter = 0;
	// current message counter
	private int currentCounter = 0;
	// set to true if the application is connected to the broker.
	private boolean isConnected = false;

	/**
	 * Default constructor - do nothing. Properties are passed in from init()
	 * method.
	 * @throws SQLException 
	 */
	public FailoverQSender() throws SQLException {

		// set up JMS environment
		setup();
	}

	/**
     * Connection Exception listener.
     */
    public void onException (JMSException e) {

        //The run() method will exit.
        this.isConnected = false;

        log ("Exception listener is called.  Connection is closed by MQ client runtime." );
        log (e);
    }

	/**
	 * this method is called when a MQ connection event occurred.
	 */
	public void onEvent(Event connectionEvent) {
		log(connectionEvent);
	}

	/**
	 * Rollback the application data.
	 *
	 */
	private void rollBackApplication() {

		this.currentCounter = this.commitCounter;
		log("Application rolled back., current (commit) counter: " + currentCounter);
	}

	/**
	 * Roll back the current jms session.
	 */
	private void rollBackJMS() {

		try {

			log("Rolling back JMS ...., commit counter: " + commitCounter);
			session.rollback();
		} catch (JMSException jmse) {
			log("Rollback failed");
			log(jmse);
			// application may decide to log and continue sending messages
			// without closing the application.
			close();
		}
	}

	/**
	 * rollback application data and jms session.
	 *
	 */
	private void rollBackAll() {
		// rollback jms
		rollBackJMS();
		// rollback app data
		rollBackApplication();
	}

	/**
	 * close JMS connection and stop the application
	 *
	 */
	private void close() {

		try {
			if (conn != null) {
				// close the connection
				conn.close();
			}
		} catch (Exception e) {
			// log exception
			log(e);
		} finally {
			// set flag to true. application thread will exit
			isConnected = false;
		}
	}

	/**
	 * Send messages in a loop until the connection is closed. Session is committed
	 * for each message sent.
	 */
	public void run() {

		// start producing messages
		while (isConnected) {

			try {
				// reset message counter if it reaches max int value
				checkMessageCounter();
				// create a message
				Message m = session.createMessage();
				// get the current message counter value
				int messageCounter = this.getMessageCounter();
				// set message counter to message property
				m.setIntProperty(MESSAGE_COUNTER, messageCounter);
				// set commit property
				m.setBooleanProperty(COMMIT_PROPERTY_NAME, true);
				// send the message
				sender.send(m);

				//log("Sending message: " + messageCounter + ", current connected broker: " + this.getCurrentConnectedBrokerAddress());

				// commit the message
				this.commit();

				// pause 3 seconds
				sleep(100);

			} catch (TransactionRolledBackException trbe) {
				// rollback app data
				rollBackApplication();
			} catch (JMSException jmse) {
				if (isConnected == true) {
					// rollback app data and JMS session
					rollBackAll();
				}
			}
		}
	}

	/**
	 * Reset all counters if integer max value is reached.
	 */
	private void checkMessageCounter() {

		if (currentCounter == Integer.MAX_VALUE) {
			currentCounter = 0;
			commitCounter = 0;
		}
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
	 * Set up testing parameters - connection, destination, etc
	 * @throws SQLException 
	 */
	protected void setup() throws SQLException {
		try {
			// get connection factory
			QueueConnectionFactory factory = AQjmsFactory.getQueueConnectionFactory(getOracleDataSource());
			// create a queue connection
			conn = factory.createQueueConnection();

			// set exception listener
			conn.setExceptionListener(this);

			// set event listener
			//((com.sun.messaging.jms.Connection) conn).setEventListener(this);
			//conn.setEventListener(this);
			// get destination name
			String destName = TEST_DEST_NAME;

			// create a transacted session
			session = conn.createQueueSession(true, Session.AUTO_ACKNOWLEDGE);

			// get destination
			queue = session.createQueue(destName);
			// create queue sender
			sender = session.createSender(queue);

			// set isConnected flag to true.
			this.isConnected = true;

		} catch (JMSException jmse) {
			this.isConnected = false;
		}
	}

	/**
	 * get the next message counter.
	 */
	private synchronized int getMessageCounter() {
		return ++currentCounter;
	}

	/**
	 * commit the current transaction/session.
	 */
	private void commit() throws JMSException {
		session.commit();
		this.commitCounter = currentCounter;

		log("Transaction committed, commit counter: " + commitCounter);
	}


	/**
	 * log a string message.
	 * 
	 * @param msg
	 */
	private synchronized void log(String msg) {
		System.out.println(new Date() + ": " + msg);
	}

	/**
	 * Log an exception received.
	 */
	private synchronized void log(Exception e) {
		System.out.println(new Date() + ": Exception:");
		e.printStackTrace();
	}

	/**
	 * Log the specified MQ event.
	 */
	private synchronized void log(Event event) {

		try {
			System.out.println(new Date() + ": Received MQ event notification.");
			System.out.println("*** Event code: " + event.getEventCode());
			System.out.println("*** Event message: " + event.getEventMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * pause the specified milli seconds.
	 */
	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (java.lang.InterruptedException inte) {
			log(inte);
		}
	}

	/**
	 * The main program.
	 * @throws SQLException 
	 */
	public static void main(String args[]) throws SQLException {
		FailoverQSender fp = new FailoverQSender();
		fp.run();
	}
}

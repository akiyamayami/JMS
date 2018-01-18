package OAQ;

import java.sql.SQLException;
import java.util.Date;
import java.util.Vector;
import javax.jms.*;
import com.sun.messaging.jms.notification.*;

import oracle.jdbc.pool.OracleDataSource;
import oracle.jms.AQjmsFactory;

/**
 * This sample program uses a transacted session to receive messages. It is
 * designed to run with test.jmsclient.ha.FailoverQSender.
 *
 * @version 1.0
 */
public class FailoverQReceiver implements ExceptionListener, EventListener, Runnable {

	// queue connection
	private QueueConnection conn = null;
	// queue session
	private QueueSession session = null;
	// qreceiver
	private QueueReceiver qreceiver = null;
	// queue destination
	private Queue queue = null;

	// commmitted counter.
	private int commitCounter = 0;
	// flag to indicate if the connection is connected to the broker.
	private boolean isConnected = false;
	// flag to indicate if current connection is to HA broker cluster.
	private boolean isHAConnection = false;
	// application data holder.
	private Vector data = new Vector();

	/**
	 * Default constructor - JMS setup.
	 * @throws SQLException 
	 */
	public FailoverQReceiver() throws SQLException {
		// set up JMS environment
		setup();
	}

	/**
     * Connection Exception listener.
     */
    public void onException (JMSException e) {

        //The run() method will exit.
        this.isConnected = false;

        log ("Exception listener is called. Connection is closed by MQ client runtime." );
        log (e);
    }

	/**
	 * log the connection event.
	 */
	public void onEvent(Event connectionEvent) {
		log(connectionEvent);
	}

	/**
     * Roll back application data.
     */
    private void rollBackApplication() {
        //reset application data
    	    this.reset();

        log ("Rolled back application data, current commit counter: " + commitCounter);
    }

	/**
	 * Clear the application data for the current un-committed transaction.
	 */
	private void reset() {
		data.clear();
	}

	/**
	 * Roll back JMS transaction and application.
	 */
	private void rollBackAll() {
		try {
			// rollback JMS
			rollBackJMS();
			// rollback application data
			rollBackApplication();
		} catch (Exception e) {

			log("rollback failed. closing JMS connection ...");

			// application may decide NOT to close connection if rollback failed.
			close();
		}
	}

	/**
     * Roll back jms session.
     */
    private void rollBackJMS() throws JMSException {
		      session.rollback();
		      log("JMS session rolled back ...., commit counter:  " + commitCounter);

	   }

	/**
	 * Close JMS connection and exit the application.
	 */
	private void close() {
		try {
			if (conn != null) {
				conn.close();
			}
		} catch (Exception e) {
			log(e);
		} finally {
			isConnected = false;
		}
	}

	/**
	 * Receive, validate, and commit messages.
	 */
	public void run() {

		// produce messages
		while (isConnected) {

			try {
				// receive message
				Message m = qreceiver.receive();
				// process message -- add message to the data holder
				processMessage(m);
				// check if the commit flag is set in the message property
				if (shouldCommit(m)) {
					// commit the transaction
					commit(m);
				}
				Thread.sleep(1000);
				
			} catch (TransactionRolledBackException trbe) {
				log("transaction rolled back by MQ  ...");
				// rollback application data
				rollBackApplication();
			} catch (JMSException jmse) {
				// The exception can happen when receiving messages
				// and the connected broker is killed.
				if (isConnected == true) {
					// rollback MQ and application data
					rollBackAll();
				}

			} catch (Exception e) {
				log(e);

				// application may decide NOT to close the connection
				// when an unexpected Exception occurred.
				close();
			}
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

			// create jms connection
			conn = factory.createQueueConnection();

			// set exception listener
			conn.setExceptionListener(this);

			// set event listener
			//((com.sun.messaging.jms.Connection) conn).setEventListener(this);

			// test if this is a HA connection
			//isHAConnection = ((com.sun.messaging.jms.Connection) conn).isConnectedToHABroker();
			//log("Is connected to HA broker cluster: " + isHAConnection);

			// get destination name
			String destName = FailoverQSender.TEST_DEST_NAME;

			// create a transacted session
			session = conn.createQueueSession(true, -1);

			// get destination
			queue = session.createQueue(destName);

			// create queue receiver
			qreceiver = session.createReceiver(queue);
			// set isConnected flag to true
			isConnected = true;
			// start the JMS connection
			conn.start();
			log("Ready to receive on destination: " + destName);
		} catch (JMSException jmse) {
			isConnected = false;
			log(jmse);
			close();
		}
	}

	/**
	 * Check if we should commit the transaction.
	 */
	private synchronized boolean shouldCommit(Message m) {

		boolean flag = false;

		try {
			// get the commit flag set by the FailoverQSender
			flag = m.getBooleanProperty(FailoverQSender.COMMIT_PROPERTY_NAME);

//			if (flag) {
//				// check if message property contains expected message counter
//				validate(m);
//			}

		} catch (JMSException jmse) {
			log(jmse);
		}

		return flag;
	}

	/**
     * A very simple validation only.  More logic may be added to validate
     * message ordering and message content.
     * @param m Message  The last message received for the current transaction.
     */
    private void validate (Message m) {

      try {
       //get message counter property
       int counter = m.getIntProperty(FailoverQSender.MESSAGE_COUNTER);
       //The counter is set sequentially and must be received in right order.
       //Each message is committed after validated.
            if (counter != (commitCounter + 1)) {
				        this.printData();
				        throw new RuntimeException("validation failed.");
			        }

            log ("messages validated.  ready to commit ...");
            } catch (JMSException jmse) {
              log (jmse);
            
        	printData();
            
            throw new RuntimeException("Exception occurred during validation: " + jmse);
        }
    }

	/**
     * Get the message counter and put it in the data holder.
     * @param m the current message received
     */
    private synchronized void processMessage(Message m)  throws JMSException {

		// get message counter. this value is set by the FailoverQSender.
		int ct = m.getIntProperty(FailoverQSender.MESSAGE_COUNTER);
		// log the message
		//log("received message: " + ct  +", current connected broker:  " + this.getCurrentConnectedBrokerAddress());
		
		// saved the data in data holder.
		data.addElement(new Integer(ct));
	}

	/**
	 * commit the current transaction.
	 * 
	 * @param m
	 *            the last received message to be committed.
	 * @throws JMSException
	 *             if commit failed.
	 */
	private void commit(Message m) throws JMSException {
		// commit the transaction
		session.commit();

		// get the current message counter
		int counter = m.getIntProperty(FailoverQSender.MESSAGE_COUNTER);
		// set the commit counter

		commitCounter = counter;
		// clear app data
		this.reset();

		log("Messages committed, commitCounter: " + commitCounter);
	}

	/**
	 * log exception.
	 */
	private synchronized void log(Exception e) {
		System.out.println(new Date() + ": Exception Stack Trace: ");
		e.printStackTrace();
	}

	/**
	 * log connection event.
	 */
	private synchronized void log(Event event) {

		try {
			System.out.println(new Date() + ": Received MQ event notification.");
			System.out.println("*** Event Code: " + event.getEventCode());
			System.out.println("*** Event message: " + event.getEventMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Log the specified message.
	 */
	private void log(String msg) {
		System.out.println(new Date() + ": " + msg);
	}

	/**
	 * print values stored in the data holder.
	 *
	 */
	private void printData() {

		for (int i = 0; i < data.size(); i++) {
			log(" *** data index " + i + " = " + data.elementAt(i));
		}
	}

//	private String getCurrentConnectedBrokerAddress() {
//		return ((com.sun.messaging.jms.Connection) conn).getBrokerAddress();
//	}

	/**
	 * The main method. This starts the failover queue receiver.
	 * @throws SQLException 
	 */
	public static void main(String args[]) throws SQLException {
		FailoverQReceiver fqr = new FailoverQReceiver();
		fqr.run();
	}

}
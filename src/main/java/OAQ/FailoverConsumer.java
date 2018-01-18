package OAQ;

import java.util.Date;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Connection;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TransactionRolledBackException;
import com.sun.messaging.ConnectionConfiguration;

public class FailoverConsumer implements ExceptionListener, Runnable {

	// JMS connection
	private Connection conn = null;
	// JMS session
	private Session session = null;
	// JMS Message consumer
	private MessageConsumer messageConsumer = null;
	// JMS destination.
	private Destination destination = null;
	// flag indicates whether this program should continue running.
	private boolean isConnected = false;
	// destination name.
	private static final String DURA_TEST_TOPIC = "DuraTestTopic";
	// the commit counter, for information only.
	private long commitCounter = 0;

	/**
	 * message counter property set by the producer.
	 */
	public static final String MESSAGE_COUNTER = "MESSAGE_COUNTER";

	/**
	 * Message in doubt bit set by the producer
	 */
	public static final String MESSAGE_IN_DOUBT = "MESSAGE_IN_DOUBT";

	/**
	 * receive time out
	 */
	public static final long RECEIVE_TIMEOUT = 0;

	/**
	 * Default constructor - Set up JMS Environment.
	 */
	public FailoverConsumer() {
		setup();
	}

	/*
	 * Connection Exception listener. This is called when connection breaks and no
	 * reconnect attempts are performed by MQ client runtime.
	 */
	public void onException(JMSException e) {

		print("Reconnect failed.  Shutting down the connection ...");

		/**
		 * Set this flag to false so that the run() method will exit.
		 */
		this.isConnected = false;
		e.printStackTrace();
	}

	/**
     * Best effort to roll back a jms session.  When a broker crashes, an
     * open-transaction should be rolled back.  But the re-started broker 
     * may not have the uncommitted tranaction information due to system
     * failure.  In a situation like this, an application can just quit
     * calling rollback after retrying a few times  The uncommitted 
     * transaction (resources) will eventually be removed by the broker.
     */
    private void rollBackJMS() {
    	
    	//rollback fail count
    	int failCount = 0;
    	
        boolean keepTrying = true;
        
        while ( keepTrying ) {

            try {

                print ("<<< rolling back JMS ...., consumer commit counter: " +  this.commitCounter);

                session.rollback();
                
                print("<<< JMS rolled back ...., consumer commit counter: " + this.commitCounter);
                keepTrying = false;
            } catch (JMSException jmse) {
               
            	failCount ++;
                jmse.printStackTrace();

                sleep (3000); //3 secs
                
                if ( failCount == 1 ) {

                    print ("<<< rollback failed : total count" + failCount);
                    keepTrying = false;
                }
            }
        }
    }

	/**
	 * Close the JMS connection and exit the program.
	 *
	 */
	private void close() {
		try {

			if (conn != null) {
				conn.close();
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			this.isConnected = false;
		}
	}

	/* Receive messages in a loop until closed. */

	public void run() {

		while (isConnected) {

			try {

				/* receive message with specified timeout. */

				Message m = messageConsumer.receive(RECEIVE_TIMEOUT);

				/* process the message. */
				processMessage(m);

				/* commit JMS transaction. */
				this.commit();

				/* increase the commit counter. */
				this.commitCounter++;

			} catch (TransactionRolledBackException trbe) {

				/**
				 * the transaction is rolled back a new transaction is automatically started.
				 */
				trbe.printStackTrace();
			} catch (JMSException jmse) {

				/*
				 * The transaction is in unknown state. We need to roll back the transaction.
				 */

				jmse.printStackTrace();

				/*
				 * roll back if not closed.
				 */
				if (this.isConnected == true) {
					this.rollBackJMS();
				}

			} catch (Exception e) {

				e.printStackTrace();

				/*
				 * Exit if this is an unexpected Exception.
				 */
				this.close();

			} finally {
				;// do nothing
			}
		}

		print(" <<< consumer exit ...");
	}

	/* Set up connection, destination, etc */

	protected void setup() {

		try {

			// create connection factory
			com.sun.messaging.ConnectionFactory factory = new com.sun.messaging.ConnectionFactory();

			// set auto reconnect to true.
			factory.setProperty(ConnectionConfiguration.imqReconnectEnabled, "true");
			// A value of -1 will retry forever if connection is broken.
			factory.setProperty(ConnectionConfiguration.imqReconnectAttempts, "-1");
			// retry interval - every 10 seconds
			factory.setProperty(ConnectionConfiguration.imqReconnectInterval, "10000");
			// create connection
			conn = factory.createConnection();
			// set client ID
			conn.setClientID(DURA_TEST_TOPIC);

			// set exception listener
			conn.setExceptionListener(this);

			// create a transacted session
			session = conn.createSession(true, -1);

			// get destination
			destination = session.createTopic(DURA_TEST_TOPIC);

			// message consumer
			messageConsumer = session.createDurableSubscriber((Topic) destination, DURA_TEST_TOPIC);
			// set flag to true
			this.isConnected = true;
			// we are ready, start the connection
			conn.start();

			print("<<< Ready to receive on destination: " + DURA_TEST_TOPIC);

		} catch (JMSException jmse) {
			this.isConnected = false;
			jmse.printStackTrace();

			this.close();
		}
	}

	/**
     * Process the received message message. 
     *  This prints received message counter.
     * @param m the message to be processed.
     */
    private synchronized void processMessage(Message m) {
        
    	try {
    		//in this example, we do not expect a timeout, etc.
    		if ( m == null ) {
    			throw new RuntimeException ("<<< Received null message.  Maybe reached max time out. ");
    		}
    		
    		//get message counter property
        	long msgCtr = m.getLongProperty (MESSAGE_COUNTER);
        	
        	//get message in-doubt bit
        	boolean indoubt = m.getBooleanProperty(MESSAGE_IN_DOUBT);
        	
        	if ( indoubt) {
        		print("<<< received message: " + msgCtr + ", indoubt bit is true");
        	} else {
        		print("<<< received message: " + msgCtr);
        	}
        	
        } catch (JMSException jmse) {
            jmse.printStackTrace();
        }
    }

	/**
	 * Commit a JMS transaction.
	 * 
	 * @throws JMSException
	 */
	private void commit() throws JMSException {
		session.commit();
	}

	/**
	 * Sleep for the specified time.
	 * 
	 * @param millis
	 *            sleep time in milli-seconds.
	 */
	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (java.lang.InterruptedException inte) {
			print(inte);
		}
	}

	/**
	 * Print the specified message.
	 * 
	 * @param msg
	 *            the message to be printed.
	 */
	private static void print(String msg) {
		System.out.println(new Date() + ": " + msg);
	}

	/**
	 * Print Exception stack trace.
	 * 
	 * @param e
	 *            the exception to be printed.
	 */
	private static void print(Exception e) {
		System.out.print(e.getMessage());
		e.printStackTrace();
	}

	/**
	 * Start this example program.
	 */
	public static void main(String args[]) {
		FailoverConsumer fc = new FailoverConsumer();
		fc.run();
	}

}

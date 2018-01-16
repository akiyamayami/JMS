package jms.test.mm;


import java.net.URI;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.QueueSession;
import javax.jms.QueueReceiver;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;

public class Receiver implements MessageListener{
	private boolean quit = false;
	private QueueConnectionFactory qconFactory;
	private QueueConnection qcon;
	private QueueSession qsession;
	private QueueReceiver qreceiver;
	private Queue queue;
	
	public void init(Context ctx) throws JMSException, Exception {
		// lookup the queue object
		queue = (Queue) ctx.lookup("queueSampleQueue");

		// lookup the queue connection factory
		qconFactory = (QueueConnectionFactory) ctx.lookup("QueueConnectionFactory");

		// create a queue connection
		qcon = qconFactory.createQueueConnection();

		// create a queue session
		qsession = qcon.createQueueSession(false,Session.AUTO_ACKNOWLEDGE);

		// create a queue receiver
		qreceiver = qsession.createReceiver(queue);

		// start the connection
		qcon.start();
	}
	
	public void close() throws JMSException {
		qreceiver.close();
		qsession.close();
		qcon.close();
	}
	public static void main(String args[]) throws Exception {
		Properties env = new Properties();					   				
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"org.apache.activemq.jndi.ActiveMQInitialContextFactory");
		env.put(Context.PROVIDER_URL, "tcp://localhost:61616");
		env.put("queue.queueSampleQueue","MyNewQueue");
		// get the initial context
		InitialContext ctx = new InitialContext(env);
		Receiver rv = new Receiver();
		rv.init(ctx);
		System.out.println("JMS Ready To Receive Messages (To quit, send a \"quit\" message).");
		synchronized (rv) {
			while (!rv.quit) {
				System.out.println("while");
				try {
					rv.wait();
				} catch (InterruptedException ie) {
				}
			}
		}
		rv.close();
	}
	public void onMessage(Message msg) {
		System.out.println("revice");
		try {
			String msgText;
			if (msg instanceof TextMessage) {
				msgText = ((TextMessage) msg).getText();
			} else {
				msgText = msg.toString();
			}
			System.out.println("Message Received: " + msgText);
			if (msgText.equalsIgnoreCase("quit")) {
				synchronized (this) {
					quit = true;
					this.notifyAll(); // Notify main thread to quit
				}
			}
		} catch (JMSException jmse) {
			System.err.println("An exception occurred: " + jmse.getMessage());
		}
	}
}
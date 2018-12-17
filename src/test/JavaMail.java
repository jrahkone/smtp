package test;

import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class JavaMail {
	
	public static void main(String args[]) throws Exception {
		Properties props = System.getProperties();
		String from = "super.user@foo.bar";
		String to = "vastaan.ottaja@foo.com";
		boolean isHtml = false;
		String title = "otsikko "+(new Date()).toString();
		String msg = "tämä on testiviesti\n";
		
		String type = "text/plain";
		if (isHtml) type = "text/html; charset=utf-8";

		// Setup mail server
		props.put("mail.smtp.host", "localhost");
		props.put("mail.smtp.port", "9999");
		props.put("mail.smtp.auth", "false");  // no authentication
		Session session = Session.getDefaultInstance(props, null);

		// Define message
		MimeMessage message = new MimeMessage(session);
		message.setFrom(new InternetAddress(from));
		message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));			
		message.setSubject(title);
		message.setContent(msg,type);

		// Send message
		Transport transport = session.getTransport("smtp");
		transport.connect("localhost",null,null);
		transport.sendMessage(message, message.getAllRecipients());
		transport.close();
		System.out.println("sent mail ok");
	}
}

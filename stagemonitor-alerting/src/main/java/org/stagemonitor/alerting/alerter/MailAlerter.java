package org.stagemonitor.alerting.alerter;

import static org.stagemonitor.core.util.StringUtils.isNotEmpty;

import java.util.Properties;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import org.stagemonitor.alerting.AlertingPlugin;
import org.stagemonitor.core.Stagemonitor;

public class MailAlerter extends Alerter {

	private final AlertingPlugin alertingPlugin;
	private final AlertTemplateProcessor alertTemplateProcessor;

	public MailAlerter() {
		this(Stagemonitor.getPlugin(AlertingPlugin.class));
	}

	public MailAlerter(AlertingPlugin alertingPlugin) {
		this.alertingPlugin = alertingPlugin;
		this.alertTemplateProcessor = alertingPlugin.getAlertTemplateProcessor();
	}

	@Override
	public void alert(AlertArguments alertArguments) {
		MailRequest mailRequest = new MailRequest(alertTemplateProcessor.processShortDescriptionTemplate(alertArguments.getIncident()),
				alertingPlugin.getSmtpFrom(), alertArguments.getSubscription().getTarget())
				.textPart(alertTemplateProcessor.processPlainTextTemplate(alertArguments.getIncident()))
				.htmlPart(alertTemplateProcessor.processHtmlTemplate(alertArguments.getIncident()));
		sendMail(mailRequest);
	}

	private void sendMail(MailRequest mailRequest) {
		try {
			Session session = getSession();
			Transport transport = getTransport(session);
			try {
				MimeMessage msg = mailRequest.createMimeMessage(session);
				transport.sendMessage(msg, msg.getAllRecipients());
			} finally {
				transport.close();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Transport getTransport(Session session) throws MessagingException {
		Transport transport = session.getTransport(alertingPlugin.getSmtpProtocol());
		transport.connect(alertingPlugin.getSmtpHost(), alertingPlugin.getSmtpPort(), alertingPlugin.getSmtpUser(), alertingPlugin.getSmtpPassword());
		return transport;
	}

	private Session getSession() {
		Properties props = (Properties) System.getProperties().clone();
		if (isNotEmpty(alertingPlugin.getSmtpUser()) && isNotEmpty(alertingPlugin.getSmtpPassword())) {
			props.setProperty("mail.smtps.auth", "true");
		}
		int smtpPort = alertingPlugin.getSmtpPort();
		props.setProperty("mail.smtp.port", Integer.toString(smtpPort));
		if (smtpPort == 587) {
			props.put("mail.smtp.starttls.enable", "true");
		}
		return Session.getInstance(props);
	}

	@Override
	public String getAlerterType() {
		return "Email";
	}

	@Override
	public boolean isAvailable() {
		return isNotEmpty(alertingPlugin.getSmtpHost()) && isNotEmpty(alertingPlugin.getSmtpFrom());
	}

	@Override
	public String getTargetLabel() {
		return "Email Address";
	}

}

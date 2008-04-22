package com.gnizr.web.action.user;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import com.gnizr.core.exceptions.NoSuchUserException;
import com.gnizr.core.user.PasswordManager;
import com.gnizr.core.user.UserManager;
import com.gnizr.db.dao.User;
import com.gnizr.web.action.AbstractAction;
import com.gnizr.web.action.error.ActionErrorCode;

import freemarker.template.Configuration;
import freemarker.template.Template;

public class RequestPasswordReset extends AbstractAction{


	/**
	 * 
	 */
	private static final long serialVersionUID = 2177445666023620630L;

	private static final Logger logger = Logger.getLogger(RequestPasswordReset.class);
	
	private String username;
	
	private UserManager userManager;
	private PasswordManager passwordManager;
	private MailSender mailSender;
	private SimpleMailMessage templateMessage;
	private Configuration freemarkerEngine;
	
	public Configuration getFreemarkerEngine() {
		return freemarkerEngine;
	}

	public void setFreemarkerEngine(
			Configuration freemarkerEngine) {
		this.freemarkerEngine = freemarkerEngine;
	}

	public SimpleMailMessage getTemplateMessage() {
		return templateMessage;
	}

	public void setTemplateMessage(SimpleMailMessage templateMessage) {
		this.templateMessage = templateMessage;
	}

	public MailSender getMailSender() {
		return mailSender;
	}

	public void setMailSender(MailSender mailSender) {
		this.mailSender = mailSender;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public UserManager getUserManager() {
		return userManager;
	}

	public void setUserManager(UserManager userManager) {
		this.userManager = userManager;
	}

	public PasswordManager getPasswordManager() {
		return passwordManager;
	}

	public void setPasswordManager(PasswordManager passwordManager) {
		this.passwordManager = passwordManager;
	}

	@Override
	protected String go() throws Exception {
		User user = null;
		try{
			user = userManager.getUser(username);
		}catch(NoSuchUserException e){
			logger.debug("RequestPasswordReset: No such user in the system. Username = " + username);
			addActionError(String.valueOf(ActionErrorCode.ERROR_NO_SUCH_USER));
			return ERROR;
		}
		
		String token = passwordManager.createResetToken(user);
		if(token != null){
			if(sendPasswordResetEmail(token, user) == false){
				return ERROR;
			}
		}else{
			addActionError(String.valueOf(ActionErrorCode.ERROR_NO_RESET_TOKEN));
		}		
		return SUCCESS;
	}

	private boolean sendPasswordResetEmail(String token, User user){
		Map<String,Object> model = new HashMap<String, Object>();
		model.put("token", token);
		model.put("username",user.getUsername());
		model.put("gnizrConfiguration", getGnizrConfiguration());
		
		if(getTemplateMessage() == null){
			logger.error("RequestPasswordReset: templateMessge bean is not defined");
			addActionError(String.valueOf(ActionErrorCode.ERROR_CONFIG));
			return false;
		}
		String toEmail = user.getEmail();
		if(toEmail == null){
			logger.error("RequestPasswordReset: the email of user " + user.getUsername() + " is not defined");
			addActionError(String.valueOf(ActionErrorCode.ERROR_EMAIL_UNDEF));
			return false;
		}
		SimpleMailMessage msg = new SimpleMailMessage(getTemplateMessage());
		msg.setTo(toEmail);
		
		Template fmTemplate = null;
		String text = null;
		try{			
			fmTemplate = freemarkerEngine.getTemplate("login/notifyreset-template.ftl");
			text = FreeMarkerTemplateUtils.processTemplateIntoString(fmTemplate,model);
		}catch(Exception e){
			logger.error("RequestPasswordReset: error creating message template from Freemarker engine");
		}

		msg.setText(text);
		
		if(getMailSender() == null){
			logger.error("RequestPasswordReset: mailSender bean is not defined");
			addActionError(String.valueOf(ActionErrorCode.ERROR_CONFIG));
			return false;
		}
		try{
			getMailSender().send(msg);
			return true;
		}catch(Exception e){
			logger.error("RequestPasswordReset: send mail error. " + e);
			addActionError(String.valueOf(ActionErrorCode.ERROR_INTERNAL));
		}		
		return false;
	}
}

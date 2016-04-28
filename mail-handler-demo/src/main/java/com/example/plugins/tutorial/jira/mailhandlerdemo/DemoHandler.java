//
package com.example.plugins.tutorial.jira.mailhandlerdemo;

//import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.service.util.handler.MessageHandler;
import com.atlassian.jira.service.util.handler.MessageHandlerContext;
import com.atlassian.jira.service.util.handler.MessageHandlerErrorCollector;
import com.atlassian.jira.service.util.handler.MessageUserProcessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.mail.MailUtils;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.apache.commons.lang.StringUtils;

import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.Message;
import javax.mail.MessagingException;

@Named("DemoHandler")
public class DemoHandler implements MessageHandler
{
    private String issueKey;
    //@ComponentImport
    private final IssueKeyValidator issueKeyValidator;
    @ComponentImport
    private final MessageUserProcessor messageUserProcessor;
    public static final String KEY_ISSUE_KEY = "issueKey";

    // we can use dependency injection here too!
    @Inject
    public DemoHandler(MessageUserProcessor messageUserProcessor, IssueKeyValidator issueKeyValidator)
    {
        this.messageUserProcessor = messageUserProcessor;
        this.issueKeyValidator = issueKeyValidator;
    }

    @Override
    public void init(Map<String, String> params, MessageHandlerErrorCollector monitor)
    {
        // getting here issue key configured by the user
        issueKey = params.get(KEY_ISSUE_KEY);
        if (StringUtils.isBlank(issueKey))
        {
            // this message will be either logged or displayed to the user (if the handler is tested from web UI)
            monitor.error("Issue key has not been specified ('" + KEY_ISSUE_KEY + "' parameter). This handler will not work correctly.");
        }
        issueKeyValidator.validateIssue(issueKey, monitor);
    }

    @Override
    public boolean handleMessage(Message message, MessageHandlerContext context) throws MessagingException
    {
        // let's again validate the issue key - meanwhile issue could have been deleted, closed, etc..
        final Issue issue = issueKeyValidator.validateIssue(issueKey, context.getMonitor());

        if (issue == null) {
            return false; // returning false means that we were unable to handle this message. It may be either
            // forwarded to specified address or left in the mail queue (if forwarding not enabled)
        }
        // this is a small util method JIRA API provides for us, let's use it.
        final ApplicationUser sender = messageUserProcessor.getAuthorFromSender(message);

        if (sender == null)
        {
            context.getMonitor().error("Message sender(s) '" + StringUtils.join(MailUtils.getSenders(message), ",")
                    + "' do not have corresponding users in JIRA. Message will be ignored");
            return false;
        }

        final String body = MailUtils.getBody(message);
        final StringBuilder commentBody = new StringBuilder(message.getSubject());

        if (body != null)
        {
            commentBody.append("\n").append(StringUtils.abbreviate(body, 100000)); // let trim too long bodies
        }
        // thanks to using passed context we don't need to worry about normal run vs. test run - our call
        // will be dispatched accordingly
        context.createComment(issue, sender, commentBody.toString(), false);
        return true; // returning true means that we have handled the message successfully. It means it will be deleted next.
    }
}
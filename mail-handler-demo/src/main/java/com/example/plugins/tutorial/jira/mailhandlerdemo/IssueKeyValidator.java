//
package com.example.plugins.tutorial.jira.mailhandlerdemo;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.service.util.handler.MessageHandlerErrorCollector;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.apache.commons.lang.StringUtils;

import javax.inject.Inject;
import javax.inject.Named;

@Named("IssueKeyValidator")
public class IssueKeyValidator
{
    @ComponentImport
    private final IssueManager issueManager;

    @Inject
    public IssueKeyValidator(IssueManager issueManager)
    {
        this.issueManager = issueManager;
    }

    public Issue validateIssue(String issueKey, MessageHandlerErrorCollector collector)
    {
        if (StringUtils.isBlank(issueKey))
        {
            collector.error("Issue key cannot be undefined.");
            return null;
        }

        final Issue issue = issueManager.getIssueObject(issueKey);

        if (issue == null)
        {
            collector.error("Cannot add a comment from mail to issue '" + issueKey + "'. The issue does not exist.");
            return null;
        }

        if (!issueManager.isEditable(issue))
        {
            collector.error("Cannot add a comment from mail to issue '" + issueKey + "'. The issue is not editable.");
            return null;
        }
        return issue;
    }
}
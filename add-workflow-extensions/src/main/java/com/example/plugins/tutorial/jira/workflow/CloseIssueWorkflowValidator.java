package com.example.plugins.tutorial.jira.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.atlassian.jira.issue.Issue;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.Validator;
import com.opensymphony.workflow.InvalidInputException;
import java.util.Map;

public class CloseIssueWorkflowValidator implements Validator
{
    private static final Logger log = LoggerFactory.getLogger(CloseIssueWorkflowValidator.class);

    public void validate(Map transientVars, Map args, PropertySet ps) throws InvalidInputException
    {
        Issue issue = (Issue)transientVars.get("issue");

        // The Issue must have a fixVersion, otherwise you can't close that issue
        if (issue.getFixVersions() == null || issue.getFixVersions().isEmpty() ) {
            throw new InvalidInputException("Issue must have a fix version");
        }
    }
}

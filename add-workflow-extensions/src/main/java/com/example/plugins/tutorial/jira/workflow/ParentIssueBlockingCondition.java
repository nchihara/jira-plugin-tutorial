package com.example.plugins.tutorial.jira.workflow;

// import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.workflow.WorkflowFunctionUtils;
import com.atlassian.jira.workflow.condition.AbstractJiraCondition;
// import com.atlassian.jira.issue.status.Status;
import com.opensymphony.module.propertyset.PropertySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.StringTokenizer;

public class ParentIssueBlockingCondition extends AbstractJiraCondition
{
    private static final Logger log = LoggerFactory.getLogger(ParentIssueBlockingCondition.class);

    public static final String FIELD_WORD = "word";

    public boolean passesCondition(Map transientVars, Map args, PropertySet ps)
    {
        Issue subTask = (Issue) transientVars.get(WorkflowFunctionUtils.ORIGINAL_ISSUE_KEY);

        // JIRA 7.1.2 API
        Issue parentIssue = subTask.getParentObject();
        if (parentIssue == null)
        {
            return false;
        }

        String statuses = (String) args.get("statuses");
        StringTokenizer st = new StringTokenizer(statuses, ",");

        while(st.hasMoreTokens())
        {
            String statusId = st.nextToken();

            // Since JIRA 7.0, Issue.getStatusObject() is deprecated
            // instead of parentIssue.getStatusObject().getId(),
            // String parentIssue.getStatusID()
            if (parentIssue.getStatusId().equals(statusId))
            {
                return true;
            }
        }
        return false;
    }
}

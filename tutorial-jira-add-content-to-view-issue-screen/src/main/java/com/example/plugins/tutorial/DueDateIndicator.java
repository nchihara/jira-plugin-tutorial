package com.example.plugins.tutorial;

//import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.webfragment.contextproviders.AbstractJiraContextProvider;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class DueDateIndicator extends AbstractJiraContextProvider
{
    private static final int MILLIS_IN_DAY = 24 * 60 * 60 * 1000;

    // JIRA 7.1.2, parameters are changed
    // AbstructJiraContextProvider::getContextMap(
    //                       com.atlassian.jira.user.ApplicationUser user,
    //                       com.atlassian.jira.plugin.webfragment.model.JiraHelper jiraHelper)
    @Override
    public Map getContextMap(ApplicationUser user, JiraHelper jiraHelper) {
        Map contextMap = new HashMap();
        Issue currentIssue = (Issue) jiraHelper.getContextParams().get("issue");
        Timestamp dueDate = currentIssue.getDueDate();
        if (dueDate != null)
        {
            int currentTimeInDays = (int) (System.currentTimeMillis() / MILLIS_IN_DAY);
            int dueDateTimeInDays = (int) (dueDate.getTime() / MILLIS_IN_DAY);
            int daysAwayFromDueDateCalc = dueDateTimeInDays - currentTimeInDays + 1;
            contextMap.put("daysAwayFromDueDate", daysAwayFromDueDateCalc);
        }
        return contextMap;
    }

}
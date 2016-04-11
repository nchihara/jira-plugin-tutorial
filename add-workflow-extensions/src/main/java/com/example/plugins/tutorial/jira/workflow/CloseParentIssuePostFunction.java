package com.example.plugins.tutorial.jira.workflow;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.atlassian.annotations.PublicApi;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.IssueService.IssueResult;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.workflow.JiraWorkflow;
import com.atlassian.jira.workflow.WorkflowException;
import com.atlassian.jira.workflow.WorkflowManager;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.jira.issue.status.Status;

import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.loader.ActionDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * This is the post-function class that gets executed at the end of the transition.
 * Any parameters that were saved in your factory class will be available in the transientVars Map.
 */
@Named("CloseParentIssuePostFunction")
public class CloseParentIssuePostFunction extends AbstractJiraFunctionProvider
{
    private static final Logger log = LoggerFactory.getLogger(CloseParentIssuePostFunction.class);
//    public static final String FIELD_MESSAGE = "messageField";

    @ComponentImport
    public final SubTaskManager subTaskManager;
    @ComponentImport
    public final WorkflowManager workflowManager;
    @ComponentImport
    private final JiraAuthenticationContext  authenticationContext;

    // refer to https://docs.atlassian.com/jira/latest/com/atlassian/jira/issue/status/Status.html
    @PublicApi
    private final Status closedStatus;


    @Inject
    public CloseParentIssuePostFunction(
                                        ConstantsManager constantsManager,
                                        WorkflowManager workflowManager,
                                        SubTaskManager subTaskManager,
                                        JiraAuthenticationContext authenticationContext
                                       )
    {
        this.workflowManager = workflowManager;
        this.subTaskManager  = subTaskManager;
        this.authenticationContext = authenticationContext;
        this.closedStatus = constantsManager.getStatus(new Integer(IssueFieldConstants.CLOSED_STATUS_ID).toString());
    }

    public void execute(Map transientVars, Map args, PropertySet ps) throws WorkflowException
    {
        MutableIssue subTask = getIssue(transientVars);
        // because getParenetObject returns "Issue" object, cast it to "MutableIssue".
        MutableIssue parentIssue = (MutableIssue) subTask.getParentObject();

        // getStatusObjcet() is deprecated on JIRA 7.1
        if(IssueFieldConstants.CLOSED_STATUS_ID == Integer.parseInt(parentIssue.getStatus().getId()))
        {
            return;
        }

        // check that ALL other sub-tasks are closed or not
        Collection<Issue> subTasks = subTaskManager.getSubTaskObjects(parentIssue);

        for( Iterator<Issue> iterator = subTasks.iterator(); iterator.hasNext();)
        {
            Issue assocaitedSubTask = iterator.next();
            if(!subTask.getKey().equals(assocaitedSubTask.getKey()))
            {
                // if other associated sub-task is still OPEN - do not continue
                // getStatusObjcet() is deprecated on JIRA 7.1
                if( IssueFieldConstants.CLOSED_STATUS_ID != Integer.parseInt(assocaitedSubTask.getStatus().getId()))
                {
                    return;
                }
            }
        }

        // All sub-tasks are now closed - close the parent issue
        try
        {
            closeIssue(parentIssue);
        }
        catch(WorkflowException e)
        {
            log.error("Error occured while closing issue: " + parentIssue.getKey( ) + ": " +e, e);
            e.printStackTrace();
        }
    }
    private void closeIssue(Issue issue) throws WorkflowException
    {
        Status currentStatus = issue.getStatus();  // getStatusObject() is deprecated
        JiraWorkflow workflow = workflowManager.getWorkflow(issue);

        List<ActionDescriptor> actions = workflow.getLinkedStep(currentStatus).getActions();

        // look for the closed transition
        ActionDescriptor closeAction = null;
        for( ActionDescriptor descriptor:actions)
        {
            if(descriptor.getUnconditionalResult().getStatus().equals(closedStatus.getName()))
            {
                closeAction = descriptor;
                break;
            }
        }

        if(closeAction != null)
        {
            ApplicationUser currentUser = authenticationContext.getLoggedInUser();
            IssueService issueService = ComponentAccessor.getIssueService();
            IssueInputParameters parameters = issueService.newIssueInputParameters();

            parameters.setRetainExistingValuesWhenParameterNotProvided(true);
            IssueService.TransitionValidationResult validationResult = issueService.validateTransition(currentUser, issue.getId(), closeAction.getId(), parameters);

            IssueResult result = issueService.transition(currentUser, validationResult);
        }
    }
}
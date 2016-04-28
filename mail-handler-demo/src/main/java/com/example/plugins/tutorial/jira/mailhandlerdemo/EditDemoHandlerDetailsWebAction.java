package com.example.plugins.tutorial.jira.mailhandlerdemo;

import com.atlassian.configurable.ObjectConfigurationException;
import com.atlassian.jira.plugins.mail.webwork.AbstractEditHandlerDetailsWebAction;
import com.atlassian.jira.service.JiraServiceContainer;
import com.atlassian.jira.service.services.file.AbstractMessageHandlingService;
import com.atlassian.jira.service.util.ServiceUtils;
import com.atlassian.jira.util.collect.MapBuilder;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.jira.component.ComponentAccessor;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

@Named("EditDemoHandlerDetailsWebAction")
public class EditDemoHandlerDetailsWebAction extends AbstractEditHandlerDetailsWebAction
{
    private final IssueKeyValidator issueKeyValidator;

    // 2016-04-28
    // initializing PluginAccessor to avoid compile error.
    // and renaming pluginAccessor to myPluginAccessor
    @ComponentImport
    protected final PluginAccessor myPluginAccessor = ComponentAccessor.getPluginAccessor();

    @Inject
    public EditDemoHandlerDetailsWebAction(PluginAccessor myPluginAccessor, IssueKeyValidator issueKeyValidator)
    {
        super(myPluginAccessor);

         this.issueKeyValidator = issueKeyValidator;
    }

    private String issueKey;

    public String getIssueKey()
    {
        return issueKey;
    }

    public void setIssueKey(String issueKey)
    {
        this.issueKey = issueKey;
    }

    // this method is called to let us populate our variables (or action state)
    // with current handler settings managed by associated service (file or mail).
    @Override
    protected void copyServiceSettings(JiraServiceContainer jiraServiceContainer) throws ObjectConfigurationException
    {
        final String params = jiraServiceContainer.getProperty(AbstractMessageHandlingService.KEY_HANDLER_PARAMS);
        final Map<String, String> parameterMap = ServiceUtils.getParameterMap(params);
        issueKey = parameterMap.get(DemoHandler.KEY_ISSUE_KEY);
    }

    @Override
    protected Map<String, String> getHandlerParams()
    {
        return MapBuilder.build(DemoHandler.KEY_ISSUE_KEY, issueKey);
    }

    @Override
    protected void doValidation()
    {
        if (configuration == null)
        {
            return; // short-circuit in case we lost session, goes directly to doExecute which redirects user
        }
        super.doValidation();
        issueKeyValidator.validateIssue(issueKey, new WebWorkErrorCollector());
    }
}
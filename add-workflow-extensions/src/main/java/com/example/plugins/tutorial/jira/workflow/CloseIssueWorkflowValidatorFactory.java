package com.example.plugins.tutorial.jira.workflow;

import com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory;
import com.atlassian.jira.plugin.workflow.WorkflowPluginValidatorFactory;
import com.google.common.collect.Maps;
import com.opensymphony.workflow.loader.AbstractDescriptor;

import java.util.Map;

public class CloseIssueWorkflowValidatorFactory extends AbstractWorkflowPluginFactory implements WorkflowPluginValidatorFactory
{
    public static final String FIELD_WORD="word";

    protected void getVelocityParamsForInput(Map velocityParams)
    {

    }

    protected void getVelocityParamsForEdit(Map velocityParams, AbstractDescriptor descriptor)
    {

    }

    protected void getVelocityParamsForView(Map velocityParams, AbstractDescriptor descriptor)
    {

    }

    public Map getDescriptorParams(Map validatorParams)
    {
        // Process The map
        return Maps.newHashMap();
    }
}
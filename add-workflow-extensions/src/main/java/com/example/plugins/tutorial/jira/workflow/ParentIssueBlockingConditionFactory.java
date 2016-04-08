package com.example.plugins.tutorial.jira.workflow;

import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.issue.comparator.ConstantsComparator;
import com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory;
import com.atlassian.jira.plugin.workflow.WorkflowPluginConditionFactory;
import com.atlassian.jira.util.collect.MapBuilder;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.opensymphony.workflow.loader.AbstractDescriptor;
import com.opensymphony.workflow.loader.ConditionDescriptor;
import com.atlassian.jira.issue.status.Status;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;

/**
 * This is the factory class responsible for dealing with the UI for the post-function.
 * This is typically where you put default values into the velocity context and where you store user input.
 */
@Named("ParentIssueBlokingConditionFactory")
public class ParentIssueBlockingConditionFactory extends AbstractWorkflowPluginFactory implements WorkflowPluginConditionFactory
{
    @ComponentImport
    private final ConstantsManager constantsManager;

    @Inject
    public ParentIssueBlockingConditionFactory(ConstantsManager constantsManager)
    {
        this.constantsManager = constantsManager;
    }

    @Override
    protected void getVelocityParamsForInput(Map velocityParams)
    {
        // all available statuses
        // constantsManager.getStatusObjects() is deprecated since 7.0
        Collection<Status> statuses = constantsManager.getStatuses();
        velocityParams.put("statuses", Collections.unmodifiableCollection(statuses));
    }

    @Override
    protected void getVelocityParamsForEdit(Map velocityParams, AbstractDescriptor descriptor)
    {
        getVelocityParamsForInput(velocityParams);
        velocityParams.put("selectedStatuses", getSelectedStatusIds(descriptor));
    }

    @Override
    protected void getVelocityParamsForView(Map velocityParams, AbstractDescriptor descriptor)
    {
        Collection selectedStatusIds = getSelectedStatusIds(descriptor);
        List selectedStatuses = new LinkedList();
        for (Iterator iterator = selectedStatusIds.iterator(); iterator.hasNext();)
        {
            String statusId = (String) iterator.next();
            // constantsManager.getStatusObject() is deprecated since 7.0
            Status selectedStatus = constantsManager.getStatus(statusId);
            if (selectedStatus != null)
            {
                selectedStatuses.add(selectedStatus);
            }
        }
        // Sort the list of statuses so as they are displayed consistently
        Collections.sort(selectedStatuses, new ConstantsComparator());

        velocityParams.put("statuses", Collections.unmodifiableCollection(selectedStatuses));
    }

    public Map getDescriptorParams(Map conditionParams)
    {
        //  process the map which will contain the request parameters
        //  for now simply concatenate into a comma separated string
        //  production code would do something more robust.
        Collection statusIds = conditionParams.keySet();
        StringBuffer statIds = new StringBuffer();

        for (Iterator iterator = statusIds.iterator(); iterator.hasNext();)
        {
            statIds.append((String) iterator.next() + ",");
        }

        return MapBuilder.build("statuses", statIds.substring(0, statIds.length() - 1));
    }

    private Collection getSelectedStatusIds(AbstractDescriptor descriptor)
    {
        Collection selectedStatusIds = new LinkedList();
        if (!(descriptor instanceof ConditionDescriptor))
        {
            throw new IllegalArgumentException("Descriptor must be a ConditionDescriptor.");
        }

        ConditionDescriptor conditionDescriptor = (ConditionDescriptor) descriptor;

        String statuses = (String) conditionDescriptor.getArgs().get("statuses");
        StringTokenizer st = new StringTokenizer(statuses, ",");

        while (st.hasMoreTokens())
        {
            selectedStatusIds.add(st.nextToken());
        }
        return selectedStatusIds;
    }
}
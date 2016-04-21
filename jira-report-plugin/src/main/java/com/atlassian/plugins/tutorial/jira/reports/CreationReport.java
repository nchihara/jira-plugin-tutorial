package com.atlassian.plugins.tutorial.jira.reports;

import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.plugin.report.impl.AbstractReport;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.action.ProjectActionSupport;
import com.atlassian.core.util.DateUtils;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.ParameterUtils;
import com.atlassian.jira.web.bean.I18nBean;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.query.Query;

import org.apache.log4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Named("CreationReport")
public class CreationReport extends AbstractReport
{
    private static final Logger log = Logger.getLogger(CreationReport.class);
    private static final int MAX_HEIGHT = 200;
    private Long DEFAULT_INTERVAL = new Long(7);
    private long maxCount = 0;
    private Collection<Long> openIssueCounts = new ArrayList<Long>();
    private Collection<Date> dates = new ArrayList<Date>();

    @ComponentImport
    private final SearchProvider searchProvider;
    @ComponentImport
    private final DateTimeFormatterFactory dateTimeFormatterFactory;
    @ComponentImport
    private final ProjectManager projectManager;
    @ComponentImport
    private final DateTimeFormatter userDateTimeFormatter;

    @Inject
    public CreationReport(SearchProvider searchProvider, DateTimeFormatterFactory dateTimeFormatterFactory, ProjectManager projectManager)
    {
        this.searchProvider = searchProvider;
        this.dateTimeFormatterFactory = dateTimeFormatterFactory;
        this.projectManager = projectManager;
        this.userDateTimeFormatter = dateTimeFormatterFactory.formatter().forLoggedInUser();
    }

    public String generateReportHtml(ProjectActionSupport action, Map params) throws Exception
    {
        ApplicationUser remoteUser = action.getLoggedInUser();

        I18nHelper i18nBean = new I18nBean(remoteUser);
        Long projectId = ParameterUtils.getLongParam(params, "selectedProjectId");

        // Refer Luis's comment on https://jira.atlassian.com/browse/JRA-34664
        Date startDate = userDateTimeFormatter.withStyle(DateTimeStyle.DATE_PICKER).parse((String) params.get("startDate"));
        Date endDate =   userDateTimeFormatter.withStyle(DateTimeStyle.DATE_PICKER).parse((String) params.get("endDate"));


        Long interval = ParameterUtils.getLongParam(params, "interval");
        if (interval == null || interval.longValue() <= 0)
        {
            interval = DEFAULT_INTERVAL;
            log.error(action.getText("report.issuecreation.default.interval"));
        }
        getIssueCount(startDate, endDate, interval, remoteUser, projectId);
        List<Number> normalCount = new ArrayList<Number>();
        if (maxCount != MAX_HEIGHT && maxCount > 0)
        {
            for (Long asLong : openIssueCounts)
            {
                Float floatValue = new Float((asLong.floatValue() / maxCount) * MAX_HEIGHT);
                // Round it back to an integer
                Integer newValue = new Integer(floatValue.intValue());
                normalCount.add(newValue);
            }
        }
        if (maxCount < 0)
            action.addErrorMessage(action.getText("report.issuecreation.error"));

        Map<String, Object> velocityParams = new HashMap<String, Object>();
        velocityParams.put("startDate", startDate);
        velocityParams.put("endDate", endDate);
        velocityParams.put("openCount", openIssueCounts);
        velocityParams.put("normalisedCount", normalCount);
        velocityParams.put("dates", dates);
        velocityParams.put("maxHeight", MAX_HEIGHT);
        velocityParams.put("outlookDate", userDateTimeFormatter.withStyle(DateTimeStyle.DATE_PICKER));// .withLocale(i18nBean.getLocale()));
        velocityParams.put("projectName", projectManager.getProjectObj(projectId).getName());
        velocityParams.put("interval", interval);
        return descriptor.getHtml("view", velocityParams);
    }


    private long getOpenIssueCount(ApplicationUser remoteUser, Date startDate, Date endDate, Long projectId) throws SearchException
    {
        JqlQueryBuilder queryBuilder = JqlQueryBuilder.newBuilder();
        Query query = queryBuilder.where().createdBetween(startDate, endDate).and().project(projectId).buildQuery();
        return searchProvider.searchCount(query, remoteUser);
    }
    private void getIssueCount(Date startDate, Date endDate, Long interval, ApplicationUser remoteUser, Long projectId) throws SearchException
    {
        long intervalValue = interval.longValue() * DateUtils.DAY_MILLIS;
        Date newStartDate;
        long count = 0;
        while (startDate.before(endDate))
        {
            newStartDate = new Date(startDate .getTime() + intervalValue);
            if (newStartDate.after(endDate))
                count = getOpenIssueCount(remoteUser, startDate, endDate, projectId);
            else
                count = getOpenIssueCount(remoteUser, startDate, newStartDate, projectId);
            if (maxCount < count)
                maxCount = count;
            openIssueCounts.add(new Long(count));
            dates.add(startDate);
            startDate = newStartDate;
        }
    }

    @Override
    public void validate(ProjectActionSupport action, Map params)
    {
        // Refer Luis's comment on https://jira.atlassian.com/browse/JRA-34664
        Date startDate = userDateTimeFormatter.withStyle(DateTimeStyle.DATE_PICKER).parse((String) params.get("startDate"));
        Date endDate  =  userDateTimeFormatter.withStyle(DateTimeStyle.DATE_PICKER).parse((String) params.get("endDate"));

        Long interval = ParameterUtils.getLongParam(params, "interval");
        Long projectId = ParameterUtils.getLongParam(params, "selectedProjectId");


        if (startDate == null || userDateTimeFormatter.format(startDate)==null)
            action.addError("startDate", action.getText("report.issuecreation.startdate.required"));
        if (endDate == null || userDateTimeFormatter.format(endDate)==null)
            action.addError("endDate", action.getText("report.issuecreation.enddate.required"));
        if (interval == null || interval.longValue() <= 0)
            action.addError("interval", action.getText("report.issuecreation.interval.invalid"));
        if (projectId == null)
            action.addError("selectedProjectId", action.getText("report.issuecreation.projectid.invalid"));
        if (startDate != null && endDate != null && endDate.before(startDate))
        {
            action.addError("endDate", action.getText("report.issuecreation.before.startdate"));
        }
    }
}   // end of class : CreationReport
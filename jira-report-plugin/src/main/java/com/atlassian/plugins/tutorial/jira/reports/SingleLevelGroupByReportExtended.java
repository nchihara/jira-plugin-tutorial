package com.atlassian.plugins.tutorial.jira.reports;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.util.collect.MapBuilder;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.IssueFactory;
import com.atlassian.jira.issue.index.IssueIndexManager;
import com.atlassian.jira.issue.search.ReaderCache;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.issue.statistics.FilterStatisticsValuesGenerator;
import com.atlassian.jira.issue.statistics.StatisticsMapper;
import com.atlassian.jira.issue.statistics.StatsGroup;
import com.atlassian.jira.issue.statistics.util.OneDimensionalDocIssueHitCollector;
import com.atlassian.jira.plugin.report.impl.AbstractReport;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.SimpleErrorCollection;
import com.atlassian.jira.web.FieldVisibilityManager;
import com.atlassian.jira.web.action.ProjectActionSupport;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.util.profiling.UtilTimerStack;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.project.ProjectManager;

import com.opensymphony.util.TextUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.search.Collector;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

@Named("SingleLevelGroupByReportExtended")
public class SingleLevelGroupByReportExtended extends AbstractReport
{
    private static final Logger log = Logger.getLogger(SingleLevelGroupByReportExtended.class);

    @ComponentImport
    private final SearchProvider searchProvider;
    @ComponentImport
    private final JiraAuthenticationContext authenticationContext;
    @ComponentImport
    private final SearchRequestService searchRequestService;
    @ComponentImport
    private final IssueFactory issueFactory;
    @ComponentImport
    private final CustomFieldManager customFieldManager;
    @ComponentImport
    private final IssueIndexManager issueIndexManager;
    @ComponentImport
    private final SearchService searchService;
    @ComponentImport
    private final FieldVisibilityManager fieldVisibilityManager;
    @ComponentImport
    private final ReaderCache readerCache;
    @ComponentImport
    private final DateTimeFormatter  userTimeFormatter;
    @ComponentImport
    private final ProjectManager projectManager;
    @ComponentImport
    private final FieldManager   fieldManager;


    @Inject
    public SingleLevelGroupByReportExtended(final SearchProvider searchProvider,
                                            final JiraAuthenticationContext authenticationContext,
                                            final SearchRequestService searchRequestService,
                                            final IssueFactory issueFactory,
                                            final CustomFieldManager customFieldManager,
                                            final IssueIndexManager issueIndexManager,
                                            final SearchService searchService,
                                            final FieldVisibilityManager fieldVisibilityManager,
                                            final ReaderCache readerCache,
                                            final DateTimeFormatter userTimeFormatter )
    {
        this.searchProvider = searchProvider;
        this.authenticationContext = authenticationContext;
        this.searchRequestService = searchRequestService;
        this.issueFactory = issueFactory;
        this.customFieldManager = customFieldManager;
        this.issueIndexManager = issueIndexManager;
        this.searchService = searchService;
        this.fieldVisibilityManager = fieldVisibilityManager;
        this.readerCache = readerCache;
        this.userTimeFormatter = userTimeFormatter;
        this.projectManager = ComponentAccessor.getProjectManager();
        this.fieldManager = ComponentAccessor.getFieldManager();
    }

    public StatsGroup getOptions(SearchRequest sr, ApplicationUser user, StatisticsMapper mapper) throws PermissionException
    {

        try
        {
            return searchMapIssueKeys(sr, user, mapper);
        }
        catch (SearchException e)
        {
            log.error("Exception rendering " + this.getClass().getName() + ".  Exception " + e.getMessage(), e);
            return null;
        }
    }

    public StatsGroup searchMapIssueKeys(SearchRequest request, ApplicationUser searcher, StatisticsMapper mapper)
            throws SearchException
    {
        try
        {
            UtilTimerStack.push("Search Count Map");
            StatsGroup statsGroup = new StatsGroup(mapper);
            Collector hitCollector = new OneDimensionalDocIssueHitCollector(
                    mapper.getDocumentConstant(),
                    statsGroup,
                    issueIndexManager.getIssueSearcher().getIndexReader(),
                    issueFactory,
                    fieldVisibilityManager,
                    readerCache,
                    fieldManager,
                    projectManager );

            searchProvider.searchAndSort((request != null) ? request.getQuery() : null, searcher, hitCollector, PagerFilter.getUnlimitedFilter());
            return statsGroup;
        }
        finally
        {
            UtilTimerStack.pop("Search Count Map");
        }
    }

    public String generateReportHtml(ProjectActionSupport action, Map params) throws Exception
    {
        String filterId = (String) params.get("filterid");
        if (filterId == null)
        {
            log.error("Single Level Group By Report run without a project selected (JRA-5042): params=" + params);
            return "<span class='errMsg'>No search filter has been selected. Please "
                    + "<a href=\"IssueNavigator.jspa?reset=Update&amp;pid="
                    + TextUtils.htmlEncode((String) params.get("selectedProjectId"))
                    + "\">create one</a>, and re-run this report. See also "
                    + "<a href=\"http://jira.atlassian.com/browse/JRA-5042\">JRA-5042</a></span>";
        }
        String mapperName = (String) params.get("mapper");
        final StatisticsMapper mapper = new FilterStatisticsValuesGenerator().getStatsMapper(mapperName);
        final JiraServiceContext ctx = new JiraServiceContextImpl(authenticationContext.getLoggedInUser());
        final SearchRequest request = searchRequestService.getFilter(ctx, new Long(filterId));

        final MapBuilder<String, Object> mapBuilder = MapBuilder.newBuilder();
        final Map<String, Object> startingParams;
        try
        {

             mapBuilder.add("action", action);
             mapBuilder.add("statsGroup", getOptions(request, authenticationContext.getLoggedInUser(), mapper));
             mapBuilder.add("searchRequest", request);
             mapBuilder.add("mapperType", mapperName);
             mapBuilder.add("customFieldManager", customFieldManager);
             mapBuilder.add("fieldVisibilityManager", fieldVisibilityManager);
             mapBuilder.add("searchService", searchService);
             mapBuilder.add("portlet", this);
             mapBuilder.add("outlookDate", userTimeFormatter);

            startingParams = mapBuilder.toMap();
            return descriptor.getHtml("view", startingParams);
        }
        catch (Exception e)
        {
            log.error(e, e);
            return null;
        }
    }

    @Override
    public void validate(ProjectActionSupport action, Map params)
    {
        super.validate(action, params);
        String filterId = (String) params.get("filterid");
        if (StringUtils.isEmpty(filterId))
        {
            action.addError("filterid", action.getText("report.singlelevelgroupby.filter.is.required"));
        }
        else
        {
            validateFilterId(action,filterId);
        }
    }

    private void validateFilterId(ProjectActionSupport action, String filterId)
    {
        try
        {
            JiraServiceContextImpl serviceContext = new JiraServiceContextImpl(
                    action.getLoggedInUser(), new SimpleErrorCollection());
            SearchRequest searchRequest = searchRequestService.getFilter(serviceContext, new Long(filterId));
            if (searchRequest == null)
            {
                action.addErrorMessage(action.getText("report.error.no.filter"));
            }
        }
        catch (NumberFormatException nfe)
        {
            action.addError("filterId", action.getText("report.error.filter.id.not.a.number", filterId));
        }
    }
} // end of class : SingleLebelGroupByReportExtended
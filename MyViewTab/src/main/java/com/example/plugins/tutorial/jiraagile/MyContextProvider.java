package com.example.plugins.tutorial.jiraagile;

import java.util.Map;
import com.atlassian.jira.util.collect.MapBuilder;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.ContextProvider;

public class MyContextProvider implements ContextProvider
{
    private Long itemCount = 4L;
    @Override
    public void init(Map<String, String> params) throws PluginParseException
    {
        if (params.containsKey("itemCount"))
        {
            this.itemCount = Long.parseLong(params.get("itemCount"));
        }
    }
    @Override
    public Map<String, Object> getContextMap(Map context)
    {
        return MapBuilder.<String, Object>newBuilder()
                .add("atl.gh.issue.details.tab.count", itemCount).toMap();
    }
}
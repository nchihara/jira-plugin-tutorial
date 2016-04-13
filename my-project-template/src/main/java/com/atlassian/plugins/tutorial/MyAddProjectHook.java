package com.atlassian.plugins.tutorial;


import com.atlassian.annotations.PublicApi;
import com.atlassian.jira.project.template.hook.AddProjectHook;
import com.atlassian.jira.project.template.hook.ConfigureData;
import com.atlassian.jira.project.template.hook.ConfigureResponse;
import com.atlassian.jira.project.template.hook.ValidateData;
import com.atlassian.jira.project.template.hook.ValidateResponse;

public class MyAddProjectHook implements AddProjectHook
{
    @PublicApi
    public ValidateResponse validate(final ValidateData validateData)
    {
        ValidateResponse validateResponse = ValidateResponse.create();
        if (validateData.projectKey().equals("TEST"))
        {
            validateResponse.addErrorMessage("Invalid Project Key");
        }

        return validateResponse;
    }

    //@Override
    @PublicApi
    public ConfigureResponse configure(final ConfigureData configureData)
    {
        // setredirect() is Deprecated.
        // All projects created via a template will be redirected to the "Browse Project" page.
        // This value will be ignored.
        // "ConfigureResponse configureResponse = ConfigureResponse.create().setRedirect("/issues/");"
        // "return configureResponse;"
        return ConfigureResponse.create();
    }
}
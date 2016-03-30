package com.example.plugins.tutorial.customfield;

import com.atlassian.jira.issue.customfields.impl.GenericTextCFType;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
// since 6.4
import com.atlassian.jira.issue.fields.TextFieldCharacterLengthValidator;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

import javax.inject.Inject;
import javax.inject.Named;
//import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
//import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

@Named("JiraCustomField")
public class JiraCustomField extends GenericTextCFType {

    @ComponentImport
    CustomFieldValuePersister customFieldValuePersister;
    @ComponentImport
    GenericConfigManager genericConfigManager;
    @ComponentImport
    TextFieldCharacterLengthValidator textFieldCharacterLengthValidator;
    @ComponentImport
    JiraAuthenticationContext jiraAuthenticationContext;

    
    @Inject
    public JiraCustomField(CustomFieldValuePersister customFieldValuePersister,
                           GenericConfigManager genericConfigManager,
                           TextFieldCharacterLengthValidator textFieldCharacterLengthValidator,
                           JiraAuthenticationContext jiraAuthenticationContext)
    {

        // since 6.4, below constructor is deprecated
        // GenericTextCFType(customFieldValuePersister, genericConfigManager);
        super(customFieldValuePersister, genericConfigManager, textFieldCharacterLengthValidator, jiraAuthenticationContext);
    }


}
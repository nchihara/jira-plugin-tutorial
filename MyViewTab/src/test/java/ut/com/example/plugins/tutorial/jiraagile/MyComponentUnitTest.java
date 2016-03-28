package ut.com.example.plugins.tutorial.jiraagile;

import org.junit.Test;
import com.example.plugins.tutorial.jiraagile.api.MyPluginComponent;
import com.example.plugins.tutorial.jiraagile.impl.MyPluginComponentImpl;

import static org.junit.Assert.assertEquals;

public class MyComponentUnitTest
{
    @Test
    public void testMyName()
    {
        MyPluginComponent component = new MyPluginComponentImpl(null);
        assertEquals("names do not match!", "myComponent",component.getName());
    }
}
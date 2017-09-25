package func;

import static dropbox.client.DropboxClientFactory.testClient;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import models.Rule;
import models.User;

import org.junit.Test;

import rules.RuleType;
import rules.RuleUtils;
import unit.models.BaseModelTest;
import unit.models.UserTest;

import com.google.common.collect.Sets;

import dropbox.Dropbox;
import dropbox.client.DropboxClient;
import dropbox.client.FileMoveCollisionException;

/**
 * Verify rule processing for a single user.
 * 
 * @author mustpax
 */
public class SingleUserRunRulesTest extends BaseModelTest {
    private User u = null;
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
        testClient = mock(DropboxClient.class);
        u = UserTest.newUser();
        u.save();
    }

    public void setRules(Rule... rules) {
        List<Rule> ruleList = Arrays.asList(rules);
        Rule.replace(u, ruleList, null);
    }

    @Override
    public void tearDown() throws Exception {
        try {
	        testClient = null;
	        u.delete();
        } finally {
            super.tearDown();
        }
    }
    
    @Test
    public void testConflict() throws Exception {
        addToSortbox("foo");
        setRules(new Rule(RuleType.NAME_CONTAINS, "foo", "/foo", 0, null));
        doThrow(new FileMoveCollisionException((String) null))
	        .when(testClient).move(Dropbox.getSortboxPath() + "/foo", "/foo/foo");
        doThrow(new FileMoveCollisionException((String) null))
	        .when(testClient).move(Dropbox.getSortboxPath() + "/foo", "/foo/foo conflict");
        
        RuleUtils.runRules(u);
        verify(testClient).move(Dropbox.getSortboxPath() + "/foo", "/foo/foo conflict 2");
    }

    @Test
    public void testBadName() throws Exception {
        setRules(new Rule(RuleType.NAME_CONTAINS, "foo", "/foo:*?", 0, null));
        addToSortbox("foo:*?");
        RuleUtils.runRules(u);
        verify(testClient).move(Dropbox.getSortboxPath() + "/foo:*?", "/foo:*?/foo---");
    }

    @Test
    public void testPriority() throws Exception {
        addToSortbox("foo");
        setRules(new Rule(RuleType.NAME_CONTAINS, "foo", "/foo", 0, null),
                 new Rule(RuleType.NAME_CONTAINS, "foo", "/bar", 0, null));
        RuleUtils.runRules(u);
        verify(testClient).move(Dropbox.getSortboxPath() + "/foo", "/foo/foo");
    }

    private static void addToSortbox(String... files) throws Exception {
        Set<String> ret = Sets.newHashSet();
        for (String file: files) {
	        ret.add(Dropbox.getSortboxPath() + "/" + file);
        }
        when(testClient.listDir(Dropbox.getSortboxPath()))
            .thenReturn(ret);
    }
}

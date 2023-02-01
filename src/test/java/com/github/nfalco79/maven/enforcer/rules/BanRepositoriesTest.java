package com.github.nfalco79.maven.enforcer.rules;

import static org.mockito.ArgumentMatchers.contains;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.project.MavenProject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class BanRepositoriesTest {

    private BanRepositories rule;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    public void setUp() throws Exception {
        MavenProject project = new MavenProject();
        Model model = new Model();
        DistributionManagement distributionManagement = new DistributionManagement();
        model.setGroupId("org.apache.maven.plugins.enforcer.test");
        model.setVersion("1.0-SNAPSHOT");

        Repository repo1 = new Repository();
        configureRepository(repo1, "repo1", "http://repo1/");
        Repository repo2 = new Repository();
        configureRepository(repo2, "repo2", "http://repo2/");
        List<Repository> repos = new ArrayList<Repository>();
        repos.add(repo1);
        repos.add(repo2);

        Repository pluginRepo1 = new Repository();
        configureRepository(pluginRepo1, "pluginrepo1", "http://repo1-plugin/");
        Repository pluginRepo2 = new Repository();
        configureRepository(pluginRepo2, "pluginrepo2", "http://repo2-plugin/");
        List<Repository> pluginRepos = new ArrayList<Repository>();
        pluginRepos.add(pluginRepo1);
        pluginRepos.add(pluginRepo2);

        DeploymentRepository deployRepo = new DeploymentRepository();
        configureRepository(deployRepo, "depRepo1", "http://repo1-deploy/");
        DeploymentRepository snapshotRepo = new DeploymentRepository();
        configureRepository(snapshotRepo, "depRepo2", "http://repo2-snapshot/");

        model.setRepositories(repos);
        model.setPluginRepositories(pluginRepos);

        distributionManagement.setRepository(deployRepo);
        distributionManagement.setSnapshotRepository(snapshotRepo);

        project.setModel(model);
        project.setDistributionManagement(distributionManagement);

        rule = new BanRepositories();
        rule.setProject(project);
    }

    void configureRepository(Repository repo, String id, String url) {
        repo.setId(id);
        repo.setUrl(url);
    }

    @Test
    public void testNoCheckRules() throws Exception {
        setUp();
        Repository repo1 = new Repository();
        repo1.setId("repo1");
        repo1.setUrl("http://repo1/");
        List<Repository> repos = new ArrayList<Repository>();
        repos.add(repo1);

        rule.execute();
    }

    @Test
    public void testBannedRepositories() throws Exception {
        setUp();

        List<String> bannedRepositories = new ArrayList<String>();
        bannedRepositories.add("*repo1*");

        rule.setBannedRepos(bannedRepositories);

        exception.expect(EnforcerRuleException.class);
        exception.expectMessage(contains("repo1"));
        rule.execute();
    }

    @Test
    public void testBannedPluginRepositories() throws Exception {
        setUp();

        List<String> bannedRepositories = new ArrayList<String>();
        bannedRepositories.add("http://repo2*");

        rule.setBannedPluginRepos(bannedRepositories);

        exception.expect(EnforcerRuleException.class);
        exception.expectMessage(contains("pluginrepo2"));
        rule.execute();
    }

    @Test
    public void testAllowedRepositories() throws Exception {
        setUp();

        List<String> allowedRepositories = new ArrayList<String>();
        allowedRepositories.add("http://repo1*");

        rule.setAllowedRepos(allowedRepositories);

        exception.expect(EnforcerRuleException.class);
        exception.expectMessage(contains("repo2"));
        rule.execute();
    }

    @Test
    public void testBannedDeployRepositories() throws Exception {
        setUp();

        List<String> bannedRepositories = new ArrayList<String>();
        bannedRepositories.add("*-deploy/");

        rule.setBannedDistributionRepos(bannedRepositories);

        exception.expect(EnforcerRuleException.class);
        exception.expectMessage(contains("repo1-deploy"));
        rule.execute();
    }

    @Test
    public void testBannedSnapshotDeployRepositories() throws Exception {
        setUp();

        List<String> bannedSnapshots = new ArrayList<String>();
        bannedSnapshots.add("http://repo2*");

        rule.setBannedDistributionSnapRepos(bannedSnapshots);

        exception.expect(EnforcerRuleException.class);
        exception.expectMessage(contains("repo2-snapshot"));
        rule.execute();
    }

    @Test
    public void testAllowedDeployRepositories() throws Exception {
        setUp();

        List<String> allowedRepositories = new ArrayList<String>();
        allowedRepositories.add("*-deploy/");
        List<String> allowedSnapshots = new ArrayList<String>();
        allowedRepositories.add("*-snapshot/");

        rule.setAllowedDistributionRepos(allowedRepositories);
        rule.setAllowedDistributionSnapRepos(allowedSnapshots);

        rule.execute();
    }
}

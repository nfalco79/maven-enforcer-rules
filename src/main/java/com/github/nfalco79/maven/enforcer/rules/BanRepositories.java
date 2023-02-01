package com.github.nfalco79.maven.enforcer.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Repository;
import org.apache.maven.project.MavenProject;

/**
 * This rule checks whether this project's maven session have banned
 * repositories.
 *
 * @author Laura Cameran
 */
@Named("banRepositories")
public class BanRepositories extends AbstractEnforcerRule {

    private boolean shouldIfail = false;

    /**
     * Specify explicitly banned non-plugin repositories. This is a list of
     * repository url patterns. Support wildcard "*".
     */
    private List<String> bannedRepos = Collections.emptyList();

    /**
     * Specify explicitly banned plugin repositories. This is a list of
     * repository url patterns. Support wildcard "*".
     */
    private List<String> bannedPluginRepos = Collections.emptyList();

    /**
     * Specify explicitly allowed non-plugin repositories, then all others
     * repositories would be banned. This is a list of repository url patterns.
     * Support wildcard "*".
     */
    private List<String> allowedRepos = Collections.emptyList();

    /**
     * Specify explicitly allowed plugin repositories, then all others
     * repositories would be banned. This is a list of repository url patterns.
     * Support wildcard "*".
     */
    private List<String> allowedPluginRepos = Collections.emptyList();

    /**
     * Specify explicitly banned distribution management repositories. This is a
     * list of repository url patterns. Support wildcard "*".
     */
    private List<String> bannedDistributionRepos = Collections.emptyList();

    /**
     * Specify explicitly banned distribution management snapshot repositories.
     * This is a list of repository url patterns. Support wildcard "*".
     */
    private List<String> bannedDistributionSnapRepos = Collections.emptyList();

    /**
     * Specify explicitly allowed distribution management repositories, then all
     * others repositories would be banned. This is a list of repository url
     * patterns. Support wildcard "*".
     */
    private List<String> allowedDistributionRepos = Collections.emptyList();

    /**
     * Specify explicitly allowed distribution management snapshot repositories,
     * then all others repositories would be banned. This is a list of
     * repository url patterns. Support wildcard "*".
     */
    private List<String> allowedDistributionSnapRepos = Collections.emptyList();

    @Inject
    private MavenProject project;

    @Override
    public void execute() throws EnforcerRuleException {
        StringBuilder errorBuilder = new StringBuilder();

        DistributionManagement distributionManagement = project.getDistributionManagement();
        if (distributionManagement != null) {
            DeploymentRepository deployRepo = distributionManagement.getRepository();
            DeploymentRepository snapDeployRepo = distributionManagement.getSnapshotRepository();
            if (isBanned(deployRepo, allowedDistributionRepos, bannedDistributionRepos)) {
                errorBuilder.append(getRepositoryUrlString(Arrays.asList(new DeploymentRepository[] { deployRepo })));
            }
            if (isBanned(snapDeployRepo, allowedDistributionSnapRepos, bannedDistributionSnapRepos)) {
                errorBuilder.append(getRepositoryUrlString(Arrays.asList(new DeploymentRepository[] { snapDeployRepo })));
            }
        }

        List<Repository> resultBannedRepos = checkBannedRepositories(project.getRepositories(), this.allowedRepos, this.bannedRepos);
        List<Repository> resultBannedPluginRepos = checkBannedRepositories(project.getPluginRepositories(), this.allowedPluginRepos, this.bannedPluginRepos);

        if (!resultBannedRepos.isEmpty()) {
            errorBuilder.append(getRepositoryUrlString(resultBannedRepos));
        }
        if (!resultBannedPluginRepos.isEmpty()) {
            errorBuilder.append(getRepositoryUrlString(resultBannedPluginRepos));
        }

        if (errorBuilder.length() > 0) {
            throw new EnforcerRuleException("Current maven session contains banned repositories: " + errorBuilder.toString());
        }
    }

    /**
     * Check whether specified deployment repositories have banned repositories.
     *
     * @param repo candidate repository.
     * @param includes 'include' patterns.
     * @param excludes 'exclude' patterns.
     * @return Banned repositories.
     */
    protected boolean isBanned(DeploymentRepository repo, List<String> includes, List<String> excludes) {
        boolean isBanned = false;

        if (repo != null) {
            String url = repo.getUrl().trim();
            if (includes.size() > 0 && !match(url, includes)) {
                isBanned = true;
            } else if (excludes.size() > 0 && match(url, excludes)) {
                isBanned = true;
            }
        }

        return isBanned;
    }

    /**
     * Check whether specified repositories have banned repositories.
     *
     * @param repositories candidate repositories.
     * @param includes 'include' patterns.
     * @param excludes 'exclude' patterns.
     * @return Banned repositories.
     */
    protected List<Repository> checkBannedRepositories(List<? extends Repository> repositories,
                                                       List<String> includes,
                                                       List<String> excludes) {
        List<Repository> banned = new ArrayList<Repository>();

        for (Repository repo : repositories) {
            String url = repo.getUrl().trim();
            if (includes.size() > 0 && !match(url, includes)) {
                banned.add(repo);
                continue;
            }
            if (excludes.size() > 0 && match(url, excludes)) {
                banned.add(repo);
            }
        }

        return banned;
    }

    private boolean match(String url, List<String> patterns) {
        for (String pattern : patterns) {
            if (this.match(url, pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean match(String text, String pattern) {
        return text.matches(globPatternToRegexp(pattern));
    }

    private String globPatternToRegexp(String pattern) {
        return pattern.replace("?", ".").replace("*", ".*?");
    }

    private String getRepositoryUrlString(List<? extends Repository> resultBannedRepos) {
        StringBuilder builder = new StringBuilder();
        for (Repository repo : resultBannedRepos) {
            builder.append(repo.getId() + " - " + repo.getUrl() + "\n");
        }
        return builder.toString();
    }

    /**
     * If your rule is cacheable, you must return a unique id when parameters or
     * conditions change that would cause the result to be different. Multiple
     * cached results are stored based on their id.
     * <p>
     * The easiest way to do this is to return a hash computed from the values
     * of your parameters.
     * <p>
     * If your rule is not cacheable, then you don't need to override this
     * method or return null
     */
    @Override
    public String getCacheId() {
        // no hash on boolean...only parameter so no hash is needed.
        return Boolean.toString(shouldIfail);
    }

    /**
     * Returns banned plugin repositories.
     *
     * @return a list of repository url glob patterns.
     */
    public List<String> getBannedRepos() {
        return Collections.unmodifiableList(bannedRepos);
    }

    /**
     * Sets banned plugin repositories.
     *
     * @param bannedRepos a list of repository url glob patterns.
     */
    public void setBannedRepos(List<String> bannedRepos) {
        this.bannedRepos = Collections.unmodifiableList(bannedRepos);
    }

    /**
     * Returns banned plugin repositories.
     *
     * @return a list of repository url glob patterns.
     */
    public List<String> getBannedPluginRepos() {
        return Collections.unmodifiableList(bannedPluginRepos);
    }

    /**
     * Sets banned plugin repositories.
     *
     * @param bannedPluginRepos a list of repository url glob patterns.
     */
    public void setBannedPluginRepos(List<String> bannedPluginRepos) {
        this.bannedPluginRepos = Collections.unmodifiableList(bannedPluginRepos);
    }

    /**
     * Returns allowed non-plugin repositories, any others repositories would be
     * banned.
     *
     * @return a list of repository url glob patterns.
     */
    public List<String> getAllowedRepos() {
        return Collections.unmodifiableList(allowedRepos);
    }

    /**
     * Sets allowed non-plugin repositories.
     *
     * @param allowedRepos a list of repository url glob patterns.
     */
    public void setAllowedRepos(List<String> allowedRepos) {
        this.allowedRepos = Collections.unmodifiableList(allowedRepos);
    }

    /**
     * Returns allowed plugin repositories, any others repositories would be
     * banned.
     *
     * @return a list of repository url glob patterns.
     */
    public List<String> getAllowedPluginRepos() {
        return Collections.unmodifiableList(allowedPluginRepos);
    }

    /**
     * Sets allowed plugin repositories.
     *
     * @param allowedPluginRepos a list of repository url glob patterns.
     */
    public void setAllowedPluginRepos(List<String> allowedPluginRepos) {
        this.allowedPluginRepos = Collections.unmodifiableList(allowedPluginRepos);
    }

    /**
     * Returns banned distribution management repositories.
     *
     * @return a list of repository url glob patterns.
     */
    public List<String> getBannedDistributionRepos() {
        return Collections.unmodifiableList(bannedDistributionRepos);
    }

    /**
     * Sets banned distribution management repositories.
     *
     * @param bannedDistributionRepos a list of repository url glob patterns.
     */
    public void setBannedDistributionRepos(List<String> bannedDistributionRepos) {
        this.bannedDistributionRepos = Collections.unmodifiableList(bannedDistributionRepos);
    }

    /**
     * Returns banned distribution management snapshot repositories.
     *
     * @return a list of repository url glob patterns.
     */
    public List<String> getBannedDistributionSnapRepos() {
        return Collections.unmodifiableList(bannedDistributionSnapRepos);
    }

    /**
     * Sets banned distribution management snapshot repositories.
     *
     * @param bannedDistributionSnapRepos a list of repository url glob
     *        patterns.
     */
    public void setBannedDistributionSnapRepos(List<String> bannedDistributionSnapRepos) {
        this.bannedDistributionSnapRepos = Collections.unmodifiableList(bannedDistributionSnapRepos);
    }

    /**
     * Return allowed distribution management repositories, any others
     * repositories would be banned.
     *
     * @return a list of url glob patterns.
     */
    public List<String> getAllowedDistributionRepos() {
        return Collections.unmodifiableList(allowedDistributionRepos);
    }

    /**
     * Sets allowed distribution management repositories.
     *
     * @param allowedDistributionRepos a list of url glob patterns.
     */
    public void setAllowedDistributionRepos(List<String> allowedDistributionRepos) {
        this.allowedDistributionRepos = Collections.unmodifiableList(allowedDistributionRepos);
    }

    /**
     * Returns allowed distribution management snapshot repositories, any others
     * repositories would be banned.
     *
     * @return a list of url glob patterns.
     */
    public List<String> getAllowedDistributionSnapRepos() {
        return Collections.unmodifiableList(allowedDistributionSnapRepos);
    }

    /**
     * Sets allowed distribution management snapshot repositories
     *
     * @param allowedDistributionSnapRepos a list of url glob patterns.
     */
    public void setAllowedDistributionSnapRepos(List<String> allowedDistributionSnapRepos) {
        this.allowedDistributionSnapRepos = Collections.unmodifiableList(allowedDistributionSnapRepos);
    }

    /* for test purpose */
    /* package */ void setProject(MavenProject project) {
        this.project = project;
    }
}

package com.quicksign.jgitflowsemver.strategy;

import com.quicksign.jgitflowsemver.dsl.GitflowVersioningConfiguration;
import com.quicksign.jgitflowsemver.version.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;

/**
 * The strategy to use when Gitflow's <strong>develop</strong> branch is the current branch.
 *
 * @author Max Käufer
 * @author <a href="mailto:cedric.vidal@quicksign.com">Cedric Vidal, Quicksign</a>
 */
public class BranchDevelopStrategy extends AbstractStrategy implements Strategy {

    @Override
    public boolean canInfer(final Repository repo) throws IOException {
        return repo.getBranch().equals(getDevelopBranchName(repo));
    }

    private static String getDevelopBranchName(final Repository repo) {
        final String name = AbstractStrategy.getBranchName(repo, CONFIG_BRANCH_DEVELOP);
        return name != null ? name : DEFAULT_BRANCH_DEVELOP;
    }

    private static final String CONFIG_BRANCH_DEVELOP = "develop";
    private static final String DEFAULT_BRANCH_DEVELOP = "develop";

    @Override
    protected VersionWithType doInfer(Repository repo, GitflowVersioningConfiguration conf) throws GitAPIException, IOException {
        NearestVersion nearestVersion = new NearestVersionLocator().locate(repo);

        return new VersionWithTypeBuilder(nearestVersion)
            .branch(conf.getPreReleaseIds().getDevelop())
            .distanceFromRelease()
            .sha(repo, conf)
            .dirty(repo, conf)
            .type(VersionType.DEVELOP)
            .build();
    }

}
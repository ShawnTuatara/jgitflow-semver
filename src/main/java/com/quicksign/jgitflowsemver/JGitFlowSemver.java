package com.quicksign.jgitflowsemver;

import com.github.zafarkhaja.semver.Version;
import com.quicksign.jgitflowsemver.dsl.GitflowVersioningConfiguration;
import com.quicksign.jgitflowsemver.strategy.Strategy;
import com.quicksign.jgitflowsemver.version.VersionWithType;
import org.apache.commons.cli.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 * @author <a href="mailto:cedric.vidal@quicksign.com">Cedric Vidal, Quicksign</a>
 */
public class JGitFlowSemver {
    private final Repository repository;

    private static Logger LOGGER = LoggerFactory.getLogger(JGitFlowSemver.class);
    private final GitflowVersioningConfiguration conf;
    private final Git git;

    public static void main(String[] args) throws Exception {

        // create Options object
        Options options = new Options();

        options.addOption("b", "branch", true, "force branch name in case of a detached repo");
        options.addOption("s", "snapshot", false, "Use Maven SNAPSHOT instead of semver build metadata");
        options.addOption("m", "maven", false, "Maven compatible semver versions");
        options.addOption("l", "logLevel", true, "Configures log level to TRACE|DEBUG|INFO|WARN|ERROR");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse( options, args);

        String logLevel = cmd.getOptionValue("l", "error"); //$NON-NLS-1$
        System.setProperty("org.slf4j.simpleLogger.log.com.quicksign", logLevel); //$NON-NLS-1$
        LOGGER = LoggerFactory.getLogger(JGitFlowSemver.class);

        final GitflowVersioningConfiguration conf = new GitflowVersioningConfiguration();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse( options, args );
            if(line.hasOption('b')) {
                conf.setForceBranch(line.getOptionValue('b'));
                LOGGER.debug("Forcing branch name {}", conf.getForceBranch());
            }
            if(line.hasOption('s')) {
                conf.useMavenSnapshot();
                LOGGER.debug("Maven snapshots mode");
            }
            if(line.hasOption('m')) {
                conf.mavenCompatibility();
                LOGGER.debug("Maven compatibility mode");
            }
            args = line.getArgs();
        }
        catch( ParseException exp ) {
            printUsage(options, System.err);
            System.exit(1);
        }

        if(args.length != 1) {
            printUsage(options, System.err);
            System.exit(1);
        }

        try {
            Version v = null;
            final File commandLineRoot = new File(args[0]).getAbsoluteFile();

            if(new File(commandLineRoot, "pom.xml").exists()) {
                LOGGER.debug("Detected Maven pom.xml so activating Maven compatibility");
                conf.mavenCompatibility();
            }

            v = new JGitFlowSemver(conf).infer();
            String adjusted = v.toString();
            if(conf.isMavenCompatibility()) {
                adjusted = adjusted.replaceAll("\\+", ".");
            }
            System.out.println(adjusted);
        } catch (Exception e) {
            System.err.println("An error ocured: " + e);
            LOGGER.error("An error occured", e);
            System.exit(2);
        }
    }

    private static void printUsage(Options options, PrintStream stream) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("jgitflow-semver [options]... <path>", options, false);
    }

    public Version infer() throws Exception {

        Ref headRef = repository.getRef( Constants.HEAD );
        if( headRef == null || headRef.getObjectId() == null ) {
            LOGGER.warn("No commit yet");
        }

        VersionWithType versionWithType = null;
        for (Strategy strategy : Strategy.STRATEGIES) {
            if(strategy.canInfer(repository, conf)) {
                if(LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Applying strategy {}", strategy.getClass().getSimpleName());
                }
                versionWithType = strategy.infer(git, conf);

                if(versionWithType != null) {
                    return versionWithType.getVersion();
                }
            }
        }

        return null;

    }

    public JGitFlowSemver(GitflowVersioningConfiguration conf) throws IOException {
    	this(null, conf);
    }
    
    public JGitFlowSemver(File dir, GitflowVersioningConfiguration conf) throws IOException {
        this.conf = conf;

        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        repository = builder.setGitDir(dir)
            .readEnvironment() // scan environment GIT_* variables
            .findGitDir() // scan up the file system tree
            .build();
        
        conf.setRepositoryRoot(repository.getWorkTree().getAbsolutePath());
        this.git = Git.wrap(repository);
    }

}

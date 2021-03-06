package org.openmrs.maven.plugins;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.apache.commons.lang.SystemUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.components.interactivity.Prompter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

abstract class AbstractDockerMojo extends AbstractMojo {

    protected static final String DEFAULT_MYSQL_CONTAINER = "openmrs-sdk-mysql-v3-2";
    protected static final String DEFAULT_MYSQL_EXPOSED_PORT = "3308";
    protected static final String DEFAULT_MYSQL_PASSWORD = "Admin123";
    protected static final String MYSQL_5_6 = "mysql:5.6";
    protected static final String DEFAULT_MYSQL_DBURI = "jdbc:mysql://localhost:3307/";
    protected static final String API_VERSION = "1.18";
    protected static final String DEFAULT_HOST_LINUX = "unix:///var/run/docker.sock";

    private static final String NOT_LINUX_UNABLE_TO_CONNECT_MESSAGE = "Could not connect to Docker at " +
            "dockerHostUrl. Please make sure Docker is running or if you are using 'Docker Toolbox', " +
            "please make sure you run the SDK command from the 'Docker  Toolbox' terminal. If the Docker " +
            "host URL is not correct, please reset to the default value by passing the -DdockerHost parameter" +
            " or set it manually -DdockerHost=\"correct/url\"";

    private static final String LINUX_UNABLE_TO_CONNECT_MESSAGE = "Could not connect to Docker at dockerHostUrl." +
            " If the Docker host URL is not correct, please reset to the default value by passing the -DdockerHost" +
            " parameter or set it manually -DdockerHost=\"correct/url\"";

    /**
     * Option to include demo data
     *
     * @parameter expression="${dockerHost}"
     */
    protected String dockerHost;

    /**
     * @component
     * @required
     */
    protected Prompter prompter;

    protected DockerClient docker;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        resolveDocker();
        executeTask();
        try {
            docker.close();
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to close Docker client");
        }
    }

    public abstract void executeTask() throws MojoExecutionException, MojoFailureException;

    protected void resolveDocker() {

        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .withApiVersion(API_VERSION)
                .build();

        docker = DockerClientBuilder.getInstance(config).build();

        try {
            docker.infoCmd().exec();
        } catch (Exception e) {
            if (SystemUtils.IS_OS_LINUX) {
                throw new RuntimeException(LINUX_UNABLE_TO_CONNECT_MESSAGE, e);
            }
            else {
                throw new RuntimeException(NOT_LINUX_UNABLE_TO_CONNECT_MESSAGE, e);
            }
        }
    }

    protected Container findContainer(String id){
        List<Container> containers = docker.listContainersCmd().withShowAll(true).exec();

        for (Container container : containers) {
            if (container.getId().equals(id)) {
                return container;
            }
        }

        for (Container container: containers) {
            if (Arrays.asList(container.getNames()).contains(id)) {
                return container;
            }
        }

        //on Linux name is prepended with '/'
        String idWithSlash = "/" + id;
        for (Container container: containers) {
            if (Arrays.asList(container.getNames()).contains(idWithSlash)) {
                return container;
            }
        }

        for (Container container: containers) {
            if (container.getLabels().containsKey(id)) {
                return container;
            }
        }
        return null;
    }

    protected void showMessage(String message) {
        System.out.println("\n" + message);
    }
}



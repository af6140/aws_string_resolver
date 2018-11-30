package com.entertainment.aws.string;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClient;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.ParameterType;
import com.amazonaws.services.simplesystemsmanagement.model.PutParameterRequest;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.*;
import org.junit.*;

import java.util.*;

public class StringResolverSSMTest {

    private static DockerClient docker=null;
    private static String container_id = null;

    @BeforeClass
    public static  void setup() {
        String image = "picadoh/motocker";
        try {
            docker = new DefaultDockerClient("unix:///var/run/docker.sock");

            docker.pull(image);

            final String[] ports = {"5000"};
            final Map<String, List<PortBinding>> portBindings = new HashMap<>();
            for (String port : ports) {
                List<PortBinding> hostPorts = new ArrayList<>();
                hostPorts.add(PortBinding.of("127.0.0.1", port));
                portBindings.put(port, hostPorts);
            }


            HostConfig hostConfig = HostConfig.builder().portBindings(portBindings).build();
            List env = new LinkedList();
            env.add("MOTO_SERVICE=ssm");
            // Create container with exposed ports
            final ContainerConfig containerConfig = ContainerConfig.builder()
                    .hostConfig(hostConfig)
                    .image(image).exposedPorts(ports).env(env)
                    .build();

            final ContainerCreation creation = docker.createContainer(containerConfig);
            container_id = creation.id();

            // Inspect container
            final ContainerInfo info = docker.inspectContainer(container_id);

            // Start container
            docker.startContainer(container_id);


        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (DockerException e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        prepareSSM();


    }

    private static void prepareSSM () {
        System.setProperty("aws.accessKeyId", "foo");
        System.setProperty("aws.secretKey", "foo");
        AWSSimpleSystemsManagementClientBuilder clientBuilder = AWSSimpleSystemsManagementClient.builder();
        clientBuilder.setEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://localhost:5000", Region.getRegion(Regions.US_EAST_1).toString()));
        AWSSimpleSystemsManagement client = clientBuilder.build();
        PutParameterRequest putRequest =new PutParameterRequest();
        putRequest.setName("/test/param");
        putRequest.setType(ParameterType.String);
        putRequest.setValue("congratulation");
        client.putParameter(putRequest);

    }


    @Test
    public void testResolveSSMString() {
        StringResolver resolver =new StringResolver();
        resolver.getClientBuilder().setEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://localhost:5000", Region.getRegion(Regions.US_EAST_1).toString()));

        try {
            String resolved = resolver.resolveSsmString("{{resolve:ssm:/test/param}}");
            assert("congratulation".equals(resolved));
        }catch (ResolveException e) {
            System.out.println(e.getMessage());
            assert(false);
        }


    }

    @AfterClass
    public static void tearDown() {
        if (docker!=null && container_id!=null) {
            System.out.println("Kill container");
            try {
                docker.killContainer(container_id);
                docker.removeContainer(container_id);
            } catch (DockerException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

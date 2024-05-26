package com.example.thesisprototype.controllers;

import org.springframework.web.bind.annotation.*;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yaml.snakeyaml.Yaml;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/containers")
public class ContainerController {

    // We tried this but we are goind with different approach.
//    @PostMapping("/nginx")
//    public String createNginxContainer() {
//        try (DockerClient dockerClient = DockerClientBuilder.getInstance().build()){
//            System.out.println("ypppppppppppppppppppppppppppppp");
//
//            ExposedPort exposedPort = ExposedPort.tcp(80);
//            PortBinding portBinding = PortBinding.parse("8080:80");
//
//            CreateContainerResponse containerResponse = dockerClient.createContainerCmd("nginx:latest")
//                    .withExposedPorts(exposedPort)
//                    .withHostConfig(HostConfig.newHostConfig().withPortBindings(portBinding))
//                    .exec();
//
//            dockerClient.startContainerCmd(containerResponse.getId()).exec();
//
//            return "NGINX container created successfully";
//        } catch (Exception e) {
//            e.printStackTrace();
//            return "Failed to create NGINX container";
//        }
//    }
    @PostMapping("/createFromYamlString")
    @CrossOrigin(origins = "http://localhost:3000")
    public String createContainerFromYamlString(@RequestBody Map<String, String> requestBody) {
        String yamlString = requestBody.get("yamlData");
        Yaml yaml = new Yaml();
        Map<String, Object> yamlMap = yaml.load(yamlString);

        if (!yamlMap.containsKey("services")) {
            throw new IllegalArgumentException("YAML data does not contain 'services' information");
        }

        Map<String, Object> services = (Map<String, Object>) yamlMap.get("services");

        try {
            DockerClient dockerClient = DockerClientBuilder.getInstance()
                    .withDockerHttpClient(new ApacheDockerHttpClient.Builder()
                            .dockerHost(new URI("tcp://localhost:2375"))
                            .sslConfig(null)
                            .maxConnections(100)
                            .connectionTimeout(Duration.ofSeconds(30))
                            .responseTimeout(Duration.ofSeconds(45))
                            .build())
                    .build();

            for (Map.Entry<String, Object> serviceEntry : services.entrySet()) {
                String serviceName = serviceEntry.getKey();
                Map<String, Object> serviceDetails = (Map<String, Object>) serviceEntry.getValue();

                String imageName = (String) serviceDetails.get("image");
                List<String> ports = (List<String>) serviceDetails.get("ports");

                for (String port : ports) {
                    String[] portParts = port.split(":");
                    int hostPort = Integer.parseInt(portParts[0]);
                    int containerPort = Integer.parseInt(portParts[1]);

                    ExposedPort tcpPort = ExposedPort.tcp(containerPort);
                    PortBinding portBinding = new PortBinding(Ports.Binding.bindPort(hostPort), tcpPort);

                    // Pull the image if it doesn't exist
                    try {
                        dockerClient.inspectImageCmd(imageName).exec();
                    } catch (DockerException e) {
                        if (e.getMessage().contains("No such image")) {
                            dockerClient.pullImageCmd(imageName).start().awaitCompletion();
                        } else {
                            throw e;
                        }
                    }

                    CreateContainerResponse containerResponse = dockerClient.createContainerCmd(imageName)
                            .withExposedPorts(tcpPort)
                            .withHostConfig(HostConfig.newHostConfig().withPortBindings(portBinding))
                            .exec();

                    dockerClient.startContainerCmd(containerResponse.getId()).exec();
                }
            }
            return "All containers created and started successfully.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to create containers: " + e.getMessage();
        }
    }
}


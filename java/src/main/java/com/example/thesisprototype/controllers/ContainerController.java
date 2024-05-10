package com.example.thesisprototype.controllers;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.core.DockerClientBuilder;

@RestController
@RequestMapping("/containers")
public class ContainerController {
    @PostMapping("/nginx")
    public String createNginxContainer() {
        try (DockerClient dockerClient = DockerClientBuilder.getInstance().build()){
            System.out.println("ypppppppppppppppppppppppppppppp");

            ExposedPort exposedPort = ExposedPort.tcp(80);
            PortBinding portBinding = PortBinding.parse("8080:80");

            CreateContainerResponse containerResponse = dockerClient.createContainerCmd("nginx:latest")
                    .withExposedPorts(exposedPort)
                    .withHostConfig(HostConfig.newHostConfig().withPortBindings(portBinding))
                    .exec();

            dockerClient.startContainerCmd(containerResponse.getId()).exec();

            return "NGINX container created successfully";
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to create NGINX container";
        }
    }
}

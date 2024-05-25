package com.example.thesisprototype.controllers;

import org.springframework.web.bind.annotation.*;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.core.DockerClientBuilder;
import org.yaml.snakeyaml.Yaml;

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
        // Get the YAML data from the request body
        String yamlString = requestBody.get("yamlData");
        // Assuming yamlData is a String containing the YAML data
        Yaml yaml = new Yaml();
        Map<String, Object> yamlMap = yaml.load(yamlString);

// Check if 'services' key exists and get its value
        if (yamlMap.containsKey("services")) {
            Map<String, Object> services = (Map<String, Object>) yamlMap.get("services");

            // Check if 'myapp' key exists and get its value
            if (services.containsKey("myapp")) {
                Map<String, Object> myapp = (Map<String, Object>) services.get("myapp");

                // Check if 'ports' key exists and get its value
                if (myapp.containsKey("ports")) {
                    List<String> ports = (List<String>) myapp.get("ports");

                    // Iterate over the list of ports
                    for (String port : ports) {
                        System.out.println("Port: " + port);
                    }
                } else {
                    throw new IllegalArgumentException("YAML data does not contain 'ports' information");
                }
            } else {
                throw new IllegalArgumentException("YAML data does not contain 'myapp' service information");
            }
        } else {
            throw new IllegalArgumentException("YAML data does not contain 'services' information");
        }
        return "Container created successfully";
    }
}

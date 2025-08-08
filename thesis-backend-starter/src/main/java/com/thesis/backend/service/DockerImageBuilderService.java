package com.thesis.backend.service;

import com.thesis.backend.entity.ImageTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DockerImageBuilderService {
    
    private static final String DOCKERFILE_DIR = "dockerfiles";
    private static final String IMAGE_PREFIX = "thesis";
    
    public String buildCustomImage(ImageTemplate template) {
        try {
            // Generate Dockerfile
            String dockerfile = generateDockerfile(template);
            
            // Create dockerfile directory if it doesn't exist
            createDockerfileDirectory();
            
            // Write Dockerfile to disk
            String dockerfileName = writeDockerfile(template.getName(), dockerfile);
            
            // Build image using Docker command
            String imageName = buildImage(template, dockerfileName);
            
            log.info("Successfully built image: {}", imageName);
            return imageName;
            
        } catch (Exception e) {
            log.error("Failed to build image for template: {}", template.getName(), e);
            throw new RuntimeException("Failed to build Docker image", e);
        }
    }
    
    public ImageTemplate buildImageAndUpdateTemplate(ImageTemplate template) {
        try {
            String imageName = buildCustomImage(template);
            
            // Create a new template with updated values
            ImageTemplate updatedTemplate = ImageTemplate.builder()
                .id(template.getId())
                .name(template.getName())
                .dockerImage(imageName)
                .description(template.getDescription())
                .technology(template.getTechnology())
                .version(template.getVersion())
                .dockerfile(template.getDockerfile())
                .preInstalledTools(template.getPreInstalledTools())
                .environmentVariables(template.getEnvironmentVariables())
                .startupCommands(template.getStartupCommands())
                .courseCode(template.getCourseCode())
                .professorId(template.getProfessorId())
                .isPublic(template.isPublic())
                .isBuilt(true)
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
            
            return updatedTemplate;
            
        } catch (Exception e) {
            log.error("Failed to build image and update template: {}", template.getName(), e);
            throw new RuntimeException("Failed to build Docker image", e);
        }
    }
    
    private String generateDockerfile(ImageTemplate template) {
        StringBuilder dockerfile = new StringBuilder();
        
        // Base image
        dockerfile.append("FROM ubuntu:20.04\n\n");
        dockerfile.append("# Set environment variables to avoid interactive prompts\n");
        dockerfile.append("ENV DEBIAN_FRONTEND=noninteractive\n\n");
        
        // Update package list
        dockerfile.append("# Update package list and install basic tools\n");
        dockerfile.append("RUN apt-get update && apt-get install -y \\\n");
        dockerfile.append("    curl \\\n");
        dockerfile.append("    wget \\\n");
        dockerfile.append("    git \\\n");
        dockerfile.append("    vim \\\n");
        dockerfile.append("    nano \\\n");
        dockerfile.append("    htop \\\n");
        dockerfile.append("    tree \\\n");
        dockerfile.append("    net-tools \\\n");
        dockerfile.append("    iputils-ping \\\n");
        dockerfile.append("    build-essential \\\n");
        dockerfile.append("    && apt-get clean \\\n");
        dockerfile.append("    && rm -rf /var/lib/apt/lists/*\n\n");
        
        // Install SSH server
        dockerfile.append("# Install SSH server\n");
        dockerfile.append("RUN apt-get update && apt-get install -y openssh-server sudo \\\n");
        dockerfile.append("    && apt-get clean \\\n");
        dockerfile.append("    && rm -rf /var/lib/apt/lists/*\n\n");
        
        // Configure SSH
        dockerfile.append("# Configure SSH\n");
        dockerfile.append("RUN mkdir /var/run/sshd\n");
        dockerfile.append("RUN sed -i 's/#PermitRootLogin prohibit-password/PermitRootLogin yes/' /etc/ssh/sshd_config \\\n");
        dockerfile.append("    && sed -i 's/#PasswordAuthentication yes/PasswordAuthentication yes/' /etc/ssh/sshd_config \\\n");
        dockerfile.append("    && sed -i 's/#PubkeyAuthentication yes/PubkeyAuthentication yes/' /etc/ssh/sshd_config \\\n");
        dockerfile.append("    && sed 's@session\\s*required\\s*pam_loginuid.so@session optional pam_loginuid.so@g' -i /etc/pam.d/sshd\n\n");
        
        // Install technology-specific packages
        installTechnologyPackages(dockerfile, template);
        
        // Install pre-installed tools
        if (template.getPreInstalledTools() != null && !template.getPreInstalledTools().isEmpty()) {
            installPreInstalledTools(dockerfile, template.getPreInstalledTools());
        }
        
        // Set environment variables
        if (template.getEnvironmentVariables() != null && !template.getEnvironmentVariables().isEmpty()) {
            setEnvironmentVariables(dockerfile, template.getEnvironmentVariables());
        }
        
        // Set working directory
        dockerfile.append("# Set working directory\n");
        dockerfile.append("WORKDIR /workspace\n\n");
        
        // Copy startup script
        dockerfile.append("# Copy startup script\n");
        dockerfile.append("COPY startup.sh /usr/local/bin/startup.sh\n");
        dockerfile.append("RUN chmod +x /usr/local/bin/startup.sh\n\n");
        
        // Expose SSH port
        dockerfile.append("# Expose SSH port\n");
        dockerfile.append("EXPOSE 22\n\n");
        
        // Start SSH server
        dockerfile.append("# Start SSH server\n");
        dockerfile.append("CMD [\"/usr/local/bin/startup.sh\"]\n");
        
        return dockerfile.toString();
    }
    
    private void installTechnologyPackages(StringBuilder dockerfile, ImageTemplate template) {
        String technology = template.getTechnology();
        String version = template.getVersion();
        
        dockerfile.append("# Install ").append(technology).append(" ").append(version).append("\n");
        
        switch (technology.toLowerCase()) {
            case "python":
                installPythonPackages(dockerfile, version);
                break;
            case "nodejs":
                installNodeJSPackages(dockerfile, version);
                break;
            case "java":
                installJavaPackages(dockerfile, version);
                break;
            case "golang":
                installGoPackages(dockerfile, version);
                break;
            default:
                log.warn("Unknown technology: {}", technology);
        }
    }
    
    private void installPythonPackages(StringBuilder dockerfile, String version) {
        dockerfile.append("RUN apt-get update && apt-get install -y python3 python3-pip python3-venv \\\n");
        dockerfile.append("    && apt-get clean \\\n");
        dockerfile.append("    && rm -rf /var/lib/apt/lists/*\n\n");
        
        // Install common Python packages
        dockerfile.append("# Install common Python packages\n");
        dockerfile.append("RUN pip3 install --no-cache-dir \\\n");
        dockerfile.append("    numpy \\\n");
        dockerfile.append("    pandas \\\n");
        dockerfile.append("    matplotlib \\\n");
        dockerfile.append("    jupyter \\\n");
        dockerfile.append("    requests \\\n");
        dockerfile.append("    flask \\\n");
        dockerfile.append("    django\n\n");
    }
    
    private void installNodeJSPackages(StringBuilder dockerfile, String version) {
        dockerfile.append("RUN apt-get update && apt-get install -y curl \\\n");
        dockerfile.append("    && curl -fsSL https://deb.nodesource.com/setup_").append(version).append(".x | bash - \\\n");
        dockerfile.append("    && apt-get install -y nodejs \\\n");
        dockerfile.append("    && apt-get clean \\\n");
        dockerfile.append("    && rm -rf /var/lib/apt/lists/*\n\n");
        
        // Install global npm packages
        dockerfile.append("# Install global npm packages\n");
        dockerfile.append("RUN npm install -g \\\n");
        dockerfile.append("    nodemon \\\n");
        dockerfile.append("    yarn \\\n");
        dockerfile.append("    typescript \\\n");
        dockerfile.append("    @angular/cli \\\n");
        dockerfile.append("    react-scripts\n\n");
    }
    
    private void installJavaPackages(StringBuilder dockerfile, String version) {
        dockerfile.append("RUN apt-get update && apt-get install -y openjdk-").append(version).append("-jdk \\\n");
        dockerfile.append("    maven \\\n");
        dockerfile.append("    gradle \\\n");
        dockerfile.append("    && apt-get clean \\\n");
        dockerfile.append("    && rm -rf /var/lib/apt/lists/*\n\n");
        
        // Set JAVA_HOME
        dockerfile.append("# Set JAVA_HOME\n");
        dockerfile.append("ENV JAVA_HOME=/usr/lib/jvm/java-").append(version).append("-openjdk-amd64\n");
        dockerfile.append("ENV PATH=$PATH:$JAVA_HOME/bin\n\n");
    }
    
    private void installGoPackages(StringBuilder dockerfile, String version) {
        dockerfile.append("RUN apt-get update && apt-get install -y wget \\\n");
        dockerfile.append("    && wget https://golang.org/dl/go").append(version).append(".linux-amd64.tar.gz \\\n");
        dockerfile.append("    && tar -C /usr/local -xzf go").append(version).append(".linux-amd64.tar.gz \\\n");
        dockerfile.append("    && rm go").append(version).append(".linux-amd64.tar.gz \\\n");
        dockerfile.append("    && apt-get clean \\\n");
        dockerfile.append("    && rm -rf /var/lib/apt/lists/*\n\n");
        
        // Set Go environment variables
        dockerfile.append("# Set Go environment variables\n");
        dockerfile.append("ENV PATH=$PATH:/usr/local/go/bin\n");
        dockerfile.append("ENV GOPATH=/go\n");
        dockerfile.append("ENV PATH=$PATH:$GOPATH/bin\n\n");
    }
    
    private void installPreInstalledTools(StringBuilder dockerfile, String preInstalledTools) {
        try {
            // Parse JSON array of tools
            String[] tools = preInstalledTools.replaceAll("[\\[\\]\"]", "").split(",");
            
            dockerfile.append("# Install pre-installed tools\n");
            dockerfile.append("RUN apt-get update && apt-get install -y \\\n");
            
            for (int i = 0; i < tools.length; i++) {
                String tool = tools[i].trim();
                if (!tool.isEmpty()) {
                    dockerfile.append("    ").append(tool);
                    if (i < tools.length - 1) {
                        dockerfile.append(" \\\n");
                    }
                }
            }
            
            dockerfile.append(" \\\n");
            dockerfile.append("    && apt-get clean \\\n");
            dockerfile.append("    && rm -rf /var/lib/apt/lists/*\n\n");
            
        } catch (Exception e) {
            log.warn("Failed to parse pre-installed tools: {}", preInstalledTools, e);
        }
    }
    
    private void setEnvironmentVariables(StringBuilder dockerfile, String environmentVariables) {
        try {
            // Parse JSON object of environment variables
            String envVars = environmentVariables.replaceAll("[{}]", "");
            String[] vars = envVars.split(",");
            
            dockerfile.append("# Set environment variables\n");
            for (String var : vars) {
                String[] parts = var.split(":");
                if (parts.length == 2) {
                    String key = parts[0].replaceAll("\"", "").trim();
                    String value = parts[1].replaceAll("\"", "").trim();
                    dockerfile.append("ENV ").append(key).append("=").append(value).append("\n");
                }
            }
            dockerfile.append("\n");
            
        } catch (Exception e) {
            log.warn("Failed to parse environment variables: {}", environmentVariables, e);
        }
    }
    
    private void createDockerfileDirectory() throws IOException {
        Path dockerfilePath = Paths.get(DOCKERFILE_DIR);
        if (!Files.exists(dockerfilePath)) {
            Files.createDirectories(dockerfilePath);
        }
    }
    
    private String writeDockerfile(String templateName, String dockerfile) throws IOException {
        String dockerfileName = DOCKERFILE_DIR + "/Dockerfile." + templateName.toLowerCase().replaceAll("\\s+", "_");
        
        try (FileWriter writer = new FileWriter(dockerfileName)) {
            writer.write(dockerfile);
        }
        
        log.info("Created Dockerfile: {}", dockerfileName);
        return dockerfileName;
    }
    
    private String buildImage(ImageTemplate template, String dockerfileName) throws IOException, InterruptedException {
        String imageName = IMAGE_PREFIX + "-" + template.getName().toLowerCase().replaceAll("\\s+", "-") + ":" + template.getVersion();
        
        // Build Docker image
        ProcessBuilder pb = new ProcessBuilder(
            "docker", "build", "-f", dockerfileName, "-t", imageName, "."
        );
        pb.inheritIO();
        
        Process process = pb.start();
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            throw new RuntimeException("Docker build failed with exit code: " + exitCode);
        }
        
        log.info("Successfully built Docker image: {}", imageName);
        return imageName;
    }
    
    public List<String> getAvailableTechnologies() {
        return Arrays.asList("python", "nodejs", "java", "golang");
    }
    
    public Map<String, List<String>> getTechnologyVersions() {
        return Map.of(
            "python", Arrays.asList("3.8", "3.9", "3.10", "3.11"),
            "nodejs", Arrays.asList("14", "16", "18", "20"),
            "java", Arrays.asList("8", "11", "17", "21"),
            "golang", Arrays.asList("1.19", "1.20", "1.21")
        );
    }
} 
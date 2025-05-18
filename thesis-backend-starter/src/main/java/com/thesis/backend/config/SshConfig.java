package com.thesis.backend.ssh.config;

import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Paths;
import java.security.PublicKey;

@Configuration
public class SshConfig {

    private static final Logger logger = LoggerFactory.getLogger(SshConfig.class);

    @Value("${ssh.server.port:2222}")
    private int sshPort;

    @Value("${ssh.server.host:0.0.0.0}")
    private String sshHost;

    @Value("${ssh.server.hostkey.path:hostkey.ser}")
    private String hostKeyPath;

    @Bean
    public SshServer sshServer() throws IOException {
        SshServer sshServer = SshServer.setUpDefaultServer();
        sshServer.setHost(sshHost);
        sshServer.setPort(sshPort);
        
        // Use a simple host key provider
        sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Paths.get(hostKeyPath)));
        
        // We'll implement and inject these beans separately
        // sshServer.setShellFactory(shellFactory);
        // sshServer.setCommandFactory(commandFactory);
        
        // For now, accept all public keys - we'll implement proper auth later
        sshServer.setPublickeyAuthenticator(new PublickeyAuthenticator() {
            @Override
            public boolean authenticate(String username, PublicKey key, ServerSession session) {
                // In a real implementation, we would verify the key against stored user keys
                logger.info("SSH authentication attempt for user: {}", username);
                return true;
            }
        });

        return sshServer;
    }
}

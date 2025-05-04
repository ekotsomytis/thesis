
package com.thesis.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ContainerInstance {
    @Id @GeneratedValue private Long id;
    private String name;
    private String status;
    private String kubernetesPodName;

    @ManyToOne
    private User owner;

    @ManyToOne
    private ImageTemplate imageTemplate;
}

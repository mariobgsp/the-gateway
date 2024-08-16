package com.example.gatewayservice.models.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "system_properties", schema = "public")
public class SystemProperties {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String key;
    private String value;
    private String description;
    @Column(name = "vgroup")
    private String vgroup;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVgroup() {
        return vgroup;
    }

    public void setVgroup(String vgroup) {
        this.vgroup = vgroup;
    }
}

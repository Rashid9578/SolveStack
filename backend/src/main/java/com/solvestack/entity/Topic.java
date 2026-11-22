package com.solvestack.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "topics")
public class Topic {
    @Id
    private Long id;

    @Column(nullable = false)
    private String slug;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String accent;

    @Column(nullable = false)
    private int position;

    @OneToMany(mappedBy = "topic")
    private List<Problem> problems = new ArrayList<>();

    protected Topic() {}
}

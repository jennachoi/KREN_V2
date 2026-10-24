package com.kren.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Fixed catalogue seeded by Flyway; read-only from the application's point of view. */
@Entity
@Table(name = "interests")
public class Interest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50)
	private String name;

	protected Interest() {
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

}

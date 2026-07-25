package com.example.smartinventory.model;

/** Kind of mutation recorded in an {@link AuditLog} entry. */
public enum AuditAction {

    /** A new entity was created. */
    CREATE,

    /** An existing entity was updated. */
    UPDATE,

    /** An entity was deleted. */
    DELETE

}

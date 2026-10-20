package com.elan.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, String slug) {
        super(resource + " not found: " + slug);
    }
}

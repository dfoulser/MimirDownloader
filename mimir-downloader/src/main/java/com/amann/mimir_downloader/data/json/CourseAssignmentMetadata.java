package com.amann.mimir_downloader.data.json;

import java.time.Instant;

// Metadata about assignment that is part of the course JSON file.
public final class CourseAssignmentMetadata {
  private String id;
  private String name;
  private Instant openDate;

  public String getName() {
    return name;
  }

  public String getId() {
    return id;
  }

  public Instant getOpenDate() {
    return openDate;
  }
}

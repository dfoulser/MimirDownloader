package com.amann.mimir_downloader;

import java.io.File;
import java.io.IOException;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.amann.mimir_downloader.data.processed.Assignment;
import com.amann.mimir_downloader.data.processed.Course;

public final class MultiFileCourseWriter {
  public static void writeCourse(Course course, File folder,
      boolean overwriteFiles) throws Exception {
    // First write index of assignments\
    File target = new File(folder, "index.html");
    Util.checkShouldOverwrite(target, overwriteFiles);
    Document doc = Document.createShell("fake_url");
    generateHtml(course, doc);
    Util.printToFile(doc, target);

    // Then write actual assignments
    for (Assignment assignment : course.getAssignments()) {
      AssignmentWriter.writeAssignment(assignment, folder, overwriteFiles);
    }
  }

  private static void generateHtml(Course course, Document doc) {
    doc.title(course.getName());
    Util.addHeaderElements(doc);
    generateBody(course, doc.body());
  }

  private static void generateBody(Course course, Element body) {
    body.appendChild(new Element("h1").text(course.getName()));
    Element list = body.appendElement("ul");

    for (Assignment assignment : course.getAssignments()) {
      Element outputAssignment = list.appendElement("li");
      outputAssignment.appendElement("a").text(assignment.getName())
          .attr("href", Util.assignmentFileName(assignment.getName()));
    }
  }
}

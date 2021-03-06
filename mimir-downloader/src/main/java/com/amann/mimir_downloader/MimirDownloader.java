package com.amann.mimir_downloader;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import com.amann.mimir_downloader.data.json.Config;
import com.amann.mimir_downloader.data.processed.Assignment;
import com.amann.mimir_downloader.data.processed.Course;

enum OutputFormat {
  SINGLE_FILE,
  MULTI_FILE,
  CODE_DIRECTORY
};

public class MimirDownloader {
  public static final String HELP_PREFIX = "mimir-downloader [OPTIONS] <course URL copied from browser> <target (folder for multi-file)>";

  public static void main(String[] args) throws Exception {
    Options options = new Options();
    options.addOption("u", "user", true, "mimir user name (email)");
    options.addOption("p", "password", true, "mimir password");
    options.addOption("o", "overwrite", false,
        "overwriting existing files in directory");
    options.addOption("f", "format", true,
        "ouput format: one of multi-file (default), single-file, code-directory");
    options.addOption("h", "help", false, "print help");
    CommandLineParser parser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();
    CommandLine cmd = parser.parse(options, args);
    
    List<String> otherArgs = cmd.getArgList();
    if (otherArgs.size() != 2 || cmd.hasOption('h')) {
      formatter.printHelp(HELP_PREFIX, options);
      return;
    }
    
    String format = cmd.hasOption('f') ? cmd.getOptionValue('f') : "multi-file";
    if (!(format.equals("multi-file")
        || format.equals("single-file")
        || format.equals("code-directory"))) {
      System.out.println("Invalid output format.");
      formatter.printHelp(HELP_PREFIX, options);
      return;
    }
    OutputFormat outputFormat;
    if (format.equals("single-file")) {
	outputFormat = OutputFormat.SINGLE_FILE;
    } else if (format.equals("code-directory")) {
	outputFormat = OutputFormat.CODE_DIRECTORY;
    } else {
	// format.equals("multi-file")
	outputFormat = OutputFormat.MULTI_FILE;
    }

    String courseUrl = otherArgs.get(0);
    String target = otherArgs.get(1);
    boolean overwriteFiles = cmd.hasOption('o');

    String home = System.getProperty("user.home");
    File downloaderRoot = new File(home, ".mimir_downloader");
    Util.createDir(downloaderRoot);
    Config config = getAuthConfigFromArgs(cmd, downloaderRoot);
    if (config == null) {
      formatter.printHelp(HELP_PREFIX, options);
      return;
    }

    String courseId = CourseLoader.getCourseId(courseUrl);
    if (courseId == null) {
      System.out.println("Incorrect course URL. Course URLs start "
          + "with 'https://class.mimir.io/courses/'.");
      formatter.printHelp(HELP_PREFIX, options);
      return;
    }
    
    Course course = CourseLoader.loadCourse(courseId, config);

    switch (outputFormat) {
    case MULTI_FILE:
    default:
      saveCourseMultiFile(course, target, overwriteFiles);
      break;
    case SINGLE_FILE:
      saveCourseSingleFile(course, target, overwriteFiles);
      break;
    case CODE_DIRECTORY:
      saveCourseCodeDirectory(course, target, overwriteFiles);
      break;
    }

//     File targetFolder = new File(target);

//    Course course = CourseLoader
//        .loadCourseFromFile(new File("realCourse.json"));
//     MultiFileCourseWriter.writeCourse(course, targetFolder, overwriteFiles);

//    Assignment parsedAssignment = AssignmentLoader
//        .loadAssignmentFromFile(new File("realAssignment2.json"));
//    course.addAssignment(parsedAssignment);
//    SingleFileCourseWriter.writeCourse(course, new File(target), overwriteFiles);

//     Assignment parsedAssignment = AssignmentLoader
//         .loadAssignmentFromFile(new File("assignment.json"));
//     AssignmentWriter.writeAssignmentCodeTree(parsedAssignment, targetFolder,
//         overwriteFiles);
  }

  private static void saveCourseMultiFile(Course course, String target,
      boolean overwriteFiles) throws IOException, Exception {
    File targetFolder = new File(target);
    if (!targetFolder.isDirectory() && !targetFolder.mkdirs()) {
      System.out.println("Could not create output directory");
      return;
    }
    Util.copyResources(targetFolder, overwriteFiles);
    MultiFileCourseWriter.writeCourse(course, targetFolder, overwriteFiles);
  }
  
  private static void saveCourseSingleFile(Course course, String target,
      boolean overwriteFiles) throws IOException, Exception {
    File targetFile = new File(target);
    SingleFileCourseWriter.writeCourse(course, targetFile, overwriteFiles);
  }

  private static void saveCourseCodeDirectory(Course course, String target,
      boolean overwriteFiles) throws IOException, Exception {
    File targetFile = new File(target);
    DirectoryCourseWriter.writeCourse(course, targetFile, overwriteFiles);
  }

  private static Config getAuthConfigFromArgs(CommandLine cmd,
      File downloaderRoot) throws IOException {
    Config config = Util.readConfig(downloaderRoot);

    if (cmd.hasOption('u') && !cmd.hasOption('p')
        || !cmd.hasOption('u') && cmd.hasOption('p')) {
      System.out.println(
          "Mimir user name and password need to be specified together");
      return null;
    }
    if (cmd.hasOption('u') && cmd.hasOption('p')) {
      // Sign user in and store credentials in config
      if (Networking.createSession(config, cmd.getOptionValue('u'),
          cmd.getOptionValue('p'))) {
        Util.writeConfig(downloaderRoot, config);
      } else {
        System.out.println("Invalid user name or password");
        return null;
      }
    }
    if (!Networking.verifySession(config)) {
      System.out.println("No valid Mimir session token in storage. "
          + "Must specify user name and password.");
      return null;
    }
    return config;
  }
}

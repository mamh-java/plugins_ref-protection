package com.googlesource.gerrit.plugins.refprotection;

import static org.eclipse.jgit.lib.Constants.R_HEADS;
import static org.eclipse.jgit.lib.Constants.R_REFS;
import static org.eclipse.jgit.lib.Constants.R_TAGS;

import java.text.SimpleDateFormat;
import java.util.Date;

public class BackupBranch {
  public static final String R_BACKUPS = R_REFS + "backups/";

  public static String get(String branchName) {
    if (branchName.startsWith(R_HEADS) || branchName.startsWith(R_TAGS)) {
      return String.format("%s-%s",
          R_BACKUPS + branchName.replaceFirst(R_REFS, ""),
          new SimpleDateFormat("YYYYMMdd-HHmmss").format(new Date()));
    }

    return branchName;
  }
}

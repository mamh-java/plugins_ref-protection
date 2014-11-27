package com.googlesource.gerrit.plugins.refprotection;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class BackupBranchNameTest {

  @Test
  public void backupBranchNameForTag() throws Exception {
    String name = BackupBranch.get("refs/tags/v1.0");
    String expected_prefix = BackupBranch.R_BACKUPS + "tags/v1.0-";
    assertThat(name).startsWith(expected_prefix);
  }

  @Test
  public void backupBranchNameForBranch() throws Exception {
    String name = BackupBranch.get("refs/heads/master");
    String expected_prefix = BackupBranch.R_BACKUPS + "heads/master-";
    assertThat(name).startsWith(expected_prefix);
  }

  @Test
  public void backupBranchNameForUnsupportedNamespace() throws Exception {
    String ref = "refs/changes/45/12345/1";
    assertThat(BackupBranch.get(ref)).isEqualTo(ref);
  }
}

package com.googlesource.gerrit.plugins.refprotection;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class BackupRefNameTest {

  @Test
  public void backupRefNameForTag() throws Exception {
    String name = BackupRef.get("refs/tags/v1.0");
    String expected_prefix = BackupRef.R_BACKUPS + "tags/v1.0-";
    assertThat(name).startsWith(expected_prefix);
  }

  @Test
  public void backupRefNameForBranch() throws Exception {
    String name = BackupRef.get("refs/heads/master");
    String expected_prefix = BackupRef.R_BACKUPS + "heads/master-";
    assertThat(name).startsWith(expected_prefix);
  }

  @Test
  public void backupRefNameForUnsupportedNamespace() throws Exception {
    String ref = "refs/changes/45/12345/1";
    assertThat(BackupRef.get(ref)).isEqualTo(ref);
  }
}

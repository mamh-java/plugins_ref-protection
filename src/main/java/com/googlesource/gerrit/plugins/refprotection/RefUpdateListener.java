package com.googlesource.gerrit.plugins.refprotection;

import static org.eclipse.jgit.lib.Constants.R_HEADS;
import static org.eclipse.jgit.lib.Constants.R_TAGS;

import com.google.gerrit.extensions.events.GitReferenceUpdatedListener;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.project.CreateBranch;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gerrit.server.project.ProjectResource;
import com.google.inject.Inject;

import org.eclipse.jgit.lib.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

class RefUpdateListener implements GitReferenceUpdatedListener {

  private static final Logger log = LoggerFactory
      .getLogger(RefUpdateListener.class);
  private final CreateBranch.Factory createBranchFactory;
  private final ProjectControl.GenericFactory projectControl;
  private final CurrentUser user;

  @Inject
  RefUpdateListener(CreateBranch.Factory createBranchFactory,
      ProjectControl.GenericFactory p, CurrentUser user) {
    this.createBranchFactory = createBranchFactory;
    this.projectControl = p;
    this.user = user;
  }

  @Override
  public void onGitReferenceUpdated(final Event event) {
    if (!isNewRef(event) && isRelevantRef(event)) {
      if (isRefDeleted(event) || isNonFastForwardUpdate(event)) {
        createBackupBranch(event);
      }
    }
  }

  /**
   * Create a backup branch for the given ref.
   *
   * @param event the Event
   */
  private void createBackupBranch(Event event) {
    String branchName = event.getRefName().replaceFirst(R_HEADS, "");
    try {
      String branchPrefix = "";
      int n = branchName.lastIndexOf("/");
      if (n != -1) {
        branchPrefix = branchName.substring(0, n + 1);
        branchName = branchName.substring(n + 1);
      }

      String ref =
          String.format("%sbackup-%s-%s", branchPrefix, branchName,
              new SimpleDateFormat("YYYYMMdd-HHmmss").format(new Date()));

      CreateBranch.Input input = new CreateBranch.Input();
      input.ref = ref;
      input.revision = event.getOldObjectId();
      Project.NameKey nameKey = new Project.NameKey(event.getProjectName());
      ProjectResource project =
          new ProjectResource(projectControl.controlFor(nameKey, user));

      createBranchFactory.create(ref).apply(project, input);
    } catch (BadRequestException | AuthException | ResourceConflictException
        | IOException | NoSuchProjectException e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Is the event on a relevant ref?
   *
   * @param event the Event
   * @return True if relevant, otherwise False.
   */
  private boolean isRelevantRef(Event event) {
    return event.getRefName().startsWith(R_HEADS)
        || event.getRefName().startsWith(R_TAGS);
  }

  /**
   * Is the event a new ref?
   *
   * @param event the Event
   * @return True if a new ref, otherwise False.
   */
  private boolean isNewRef(Event event) {
    return event.getOldObjectId().equals(ObjectId.zeroId().getName());
  }

  /**
   * Is the event a ref deletion?
   *
   * @param event the Event
   * @return True if a ref deletion, otherwise False.
   */
  private boolean isRefDeleted(Event event) {
    if (event.getNewObjectId().equals(ObjectId.zeroId().getName())) {
      log.info(String.format(
          "Ref Deleted: project [%s] refname [%s] old object id [%s]",
          event.getProjectName(), event.getRefName(), event.getOldObjectId()));
      return true;
    }
    return false;
  }

  /**
   * Is the event a non-fast-forward update?
   *
   * @param event the Event
   * @return True if a non-fast-forward update, otherwise False.
   */
  private boolean isNonFastForwardUpdate(Event event) {
    // TODO
    return false;
  }
}

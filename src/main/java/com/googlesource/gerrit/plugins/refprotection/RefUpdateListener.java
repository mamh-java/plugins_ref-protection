package com.googlesource.gerrit.plugins.refprotection;

import static org.eclipse.jgit.lib.Constants.R_HEADS;
import static org.eclipse.jgit.lib.Constants.R_TAGS;

import com.google.gerrit.extensions.events.GitReferenceUpdatedListener;

import org.eclipse.jgit.lib.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RefUpdateListener implements GitReferenceUpdatedListener {

  private static final Logger log = LoggerFactory
      .getLogger(RefUpdateListener.class);

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
    // TODO
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

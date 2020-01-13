/*
 *  The MIT License
 *
 *  Copyright 2015 Sony Mobile Communications AB. All rights reserved.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package com.googlesource.gerrit.plugins.refprotection;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.events.RefUpdatedEvent;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.project.CreateBranch;
import com.google.gerrit.server.project.ProjectResource;
import com.google.inject.Inject;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TagBuilder;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.eclipse.jgit.lib.Constants.R_HEADS;
import static org.eclipse.jgit.lib.Constants.R_TAGS;

public class BackupRef {
    static final Logger log = LoggerFactory.getLogger(RefProtectionLogFile.REFPROTECTION_LOG_NAME);

    public static final String R_BACKUPS = "backup/";

    private final CreateBranch.Factory createBranchFactory;

    @Inject
    private static PluginConfigFactory cfg;

    @Inject
    private static GitRepositoryManager repoManager;

    @Inject
    @PluginName
    private static String pluginName;

    @Inject
    BackupRef(CreateBranch.Factory createBranchFactory) {
        this.createBranchFactory = createBranchFactory;
    }

    public void createBackup(RefUpdatedEvent event, ProjectResource project) {
        String refName = event.getRefName();  // 原来的分支名称
        String backupRef = getTimestampBranch(refName);  // 备份分支名称

        if (backupRef.equals(refName)) {
            return;
        }

        try (Repository git = repoManager.openRepository(project.getNameKey())) {
            try (RevWalk revWalk = new RevWalk(git)) {
                if (cfg.getFromGerritConfig(pluginName).getBoolean("createTag", false)) { //这个 是 以 创建 tag 的方式 来备份 分支
                    craeteBackupbyTags(git, revWalk, event, backupRef);
                } else {// 这个 是 以 创建 branch 的方式 来备份 分支
                    createBackupByBranch(project, revWalk, event, backupRef);
                }
            }//end try (RevWalk revWalk = new RevWalk(git))
        } catch (RepositoryNotFoundException e) {
            log.error("Repository does not exist", e);
        } catch (IOException e) {
            log.error("Could not open repository", e);
        }
    }

    private void createBackupByBranch(ProjectResource project, RevWalk revWalk, RefUpdatedEvent event, String backupRef) throws IOException{
        CreateBranch.Input input = new CreateBranch.Input();
        input.ref = backupRef;
        input.revision = ObjectId.toString(revWalk.parseCommit(ObjectId.fromString(event.refUpdate.oldRev)).getId());
        try {
            log.info(String.format(
                    "Ref  Backup: project [%s] refname [%s] oldRev [%s] newRev [%s]",
                    event.getProjectNameKey().toString(),
                    input.ref,
                    event.refUpdate.oldRev,
                    event.refUpdate.newRev
            ));
            createBranchFactory.create(backupRef).apply(project, input);
        } catch (BadRequestException | AuthException | ResourceConflictException | IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void craeteBackupbyTags(Repository git, RevWalk revWalk, RefUpdatedEvent event, String backupRef) throws IOException{
        TagBuilder tag = new TagBuilder();
        tag.setTagger(new PersonIdent(event.submitter.name, event.submitter.email));
        tag.setObjectId(revWalk.parseCommit(ObjectId.fromString(event.refUpdate.oldRev)));
        String update = "Non-fast-forward update to";
        if (event.refUpdate.newRev.equals(ObjectId.zeroId().getName())) {
            update = "Deleted";
        }
        String type = "branch";
        String fullMessage = "";
        if (event.refUpdate.refName.startsWith(R_TAGS)) {
            type = "tag";
            try {
                RevTag origTag = revWalk.parseTag(ObjectId.fromString(event.refUpdate.oldRev));
                SimpleDateFormat format = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy ZZZZ");
                PersonIdent taggerIdent = origTag.getTaggerIdent();
                String tagger = String.format("Tagger: %s <%s>\nDate:   %s",
                        taggerIdent.getName(),
                        taggerIdent.getEmailAddress(),
                        format.format(taggerIdent.getWhen()));
                fullMessage = "\n\nOriginal tag:\n" + tagger + "\n\n" + origTag.getFullMessage();
            } catch (MissingObjectException e) {
                log.warn("Original tag does not exist", e);
            } catch (IncorrectObjectTypeException e) {
                log.warn("Original tag was not a tag", e);
            } catch (IOException e) {
                log.warn("Unable to read original tag details", e);
            }
        }
        tag.setMessage(update + " " + type + " " + event.refUpdate.refName + fullMessage);
        tag.setTag(backupRef);

        ObjectInserter inserter = git.newObjectInserter();
        ObjectId tagId = inserter.insert(tag);
        inserter.flush();
        RefUpdate tagRef = git.updateRef(tag.getTag());
        tagRef.setNewObjectId(tagId);
        tagRef.setRefLogMessage("tagged deleted branch/tag " + tag.getTag(), false);
        tagRef.setForceUpdate(false);
        Result result = tagRef.update();
        switch (result) {
            case NEW:
            case FORCED:
                log.debug("Successfully created backup tag");
                break;

            case LOCK_FAILURE:
                log.error("Failed to lock repository while creating backup tag");
                break;

            case REJECTED:
                log.error("Tag already exists while creating backup tag");
                break;

            default:
                log.error("Unknown error while creating backup tag");
        }
    }

    private static String getTimestampBranch(String refName) {
        if (refName.startsWith(R_HEADS)) {
            String newRefName = String.format("%s_%s", R_HEADS + R_BACKUPS + refName.replaceFirst(R_HEADS, ""),
                    new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()));

            return newRefName;
        }
        if (refName.startsWith(R_TAGS)) {
            String newRefName = String.format("%s_%s", R_TAGS + R_BACKUPS + refName.replaceFirst(R_TAGS, ""),
                    new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()));

            return newRefName;
        }
        return refName;
    }


}

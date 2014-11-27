Ref protection plugin.

Protects against commits being lost by creating backups of deleted refs under
the `refs/backups/` namespace.

Branches under `refs/heads/` that are deleted or rewritten are backed up
as `refs/backups/heads/branch-name-YYYYMMDD-HHmmss`.

Tags under `refs/tags/` that are deleted are backed up (as branches) as
`refs/backups/tags/tag-name-YYYYMMDD-HHmmss`.

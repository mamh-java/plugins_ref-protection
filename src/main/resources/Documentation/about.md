Ref protection plugin. 保护分支插件

```text
1.
对于 refs/heads/* 下面的,我通常称之为 分支. 这个插件就是保护分支,当删除了
某个分支,或者强制推送修改了某个分支,就会触发这个插件,这个插件会在 refs/heads/backup/
下面建立个备份的分支,备份分支命名是 原始分支名称 + 时间戳. 
类似这样的格式的refs/heads/backup/branch-name-yyyyMMDD-HHmmss

2.
对于 refs/tags/* 下面的,我通常称之为 标签. 这个插件同样的也会备份这个标签.
和分支原理一样,refs/tags/backup/tag-name-yyyyMMDD-HHmmss

这个插件还有个这个 配置选项plugin.ref-protection.createTag true,默认这个是false的.
如果打开这个,就会以创建备份的 tags 来备份 上面所说的 1,和2  分支/标签.



plugin.ref-protection.useTimestamp 删除了

plugin.ref-protection.protectFastForward 删除了


```
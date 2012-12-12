# Changelog for Illicium

## 0.8.4

* Moved to new git repository.
* Updated example config files.

## 0.8.3

* From Humulus plugin: database-config is now read from "${System.env.CINNAMON_HOME_DIR}/database-config.groovy"
  instead of $appName-config.groovy. Please update your Illicium and Dandelion configuration accordingly.

## 0.8.2

* New: Edit custom metadata on folders.
* Fixed Javascript declaration order (so resources plugin loads jquery before custom script).

## 0.8.1

* Upgrade to Grails 2.0
* Upgrade to Humulus 0.5
* added zipFolder

## 0.8

* Show version status in system metadata overview (show if the selected object is the newest in this branch
  and / or the main branch).
* Updated to Spring-Security-Plugin 1.2.4 (and Humulus 0.4.6)

## 0.7

* Humulus upgraded to 0.4.4 to fix problem with User.equals()
* Fixed problem with lock/unlock objects.
* improved folder navigation speed.
* OsdController.newVersion now checks ACLs before displaying folderContent.
* FolderController.fetchFolderContent now is more resilient to unexpected exceptions.
* [2115] Fixed: link to object in relation list (left, right) was broken.

## 0.6

* Added delete Folder (only empty folders, currently).
* Filter objects and folders according to ACL in fetchFolderContent.
* Added folder-independent selection option for OSDs.
* Implemented delete & deleteAll for selected OSDs.
* Added folder selection option.
* Removed experimental drag & drop code.
* Added select-all and deselect-all to folder and object lists.
* Fixed a bug in template use in OsdController.newVersion

## 0.5

* Added createFolder
* refactored CSS

## 0.4

* Fixed: css bug which corrupted folder content display
* Removed obsolete /index.gsp.

## 0.3

* Fixed: encoding error on /folder/index.gsp
* Fixed: img tag was not closed in /folder/index.gsp

## 0.2

* Upgrade to CodeMirror 2.01
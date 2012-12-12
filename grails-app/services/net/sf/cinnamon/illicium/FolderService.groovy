package net.sf.cinnamon.illicium

import server.Folder
import server.data.ObjectSystemData
import server.global.Constants
import safran.Client

/**
 *
 */
class FolderService {

    /**
     * Check if a folder has content objects (meaning OSD, not sub-folders)
     * @param folder the folder to check
     * @return true if there is at least one OSD which has this folder as parent, false otherwise.
     */

    Boolean hasContent(Folder folder) {
        return ObjectSystemData.findWhere(parent:folder) != null
    }

    /**
     * @return the root folder of the repository to which the user is logged in.
     */
    Folder findRootFolder(){
        return (Folder) Folder.find("from Folder as f where name=:name and parent_id=id", [name : Constants.ROOT_FOLDER_NAME])
    }

    List<Folder> getParentFolders(Folder folder){
		List<Folder> folders = new ArrayList<Folder>();
		Folder root = findRootFolder();
		folder = folder.getParent();
		while(folder != null && folder !=  root ){
			folders.add(folder);
			folder = folder.getParent();
		}
		return folders;
	}

    /**
     * Create a set of
     * @param folderId
     * @param osdId
     * @return
     */
    Set<String> createTriggerSet(folderId, osdId){
        log.debug("createTriggerSet")
        def folderList = new ArrayList<Folder>()
        def triggerSet = new HashSet<String>()
        if(folderId){
            Folder folder = Folder.get(folderId)
            if(folder){
                folderList.addAll(getParentFolders(folder)?.reverse())
                triggerSet.addAll(folderList.collect {"fetchLink_"+it.id})
                folderList.add(folder)
//                if(osdId){
//                    ObjectSystemData osd = ObjectSystemData.get(osdId)
//                    if(osd){
//                        triggerSet.add("fetchDetailsLink_${osd.id}")
//                    }
//                }
            }
        }
        log.debug("triggerSet: ${triggerSet}")
        return triggerSet
    }

    Map<String,List> delete(Client client, idList){
        def msgMap = [:]
        idList.each{ id ->
            try{
                log.debug("delete folder: $id")
                client.deleteFolder(Long.parseLong(id));
                msgMap.put(id, ['folder.delete.ok'])
            }
            catch (Exception e){
                msgMap.put(id, ['folder.delete.fail', e.message])
            }
        }
        return msgMap

    }
}

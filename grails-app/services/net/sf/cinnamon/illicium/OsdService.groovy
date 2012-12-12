package net.sf.cinnamon.illicium

import server.data.ObjectSystemData
import safran.Client

/**
 */
class OsdService {

    Map<String,List> delete(Client client, idList){
        def msgMap = [:]
        idList.each{ id ->
            try{
                log.debug("delete: $id")
                client.delete(Long.parseLong(id));
                msgMap.put(id, ['osd.delete.ok'])
            }
            catch (Exception e){
                msgMap.put(id, ['osd.delete.fail', e.message])
            }
        }
        return msgMap

    }

    Map<String,List> deleteAllVersions(Client client, idList){
        def msgMap = [:]
        idList.each{ id ->
            try{
                client.deleteAllVersions(Long.parseLong(id));
                msgMap.put(id, ['osd.delete.all.ok'])
            }
            catch (Exception e){
                msgMap.put(id, ['osd.delete.all.fail', e.message])
            }
        }
        return msgMap

    }

}

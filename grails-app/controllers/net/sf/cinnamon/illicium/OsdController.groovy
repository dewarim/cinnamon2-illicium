package net.sf.cinnamon.illicium

import server.data.ObjectSystemData

import server.User

import eu.hornerproject.humulus.IgnorableException
import server.Folder

import server.global.PermissionName
import safran.Client
import server.Format
import server.ObjectType

import org.springframework.web.multipart.MultipartFile
import eu.hornerproject.humulus.RepositoryService
import server.global.ConfThreadLocal
import server.global.Conf
import eu.hornerproject.humulus.InputValidationService
import eu.hornerproject.humulus.UserService

/**
 *
 */
//@Secured(["hasRole('_superusers')"])
@grails.plugins.springsecurity.Secured(["isAuthenticated()"])
class OsdController extends BaseController {

    def osdService

    def editMetadata () {
        ObjectSystemData osd = ObjectSystemData.get(params.osd)
        // TODO: filter according to ACL
        if (!osd) {
            return render(status: 503, text: message(code: 'error.osd.not.found'))
        }
        return render(template: '/osd/editMetadata', model: [osd: osd])
    }

    def saveMetadata () {
        ObjectSystemData osd = ObjectSystemData.get(params.osd)
        // TODO: filter according to ACL
        if (!osd) {
            return render(status: 503, text: message(code: 'error.osd.not.found'))
        }

        def metadata = params.metadata
        if (!metadata || metadata.trim().length() == 0) {
            metadata = '<meta/>'
        }

        def repositoryName = session.repositoryName
        def oldMetadata = "<meta/>"
        def errorMsg = null
        try {
            // only save if osd has changed:
            log.debug("trying to save metadata '$metadata'")
            if (!osd.metadata.equals(metadata)) {
                oldMetadata = osd.metadata
                User user = userService.user
                repositoryService.acquireLock(user, osd, repositoryName)
                repositoryService.setMetadata(user, params.osd, metadata, repositoryName)
                osd.refresh() // expected: osd is changed by server, so we need to reload it.
                repositoryService.unlockOsd(user, osd, repositoryName)
            }
        }
        catch (Exception ex) {
            log.debug("failed to update metadata: ", ex)
            errorMsg = message(code: 'error.save.metadata', args: [message(code: ex.message)])
        }
        finally {
            if (errorMsg) {
                // reverse any changes:d
                osd.metadata = oldMetadata
            }
        }

        if (errorMsg) {
            log.debug("error: $errorMsg")
            return render(status: 503, text: errorMsg)
        }
        else {
            osd.refresh() // reload changed object from DB 
            return render(template: 'objectDetails', model: [osd: osd, permissions: loadUserPermissions(osd.acl)])
        }
    }

    def unlockOsd () {
        ObjectSystemData osd = ObjectSystemData.get(params.osd)
        if (!osd) {
            return render(status: 503, text: message(code: 'error.osd.not.found'))
        }

        try {
            User user = userService.user
            repositoryService.unlockOsd(user, osd, session.repositoryName)
        }
        catch (Exception ex) {
            log.debug("unlocking of OSD failed ", ex)
            return render(status: 503, text: message(code: 'error.acquire.lock'))
        }
        flash.message = message(code: 'osd.is.unlocked', args: [osd.id])
        return redirect(action: 'fetchFolderContent', controller: 'folder', params: [id: osd.parent.id])
    }

    def lockOsd () {
        ObjectSystemData osd = ObjectSystemData.get(params.osd)
        if (!osd) {
            return render(status: 503, text: message(code: 'error.osd.not.found'))
        }

        try {
            User user = userService.user
            repositoryService.acquireLock(user, osd, session.repositoryName)
        }
        catch (Exception ex) {
            log.debug("locking of OSD failed", ex)
            return render(status: 503, text: message(code: 'error.acquire.lock'))
        }
        flash.message = message(code: 'osd.is.locked', args: [osd.id])
        return redirect(action: 'fetchFolderContent', controller: 'folder', params: [id: osd.parent.id])
    }

    def editContent () {

    }

    def listRelations () {
        ObjectSystemData osd = ObjectSystemData.get(params.osd)
        // TODO: filter according to ACL
        if (!osd) {
            return render(status: 503, text: message(code: 'error.osd.not.found'))
        }

        def leftRelations = server.Relation.findAllWhere(leftOSD: osd)
        def rightRelations = server.Relation.findAllWhere(rightOSD: osd)

        return render(template: '/osd/listRelations',
                model: [leftRelations: leftRelations, rightRelations: rightRelations,
                        osd: osd
                ]
        )
    }

    def fetchObjectDetails () {
        ObjectSystemData osd = ObjectSystemData.get(params.osd)
        // TODO: filter according to ACL
        if (!osd) {
            return render(status: 503, text: message(code: 'error.osd.not.found'))
        }

        log.debug("found object. ${params.id}: $osd")
        log.debug("contenttype: ${osd.format?.contenttype}")
        def hasRelations = server.Relation.find("from Relation as r where r.rightOSD=:o1 or r.leftOSD=:o2",
                [o1: osd, o2: osd]) ? true : false
        def permissions = loadUserPermissions(osd.acl)
        def user = userService.user
        return render(template: "/osd/objectDetails",
                model: [osd: osd, permissions: permissions,
                        superuserStatus: userService.isSuperuser(user),
                        hasRelations: hasRelations])
    }

    def renderPreview () {
        //TODO: filter according to ACL
        ObjectSystemData osd = ObjectSystemData.get(params.osd)
        if (!osd) {
            return render(status: 503, text: message(code: 'error.osd.not.found'))
        }

        def osdContent
        try {
            User user = userService.user
            def repositoryName = session.repositoryName
            osdContent = repositoryService.getTextContent(user, osd, repositoryName)
            // osd.getContent(session.repositoryName, 'UTF-8')
        }
        catch (Exception ioe) {
            log.debug("Content of OSD was not found: ", ioe)
            return render(status: 503, text: message(code: 'error.content.not.found'))
        }
//        log.debug("osdContent: $osdContent contentType: ${osd.format.contenttype}")
        return render(template: 'objectPreview', model: [osd: osd, ctype: osd.format?.contenttype, osdContent: osdContent])
    }

    def renderMetadata () {
        //TODO: filter according to ACL
        ObjectSystemData osd = ObjectSystemData.get(params.osd)
        if (!osd) {
            return render(status: 503, text: message(code: 'error.osd.not.found'))
        }

        return render(template: 'renderMetadata', model: [osd: osd])
    }

    def imageLoader () {
        //TODO: filter according to ACL

        ObjectSystemData osd = ObjectSystemData.get(params.id)
        if (!osd) {
            return render(status: 503, text: message(code: 'error.osd.not.found'))
        }
        if (!osd.format?.contenttype?.startsWith('image/')) {
            return render(status: 503, text: message(code: 'error.wrong.format'))
        }
        response.setContentType(osd.format.contenttype)
        server.global.Conf conf = server.global.ConfThreadLocal.getConf()
        log.debug("repository: ${session.repositoryName}")
        def filename = conf.getDataRoot() + File.separator + session.repositoryName +
                File.separator + osd.contentPath
        log.debug("filename:$filename")
        File image = new File(filename)
        if (!image.exists()) {
//            image = new File( "/home/ingo/workspace2/Dandelion/web-app/images/no.png" )
            log.debug("could not find: $filename")
            return render(status: 503, text: message(code: 'error.image.not.found'))
        }
        response.outputStream << image.readBytes()
        response.outputStream.close()
        return null
    }

    def getContent () {
        Folder folder = (Folder) inputValidationService.checkObject(Folder.class, params.folder, true)
        if (!folder) {
            log.debug("getContent: folder not found")
            flash.message = message(code: 'error.folder.not.found')
            return redirect(controller: 'folder', action: 'index', params: params)
        }

        ObjectSystemData osd = ObjectSystemData.get(params.osd)
        if (!osd) {
            log.debug("getContent: osd not found")
            flash.message = message(code: 'error.osd.not.found')
            return redirect(controller: 'folder', action: 'index', params: params)
        }
        if (osd.contentSize == null || osd.contentSize == 0) {
            log.debug("getContent: no content")
            flash.message = message(code: 'error.content.not.found')
            return redirect(controller: 'folder', action: 'index', params: params)
        }

        Conf conf = ConfThreadLocal.getConf()
        def filename = conf.getDataRoot() + File.separator + session.repositoryName +
                File.separator + osd.contentPath
        log.debug("getContent called for #${osd.id} @ $filename")
        File data = new File(filename)
        if (!data.exists()) {
            log.debug("could not find: $filename")
            flash.message = message(code: 'error.file.not.found')
            return redirect(controller: 'folder', action: 'index', params: params)
        }
        response.setHeader("Content-disposition", "attachment; filename=${osd.name.encodeAsURL()}.${osd.format.extension}");
        response.setContentType(osd.format.contenttype)
        response.outputStream << data.newInputStream()
        response.outputStream.flush()
        return
    }

    def editName () {
        try {
            ObjectSystemData osd = loadOsd(params.osd)
            verifyOsdViewable(osd)
            // Note: this does not check if the user may actually change the name -
            // this happens both before (in the GSP) and after (in the CinnamonServer).
            render(template: 'editName', model: [osd: osd])
        }
        catch (IgnorableException ie) {
            log.debug("ignorableException.")
        }
    }

    def editAcl () {
        try {
            ObjectSystemData osd = loadOsd(params.osd)
            verifyOsdViewable(osd)
            render(template: 'editAcl', model: [osd: osd])
        }
        catch (IgnorableException ie) {
            log.debug("ignorableException.")
        }
    }

    def editLanguage () {
        try {
            ObjectSystemData osd = loadOsd(params.osd)
            verifyOsdViewable(osd)
            render(template: 'editLanguage', model: [osd: osd])
        }
        catch (IgnorableException ie) {
            log.debug("ignorableException.")
        }
    }

    def editOwner () {
        try {
            ObjectSystemData osd = loadOsd(params.osd)
            verifyOsdViewable(osd)
            render(template: 'editOwner', model: [osd: osd])
        }
        catch (IgnorableException ie) {
            log.debug("ignorableException.")
        }
    }

    def editType = {
        try {
            ObjectSystemData osd = loadOsd(params.osd)
            verifyOsdViewable(osd)
            render(template: 'editType', model: [osd: osd])
        }
        catch (IgnorableException ie) {
            log.debug("ignorableException.")
        }
    }

    protected void verifyOsdViewable(ObjectSystemData osd) {
        if (!repositoryService.mayBrowseOsd(userService.user,
                osd, session.repositoryName)) {
            render(status: 401, text: message(code: 'error.access.denied'))
            throw new IgnorableException()
        }
    }

    protected ObjectSystemData loadOsd(String id) {
        ObjectSystemData osd = ObjectSystemData.get(id)
        if (!osd) {
            render(status: 503, text: message(code: 'error.osd.not.found'))
            throw new IgnorableException("ignore me")
        }
        return osd
    }

    def saveField () {
        try {
            if (fieldNameAllowed(params.fieldName)) {
                tryUpdate({ userName ->
                    log.debug("going to updateOsd: ${params.dump()}")
                    repositoryService.updateOsd(userName, params.osd, params.fieldName, params.fieldValue, session.repositoryName)
                })

                fetchObjectDetails()
            }
            else {
                render(status: 401, text: message(code: 'error.illegal.parameter', args: [params.fieldName?.encodeAsHTML()]))
            }
        } catch (IgnorableException ex) {
            log.debug("ignore IngorableException.")
        }
    }

    static List<String> allowedFields = ['name', 'format', 'acl_id', 'objtype', 'language_id', 'owner']

    protected Boolean fieldNameAllowed(String name) {
        return allowedFields.contains(name)
    }

    def create () {
        Folder folder = (Folder) inputValidationService.checkObject(Folder.class, params.folder, true)
        if (!folder) {
            return redirect(controller: 'folder', action: 'index')
        }
        if (repositoryService.checkPermission(userService.user, folder.acl, session.repositoryName, PermissionName.CREATE_OBJECT)) {
            return [folder: folder]
        }
        else {
            flash.message = message(code: 'error.access.forbidden')
            return redirect(controller: 'folder', action: 'index', model: [folder: params.folder])
        }
    }

    def saveObject () {
        Folder folder = null
        try {
            User user = userService.user

            folder = (Folder) inputValidationService.checkObject(Folder.class, params.folder)
            def id = 0

            if (!folder) {
                throw new RuntimeException('error.missing.folder')
            }
            if (!repositoryService.mayBrowseFolder(user, folder, session.repositoryName)) {
                throw new RuntimeException('error.access.denied')
            }

            ObjectType objectType = (ObjectType) inputValidationService.checkObject(ObjectType.class, params.objectType, true)
            if (!objectType) {
                throw new RuntimeException('error.missing.objectType')
            }

            Client client = repositoryService.getClient(user.name, session.repositoryName)
            def name = params.name
            MultipartFile file = request.getFile('file')

            if (file.isEmpty()) {
                if (!name) {
                    throw new RuntimeException('error.missing.name')
                }
                id = client.create('<meta/>', name, folder.id, objectType.id)
            }
            else {
                File tempFile = File.createTempFile('illicium_upload_', null)
                file.transferTo(tempFile)
                if (!name) {
                    name = file.originalFilename
                }
                Format format = (Format) inputValidationService.checkObject(Format.class, params.format, true)
                if (!format) {
                    throw new RuntimeException('error.missing.format')
                }
                id = client.create('<meta/>', name.encodeAsHTML(), tempFile.absolutePath,
                        format.name, format.contenttype, folder.id)
            }
            // on success: redirect fetchFolderContent
            log.debug("created object with id: ${id}")
            return redirect(controller: 'folder', action: 'index', params: [folder: folder.id, osd: id])
        }
        catch (RuntimeException e) {
            log.debug("Failed to save object: ", e)
            flash.message = message(code: e.getMessage())
            if (!folder) {
                return redirect(controller: 'folder', action: 'index')
            }
            return redirect(controller: 'osd', action: 'create', params: [folder: folder.id])
        }
    }

    def saveContent () {
        ObjectSystemData osd = null
        try {
            User user = userService.user
            osd = (ObjectSystemData) inputValidationService.checkObject(ObjectSystemData.class, params.osd, true)
            if (!osd) {
                throw new RuntimeException('error.missing.object')
            }

            if (!repositoryService.mayBrowseFolder(user, osd.parent, session.repositoryName)) {
                throw new RuntimeException('error.access.denied')
            }
            if (!repositoryService.checkPermission(user, osd.acl, session.repositoryName, PermissionName.WRITE_OBJECT_CONTENT)) {
                throw new RuntimeException('error.access.denied')
            }

            Client client = repositoryService.getClient(user.name, session.repositoryName)
            MultipartFile file = request.getFile('file')

            if (file.isEmpty()) {
                throw new RuntimeException('error.missing.content')
            }
            else {
                File tempFile = File.createTempFile('illicium_upload_', null)
                file.transferTo(tempFile)
                Format format = (Format) inputValidationService.checkObject(Format.class, params.format, true)
                if (!format) {
                    throw new RuntimeException('error.missing.format')
                }
                client.lock(osd.id)
                client.setContent(tempFile, format.name, osd.id)
                client.unlock(osd.id)
            }
            // on success: redirect fetchFolderContent
            log.debug("set content on object #${osd.id}")
            return redirect(controller: 'folder', action: 'index', params: [folder: params.folder, osd: params.osd])
        }
        catch (RuntimeException e) {
            log.debug("Failed to set content on object ${osd?.id}: ", e)
            flash.message = message(code: e.getMessage())
            if (!osd) {
                return redirect(controller: 'folder', action: 'index')
            }
            return redirect(controller: 'osd', action: 'setContent', params: [folder: params.folder, osd: params.osd])
        }
    }

    def setContent () {
        try {
            ObjectSystemData osd = (ObjectSystemData) inputValidationService.checkObject(ObjectSystemData.class, params.osd, true)
            if (!osd) {
                throw new RuntimeException('error.missing.object')
            }
            if (repositoryService.checkPermission(userService.user, osd.acl, session.repositoryName, PermissionName.WRITE_OBJECT_CONTENT)) {
                return [
                        osd: osd,
                        folder: osd.parent
                ]
            }
            else {
                throw new RuntimeException('error.access.forbidden')
            }

        }
        catch (RuntimeException e) {
            flash.message = message(code: e.getMessage())
            return redirect(controller: 'folder', action: 'index', params: [folder: params.folder, osd: params.osd])
        }
    }

    def newVersion = {
        def repositoryName = session.repositoryName
        try {
            def user = userService.user
            ObjectSystemData osd = (ObjectSystemData) inputValidationService.checkObject(ObjectSystemData.class, params.osd)
            if (!osd) {
                throw new RuntimeException('error.missing.object')
            }
            if (!repositoryService.mayBrowseFolder(user, osd.parent, repositoryName)) {
                throw new RuntimeException('error.access.denied')
            }
            Client client = repositoryService.getClient(user.name, repositoryName)
            def newOsd = client.version(osd.id)

            def osdList = repositoryService.getObjects(user, osd.parent, repositoryName, params.versions)

            return render(template: '/folder/folderContent', model: [folder: osd.parent,
                    osdList: osdList,
                    folders: repositoryService.getFolders(user, osd.parent, repositoryName),
                    permissions: loadUserPermissions(osd.parent.acl),
                    superuserStatus: userService.isSuperuser(user),
                    triggerOsd: newOsd,
                    selectedVersion: params.versions,
                    versions: [all: 'folder.version.all', head: 'folder.version.head', branch: 'folder.version.branch']
            ])
        }
        catch (RuntimeException e) {
            log.debug("Failed to version object:", e)
            return renderException(e)
        }
    }

    def iterate () {
        def msgMap
        def msgList = []
        try {

            def idList = params.list("osd")
            def folderList = params.list("folder")
            if (idList.isEmpty() && folderList.isEmpty()) {
                // nothing to do
                return redirect(controller: 'folder', action: 'index')
            }

            if (params.delete) {
                Client client = repositoryService.getClient(userService.user.name, session.repositoryName)
                msgMap = osdService.delete(client, idList)
                msgList.addAll(convertMsgMap(msgMap))
                msgMap = folderService.delete(client, folderList)
                msgList.addAll(convertMsgMap(msgMap))
            }
            else if (params.deleteAll) {
                Client client = repositoryService.getClient(userService.user.name, session.repositoryName)
                msgMap = osdService.deleteAllVersions(client, idList)
                log.debug("msgMap in deleteAll: $msgMap")
                msgList.addAll(convertMsgMap(msgMap))
                msgMap = folderService.delete(client, folderList)
                msgList.addAll(convertMsgMap(msgMap))
            }
        }
        catch (Exception e) {
            log.debug("Failed to iterate over ${params.osd}.", e)
            flash.message = message(code: 'iterate.fail', args: [message(code: filterErrorMessage(e.message))])
        }

        flash.msgList = msgList
        return redirect(controller: 'folder', action: 'index')
    }

    protected List convertMsgMap(msgMap) {
        def msgList = []
        msgMap.each {String k, List v ->
            if (v.size() == 1) {
                msgList.add(message(code: v.get(0), args: [k]))
            }
            else {
                msgList.add(message(code: v.get(0), args: [k, message(code: filterErrorMessage(v.get(1)))]))
            }
        }
        return msgList
    }
}

package net.sf.cinnamon.illicium

import server.Folder

import eu.hornerproject.humulus.EnvironmentHolder

import eu.hornerproject.humulus.IgnorableException

import server.RelationType
import server.data.ObjectSystemData
import server.User
import safran.Client
import server.data.Validator
import utils.HibernateSession
import utils.PersistenceSessionProvider
import utils.DefaultPersistenceSessionProvider
import javax.persistence.EntityManager
import server.global.Conf
import server.global.ConfThreadLocal

/**
 *
 */
//@Secured(["hasRole('_superusers')"])
@grails.plugins.springsecurity.Secured(["isAuthenticated()"])
class FolderController extends BaseController {

    def index() {
        try {
            Folder rootFolder = folderService.findRootFolder()
            if (!rootFolder) {
                def logoutMessage = message(code: "error.no.rootFolder")
                return redirect(controller: 'logout', action: 'info', params: [logoutMessage: logoutMessage])
            }
            Collection childFolders = fetchChildFolders(rootFolder)
            Map grandChildren = [:]
            Set<Folder> contentSet = new HashSet<Folder>()
            childFolders.each { child ->
                Collection<Folder> gc = fetchChildFolders(child)
                grandChildren.put(child, gc)

                if (folderService.hasContent(child)) {
                    contentSet.add(child)
                }

                def grandChildrenWithContent = fetchChildrenWithContent(child)
                contentSet.addAll(grandChildrenWithContent)
            }
            def triggerSet = folderService.createTriggerSet(params.folder, params.osd)
            session.triggerFolder = params.folder
            session.triggerOsd = params.osd

            return [rootFolder: rootFolder,
                    contentSet: contentSet,
                    grandChildren: grandChildren,
                    children: childFolders,
                    triggerSet: triggerSet,
                    triggerFolder: params.folder,
                    envId: EnvironmentHolder.getEnvironment()?.get('id'),
                    msgList: flash.msgList
            ]

        }
        catch (Exception e) {
            log.debug("failed to show index:",e)
            def logoutMessage = message(code: 'error.loading.folders', args: [e.getMessage()])
            return redirect(controller: 'logout', action: 'info', params: [logoutMessage: logoutMessage])
        }


    }

    // not in folderService because it needs access to session.
    protected Collection<Folder> fetchChildFolders(Folder folder) {
        Collection<Folder> folderList = Folder.findAll("from Folder as f where f.parent=:parent and f.id != :id",
                [parent: folder, id: folder.id])

        Validator validator = fetchValidator()
        return validator.filterUnbrowsableFolders(folderList)
    }

    protected Validator fetchValidator(){
        def env = EnvironmentHolder.getEnvironment()
        PersistenceSessionProvider psp = new DefaultPersistenceSessionProvider(
                session.repositoryName, env.persistenceUnit, env.dbConnectionUrl
        )
        EntityManager em = HibernateSession.getRepositoryEntityManager(psp)
        HibernateSession.setLocalEntityManager(em)
        User user = userService.getUser()
        Validator validator = new Validator(user, em)
    }

    // not in folderService because it needs access to session.
    protected Collection<Folder> fetchChildrenWithContent(Folder folder) {
        Collection<Folder> folderList =
        Folder.findAll("from Folder as f where f.parent=:parent and f in (select p.parent from ObjectSystemData as p where p.parent.parent=:parent2)",
                [parent: folder, parent2: folder])
        Validator validator = fetchValidator()
        return validator.filterUnbrowsableFolders(folderList)
    }

    def fetchFolder(){
        Folder folder = Folder.get(params.folder)
        if (!folder) {
            return render(status: 503, text: message(code: 'error.folder.not.found'))
        }
        server.User user = userService.user
        if (!repositoryService.mayBrowseFolder(user, folder, session.repositoryName)) {
            return render(status: 401, text: message(code: 'error.access.denied'))
        }

        def childFolders = fetchChildFolders(folder)
        def grandChildren = [:]

        def childrenWithContent = fetchChildrenWithContent(folder)
        Set<Folder> contentSet = new HashSet<Folder>()
        contentSet.addAll(childrenWithContent)

        childFolders.each {child ->
            def gc = fetchChildFolders(child)
            if (gc.isEmpty()) {
                log.debug("${child.name} has no subfolders.")
            }
            else {
                log.debug("${child.name} has subfolders.")
            }
            grandChildren.put(child, gc)

            def grandChildrenWithContent = fetchChildrenWithContent(child)
            contentSet.addAll(grandChildrenWithContent)

        }

        def triggerSet = null
        if (session.triggerFolder) {
            triggerSet = folderService.createTriggerSet(session.triggerFolder, session.triggerOsd)
        }

        return render(
                template: "/folder/subFolders",
                model: [folder: folder,
                        children: childFolders,
                        grandChildren: grandChildren,
                        contentSet: contentSet,
                        triggerSet: triggerSet,
                        triggerFolder: session.triggerFolder,
                ])
    }

    def fetchFolderContent () {
        def repositoryName = session.repositoryName
        Folder folder
        try {
            if (params.folder) {
                folder = Folder.get(params.folder) // called by OSD
            }
            else {
                folder = Folder.get(params.id) // called by remoteFunction
            }
            if (!folder) {
                throw new RuntimeException('error.folder.not.found')
            }
            server.User user = userService.user
            if (!repositoryService.mayBrowseFolder(user, folder, repositoryName)) {
                return render(status: 401, text: message(code: 'error.access.denied'))
            }

            log.debug("found folder. ${params.folder}: $folder")
            if (!params.versions?.trim()?.matches('^all|head|branch$')) {
                // log.debug("params.versions: ${params.versions}")
                params.versions = 'head'
            }
            log.debug("fetch OSDs from Cinnamon server")
            def osdList = repositoryService.getObjects(user, folder, repositoryName, params.versions)
            /*
            * if this folder contains the triggerOsd, we add it to the osdList even if it
            * is not of the default version (all/head/branch).
            */
            def triggerOsd = session.triggerOsd
            if (triggerOsd && folder.id.toString().equals(session.triggerFolder)) {
                def id = Long.parseLong(triggerOsd)
                if (!osdList.find {it.id.equals(id)}) {
                    osdList.add(ObjectSystemData.get(triggerOsd))
                    session.triggerOsd = null
                    session.triggerFolder = null
                }
            }

            Set<String> permissions
            try {
                permissions = loadUserPermissions(folder.acl)
            } catch (RuntimeException ex) {
                log.debug("getUserPermissions failed", ex)
                throw new RuntimeException('error.access.failed')
            }

            log.debug("superuserStatus: ${userService.isSuperuser(user)}")

            return render(template: "/folder/folderContent", model: [folder: folder,
                    osdList: osdList,
                    permissions: permissions,
                    folders: repositoryService.getFolders(user, folder, repositoryName),
                    superuserStatus: userService.isSuperuser(user),
                    selectedVersion: params.versions,
                    versions: [all: 'folder.version.all', head: 'folder.version.head', branch: 'folder.version.branch']
            ])
        }
        catch (Exception e) {
            return render(status: 500, text: message(code: filterErrorMessage(e.message)))
        }
    }

    def copyFolder (){
        try {
            Folder folder = loadFolder(params.folder)
            Folder target = loadFolder(params.target)
            verifyFolderViewable(folder)
            /*
            check:
            folder exists
            folder visible
            target folder exists
            target folder visible
            createPermission in target folder
            todo:
            recursively copy (should be a CinnamonServer function)
            CS should return a) list of problems (like unreadable objects / folders).
            copyFolder should render the new target folder.

             */
            params.folder = params.target
            fetchFolderContent()
        }
        catch (IgnorableException ie) {
            log.debug("ignorable Exception")
        }
    }

    def fetchFolderMeta () {
        Folder folder = Folder.get(params.folder)
        if (!folder) {
            return render(status: 503, text: message(code: 'error.folder.not.found'))
        }
        User user = userService.user
        if (!repositoryService.mayBrowseFolder(user, folder, session.repositoryName)) {
            return render(status: 401, text: message(code: 'error.access.denied'))
        }

        Set<String> permissions
        try {
            permissions = loadUserPermissions(folder.acl)
        } catch (RuntimeException ex) {
            log.debug("getUserPermissions failed", ex)
            return render(status: 503, text: message(code: 'error.access.failed'))
        }

        return render(template: '/folder/folderMeta', model: [folder: folder, permissions: permissions])
    }

    def editName () {
        try {
            Folder folder = loadFolder(params.folder)
            verifyFolderViewable(folder)
            // Note: this does not check if the user may actually change the name -
            // this happens both before (in the GSP) and after (in the CinnamonServer).
            render(template: 'editName', model: [folder: folder])
        }
        catch (IgnorableException ie) {
            log.debug("ignorableException.")
        }
    }

    // AJAX
    def fetchRelationTypeDialog () {
        RelationType rt = RelationType.get(params.relationType)
        return render(template: 'fetchRelationTypeDialog', model: [relationType: rt])
    }

    def editOwner () {
        try {
            Folder folder = loadFolder(params.folder)
            verifyFolderViewable(folder)
            // Note: this does not check if the user may actually change the owner -
            // this happens both before (in the GSP) and after (in the CinnamonServer).
            render(template: 'editOwner', model: [folder: folder])
        }
        catch (IgnorableException ie) {
            log.debug("ignorableException.")
        }
    }

    protected void verifyFolderViewable(Folder folder) {
        if (!repositoryService.mayBrowseFolder(userService.user,
                folder, session.repositoryName)) {
            render(status: 401, text: message(code: 'error.access.denied'))
            throw new IgnorableException()
        }
    }

    protected Folder loadFolder(String id) {
        Folder folder = Folder.get(id)
        if (!folder) {
            render(status: 503, text: message(code: 'error.folder.not.found'))
            throw new IgnorableException("ignore me")
        }
        return folder
    }

    /**
     * Render the folderMeta-template to an AJAX-client.
     * @param nameChanged set to true if the name of the folder has changed and because of
     * that the folder-tree should be updated to reflect this change.
     */
    protected void renderFolderMeta(Boolean nameChanged) {
        Folder folder = loadFolder(params.folder)
        def permissions = loadUserPermissions(folder.acl)
        render(template: 'folderMeta',
                model: [folder: folder, permissions: permissions, nameChanged: nameChanged])
    }

    def editAcl () {
        try {
            Folder folder = loadFolder(params.folder)
            verifyFolderViewable(folder)
            render(template: 'editAcl', model: [folder: folder])
        }
        catch (IgnorableException ie) {
            log.debug("ignorableException.")
        }
    }

    def editType () {
        try {
            Folder folder = loadFolder(params.folder)
            verifyFolderViewable(folder)
            render(template: 'editType', model: [folder: folder])
        }
        catch (IgnorableException ie) {
            log.debug("ignorableException.")
        }
    }

    def saveField () {
        try {
            if (fieldNameAllowed(params.fieldName)) {
                tryUpdate({ userName ->
                    repositoryService.updateFolder(userName, params.folder, params.fieldName, params.fieldValue, session.repositoryName)
                })
                renderFolderMeta(false)
            }
            else {
                render(status: 401, text: message(code: 'error.illegal.parameter', args: [params.fieldName?.encodeAsHTML()]))
            }
        } catch (IgnorableException ex) {
            log.debug("ingore IngorableException.")
        }
    }

    static List<String> allowedFields = ['name', 'acl_id', 'typeid', 'owner']

    protected Boolean fieldNameAllowed(String name) {
        return allowedFields.contains(name)
    }

    def searchSimple () {
        def query = params.query
        try {
            User user = userService.user
            Client client = repositoryService.getClient(user.name, session.repositoryName)
            String result = client.searchSimple(query, 10, 0)
            def xml = new XmlSlurper().parseText(result)
            def folders = xml.item.findAll {it.folder?.id?.text()?.length() > 0}.collect {Folder.get(it.folder.id.text())}
            log.debug("folders: ")
            folders.each {
                log.debug(it.name)
            }
            def objects = xml.item.findAll {it.object?.id?.text()?.length() > 0}.collect {ObjectSystemData.get(it.object.id.text())}
            log.debug("objects:")
            objects.each {
                log.debug(it.name)
            }
            render(template: 'searchResult', model: [searchResult: result, folders: folders, objects: objects])
        }
        catch (Exception e) {
            render(status: 503, text: message(code: e.message))
        }
    }

    def create () {
        try {
            def parentFolder = inputValidationService.checkObject(Folder.class, params.parent)
            if (!parentFolder) {
                throw new RuntimeException('error.folder.not.found')
            }
            return render(template: 'create', model: [parent: parentFolder])

        }
        catch (Exception e) {
            flash.message = message(code: 'error.create.folder', args: [message(code: e.message)])
            return render(status: 500, text: message(code: e.message))
        }
    }

    def save () {
        def parentFolder = null
        try {
            def folderName = params.name?.encodeAsHTML()
            parentFolder = inputValidationService.checkObject(Folder.class, params.parent, true)
            if (!parentFolder) {
                flash.message = message(code: 'error.folder.not.found')
                return redirect(controller: 'folder', action: 'index')
            }
            def user = userService.user
            Client client = repositoryService.getClient(user.name, session.repositoryName)
            def id = client.createFolder(folderName, parentFolder.id)
            def folder = Folder.get(id)
            return redirect(controller: 'folder', action: 'index', params: [folder: folder.id])
        }
        catch (Exception e) {
            flash.message = message(code: filterErrorMessage(e.message))
            return redirect(controller: 'folder', action: 'index', params: [folder: parentFolder?.id])
        }
    }

    def delete () {
        try {
            def folder = (Folder) inputValidationService.checkObject(Folder.class, params.folder, true)
            if (!folder) {
                throw new RuntimeException('error.folder.not.found')
            }
            Folder parent = folder.parent
            def name = folder.name
            def user = userService.user
            Client client = repositoryService.getClient(user.name, session.repositoryName)
            def result = client.deleteFolder(folder.id)
            log.debug("delete folder returned: $result")
            flash.message = message(code: 'folder.delete.success', args: [name])
            return redirect(controller: 'folder', action: 'index', params: [folder: parent?.id])
        }
        catch (Exception e) {
            def exceptionMessage = filterErrorMessage(e.message)
            log.debug("Error message: ${e.message}")
            flash.message = message(code: 'error.delete.folder', args: [message(code: exceptionMessage)])
            return redirect(controller: 'folder', action: 'index', params: params)
        }
    }

    def zipFolder(){
        Folder folder = (Folder) inputValidationService.checkObject(Folder.class, params.folder, true)
        if (!folder) {
            log.debug("zipFolder: folder not found")
            flash.message = message(code: 'error.folder.not.found')
            return redirect(controller: 'folder', action: 'index', params: params)
        }
        def user = userService.user
        Client client = repositoryService.getClient(user.name, session.repositoryName)
        File data = client.zipFolder(folder.id)
        log.debug("zipFolder called for #${folder.id} @ ${data.absolutePath}")
        if (!data.exists()) {
            log.debug("could not find: ${data.absolutePath}")
            flash.message = message(code: 'error.file.not.found')
            return redirect(controller: 'folder', action: 'index', params: params)
        }
        response.setHeader("Content-disposition", "attachment; filename=${folder.name.encodeAsURL()}.zip");
        response.setContentType("application/zip")
        response.outputStream << data.newInputStream()
        response.outputStream.flush()
        return
    }

    def editMetadata () {
        Folder folder = Folder.get(params.folder)
        // TODO: filter according to ACL
        if (!folder) {
            return render(status: 503, text: message(code: 'error.folder.not.found'))
        }
        return render(template: '/folder/editMetadata', model: [folder: folder])
    }


    def saveMetadata () {
        Folder folder= Folder.get(params.folder)
        // TODO: filter according to ACL
        if (!folder) {
            return render(status: 503, text: message(code: 'error.folder.not.found'))
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
            if (!folder.metadata.equals(metadata)) {
                oldMetadata = folder.metadata
                User user = userService.user
                repositoryService.updateFolder(user.name, params.folder, "metadata", metadata, repositoryName)
                folder.refresh() // expected: folder was changed by server, so we need to reload it.
            }
        }
        catch (Exception ex) {
            log.debug("failed to update folder metadata: ", ex)
            errorMsg = message(code: 'error.save.metadata', args: [message(code: ex.message)])
        }
        finally {
            if (errorMsg) {
                // reverse any changes:d
                folder.metadata = oldMetadata
            }
        }

        if (errorMsg) {
            log.debug("error: $errorMsg")
            return render(status: 503, text: errorMsg)
        }
        else {
            folder.refresh() // reload changed folder from DB
            return render(template: 'folderMeta', model: [folder: folder, permissions: loadUserPermissions(folder.acl)])
        }
    }

    def renderMetadata () {
        //TODO: filter according to ACL
        Folder folder = Folder.get(params.folder)
        if (!folder) {
            return render(status: 503, text: message(code: 'error.osd.not.found'))
        }

        return render(template: 'renderMetadata', model: [folder: folder])
    }
}

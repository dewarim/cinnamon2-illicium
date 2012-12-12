// locations to search for config files that get merged into the main config
// config files can either be Java properties files or ConfigSlurper scripts
 grails.config.locations = [
                             "classpath:${appName}-config.properties",
                             "classpath:${appName}-config.groovy",
                             "file:/home/cinnamon/${appName}-config.groovy",
                             "file:${System.env.CINNAMON_HOME_DIR}/${appName}-config.groovy"
                             ]

 if(System.properties["${appName}.config.location"]) {
    grails.config.locations << "file:" + System.properties["${appName}.config.location"]
 }

grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.types = [ html: ['text/html','application/xhtml+xml'],
                      xml: ['text/xml', 'application/xml'],
                      text: 'text-plain',
                      js: 'text/javascript',
                      rss: 'application/rss+xml',
                      atom: 'application/atom+xml',
                      css: 'text/css',
                      csv: 'text/csv',
                      all: '*/*',
                      json: ['application/json','text/json'],
                      form: 'application/x-www-form-urlencoded',
                      multipartForm: 'multipart/form-data'
                    ]
// The default codec used to encode data with ${}
grails.views.default.codec="none" // none, html, base64
grails.views.gsp.encoding="UTF-8"
grails.converters.encoding="UTF-8"

// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true

// set per-environment serverURL stem for creating absolute links
environments {
    production {
//        grails.serverURL = "http://www.changeme.com"
    }
}

// log4j configuration
log4j = {
// note: this class is resolved during construction of the dandelion.war, not at run time.
// as such, it's rather useless to apply environmental vars... currently this is solved through
// a call to org.apache.log4j.PropertyConfigurator in Bootstrap.groovy


    appenders {
        'null' name:'stacktrace'
        rollingFile name:'file',
                maxFileSize:'10MB',
                file:"${System.env.CINNAMON_HOME_DIR}/global/log/illicium.log",
                layout: pattern(conversionPattern: "%d{ISO8601} %c %m%n")
    }

    error 'org.hibernate',
          'org.codehaus.groovy.grails.plugins.DefaultGrailsPlugin'
    error 'org.springframework.security',
          'org.codehaus.groovy.grails',
          'category.org.apache.commons.digester',
    	  'category.org.apache.commons.modeler'

    //debug 'grails',
    debug 'eu.hornerproject.humulus.CinnamonPasswordEncoder'
    debug 'eu.hornerproject.humulus'
    debug 'grails.app'
    debug 'grails.app.services'
    debug 'grails.app.controller.net.sf.cinnamon.illicium.FolderController'
    debug 'net.sf.cinnamon.illicium.FolderController'
    debug 'utils.security.HashMaker'
    debug 'server.Client'
    debug 'server.data'
    debug 'server'

    root{
        warn 'file', additivity = false
    }
}

// Added by the Spring Security Core plugin:
grails.plugins.springsecurity.userLookup.userDomainClassName = 'server.User'
grails.plugins.springsecurity.userLookup.authorityJoinClassName = 'server.GroupUser'
grails.plugins.springsecurity.authority.className = 'server.Group'
grails.plugins.springsecurity.authority.nameField = 'name'
grails.plugins.springsecurity.userLookup.usernamePropertyName='name'
grails.plugins.springsecurity.userLookup.passwordPropertyName='pwd'
grails.plugins.springsecurity.userLookup.enabledPropertyName='activated'
grails.plugins.springsecurity.userLookup.authoritiesPropertyName='groupUsers'
grails.plugins.springsecurity.successHandler.defaultTargetUrl='/folder/index'
grails.plugins.springsecurity.auth.loginFormUrl='/login/auth'
// grails.plugins.springsecurity.failureHandler.defaultFailureUrl='/login/index'

grails.logging.jul.usebridge = false
/*
 The default page is responsible for connecting to the right database,
 so we always redirect the user there:
  */
grails.plugins.springsecurity.successHandler.alwaysUseDefault=true
grails.plugins.springsecurity.http.useExpressions=false

grails.views.javascript.library="jquery"
grails.json.legacy.builder=false
grails.servlet.version = "3.0" // Change depending on target container compliance (2.5 or 3.0)

grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
//grails.project.war.file = "target/${appName}-${appVersion}.war"
grails.project.target.level = 1.6
grails.project.source.level = 1.6
grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // uncomment to disable ehcache
        // excludes 'ehcache'
        //excludes 'logback-classic'

        excludes "grails-plugin-logging"

    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    repositories {
        grailsPlugins()
        grailsHome()
        grailsCentral()
        mavenRepo "http://mirrors.ibiblio.org/pub/mirrors/maven2/"

        // uncomment the below to enable remote dependency resolution
        // from public Maven repositories
        //mavenLocal()
        //mavenCentral()
        //mavenRepo "http://snapshots.repository.codehaus.org"
        //mavenRepo "http://repository.codehaus.org"
        //mavenRepo "http://download.java.net/maven/2/"
        //mavenRepo "http://repository.jboss.com/maven2/"
    }
    dependencies {
        runtime([group:'org.hibernate', name:'hibernate-c3p0', version:'3.6.8.Final'])
        //runtime('eu.hornerproject:humulus:0.5.21')
    }

    plugins{
        runtime ":hibernate:$grailsVersion"
        build "org.grails.plugins:tomcat:$grailsVersion"
        compile(':webxml:1.4.1')
        compile(':jquery:1.8.0')
        compile(':jquery-ui:1.8.15')
        compile(':resources:1.1.6')
        compile(':spring-security-core:1.2.7.3')
        compile('cinnamon2:humulus:0.7.0')
    }
}

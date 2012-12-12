class BootStrap {

    def grailsApplication

    def init = { servletContext ->

        if (!grailsApplication.config.configLoaded) {
            log.warn("merge config file by hand")
            def configFile = new File("${System.env.CINNAMON_HOME_DIR}/illicium-config.groovy")
            def configScript = new ConfigSlurper().parse(configFile.text)
            grailsApplication.config.merge(configScript)
        }

    }

    def destroy = {

    }

}

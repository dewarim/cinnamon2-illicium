dataSource {
    pooled = true
    driverClassName = "org.h2.jdbcDriver"
    username = "sa"
    password = ""
}
hibernate {
    cache.use_second_level_cache = true
    cache.use_query_cache = true
    cache.provider_class = 'net.sf.ehcache.hibernate.EhCacheProvider'
}
// environment specific settings
environments {
    development {
        dataSource {
//            dbCreate = "create-drop" // one of 'create', 'create-drop','update'
        }
    }
    test {
        dataSource {
//            dbCreate = "create"
        }
    }
    production {
        dataSource {
//            dbCreate = "update"
        }
    }
}

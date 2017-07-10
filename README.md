# omar-wms

[![Build Status](https://jenkins.radiantbluecloud.com/buildStatus/icon?job=omar-wms-dev)]()

### Required environment variable
- OMAR_COMMON_PROPERTIES

### Optional environment variables
Only required for Jenkins pipelines or if you are running Artifactory and/or Openshift locally

- OPENSHIFT_USERNAME
- OPENSHIFT_PASSWORD
- ARTIFACTORY_USER
- ARTIFACTORY_PASSWORD

## About
The omar-wms respository is OMAR's implementation of Web Mapping Service (WMS) based of the Open Geospatial Consortium (OGC) standard. Its purpose is to process request for georeferenced map images from OMAR's map server. It has two request types defined by the standard, *GetCapabilities* and *GetMap*.

## How to Install omar-wms-app locally

1. Install omar-oms-plugin [click this link for instructions](https://github.com/ossimlabs/omar-oms)

2. Install omar-geoscript-plugin [click this link for instructions](https://github.com/ossimlabs/omar-geoscript.git)

3. Git clone the following repos or git pull the latest versions if you already have them.
```
  git clone https://github.com/ossimlabs/omar-hibernate-spatial.git
  git clone https://github.com/ossimlabs/omar-wms.git
```

4. Install omar-hibernate-spatial-plugin
```
 cd omar-wms/plugins/omar-hibernate-spatial-plugin
 ./gradlew clean install
```

5. Install omar-wms-plugin
```
 cd omar-wms/plugin/omar-wms-plugin
 ./gradlew clean install
```

6. Build/Install omar-wms-app
#### Build:
```
 cd omar-wms/apps/omar-wms-app
 ./gradlew clean build
 ```
#### Install:
```
 cd omar-wms/apps/omar-wms-app
 ./gradlew clean install
```

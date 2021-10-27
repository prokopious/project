module com.udacity.catpoint.securityService {
    requires com.udacity.catpoint.imageService;
    requires miglayout;
    requires java.desktop;
    requires java.prefs;
    requires com.google.common;
    requires com.google.gson;
    requires java.sql;
    opens com.udacity.catpoint.security.data to com.google.gson;
    exports com.udacity.catpoint.security.service;
    exports com.udacity.catpoint.security.application;
}
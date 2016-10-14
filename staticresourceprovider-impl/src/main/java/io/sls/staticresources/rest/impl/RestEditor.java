package io.sls.staticresources.rest.impl;

import io.sls.runtime.ThreadContext;
import io.sls.staticresources.IResourceDirectory;
import io.sls.staticresources.IResourceFilesManager;
import io.sls.staticresources.rest.IRestEditor;
import io.sls.utilities.FileUtilities;
import io.sls.utilities.HtmlUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.InternalServerErrorException;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * User: jarisch
 * Date: 08.08.12
 * Time: 12:37
 */

public class RestEditor implements IRestEditor {
    private static final String USER_DISPLAY_NAME = "USER_DISPLAY_NAME";
    private static final String LANGUAGE_IDENTIFIER = "LANGUAGE_FILE";
    private static final String LOGOUT_URL = "LOGOUT_URL";
    private final IResourceFilesManager resourceFilesManager;
    private final String uriEngineServerUI;
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    public RestEditor(IResourceFilesManager resourceFilesManager,
                      @Named("system.uriEngineServerUI") String uriEngineServerUI) {
        this.resourceFilesManager = resourceFilesManager;
        this.uriEngineServerUI = uriEngineServerUI;
    }

    @Override
    public String getEditor(String path, String language, String location) {
        try {
            if (resourceFilesManager.getOptions().alwaysReloadResourcesFile()) {
                resourceFilesManager.reloadResourceFiles();
            }

            IResourceDirectory resourceDirectory = resourceFilesManager.getResourceDirectories().get(0);
            String htmlFile = resourceDirectory.getWebHtmlFile();
            final StringBuilder editorFile = new StringBuilder(FileUtilities.readTextFromFile(new File(htmlFile)));
            includeLanguageFile(editorFile, resourceDirectory.getRootWebDir(), resourceDirectory.getWebI18nDir(), language, location);
            includeUserDisplayName(editorFile);
            includeRestApiHostScriptTag(editorFile);
            includeEngineServerURI(editorFile);
            return editorFile.toString();
        } catch (IOException e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new InternalServerErrorException(e.getLocalizedMessage(), e);
        }
    }

    private void includeLanguageFile(StringBuilder editorFile, String rootDir, String languageDir, String language, String location) {
        StringBuilder scriptTag = new StringBuilder();
        List<IResourceDirectory> resourceDirectories = resourceFilesManager.getResourceDirectories();
        if (resourceDirectories != null && resourceDirectories.size() > 0) {
            String pathOfLanguageFile = resourceDirectories.get(0).getPathOfLanguageFile(language, location);
            resourceFilesManager.includeJavascriptFile(scriptTag, languageDir.substring(rootDir.length()) + pathOfLanguageFile);
            int startIndex = editorFile.indexOf(LANGUAGE_IDENTIFIER);
            editorFile.replace(startIndex, startIndex + LANGUAGE_IDENTIFIER.length(), scriptTag.toString());
        } else {
            logger.error("No ResourceDirectory has been found while trying to include language file.");
        }
    }

    private void includeUserDisplayName(StringBuilder editorFile) {
        int startIndex = editorFile.indexOf(USER_DISPLAY_NAME);
        String currentUserDisplayName = (String) ThreadContext.get("currentuser:displayname");
        if (currentUserDisplayName != null) {
            editorFile.replace(startIndex, startIndex + USER_DISPLAY_NAME.length(), currentUserDisplayName);
        }
    }

    private void includeEngineServerURI(StringBuilder editorFile) {
        StringBuilder javascriptInjection = new StringBuilder();
        javascriptInjection.append("var uiEngineServer='").append(uriEngineServerUI).append("';");
        String javascriptStatement = HtmlUtilities.wrapInJavascriptStatement(javascriptInjection.toString());
        editorFile.insert(editorFile.indexOf("</head>"), javascriptStatement);
    }

    private void includeLogoutUrl(StringBuilder editorFile, String currentPath) {
        int startIndex = editorFile.indexOf(LOGOUT_URL);
        if (startIndex != -1) {
            editorFile.replace(startIndex, startIndex + LOGOUT_URL.length(), getLogoutUrl(currentPath));
        }
    }

    private String getLogoutUrl(String currentPath) {
        StringBuilder logoutUrl = new StringBuilder();
        Integer currentUrlPort = getCurrentUrlPort();
        logoutUrl.append(getCurrentUrlProtocol()).append("://").append("logout:true@").append(getCurrentUrlHost());
        if (currentUrlPort != -1 && currentUrlPort != 80 && currentUrlPort != 443) {
            logoutUrl.append(":").append(currentUrlPort);
        }
        logoutUrl.append(currentPath);
        return logoutUrl.toString();
    }

    private void includeRestApiHostScriptTag(StringBuilder editorFile) {
        StringBuilder javascriptInjection = new StringBuilder();
        javascriptInjection.append(" REST.apiURL = '");
        javascriptInjection.append(getCurrentUrlProtocol()).append("://").append(getCurrentUrlHost()).append(":").append(getCurrentUrlPort()).append("';");
        String javascriptStatement = HtmlUtilities.wrapInJavascriptStatement(javascriptInjection.toString());
        editorFile.insert(editorFile.indexOf("</head>"), javascriptStatement);
    }

    private String getCurrentUrlProtocol() {
        return (String) ThreadContext.get("currentURLProtocol");
    }

    private String getCurrentUrlHost() {
        return (String) ThreadContext.get("currentURLHost");
    }

    private Integer getCurrentUrlPort() {
        return (Integer) ThreadContext.get("currentURLPort");
    }
}
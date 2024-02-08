package com.github.barteksc.pdfviewer.annotation.core.pdf;

public class DocumentationOpenerImpl implements DocumentationOpener {
    @Override
    public void openDocumentation(String documentationID) {
        System.out.println("Opening documentation with ID: " + documentationID);
    }
}

/**
 * Copyright (C) 2000 - 2009 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection with Free/Libre
 * Open Source Software ("FLOSS") applications as described in Silverpeas's
 * FLOSS exception.  You should have received a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * "http://repository.silverpeas.com/legal/licensing"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.stratelia.webactiv.servlets;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.rmi.RemoteException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.silverpeas.util.StringUtil;
import com.silverpeas.util.web.servlet.RestRequest;
import com.stratelia.silverpeas.silvertrace.SilverTrace;
import com.stratelia.silverpeas.versioning.model.DocumentVersion;
import com.stratelia.silverpeas.versioning.model.DocumentVersionPK;
import com.stratelia.silverpeas.versioning.util.VersioningUtil;
import com.stratelia.webactiv.util.FileRepositoryManager;
import com.stratelia.webactiv.util.ResourceLocator;
import com.stratelia.webactiv.util.attachment.control.AttachmentController;
import com.stratelia.webactiv.util.attachment.ejb.AttachmentPK;
import com.stratelia.webactiv.util.attachment.model.AttachmentDetail;

/**
 * Class declaration
 * @author
 */
public class RestOnlineFileServer extends HttpServlet {

  HttpSession session;
  PrintWriter out;

  public void init(ServletConfig config) {
    try {
      super.init(config);
    } catch (ServletException se) {
      SilverTrace.fatal("peasUtil", "FileServer.init",
          "peasUtil.CANNOT_ACCESS_SUPERCLASS");
    }
  }

  public void service(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    RestRequest restRequest = new RestRequest(req, "");
    SilverTrace.info("peasUtil", "OnlineFileServer.doPost",
        "root.MSG_GEN_ENTER_METHOD");
    OnlineFile file = getWantedFile(restRequest);
    if (file != null) {
      display(res, file);
      return;
    }
    displayWarningHtmlCode(res);
  }

  protected OnlineFile getWantedFile(RestRequest restRequest)
      throws RemoteException {
    OnlineFile file = getWantedAttachment(restRequest);
    if (file == null) {
      file = getWantedVersionnedDocument(restRequest);
    }
    return file;
  }

  protected OnlineFile getWantedAttachment(RestRequest restRequest) {
    String componentId = restRequest.getElementValue("componentId");
    OnlineFile file = null;
    String attachmentId = restRequest.getElementValue("attachmentId");
    String language = restRequest.getElementValue("lang");
    if (StringUtil.isDefined(attachmentId)) {
      AttachmentDetail attachment = AttachmentController
          .searchAttachmentByPK(new AttachmentPK(attachmentId));
      if (attachment != null) {
        file = new OnlineFile(attachment.getType(language), attachment
            .getPhysicalName(language), FileRepositoryManager
            .getRelativePath(FileRepositoryManager
            .getAttachmentContext(attachment.getContext())));
        file.setComponentId(componentId);
      }
    }
    return file;
  }

  protected OnlineFile getWantedVersionnedDocument(RestRequest restRequest)
      throws RemoteException {
    String componentId = restRequest.getElementValue("componentId");
    OnlineFile file = null;
    String documentId = restRequest.getElementValue("documentId");
    if (StringUtil.isDefined(documentId)) {
      String versionId = restRequest.getElementValue("versionId");
      VersioningUtil versioning = new VersioningUtil();
      DocumentVersionPK versionPK = new DocumentVersionPK(Integer
          .parseInt(versionId), "useless", componentId);
      DocumentVersion version = versioning.getDocumentVersion(versionPK);
      if (version != null) {
        String[] path = new String[1];
        path[0] = "Versioning";
        file = new OnlineFile(version.getMimeType(), version.getPhysicalName(),
            FileRepositoryManager.getRelativePath(path));
        file.setComponentId(componentId);
      }
    }
    return file;
  }

  /**
   * This method writes the result of the preview action.
   * @param res - The HttpServletResponse where the html code is write
   * @param htmlFilePath - the canonical path of the html document generated by the parser tools. if
   * this String is null that an exception had been catched the html document generated is empty !!
   * also, we display a warning html page
   */
  private void display(HttpServletResponse res, OnlineFile file)
      throws IOException {
    String filePath = FileRepositoryManager.getAbsolutePath(file
        .getComponentId())
        + file.getDirectory() + File.separator + file.getSourceFile();

    File realFile = new File(filePath);
    if (!realFile.exists() && !realFile.isFile()) {
      displayWarningHtmlCode(res);
      return;
    }
    OutputStream out2 = res.getOutputStream();
    BufferedInputStream input = null; // for the html document generated
    SilverTrace.info("peasUtil", "OnlineFileServer.display()",
        "root.MSG_GEN_ENTER_METHOD", " htmlFilePath " + filePath);
    try {
      res.setContentType(file.getMimeType());
      input = new BufferedInputStream(new FileInputStream(realFile));
      byte[] buffer = new byte[8];
      int read = 0;
      SilverTrace.info("peasUtil", "OnlineFileServer.display()",
          "root.MSG_GEN_ENTER_METHOD", " BufferedInputStream read " + read);
      while ((read = input.read(buffer)) != -1) {
        out2.write(buffer, 0, read);
      }
    } catch (Exception e) {
      SilverTrace.warn("peasUtil", "OnlineFileServer.doPost",
          "root.EX_CANT_READ_FILE", "file name=" + filePath);
      displayWarningHtmlCode(res);
    } finally {
      SilverTrace.info("peasUtil", "OnlineFileServer.display()", "",
          " finally ");
      // we must close the in and out streams
      try {
        if (input != null) {
          input.close();
        }
        out2.close();
      } catch (Exception e) {
        SilverTrace.warn("peasUtil", "OnlineFileServer.display",
            "root.EX_CANT_READ_FILE", "close failed");
      }
    }
  }

  // Add By Mohammed Hguig

  private void displayWarningHtmlCode(HttpServletResponse res)
      throws IOException {
    StringReader sr = null;
    OutputStream out2 = res.getOutputStream();
    int read;
    ResourceLocator resourceLocator = new ResourceLocator(
        "com.stratelia.webactiv.util.peasUtil.multiLang.fileServerBundle", "");

    sr = new StringReader(resourceLocator.getString("warning"));
    try {
      read = sr.read();
      while (read != -1) {
        SilverTrace.info("peasUtil", "OnlineFileServer.displayHtmlCode()",
            "root.MSG_GEN_ENTER_METHOD", " StringReader read " + read);
        out2.write(read); // writes bytes into the response
        read = sr.read();
      }
    } catch (Exception e) {
      SilverTrace.warn("peasUtil", "OnlineFileServer.displayWarningHtmlCode",
          "root.EX_CANT_READ_FILE", "warning properties");
    } finally {
      try {
        if (sr != null)
          sr.close();
        out2.close();
      } catch (Exception e) {
        SilverTrace.warn("peasUtil", "OnlineFileServer.displayHtmlCode",
            "root.EX_CANT_READ_FILE", "close failed");
      }
    }
  }

}
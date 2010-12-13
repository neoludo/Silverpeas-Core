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

package com.stratelia.webactiv.beans.admin;

import java.util.Hashtable;
import java.util.Vector;

import com.stratelia.silverpeas.authentication.LoginPasswordAuthentication;
import com.stratelia.silverpeas.silvertrace.SilverTrace;
import com.stratelia.webactiv.organization.AdminPersistenceException;
import com.stratelia.webactiv.organization.DomainRow;
import com.stratelia.webactiv.organization.GroupRow;
import com.stratelia.webactiv.organization.KeyStoreRow;
import com.stratelia.webactiv.organization.OrganizationSchema;
import com.stratelia.webactiv.organization.OrganizationSchemaPool;
import com.stratelia.webactiv.organization.UserRow;
import com.stratelia.webactiv.util.exception.SilverpeasException;

public class DomainDriverManager extends AbstractDomainDriver {
  public OrganizationSchema organization = null;
  private Hashtable<String, AbstractDomainDriver> domainDriverInstances =
      new Hashtable<String, AbstractDomainDriver>(0);
  private DomainSynchroThread theThread = null;

  public DomainDriverManager() {
  }

  // when we are in a transaction the connection must not be released.
  private boolean inTransaction = false;
  private int nbConnected = 0;
  private Object semaphore = new Object();

  /**
   * Get an organization schema from the pool.
   */
  public void getOrganizationSchema() throws AdminException {
    synchronized (semaphore) {
      if (organization == null) {
        try {
          organization = OrganizationSchemaPool.getOrganizationSchema();
          nbConnected = 0;
        } catch (AdminPersistenceException e) {
          throw new AdminException("DomainDriverManager.getOrganizationSchema",
              SilverpeasException.FATAL, "admin.MSG_FATAL_GET_ORGANIZATION", e);
        }
      }
      nbConnected++;
      // SilverTrace.debug("admin", "----------------------->", "+++ ORG SCH",
      // "nbConnected = " + nbConnected);
    }
  }

  /**
   * Release the organization schema.
   */
  public void releaseOrganizationSchema() throws AdminException {
    synchronized (semaphore) {
      nbConnected--;
      // SilverTrace.debug("admin", "----------------------->", "--- ORG SCH",
      // "nbConnected = " + nbConnected);
      if (organization != null && !inTransaction && nbConnected <= 0) {
        OrganizationSchemaPool.releaseOrganizationSchema(organization);
        organization = null;
      }
    }
  }

  // -------------------------------------------------------------------------
  // Start Server actions
  // -------------------------------------------------------------------------

  public void startServer(Admin theAdmin, long nbSleepSec) throws Exception {
    Domain[] doms = getAllDomains();
    AbstractDomainDriver synchroDomain = null;

    theThread = new DomainSynchroThread(theAdmin, nbSleepSec);
    for (int i = 0; i < doms.length; i++) {
      try {
        synchroDomain = getDomainDriver(Integer.parseInt(doms[i].getId()));
      } catch (Exception e) {
        SilverTrace.warn("admin", "DomainDriverManager.startServer",
            "admin.CANT_GET_DOMAIN");
      }
      if (synchroDomain.isSynchroThreaded()) {
        theThread.addDomain(doms[i].getId());
      }
    }
    startSynchroThread();
  }

  public void startSynchroThread() throws Exception {
    if (theThread != null) {
      theThread.startTheThread();
    }
  }

  public void stopSynchroThread() throws Exception {
    if (theThread != null) {
      theThread.stopTheThread();
    }
  }

  /**
   * @param m_User
   * @return String
   */
  public String createUser(UserFull user) throws Exception {
    try {
      // Get a DomainDriver instance
      AbstractDomainDriver domainDriver = this.getDomainDriver(idAsInt(user
          .getDomainId()));

      // Create User in specific domain
      String sUserId = domainDriver.createUser(user);

      return sUserId;
    } catch (AdminException e) {
      throw new AdminException("DomainDriverManager.createUser",
          SilverpeasException.ERROR, "admin.EX_ERR_ADD_USER", user
          .getFirstName()
          + " " + user.getLastName(), e);
    }
  }

  public String createUser(UserDetail user) throws Exception {
    try {
      // Get a DomainDriver instance
      AbstractDomainDriver domainDriver = this.getDomainDriver(idAsInt(user
          .getDomainId()));

      // Create User in specific domain
      String sUserId = domainDriver.createUser(user);

      return sUserId;
    } catch (AdminException e) {
      throw new AdminException("DomainDriverManager.createUser",
          SilverpeasException.ERROR, "admin.EX_ERR_ADD_USER", user
          .getFirstName()
          + " " + user.getLastName(), e);
    }
  }

  /**
   * Delete given user from Silverpeas
   * @param userId user Id
   */
  public void deleteUser(String userId) throws Exception {
    try {
      // Set the OrganizationSchema (if not already done)
      this.getOrganizationSchema();

      // Get the user information
      UserRow ur = organization.user.getUser(idAsInt(userId));
      if (ur == null)
        throw new AdminException("DomainDriverManager.deleteUser",
            SilverpeasException.ERROR, "admin.EX_ERR_USER_NOT_FOUND",
            "user Id: '" + userId + "'");

      // Get a DomainDriver instance
      AbstractDomainDriver domainDriver = this.getDomainDriver(ur.domainId);

      // Get User detail from specific domain
      domainDriver.deleteUser(ur.specificId);
      
      // Delete index to given user
      domainDriver.unindexUserFull(userId);
    } catch (AdminException e) {
      throw new AdminException("DomainDriverManager.deleteUser",
          SilverpeasException.ERROR, "admin.EX_ERR_DELETE_USER", "user Id: '"
          + userId + "'", e);
    } finally {
      this.releaseOrganizationSchema();
    }
  }

  /**
   * @param m_User
   */
  public void updateUserDetail(UserDetail user) throws Exception {
    try {
      // Get a DomainDriver instance
      AbstractDomainDriver domainDriver = this.getDomainDriver(idAsInt(user
          .getDomainId()));

      // Update User detail in specific domain
      domainDriver.updateUserDetail(user);
    } catch (AdminException e) {
      throw new AdminException("DomainDriverManager.updateUser",
          SilverpeasException.ERROR, "admin.EX_ERR_UPDATE_USER", user
          .getFirstName()
          + " " + user.getLastName(), e);
    }
  }

  /**
   * @param m_User
   */
  public void updateUserFull(UserFull user) throws Exception {
    try {
      // Get a DomainDriver instance
      AbstractDomainDriver domainDriver = this.getDomainDriver(idAsInt(user
          .getDomainId()));

      // Update User detail in specific domain
      domainDriver.updateUserFull(user);
      
      // index informations relative to given user
      domainDriver.indexUserFull(user);
    } catch (AdminException e) {
      throw new AdminException("DomainDriverManager.updateUser",
          SilverpeasException.ERROR, "admin.EX_ERR_UPDATE_USER", user
          .getFirstName()
          + " " + user.getLastName(), e);
    }
  }
  
  public String[] getUserIdsOfDomain(String domainId) throws Exception {
    // Set the OrganizationSchema (if not already done)
    getOrganizationSchema();
    try {
      return organization.user.getUserIdsOfDomain(Integer.parseInt(domainId));
    } catch (AdminException e) {
      throw new AdminException("DomainDriverManager.getUser",
          SilverpeasException.ERROR, "admin.EX_ERR_GET_USERS", "domainId = " + domainId, e);
    } finally {
      releaseOrganizationSchema();
    }
  }

  public UserFull getUserFull(String userId) throws Exception {
    UserFull uf = null;

    try {
      // Set the OrganizationSchema (if not already done)
      this.getOrganizationSchema();

      // Get the user information
      UserRow ur = organization.user.getUser(idAsInt(userId));
      if (ur == null)
        throw new AdminException("DomainDriverManager.getUser",
            SilverpeasException.ERROR, "admin.EX_ERR_USER_NOT_FOUND",
            "user Id: '" + userId + "'");

      // Get a DomainDriver instance
      AbstractDomainDriver domainDriver = this.getDomainDriver(ur.domainId);

      // Get User detail from specific domain
      try {
        uf = domainDriver.getUserFull(ur.specificId);
      } catch (AdminException e) {
        SilverTrace.error("admin", "DomainDriverManager.getUser",
            "admin.MSG_ERR_GET_USER", "user Id: '" + userId + "', domain Id: '"
            + ur.domainId + "'", e);
        uf = new UserFull(domainDriver);
        uf.setLogin(ur.login);
        uf.setFirstName(ur.firstName);
        uf.setLastName(ur.lastName);
        uf.seteMail(ur.eMail);
      }

      // Fill silverpeas info of user details
      uf.setId(userId);
      uf.setSpecificId(ur.specificId);
      uf.setDomainId(idAsString(ur.domainId));
      uf.setAccessLevel(ur.accessLevel);
      uf.setLoginQuestion(ur.loginQuestion);
      uf.setLoginAnswer(ur.loginAnswer);
    } catch (AdminException e) {
      throw new AdminException("DomainDriverManager.getUser",
          SilverpeasException.ERROR, "admin.EX_ERR_GET_USER", "user Id: '"
          + userId + "', domain Id: '" + userId + "'", e);
    } finally {
      this.releaseOrganizationSchema();
    }
    return uf;
  }

  /**
   * @param userId
   * @return User
   */
  public UserDetail getUserDetail(String userId) throws Exception {
    UserDetail ud = null;

    try {
      // Set the OrganizationSchema (if not already done)
      this.getOrganizationSchema();

      // Get the user information
      UserRow ur = organization.user.getUser(idAsInt(userId));
      if (ur == null)
        throw new AdminException("DomainDriverManager.getUser",
            SilverpeasException.ERROR, "admin.EX_ERR_USER_NOT_FOUND",
            "user Id: '" + userId + "'");

      // Get a DomainDriver instance
      AbstractDomainDriver domainDriver = this.getDomainDriver(ur.domainId);

      // Get User detail from specific domain
      try {
        ud = domainDriver.getUser(ur.specificId);
      } catch (AdminException e) {
        SilverTrace.error("admin", "DomainDriverManager.getUser",
            "admin.MSG_ERR_GET_USER", "user Id: '" + userId + "', domain Id: '"
            + ur.domainId + "'", e);
        ud = new UserDetail();
        ud.setLogin(ur.login);
        ud.setFirstName(ur.firstName);
        ud.setLastName(ur.lastName);
        ud.seteMail(ur.eMail);
      }

      // Fill silverpeas info of user details
      ud.setId(userId);
      ud.setSpecificId(ur.specificId);
      ud.setDomainId(idAsString(ur.domainId));
      ud.setAccessLevel(ur.accessLevel);
    } catch (AdminException e) {
      throw new AdminException("DomainDriverManager.getUser",
          SilverpeasException.ERROR, "admin.EX_ERR_GET_USER", "user Id: '"
          + userId + "', domain Id: '" + userId + "'", e);
    } finally {
      this.releaseOrganizationSchema();
    }
    return ud;
  }

  /**
   * @param userId
   * @return User[]
   */
  public UserDetail[] getUsers(String[] userIds) throws Exception {
    UserDetail[] uds = new UserDetail[userIds.length];

    try {
      for (int nI = 0; nI < userIds.length; nI++) {
        uds[nI] = this.getUser(userIds[nI]);
      }
    } catch (AdminException e) {
      throw new AdminException("DomainDriverManager.getUsers",
          SilverpeasException.ERROR, "admin.EX_ERR_GET_USERS", e);
    }
    return uds;
  }

  /**
   * @return User[]
   */
  public UserDetail[] getAllUsers() throws Exception {
    return null;
  }

  /**
   * @return User[]
   */
  public UserDetail[] getAllUsers(String domainId) throws Exception {
    UserDetail[] uds = null;

    try {
      // Set the OrganizationSchema (if not already done)
      this.getOrganizationSchema();

      // Get a DomainDriver instance
      AbstractDomainDriver domainDriver = this
          .getDomainDriver(idAsInt(domainId));

      // Get User detail from specific domain
      uds = domainDriver.getAllUsers();
    } catch (AdminException e) {
      throw new AdminException("DomainDriverManager.getAllUsers",
          SilverpeasException.ERROR, "admin.EX_ERR_GET_ALL_USERS",
          "domain Id: '" + domainId + "'", e);
    } finally {
      this.releaseOrganizationSchema();
    }
    return uds;
  }
  
  /**
   * Indexing all users information of given domain
   * @param domainId
   * @throws Exception
   */
  public void indexAllUsers(String domainId) throws Exception {
    String[] userIds = getUserIdsOfDomain(domainId);
    AbstractDomainDriver ddm = getDomainDriver(Integer.parseInt(domainId));
    for (String userId : userIds) {
      try {
        UserFull userFull = getUserFull(userId);
        if (userFull != null) {
          ddm.indexUserFull(userFull);
        }
      } catch (Exception e) {
        SilverTrace.error("admin", "DomainDriverManager.indexAllUsers", "admin.CANT_INDEX_USER",
            "userId = " + userId, e);
      }
    }
  }

  /**
   * @param m_Group
   * @return String
   */
  public String createGroup(Group group) throws Exception {
    GroupRow gr = null;
    Group specificGroup = new Group(group);

    try {
      // Set supergroup specific Id
      if ((group.getSuperGroupId() != null)
          && (group.getSuperGroupId().length() > 0)) {
        // Get the user information
        gr = organization.group.getGroup(idAsInt(group.getSuperGroupId()));
        if (gr == null)
          throw new AdminException("DomainDriverManager.createGroup",
              SilverpeasException.ERROR, "admin.EX_ERR_GROUP_NOT_FOUND",
              "group Id: '" + group.getSuperGroupId() + "'");
        specificGroup.setSuperGroupId(gr.specificId);
      }
      // Set subUsers specific Id
      specificGroup.setUserIds(translateUserIdsToSpecificIds(idAsInt(group
          .getDomainId()), group.getUserIds()));

      // Get a DomainDriver instance
      AbstractDomainDriver domainDriver = this.getDomainDriver(idAsInt(group
          .getDomainId()));

      // Update Group in specific domain
      return domainDriver.createGroup(specificGroup);
    } catch (AdminException e) {
      throw new AdminException("DomainDriverManager.createGroup",
          SilverpeasException.ERROR, "admin.EX_ERR_UPDATE_GROUP", "group Id: '"
          + group.getId() + "'", e);
    }
  }

  /**
   * @param groupId
   */
  public void deleteGroup(String groupId) throws Exception {
    try {
      // Set the OrganizationSchema (if not already done)
      this.getOrganizationSchema();

      // Get the group information
      GroupRow gr = organization.group.getGroup(idAsInt(groupId));
      if (gr == null)
        throw new AdminException("DomainDriverManager.deleteGroup",
            SilverpeasException.ERROR, "admin.EX_ERR_GROUP_NOT_FOUND",
            "group Id: '" + groupId + "'");

      // Get a DomainDriver instance
      AbstractDomainDriver domainDriver = this.getDomainDriver(gr.domainId);

      // Get Group detail from specific domain
      domainDriver.deleteGroup(gr.specificId);
    } catch (AdminException e) {
      throw new AdminException("DomainDriverManager.deleteGroup",
          SilverpeasException.ERROR, "admin.EX_ERR_DELETE_GROUP", "group Id: '"
          + groupId + "'", e);
    } finally {
      this.releaseOrganizationSchema();
    }
  }

  /**
   * Update given group in specific domain
   * @param group
   */
  public void updateGroup(Group group) throws Exception {
    GroupRow gr = null;
    Group specificGroup = new Group(group);

    try {
      // Set supergroup specific Id
      if ((group.getSuperGroupId() != null)
          && (group.getSuperGroupId().length() > 0)) {
        // Get the user information
        gr = organization.group.getGroup(idAsInt(group.getSuperGroupId()));
        if (gr == null)
          throw new AdminException("DomainDriverManager.updateGroup",
              SilverpeasException.ERROR, "admin.EX_ERR_GROUP_NOT_FOUND",
              "group Id: '" + group.getSuperGroupId() + "'");
        specificGroup.setSuperGroupId(gr.specificId);
      }
      // Set subUsers specific Id
      specificGroup.setUserIds(translateUserIdsToSpecificIds(idAsInt(group
          .getDomainId()), group.getUserIds()));

      // Get the group information
      gr = organization.group.getGroup(idAsInt(group.getId()));
      if (gr == null)
        throw new AdminException("DomainDriverManager.updateGroup",
            SilverpeasException.ERROR, "admin.EX_ERR_GROUP_NOT_FOUND",
            "group Id: '" + group.getId() + "'");
      // Get a DomainDriver instance
      AbstractDomainDriver domainDriver = this.getDomainDriver(gr.domainId);
      specificGroup.setId(gr.specificId);

      // Update Group in specific domain
      domainDriver.updateGroup(specificGroup);
    } catch (AdminException e) {
      throw new AdminException("DomainDriverManager.updateGroup",
          SilverpeasException.ERROR, "admin.EX_ERR_UPDATE_GROUP", "group Id: '"
          + group.getId() + "'", e);
    }
  }

  /**
   * return group with given id (contains list of user ids for this group)
   * @param groupId
   * @return Group
   */
  public Group getGroup(String groupId) throws Exception {
    Group group = null;

    try {
      // Set the OrganizationSchema (if not already done)
      this.getOrganizationSchema();

      // Get the user information
      GroupRow gr = organization.group.getGroup(idAsInt(groupId));
      if (gr == null)
        throw new AdminException("DomainDriverManager.getGroup",
            SilverpeasException.ERROR, "admin.EX_ERR_GROUP_NOT_FOUND",
            "group Id: '" + groupId + "'");

      // Get a DomainDriver instance
      AbstractDomainDriver domainDriver = this.getDomainDriver(gr.domainId);

      // Get Group detail from specific domain
      group = domainDriver.getGroup(gr.specificId);

      // Fill silverpeas info of group details
      group.setId(groupId);
      group.setSpecificId(gr.specificId);
      group.setDomainId(idAsString(gr.domainId));
    } catch (AdminException e) {
      throw new AdminException("DomainDriverManager.getGroup",
          SilverpeasException.ERROR, "admin.EX_ERR_GET_GROUP", "group Id: '"
          + groupId + "'", e);
    } finally {
      this.releaseOrganizationSchema();
    }
    return group;
  }

  /**
   * return group with given group name in domain
   * @param groupName
   * @return Group
   */
  public Group getGroupByNameInDomain(String groupName, String domainId)
      throws Exception {
    Group group = null;

    try {
      // Set the OrganizationSchema (if not already done)
      this.getOrganizationSchema();

      // Get a DomainDriver instance
      AbstractDomainDriver domainDriver = this
          .getDomainDriver(idAsInt(domainId));

      // Get the group information without id and userId[]
      group = domainDriver.getGroupByName(groupName);

    } catch (AdminException e) {
      throw new AdminException("DomainDriverManager.getGroupByNameInDomain",
          SilverpeasException.ERROR, "admin.EX_ERR_GET_GROUP", "group Name: '"
          + groupName + "'", e);
    } finally {
      this.releaseOrganizationSchema();
    }
    return group;
  }

  /**
   * @param groupId
   * @return Group[]
   */
  public Group[] getGroups(String groupId) throws Exception {
    Group[] groups = null;

    try {
      // Set the OrganizationSchema (if not already done)
      this.getOrganizationSchema();

      // Get the user information
      GroupRow gr = organization.group.getGroup(idAsInt(groupId));
      if (gr == null)
        throw new AdminException("DomainDriverManager.getGroups",
            SilverpeasException.ERROR, "admin.EX_ERR_GROUP_NOT_FOUND",
            "group Id: '" + groupId + "'");

      // Get a DomainDriver instance
      AbstractDomainDriver domainDriver = this.getDomainDriver(gr.domainId);

      // Get Groups of Group from specific domain
      groups = domainDriver.getGroups(gr.specificId);
    } catch (AdminException e) {
      throw new AdminException("DomainDriverManager.getGroups",
          SilverpeasException.ERROR, "admin.EX_ERR_GET_GROUPS",
          "father group Id: '" + groupId + "'", e);
    } finally {
      this.releaseOrganizationSchema();
    }
    return groups;
  }

  /**
   * @return Group[]
   */
  public Group[] getAllGroups() throws Exception {
    return null;
  }

  /**
   * @return Group[]
   */
  public Group[] getAllGroups(String domainId) throws Exception {
    Group[] groups = null;

    try {
      // Set the OrganizationSchema (if not already done)
      this.getOrganizationSchema();

      // Get a DomainDriver instance
      AbstractDomainDriver domainDriver = this
          .getDomainDriver(idAsInt(domainId));

      // Get Group from specific domain
      groups = domainDriver.getAllGroups();
    } catch (AdminException e) {
      throw new AdminException("DomainDriverManager.getAllGroups",
          SilverpeasException.ERROR, "admin.EX_ERR_GET_ALL_GROUPS",
          "domain Id: '" + domainId + "'", e);
    } finally {
      this.releaseOrganizationSchema();
    }
    return groups;
  }

  /**
   * @return Group[]
   */
  public Group[] getAllRootGroups() throws Exception {
    return null;
  }

  /**
   * @return Group[]
   */
  public String[] getGroupMemberGroupIds(String domainId, String groupId)
      throws Exception {
    String[] groups = null;

    try {
      // Set the OrganizationSchema (if not already done)
      this.getOrganizationSchema();

      // Get a DomainDriver instance
      AbstractDomainDriver domainDriver = this
          .getDomainDriver(idAsInt(domainId));

      // Get Group from specific domain
      groups = domainDriver.getGroupMemberGroupIds(groupId);
    } catch (AdminException e) {
      throw new AdminException("DomainDriverManager.getAllRootGroups",
          SilverpeasException.ERROR, "admin.EX_ERR_GET_ALL_ROOT_GROUPS",
          "domain Id: '" + domainId + "'", e);
    } finally {
      this.releaseOrganizationSchema();
    }
    return groups;
  }

  /**
   * @return Group[]
   */
  public Group[] getAllRootGroups(String domainId) throws Exception {
    Group[] groups = null;

    try {
      // Set the OrganizationSchema (if not already done)
      this.getOrganizationSchema();

      // Get a DomainDriver instance
      AbstractDomainDriver domainDriver = this
          .getDomainDriver(idAsInt(domainId));

      // Get Group from specific domain
      groups = domainDriver.getAllRootGroups();
    } catch (AdminException e) {
      throw new AdminException("DomainDriverManager.getAllRootGroups",
          SilverpeasException.ERROR, "admin.EX_ERR_GET_ALL_ROOT_GROUPS",
          "domain Id: '" + domainId + "'", e);
    } finally {
      this.releaseOrganizationSchema();
    }
    return groups;
  }

  /**
   * @param login
   * @param password
   * @param userId
   * @param domainId
   * @return boolean
   */
  public Hashtable<String, String> authenticate(String sKey) throws Exception {
    return authenticate(sKey, true);
  }

  /**
   * @param sKey anthentication key
   * @param removeKey remove after
   * @return
   * @throws Exception
   */
  public Hashtable<String, String> authenticate(String sKey, boolean removeKey) throws Exception {
    Hashtable<String, String> loginDomainId = new Hashtable<String, String>();
    try {
      // Start transaction
      this.startTransaction(false);

      // Get the domain information
      KeyStoreRow ksr = organization.keyStore.getRecordByKey(idAsInt(sKey));
      if (ksr == null)
        throw new AdminException("DomainDriverManager.authenticate",
            SilverpeasException.ERROR, "admin.EX_ERR_KEY_NOT_FOUND", "key: '"
            + sKey + "'");

      loginDomainId.put("login", ksr.login);
      loginDomainId.put("domainId", idAsString(ksr.domainId));

      // Remove key from keytore in database
      if (removeKey) {
        organization.keyStore.removeKeyStoreRecord(idAsInt(sKey));
      }

      // Commit transaction
      this.commit();
      return loginDomainId;
    } catch (AdminPersistenceException e) {
      try {
        this.rollback();
      } catch (Exception e1) {
        SilverTrace.error("admin", "DomainDriverManager.authenticate",
            "root.EX_ERR_ROLLBACK", e1);
      }
      throw new AdminException("DomainDriverManager.authenticate",
          SilverpeasException.ERROR, "admin.EX_ERR_AUTHENTICATE", "key: '"
          + sKey + "'", e);
    }
  }

  /**
   * @return Domain[]
   */
  public Domain[] getAllDomains() throws Exception {
    Domain[] valret = null;
    int i;

    try {
      // Set the OrganizationSchema (if not already done)
      this.getOrganizationSchema();

      // Get the domain information
      DomainRow[] drs = organization.domain.getAllDomains();
      if ((drs == null) || (drs.length <= 0))
        throw new AdminException("DomainDriverManager.getAllDomains",
            SilverpeasException.ERROR, "admin.EX_ERR_NO_DOMAIN_FOUND");

      valret = new Domain[drs.length];
      for (i = 0; i < drs.length; i++) {
        valret[i] = new Domain();
        valret[i].setId(Integer.toString(drs[i].id));
        valret[i].setName(drs[i].name);
        valret[i].setDescription(drs[i].description);
        valret[i].setDriverClassName(drs[i].className);
        valret[i].setPropFileName(drs[i].propFileName);
        valret[i].setAuthenticationServer(drs[i].authenticationServer);
        valret[i].setTheTimeStamp(drs[i].theTimeStamp);
      }
    } catch (AdminPersistenceException e) {
      throw new AdminException("DomainDriverManager.getAllDomains",
          SilverpeasException.ERROR, "admin.EX_ERR_GET_ALL_DOMAINS", e);
    } finally {
      this.releaseOrganizationSchema();
    }
    return valret;
  }

  /**
   * @param groupId
   * @return Domain
   */
  public long getDomainActions(String domainId) throws Exception {
    return getDomainDriver(idAsInt(domainId)).getDriverActions();
  }

  public String createDomain(Domain theDomain) throws Exception {
    AbstractDomainDriver domainDriver = null;
    try {
      this.startTransaction(false);

      DomainRow dr = new DomainRow();
      dr.id = -1;
      dr.name = theDomain.getName();
      dr.description = theDomain.getDescription();
      dr.className = theDomain.getDriverClassName();
      dr.propFileName = theDomain.getPropFileName();
      dr.authenticationServer = theDomain.getAuthenticationServer();
      dr.theTimeStamp = theDomain.getTheTimeStamp();
      dr.silverpeasServerURL = theDomain.getSilverpeasServerURL();

      // Create domain
      organization.domain.createDomain(dr);
      this.commit();
      LoginPasswordAuthentication.initDomains();
      // Update the synchro thread
      domainDriver = getDomainDriver(dr.id);
      if (domainDriver.isSynchroThreaded() && theThread != null) {
        theThread.addDomain(idAsString(dr.id));
      }

      return idAsString(dr.id);
    } catch (AdminException e) {
      try {
        this.rollback();
      } catch (Exception e1) {
        SilverTrace.error("admin", "DomainDriverManager.createDomain",
            "root.EX_ERR_ROLLBACK", e1);
      }
      throw new AdminException("DomainDriverManager.createDomain",
          SilverpeasException.ERROR, "admin.EX_ERR_ADD_DOMAIN",
          "domain name: '" + theDomain.getName() + "'", e);
    }
  }

  public String updateDomain(Domain theDomain) throws Exception {
    AbstractDomainDriver domainDriver = null;
    try {
      this.startTransaction(false);

      DomainRow dr = new DomainRow();
      dr.id = idAsInt(theDomain.getId());
      dr.name = theDomain.getName();
      dr.description = theDomain.getDescription();
      dr.className = theDomain.getDriverClassName();
      dr.propFileName = theDomain.getPropFileName();
      dr.authenticationServer = theDomain.getAuthenticationServer();
      dr.theTimeStamp = theDomain.getTheTimeStamp();
      dr.silverpeasServerURL = theDomain.getSilverpeasServerURL();

      // Create domain
      organization.domain.updateDomain(dr);
      if (domainDriverInstances.get(theDomain.getId()) != null) {
        domainDriverInstances.remove(theDomain.getId());
      }
      this.commit();
      LoginPasswordAuthentication.initDomains();
      // Update the synchro thread
      domainDriver = getDomainDriver(dr.id);
      if (!domainDriver.isSynchroThreaded() && theThread != null) {
        theThread.removeDomain(theDomain.getId());
      }

      return theDomain.getId();
    } catch (AdminException e) {
      try {
        this.rollback();
      } catch (Exception e1) {
        SilverTrace.error("admin", "DomainDriverManager.updateDomain",
            "root.EX_ERR_ROLLBACK", e1);
      }
      throw new AdminException("DomainDriverManager.updateDomain",
          SilverpeasException.ERROR, "admin.EX_ERR_ADD_DOMAIN",
          "domain name: '" + theDomain.getName() + "'", e);
    }
  }

  public String removeDomain(String domainId) throws Exception {
    try {
      this.startTransaction(false);

      // Remove the domain
      organization.domain.removeDomain(idAsInt(domainId));
      if (domainDriverInstances.get(domainId) != null) {
        domainDriverInstances.remove(domainId);
      }
      this.commit();
      LoginPasswordAuthentication.initDomains();
      if (theThread != null) {
        // Update the synchro thread
        theThread.removeDomain(domainId);
      }
      return domainId;
    } catch (AdminException e) {
      try {
        this.rollback();
      } catch (Exception e1) {
        SilverTrace.error("admin", "DomainDriverManager.createDomain",
            "root.EX_ERR_ROLLBACK", e1);
      }
      throw new AdminException("DomainDriverManager.createDomain",
          SilverpeasException.ERROR, "admin.EX_ERR_ADD_DOMAIN", "domain id: '"
          + domainId + "'", e);
    }
  }

  /**
   * @param domainId
   * @return String
   */
  public Domain getDomain(String domainId) throws Exception {
    Domain valret = null;

    try {
      // Set the OrganizationSchema (if not already done)
      this.getOrganizationSchema();

      // Get the domain information
      DomainRow dr = organization.domain.getDomain(idAsInt(domainId));
      if (dr == null)
        throw new AdminException("DomainDriverManager.getDomain",
            SilverpeasException.ERROR, "admin.EX_ERR_DOMAIN_NOT_FOUND",
            "domain Id: '" + domainId + "'");

      valret = new Domain();
      valret.setId(Integer.toString(dr.id));
      valret.setName(dr.name);
      valret.setDescription(dr.description);
      valret.setDriverClassName(dr.className);
      valret.setPropFileName(dr.propFileName);
      valret.setAuthenticationServer(dr.authenticationServer);
      valret.setTheTimeStamp(dr.theTimeStamp);
      valret.setSilverpeasServerURL(dr.silverpeasServerURL);
    } catch (AdminPersistenceException e) {
      throw new AdminException("DomainDriverManager.getDomain",
          SilverpeasException.ERROR, "admin.EX_ERR_GET_DOMAIN", "domain id: '"
          + domainId + "'", e);
    } finally {
      this.releaseOrganizationSchema();
    }
    return valret;
  }

  /**
   * @param domainId
   * @return DomainDriver
   */
  public AbstractDomainDriver getDomainDriver(int domainId) throws Exception {
    AbstractDomainDriver domainDriver = null;
    boolean osAllocated = false;

    try {
      domainDriver = domainDriverInstances.get(idAsString(domainId));
      if (domainDriver == null) {
        // Set the OrganizationSchema (if not already done)
        this.getOrganizationSchema();
        osAllocated = true;

        // Get the domain information
        DomainRow dr = organization.domain.getDomain(domainId);
        if (dr == null)
          throw new AdminException("DomainDriverManager.getDomainDriver",
              SilverpeasException.ERROR, "admin.EX_ERR_DOMAIN_NOT_FOUND",
              "domain Id: '" + domainId + "'");

        // Get the driver class name
        try {
          domainDriver = (AbstractDomainDriver) Class.forName(dr.className)
              .newInstance();
          domainDriver.init(domainId, dr.propFileName, dr.authenticationServer);
        } catch (ClassNotFoundException e) {
          throw new AdminException("DomainDriverManager.getDomainDriver",
              SilverpeasException.ERROR, "root.EX_CLASS_NOT_FOUND", e);
        } catch (IllegalAccessException e) {
          throw new AdminException("DomainDriverManager.getDomainDriver",
              SilverpeasException.ERROR, "root.EX_ILLEGAL_ACCESS", e);
        } catch (InstantiationException e) {
          throw new AdminException("DomainDriverManager.getDomainDriver",
              SilverpeasException.ERROR, "root.EX_INSTANTIATION", e);
        }

        // Save AbstractDomainDriver instance
        domainDriverInstances.put(idAsString(domainId), domainDriver);
      }
    } catch (AdminPersistenceException e) {
      throw new AdminException("DomainDriverManager.getDomainDriver",
          SilverpeasException.ERROR, "admin.EX_ERR_GET_DOMAIN_DRIVER",
          "domain id: '" + domainId + "'", e);
    } finally {
      if (osAllocated) {
        osAllocated = false;
        this.releaseOrganizationSchema();
      }
    }
    return domainDriver;
  }

  /**
   * Called when Admin starts the synchronization on a particular Domain
   */
  public void beginSynchronization(String sdomainId) throws Exception {
    // Get a DomainDriver instance
    AbstractDomainDriver domainDriver = this
        .getDomainDriver(idAsInt(sdomainId));

    domainDriver.beginSynchronization();
  }

  /**
   * Called when Admin ends the synchronization
   * @param cancelSynchro true if the synchronization is cancelled, false if it ends normally
   */
  public String endSynchronization(String sdomainId, boolean cancelSynchro)
      throws Exception {
    // Get a DomainDriver instance
    AbstractDomainDriver domainDriver = this
        .getDomainDriver(idAsInt(sdomainId));

    return domainDriver.endSynchronization(cancelSynchro);
  }

  /**
   * Start a new transaction
   */
  public void startTransaction(boolean bAutoCommit) throws Exception {
    getOrganizationSchema();
    inTransaction = true;
  }

  /**
   * Commit transaction
   */
  public void commit() throws Exception {
    try {
      inTransaction = false;
      organization.commit();
    } catch (Exception e) {
      throw new AdminException("DomainDriverManager.commit",
          SilverpeasException.ERROR, "root.EX_ERR_COMMIT", e);
    } finally {
      releaseOrganizationSchema();
    }
  }

  /**
   * Rollback transaction
   */
  public void rollback() throws Exception {
    try {
      inTransaction = false;
      organization.rollback();
    } catch (Exception e) {
      throw new AdminException("DomainDriverManager.rollback",
          SilverpeasException.ERROR, "root.EX_ERR_ROLLBACK", e);
    } finally {
      releaseOrganizationSchema();
    }
  }

  /**
   * Start a new transaction in specific domain driver
   */
  public void startTransaction(String domainId, boolean bAutoCommit)
      throws Exception {
    try {
      // Get a AbstractDomainDriver instance
      AbstractDomainDriver domainDriver = this
          .getDomainDriver(idAsInt(domainId));

      // Start transaction
      domainDriver.startTransaction(bAutoCommit);
    } catch (Exception e) {
      throw new AdminException("DomainDriverManager.startTransaction",
          SilverpeasException.ERROR, "admin.EX_ERR_START_TRANSACTION",
          "domain Id: '" + domainId + "'", e);
    }
  }

  /**
   * Commit transaction in specific domain driver
   */
  public void commit(String domainId) throws Exception {
    try {
      // Get a DomainDriver instance
      AbstractDomainDriver domainDriver = this
          .getDomainDriver(idAsInt(domainId));

      // Commit transaction
      domainDriver.commit();
    } catch (Exception e) {
      throw new AdminException("DomainDriverManager.commit",
          SilverpeasException.ERROR, "root.EX_ERR_COMMIT", "domain Id: '"
          + domainId + "'", e);
    }
  }

  /**
   * Rollback transaction in specific domain driver
   */
  public void rollback(String domainId) throws Exception {
    try {
      // Get a DomainDriver instance
      AbstractDomainDriver domainDriver = this
          .getDomainDriver(idAsInt(domainId));

      // Commit transaction
      domainDriver.rollback();
    } catch (Exception e) {
      throw new AdminException("DomainDriverManager.rollback",
          SilverpeasException.ERROR, "root.EX_ERR_ROLLBACK", "domain Id: '"
          + domainId + "'", e);
    }
  }

  protected String[] translateUserIdsToSpecificIds(int domainId, String[] ids) {
    Vector<String> specificIds = null;
    int i = 0;
    UserRow ur = null;

    if (ids == null)
      return null;

    specificIds = new Vector<String>();
    for (i = 0; i < ids.length; i++) {
      // Get the user information
      try {
        ur = organization.user.getUser(idAsInt(ids[i]));
        if ((ur != null) && (ur.domainId == domainId))
          specificIds.add(ur.specificId);
      } catch (Exception e) {
        SilverTrace.error("admin", "DomainDriverManager.getUser",
            "admin.MSG_ERR_GET_USER", "user Id: '" + ids[i] + "'", e);
      }
    }
    return (String[]) specificIds.toArray(new String[0]);
  }
}
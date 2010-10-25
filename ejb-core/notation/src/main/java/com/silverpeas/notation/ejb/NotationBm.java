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

package com.silverpeas.notation.ejb;

import java.rmi.RemoteException;
import java.util.Collection;

import javax.ejb.EJBObject;

import com.silverpeas.notation.model.NotationDetail;
import com.silverpeas.notation.model.NotationPK;

public interface NotationBm extends EJBObject {

  public void updateNotation(NotationPK pk, int note) throws RemoteException;

  public void deleteNotation(NotationPK pk) throws RemoteException;

  public NotationDetail getNotation(NotationPK pk) throws RemoteException;

  public int countNotations(NotationPK pk) throws RemoteException;

  public boolean hasUserNotation(NotationPK pk) throws RemoteException;

  public Collection<NotationDetail> getBestNotations(NotationPK pk, int notationsCount)
      throws RemoteException;

  public Collection<NotationDetail> getBestNotations(Collection<NotationPK> pks, int notationsCount)
      throws RemoteException;

}
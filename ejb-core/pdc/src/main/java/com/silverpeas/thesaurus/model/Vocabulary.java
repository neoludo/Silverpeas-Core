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

package com.silverpeas.thesaurus.model;

import com.stratelia.webactiv.persistence.*;
import java.lang.Comparable;

/**
 * This class contains a full information about a Vocabulary
 */

public class Vocabulary extends SilverpeasBean implements Comparable {

  private static final long serialVersionUID = -5979441125808657400L;
  private String name;
  private String description;

  public Vocabulary() {
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean equals(Object voca) {
    return this.getName().equals(((Vocabulary) voca).getName());
  }

  public int compareTo(Object voca) {
    return this.getName().compareTo(((Vocabulary) voca).getName());
  }

  public String _getTableName() {
    return "SB_Thesaurus_Vocabulary";
  }

  public int _getConnectionType() {
    return SilverpeasBeanDAO.CONNECTION_TYPE_DATASOURCE_SILVERPEAS;
  }
}
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

package com.stratelia.webactiv.util.indexEngine.model;

//import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ISOLatin1AccentFilter;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardFilter;

import com.stratelia.silverpeas.silvertrace.SilverTrace;
import com.stratelia.webactiv.util.ResourceLocator;
import com.stratelia.webactiv.util.indexEngine.analysis.ElisionFilter;
import com.stratelia.webactiv.util.indexEngine.analysis.SilverTokenizer;

/**
 * Extends lucene Analyzer : prunes from a tokens stream all the meaningless words and prunes all
 * the special characters.
 */
public final class WAAnalyzer extends Analyzer {
  /**
   * Returns the analyzer to be used with texts of the given language. The analyzers are cached.
   */
  static public Analyzer getAnalyzer(String language) {
    Analyzer analyzer = (Analyzer) languageMap.get(language);

    if (analyzer == null) {
      analyzer = new WAAnalyzer(language);
      languageMap.put(language, analyzer);
    }

    return analyzer;
  }

  /**
   * Returns a tokens stream built on top of the given reader.
   */
  public TokenStream tokenStream(Reader reader) {
    // TokenStream result = new StandardTokenizer(reader);
    TokenStream result = new SilverTokenizer(reader);

    result = new StandardFilter(result); // remove 's and . from token
    result = new LowerCaseFilter(result);
    result = new StopFilter(result, stopWords); // remove some unexplicit terms
    // according to the language
    result = new ElisionFilter(result); // remove [cdjlmnst-qu]' from token
    // result = new ApostropheFilter(result); //remove [cdlmnst]' from token
    result = new ISOLatin1AccentFilter(result);
    /*
     * if (charReplacer != null) { result = new CharFilter(result, charReplacer); //unaccent terms }
     */

    /*
     * try { Token token = (Token) result.next(); while (token != null) {
     * SilverTrace.debug("indexEngine", "WAAnalyzer", "root.MSG_GEN_PARAM_VALUE",
     * "token = "+token.termText()); token = (Token) result.next(); } } catch (IOException ioe) {
     * SilverTrace.debug("indexEngine", "WAAnalyzer", "root.MSG_GEN_PARAM_VALUE",
     * "ioe = "+ioe.toString()); }
     */

    return result;
  }

  public TokenStream tokenStream(String arg0, Reader reader) {
    return tokenStream(reader);
  }

  /**
   * The constructor is private : use @link #getAnalyzer().
   */
  private WAAnalyzer(String language) {
    stopWords = getStopWords(language);
  }

  /**
   * Returns an array of words which are not usually usefull for searching.
   */
  private String[] getStopWords(String language) {
    List<String> wordList = new ArrayList<String>();

    try {
      if (language == null || language.equals("")) {
        language = "fr";
      }

      ResourceLocator resource = new ResourceLocator(
          "com.stratelia.webactiv.util.indexEngine.StopWords", language);

      Enumeration<String> stopWord = resource.getKeys();

      while (stopWord.hasMoreElements()) {
        wordList.add(stopWord.nextElement());
      }
    } catch (MissingResourceException e) {
      SilverTrace.warn("indexEngine", "WAAnalyzer",
          "indexEngine.MSG_MISSING_STOPWORDS_DEFINITION");
      return new String[0];
    }

    return (String[]) wordList.toArray(new String[wordList.size()]);
  }

  /**
   * Returns an object which while replace all the special characters. Returns null if the replacer
   * do nothing.
   */
  private CharReplacer getCharReplacer(String language) {
    CharReplacer replacer = new CharReplacer();
    int replacementCount = 0;

    try {
      if (language == null || language.equals("")) {
        language = "fr";
      }

      ResourceLocator resource = new ResourceLocator(
          "com.stratelia.webactiv.util.indexEngine.SpecialChars", language);

      Enumeration<String> replacements = resource.getKeys();

      while (replacements.hasMoreElements()) {
        String oldChars = replacements.nextElement();
        String newChars = resource.getString(oldChars);

        replacer.setReplacement(oldChars, newChars);
        replacementCount++;
      }
    } catch (MissingResourceException e) {
      SilverTrace.warn("indexEngine", "WAAnalyzer",
          "indexEngine.MSG_MISSING_SPECIALCHARS_DEFINITION");
    }

    if (replacementCount == 0) {
      return null;
    } else {
      return replacer;
    }
  }

  static private final Map<String, Analyzer> languageMap = new HashMap<String, Analyzer>();

  /**
   * The words which are usually not usefull for searching.
   */
  private String[] stopWords = null;
}
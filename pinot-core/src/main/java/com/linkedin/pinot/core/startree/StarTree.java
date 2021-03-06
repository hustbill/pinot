/**
 * Copyright (C) 2014-2016 LinkedIn Corp. (pinot-core@linkedin.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linkedin.pinot.core.startree;

import com.google.common.collect.HashBiMap;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class StarTree implements StarTreeInterf {
  private static final Logger LOGGER = LoggerFactory.getLogger(StarTree.class);
  private static final long serialVersionUID = 1L;

  StarTreeIndexNode root;
  int numNodes = 0;

  private HashBiMap<String, Integer> dimensionNameToIndexMap;

  public StarTree(StarTreeIndexNode root, HashBiMap<String, Integer> dimensionNameToIndexMap) {
    this.root = root;
    this.dimensionNameToIndexMap = dimensionNameToIndexMap;
  }

  /**
   * {@inheritDoc}
   * @return
   */
  @Override
  public StarTreeIndexNodeInterf getRoot() {
    return root;
  }

  /**
   * {@inheritDoc}
   * @return
   */
  @Override
  public StarTreeFormatVersion getVersion() {
    return StarTreeFormatVersion.ON_HEAP;
  }

  /**
   * Returns the total number of nodes in the star tree.
   * For backward compatibility (trees without this info), it computes
   * the number of nodes on the fly.
   *
   * @return
   */
  @Override
  public int getNumNodes() {
    if (numNodes == 0) {
      numNodes = getNumNodes(root);
    }
    return numNodes;
  }

  /**
   * {@inheritDoc}
   * @return
   */
  @Override
  public HashBiMap<String, Integer> getDimensionNameToIndexMap() {
    return dimensionNameToIndexMap;
  }

  /**
   * {@inheritDoc}
   * @param outputFile
   * @throws IOException
   */
  @Override
  public void writeTree(File outputFile)
      throws IOException {
    OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
    ObjectOutputStream oos = new ObjectOutputStream(outputStream);

    try {
      oos.writeObject(this);
    } catch (Exception e) {
      LOGGER.error("Exception caught while writing StarTree file", e);
    } finally {
      oos.close();
    }
  }

  @Override
  public void printTree() {
    printTree(root, 0);
  }

  /**
   * Helper method to print the tree.
   * @param root
   * @param level
   */
  public void printTree(StarTreeIndexNode root, int level) {
    for (int i = 0; i < level; i++) {
      System.out.print("  ");
    }
    System.out.println(root);

    if (!root.isLeaf()) {
      for (StarTreeIndexNode child : root.getChildren().values()) {
        printTree(child, level + 1);
      }
    }
  }

  /**
   * Helper method that computes and returns the number of nodes
   * in the tree (by performing a dfs on the tree).
   *
   * @param root
   * @return
   */
  private static int getNumNodes(StarTreeIndexNode root) {
    if (root == null) {
      return 0;
    }

    int numNodes = 1;
    Map<Integer, StarTreeIndexNode> children = root.getChildren();
    if (children != null) {
      for (StarTreeIndexNode child : children.values()) {
        numNodes += getNumNodes(child);
      }
    }
    return numNodes;
  }
}

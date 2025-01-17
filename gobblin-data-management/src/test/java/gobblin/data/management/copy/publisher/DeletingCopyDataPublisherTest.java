/*
 * Copyright (C) 2014-2015 LinkedIn Corp. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.
 */
package gobblin.data.management.copy.publisher;

import gobblin.configuration.State;
import gobblin.configuration.WorkUnitState;
import gobblin.configuration.WorkUnitState.WorkingState;
import gobblin.data.management.copy.CopySource;
import gobblin.data.management.copy.CopyableDataset;
import gobblin.data.management.copy.CopyableFile;
import gobblin.data.management.copy.CopyableFileUtils;
import gobblin.data.management.copy.TestCopyableDataset;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Closer;


@Slf4j
public class DeletingCopyDataPublisherTest {

  @Test
  public void testDeleteOnSource() throws Exception {

    State state = getTestState("testDeleteOnSource");

    Path testMethodTempPath = new Path(testClassTempPath, "testDeleteOnSource");

    DeletingCopyDataPublisher copyDataPublisher = closer.register(new DeletingCopyDataPublisher(state));

    WorkUnitState wus = new WorkUnitState();

    CopyableDataset copyableDataset = new TestCopyableDataset(new Path("origin"), new Path(testMethodTempPath, "testdataset"));

    CopyableFile cf = CopyableFileUtils.createTestCopyableFile(new Path(testMethodTempPath, "test.txt").toString());

    CopySource.serializeCopyableDataset(wus, copyableDataset);

    CopySource.serializeCopyableFiles(wus, ImmutableList.of(cf));

    Assert.assertTrue(fs.exists(new Path(testMethodTempPath, "test.txt")));

    wus.setWorkingState(WorkingState.SUCCESSFUL);

    copyDataPublisher.publishData(ImmutableList.of(wus));

    Assert.assertFalse(fs.exists(new Path(testMethodTempPath, "test.txt")));

  }

  private static final Closer closer = Closer.create();

  private FileSystem fs;
  private Path testClassTempPath;

  @BeforeClass
  public void setup() throws Exception {
    fs = FileSystem.getLocal(new Configuration());
    testClassTempPath =
        new Path(this.getClass().getClassLoader().getResource("").getFile(), "DeletingCopyDataPublisherTest");
    fs.delete(testClassTempPath, true);
    log.info("Created a temp directory for CopyDataPublisherTest at " + testClassTempPath);
    fs.mkdirs(testClassTempPath);
  }

  @AfterClass
  public void cleanup() {
    try {
      closer.close();
      fs.delete(testClassTempPath, true);
    } catch (IOException e) {
      log.warn(e.getMessage());
    }
  }

  private State getTestState(String testMethodName) {
    return CopyDataPublisherTest.getTestState(testMethodName, testClassTempPath);
  }

}

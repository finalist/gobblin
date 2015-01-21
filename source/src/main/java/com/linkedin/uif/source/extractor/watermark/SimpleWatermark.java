/* (c) 2014 LinkedIn Corp. All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.
 */

package com.linkedin.uif.source.extractor.watermark;

import java.math.RoundingMode;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.math.DoubleMath;
import com.linkedin.uif.source.extractor.extract.QueryBasedExtractor;


public class SimpleWatermark implements Watermark {
  private static final Logger LOG = LoggerFactory.getLogger(SimpleWatermark.class);
  private static final int deltaForNextWatermark = 1;
  private String watermarkColumn;

  public SimpleWatermark(String watermarkColumn, String watermarkFormat) {
    this.watermarkColumn = watermarkColumn;
  }

  @Override
  public String getWatermarkCondition(QueryBasedExtractor extractor, long watermarkValue, String operator) {
    return this.watermarkColumn + " " + operator + " " + watermarkValue;
  }

  @Override
  public int getDeltaNumForNextWatermark() {
    return deltaForNextWatermark;
  }

  @Override
  public HashMap<Long, Long> getIntervals(long lowWatermarkValue, long highWatermarkValue, int partitionInterval,
      int maxIntervals) {
    HashMap<Long, Long> intervalMap = new HashMap<Long, Long>();
    long nextNum;
    if (partitionInterval < 1) {
      partitionInterval = 1;
    }
    
    if (maxIntervals <= 0) {
      return intervalMap;
    }
    
    long interval = this.getInterval(lowWatermarkValue, highWatermarkValue, partitionInterval, maxIntervals);
    LOG.info("Recalculated partition interval:" + interval);
    if (interval == 0) {
      return intervalMap;
    }

    long startNum = lowWatermarkValue;
    long endNum = highWatermarkValue;
    boolean longOverflow = false;
    while (startNum <= endNum && !longOverflow) {
      longOverflow = (Long.MAX_VALUE - interval < startNum);
      nextNum = longOverflow ? Long.MAX_VALUE : startNum + interval;
      intervalMap.put(startNum, (nextNum <= endNum ? nextNum : endNum));
      startNum = nextNum + deltaForNextWatermark;
    }
    return intervalMap;
  }

  /**
   * recalculate interval if total number of partitions greater than maximum number of allowed partitions
   *
   * @param difference in range
   * @param partition interval
   * @param Maximum number of allowed partitions
   * @return calculated interval
   */
  private long getInterval(long lowWatermarkValue, long highWatermarkValue, int partitionInterval, int maxIntervals) {
    if (lowWatermarkValue > highWatermarkValue) {
      return 0;
    }
    long outputInterval = partitionInterval;
    
    boolean longOverflow = false;
    long totalIntervals = Long.MAX_VALUE;
    try {
      totalIntervals = DoubleMath.roundToLong((double) highWatermarkValue / partitionInterval
          - (double) lowWatermarkValue / partitionInterval, RoundingMode.CEILING);
    } catch (java.lang.ArithmeticException e) {
      longOverflow = true;
    }
    if (longOverflow || totalIntervals > maxIntervals) {
        outputInterval = DoubleMath.roundToLong((double) highWatermarkValue / maxIntervals
            - (double) lowWatermarkValue / maxIntervals, RoundingMode.CEILING);
    }
    return outputInterval;
  }
}

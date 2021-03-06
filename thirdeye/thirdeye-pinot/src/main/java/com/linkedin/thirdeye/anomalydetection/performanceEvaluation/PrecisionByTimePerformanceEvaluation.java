package com.linkedin.thirdeye.anomalydetection.performanceEvaluation;

import com.linkedin.thirdeye.api.DimensionMap;
import com.linkedin.thirdeye.datalayer.dto.MergedAnomalyResultDTO;
import com.linkedin.thirdeye.util.IntervalUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joda.time.Interval;


/**
 * The precision of the cloned function with regarding to the labeled anomalies in original function.
 * The calculation is based on the time overlapped with the labeled anomalies.
 * precision = (the overlapped time duration between detected anomalies and labelled anomalies) / (the time length of the detected anomalies)
 */
public class PrecisionByTimePerformanceEvaluation extends BasePerformanceEvaluate {
  private Map<DimensionMap, List<Interval>> knownAnomalyIntervals;      // The merged anomaly intervals which are labeled by user
  private List<MergedAnomalyResultDTO> detectedAnomalies;        // The merged anomalies which are generated by anomaly function

  public PrecisionByTimePerformanceEvaluation(List<MergedAnomalyResultDTO> knownAnomalies, List<MergedAnomalyResultDTO> detectedAnomalies) {
    this.knownAnomalyIntervals = mergedAnomalyResultsToIntervalMap(knownAnomalies);
    this.detectedAnomalies = detectedAnomalies;
  }

  @Override
  public double evaluate() {
    if(knownAnomalyIntervals == null || knownAnomalyIntervals.size() == 0) {
      return 0;
    }
    Map<DimensionMap, List<Interval>> anomalyIntervals = mergedAnomalyResultsToIntervalMap(detectedAnomalies);
    IntervalUtils.mergeIntervals(anomalyIntervals);
    Map<DimensionMap, Long> dimensionToDetectedAnomalyTimeLength = new HashMap<>();
    Map<DimensionMap, Long> dimensionToOverlapTimeLength = new HashMap<>();

    for(MergedAnomalyResultDTO detectedAnomaly : detectedAnomalies) {
      Interval anomalyInterval = new Interval(detectedAnomaly.getStartTime(), detectedAnomaly.getEndTime());
      DimensionMap dimensions = detectedAnomaly.getDimensions();
      for(Interval knownAnomalyInterval : knownAnomalyIntervals.get(dimensions)) {
        if(!dimensionToDetectedAnomalyTimeLength.containsKey(dimensions)) {
          dimensionToDetectedAnomalyTimeLength.put(dimensions, 0l);
          dimensionToOverlapTimeLength.put(dimensions, 0l);
        }
        Interval overlapInterval = knownAnomalyInterval.overlap(anomalyInterval);
        if(overlapInterval != null) {
          dimensionToOverlapTimeLength.put(dimensions,
              dimensionToOverlapTimeLength.get(dimensions) + overlapInterval.toDurationMillis());
        }
        dimensionToDetectedAnomalyTimeLength.put(dimensions,
            dimensionToDetectedAnomalyTimeLength.get(dimensions) + anomalyInterval.toDurationMillis());
      }
    }

    // take average of all the precisions
    long totalDetectedAnomalyTimeLength = 0;
    long totalDimensionOverlapTimeLength = 0;
    for(DimensionMap dimensions : dimensionToOverlapTimeLength.keySet()) {
      totalDetectedAnomalyTimeLength += dimensionToDetectedAnomalyTimeLength.get(dimensions);
      totalDimensionOverlapTimeLength += dimensionToOverlapTimeLength.get(dimensions);
    }
    if (totalDetectedAnomalyTimeLength == 0) {
      return Double.NaN;
    }
    return (double) totalDimensionOverlapTimeLength / totalDetectedAnomalyTimeLength;
  }
}

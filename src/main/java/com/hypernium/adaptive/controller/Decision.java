package com.hypernium.adaptive.controller;

import com.hypernium.adaptive.classifier.BottleneckResult;
import com.hypernium.adaptive.policy.AdaptivePolicy;

/**
 * Outcome of one evaluation.
 *
 * @param reason why this action (or why nothing happened), for the command and debugging
 */
public record Decision(
    Action action, BottleneckResult bottleneck, AdaptivePolicy policy, String reason) {

  public boolean changedPolicy() {
    return action != Action.IDLE && action != Action.HOLD;
  }
}

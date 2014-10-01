package com.eucalyptus.cloudformation.workflow.create;

import com.amazonaws.services.simpleworkflow.flow.interceptors.RetryPolicy;
import com.eucalyptus.cloudformation.resources.ResourceAction;

/**
 * Created by ethomas on 9/28/14.
 */
public interface CreateStep {
  public ResourceAction perform(ResourceAction resourceAction) throws Exception;
  public RetryPolicy getRetryPolicy();
}

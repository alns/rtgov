/*
 * 2012-3 Red Hat Inc. and/or its affiliates and other contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.overlord.rtgov.content.epn;

import java.io.Serializable;

import org.overlord.rtgov.analytics.service.InterfaceDefinition;
import org.overlord.rtgov.analytics.service.MEPDefinition;
import org.overlord.rtgov.analytics.service.OperationDefinition;
import org.overlord.rtgov.analytics.service.RequestFaultDefinition;
import org.overlord.rtgov.analytics.service.ResponseTime;
import org.overlord.rtgov.analytics.service.ServiceDefinition;

/**
 * This class provides an implementation of the EventProcessor
 * interface, used to identify and split out SOA related events
 * for use by subsequent event processor nodes.
 *
 */
public class ServiceResponseTimeProcessor extends org.overlord.rtgov.ep.EventProcessor {

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Serializable process(String source, Serializable event,
            int retriesLeft) throws Exception {
        Serializable ret=null;
        
        if (event instanceof ServiceDefinition) {
            ret = new java.util.LinkedList<ResponseTime>();

            ServiceDefinition sd=(ServiceDefinition)event;
            
            for (InterfaceDefinition idef : sd.getInterfaces()) {
                for (OperationDefinition opdef : idef.getOperations()) {
                    processOperation((java.util.LinkedList<ResponseTime>)ret,
                                 sd, idef, opdef);
                }
            }
            
            if (((java.util.LinkedList<Serializable>)ret).size() == 0) {
                ret = null;
            }
        }
        
        return (ret);
    }

    /**
     * This method processes the operation definition to extract the
     * response time information.
     * 
     * @param rts The response time list
     * @param sdef The service definition
     * @param idef The interface definition
     * @param opdef The operation definition
     */
    protected void processOperation(java.util.List<ResponseTime> rts,
            ServiceDefinition sdef, InterfaceDefinition idef, OperationDefinition opdef) {
        
        if (opdef.getRequestResponse() != null) {
            processMEP(rts, sdef, idef, opdef, opdef.getRequestResponse());
        }

        for (int i=0; i < opdef.getRequestFaults().size(); i++) {
            processMEP(rts, sdef, idef, opdef, opdef.getRequestFaults().get(i));
        }
    }
    
    /**
     * This method processes the MEP definition to extract the
     * response time information.
     * 
     * @param rts The response time list
     * @param sdef The service definition
     * @param idef The interface definition
     * @param opdef The operation definition
     * @param mep The MEP definition
     */
    protected void processMEP(java.util.List<ResponseTime> rts,
            ServiceDefinition sdef, InterfaceDefinition idef, OperationDefinition opdef, MEPDefinition mep) {
        
        ResponseTime rt=new ResponseTime();
        
        rt.setServiceType(sdef.getServiceType());
        rt.setInterface(idef.getInterface());
        rt.setOperation(opdef.getName());
        
        if (mep instanceof RequestFaultDefinition) {
            rt.setFault(((RequestFaultDefinition)mep).getFault());
        }
        
        rt.setAverage(mep.getMetrics().getAverage());
        rt.setMin(mep.getMetrics().getMin());
        rt.setMax(mep.getMetrics().getMax());
        
        rt.setTimestamp(System.currentTimeMillis());
        
        // Copy the request/response activity type ids
        rt.setRequestId(mep.getRequestId());
        rt.setResponseId(mep.getResponseId());
        
        // Copy the properties
        rt.getProperties().putAll(mep.getProperties());
        
        // Obtain context information from the service definition
        rt.getContext().addAll(sdef.getContext());
        
        rts.add(rt);
    }
}

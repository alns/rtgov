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
package org.overlord.rtgov.epn.validation;

import java.text.MessageFormat;

import org.overlord.rtgov.epn.Network;
import org.overlord.rtgov.epn.Node;
import org.overlord.rtgov.epn.Notification;
import org.overlord.rtgov.epn.Subscription;

/**
 * This class validates an Event Processor Network.
 * 
 */
public final class EPNValidator {
    
    /**
     * The default constructor.
     */
    private EPNValidator() {
    }

    /**
     * This method validates the supplied network and
     * returns results via a supplied listener.
     * 
     * @param epn The network to be validated
     * @param l The listener
     * @return Whether valid
     */
    public static boolean validate(Network epn, EPNValidationListener l) {
        boolean ret=true;
        
        if (!validateNetwork(epn, l)) {
            ret = false;
        }
        
        if (!validateSubscriptions(epn, l)) {
            ret = false;
        }
        
        if (!validateNodes(epn, l)) {
            ret = false;
        }
        
        return (ret);
    }
    
    /**
     * This method validates the network.
     * 
     * @param epn The network to be validated
     * @param l The listener
     * @return Whether valid
     */
    protected static boolean validateNetwork(Network epn, EPNValidationListener l) {
        boolean ret=true;
        
        if (epn.getName() == null) {
            l.error(epn, epn, MessageFormat.format(java.util.PropertyResourceBundle.getBundle(
                    "epn-core.Messages").getString("EPN-CORE-6"),
                    "Network", "name"));
            
            ret = false;
        }
        
        if (epn.getVersion() == null) {
            l.error(epn, epn, MessageFormat.format(java.util.PropertyResourceBundle.getBundle(
                    "epn-core.Messages").getString("EPN-CORE-6"),
                    "Network", "version"));
            
            ret = false;
        }
        
        return (ret);
    }
    
    /**
     * This method validates the network subscriptions.
     * 
     * @param epn The network to be validated
     * @param l The listener
     * @return Whether valid
     */
    protected static boolean validateSubscriptions(Network epn, EPNValidationListener l) {
        boolean ret=true;
        java.util.List<String> dups=new java.util.Vector<String>();
        
        for (Subscription sub : epn.getSubscriptions()) {
            
            if (sub.getSubject() == null) {
                l.error(epn, sub, MessageFormat.format(java.util.PropertyResourceBundle.getBundle(
                        "epn-core.Messages").getString("EPN-CORE-6"),
                        "Subscription", "nodeName"));
                ret = false;
            }
            
            if (sub.getNodeName() == null) {
                l.error(epn, sub, MessageFormat.format(java.util.PropertyResourceBundle.getBundle(
                        "epn-core.Messages").getString("EPN-CORE-6"),
                        "Subscription", "subject"));
                ret = false;
            }
            
            if (sub.getNodeName() != null && sub.getSubject() != null) {
                
                // Check for duplicate subject/nodeName pair
                String field=sub.getSubject()+":"+sub.getNodeName();
                
                if (dups.contains(field)) {
                    // Report error
                    l.error(epn, sub, MessageFormat.format(java.util.PropertyResourceBundle.getBundle(
                            "epn-core.Messages").getString("EPN-CORE-5"),
                            sub.getSubject(), sub.getNodeName()));
                    ret = false;
                } else {
                    dups.add(field);
                }
                
                // Check if specified node name is in network
                if (epn.getNode(sub.getNodeName()) == null) {
                    l.error(epn, sub, MessageFormat.format(java.util.PropertyResourceBundle.getBundle(
                            "epn-core.Messages").getString("EPN-CORE-7"),
                            sub.getNodeName(), sub.getSubject()));
                    ret = false;
                }
            }
        }
        
        return (ret);
    }
    
    /**
     * This method validates the network nodes.
     * 
     * @param epn The network to be validated
     * @param l The listener
     * @return Whether valid
     */
    protected static boolean validateNodes(Network epn, EPNValidationListener l) {
        boolean ret=true;
        
        for (Node node : epn.getNodes()) {
            
            if (node.getName() == null) {
                l.error(epn, node, MessageFormat.format(java.util.PropertyResourceBundle.getBundle(
                        "epn-core.Messages").getString("EPN-CORE-6"),
                        "Node", "name"));
                
                ret = false;
            } else if (epn.getNode(node.getName()) != node) {
                l.error(epn, node, MessageFormat.format(java.util.PropertyResourceBundle.getBundle(
                        "epn-core.Messages").getString("EPN-CORE-8"),
                        node.getName()));
                
                ret = false;
            }
            
            if (node.getEventProcessor() == null) {
                l.error(epn, node, MessageFormat.format(java.util.PropertyResourceBundle.getBundle(
                        "epn-core.Messages").getString("EPN-CORE-6"),
                        "Node", "eventProcessor"));
                
                ret = false;
            }
            
            if (node.getName() != null) {
                java.util.List<String> path=new java.util.Vector<String>();
                path.add(node.getName());
                
                for (String sourceNode : node.getSourceNodes()) {
                    
                    // Check that source node exists in network
                    if (epn.getNode(sourceNode) == null) {
                        l.error(epn, node, MessageFormat.format(java.util.PropertyResourceBundle.getBundle(
                                "epn-core.Messages").getString("EPN-CORE-9"),
                                sourceNode, node.getName()));
                        
                        ret = false;
                    }
                    
                    if (!checkCyclicDependency(epn, path, sourceNode, l)) {
                        ret = false;
                    }
                }
            }
            
            // Validate the notifications
            if (!validateNotifications(epn, node, l)) {
                ret = false;
            }
        }
        
        return (ret);
    }
    
    /**
     * This method determines whether there is a cyclic dependency.
     * 
     * @param epn The network
     * @param path The path to the node
     * @param node The source node being checked
     * @param l The listener
     * @return Whether valid
     */
    protected static boolean checkCyclicDependency(Network epn, java.util.List<String> path,
                    String node, EPNValidationListener l) {
        boolean ret=true;
        
        if (node.equals(path.get(0))) {
            // Cyclic dependency with the target node in the path
            String pathString="";
            
            for (String n : path) {
                pathString += n+"->";
            }
            
            pathString += node;
            
            l.error(epn, node, MessageFormat.format(java.util.PropertyResourceBundle.getBundle(
                    "epn-core.Messages").getString("EPN-CORE-10"),
                    pathString));
            
            ret = false;
            
        // Don't process further, if have found cyclic dependency, although
        // it does not involve the target node of interest
        } else if (!path.contains(node)) {
            Node sourceNode=epn.getNode(node);
            
            if (sourceNode != null) {
                path.add(node);
                
                for (String sn : sourceNode.getSourceNodes()) {
                    if (!checkCyclicDependency(epn, path, sn, l)) {
                        ret = false;
                    }
                }
                
                path.remove(node);
            }
        }
        
        return (ret);
    }
    
    /**
     * This method validates the notifications associated with the supplied network
     * and node.
     * 
     * @param epn The network
     * @param node The node
     * @param l The listener
     * @return Whether valid
     */
    protected static boolean validateNotifications(Network epn, Node node, EPNValidationListener l) {
        boolean ret=true;
        
        for (Notification no : node.getNotifications()) {
            if (no.getSubject() == null) {
                l.error(epn, no, MessageFormat.format(java.util.PropertyResourceBundle.getBundle(
                        "epn-core.Messages").getString("EPN-CORE-11"),
                        "Notification", "subject", node.getName()));
                ret = false;
            }
            if (no.getType() == null) {
                l.error(epn, no, MessageFormat.format(java.util.PropertyResourceBundle.getBundle(
                        "epn-core.Messages").getString("EPN-CORE-11"),
                        "Notification", "type", node.getName()));
                ret = false;
            }
        }
        
        return (ret);
    }
}

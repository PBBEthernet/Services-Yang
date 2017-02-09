package com.example.evp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import com.tailf.cdb.*;
import com.tailf.conf.*;
import com.tailf.dp.DpActionTrans;
import com.tailf.dp.DpCallbackException;
import com.tailf.dp.annotations.ActionCallback;
import com.tailf.dp.annotations.ServiceCallback;
import com.tailf.dp.proto.ActionCBType;
import com.tailf.dp.proto.ServiceCBType;
import com.tailf.dp.services.ServiceContext;
import com.tailf.dp.services.ServiceOperationType;
import com.tailf.dp.services.ServiceContext;
import com.tailf.maapi.Maapi;
import com.tailf.navu.NavuContainer;
import com.tailf.navu.NavuContext;
import com.tailf.navu.NavuList;
import com.tailf.navu.NavuNode;
import com.tailf.ncs.ns.Ncs;
import com.tailf.navu.NavuException;
import com.tailf.ncs.template.Template;
import com.tailf.ncs.template.TemplateVariables;
import com.example.evp.namespaces.*;
import com.tailf.ncs.annotations.*;

public class evpRFS {

 @Resource(type=ResourceType.CDB, scope=Scope.CONTEXT,
           qualifier="evpl-epl-service")
 private Cdb cdb;

private static Logger log = Logger.getLogger(evpRFS.class);

    /**
     * Create callback method for EVPL or EPL Service
     *
     * @param context - The current ServiceContext object
     * @param service - The NavuNode references the service node.
     * @param root    - This NavuNode references the ncs root.
     * @param opaque  - Parameter contains a Properties object.
     *                  This object may be used to transfer
     *                  additional information between consecutive
     *                  calls to the create callback.  It is always
     *                  null in the first call. I.e. when the service
     *                  is first created.
     * @return Properties the returning opaque instance
     * @throws DpCallbackException
     **/
    @ServiceCallback(servicePoint = "evpl-epl-service",
        callType = ServiceCBType.CREATE)
    public Properties create(ServiceContext context,
                             NavuNode service,
                             NavuNode root,
                             Properties opaque) throws ConfException {

      try {
      
         if (opaque == null) {
             opaque = new Properties();
         }
         NavuNode ev = service;

         // Read the service name EBD + isid
         String serviceName = ev.leaf("name").valueAsString();
         log.info("Activating service instance : " + serviceName);

         // Extract isid from service instance name
         String isid = serviceName.replace("EBD", "");

         String bridgeGroup = ev.leaf(evpl._bridge_group_).valueAsString();

         // Determine the connection type
         String conn = ev.leaf(evpl._connection_type_).valueAsString();
         log.info("Service instance (" + serviceName + ") type: " + conn 
                                      + " isid: " + isid); 
         
         // Determine the bandwidth requirement
         String bandwidth = ev.leaf(evpl._bandwidth_).valueAsString();
     
         // Determine the service description
         String serviceDescription = ev.leaf(evpl._description_).valueAsString();
         
         ////
         // Apply the template for each configured endpoint
         ////
         for (NavuContainer enp : ev.list(evpl._uni_endpoints_).elements()) {         
           try {
             log.info("Service Apply template for device:" + enp.leaf(evpl._device_).valueAsString());
             TemplateVariables vars = new TemplateVariables();
             
             // Create new template instance
             Template template = new Template(context, "pbb-edge");

             String cvlan = "";
             String portType = enp.leaf(evpl._port_type_).valueAsString();
             if (portType.equals("customer-vlan")) {
                cvlan = enp.leaf(evpl._cvlan_).valueAsString();
             }

             String svlan = "";
             if (portType.equals("service-vlan")) {
                svlan = enp.leaf(evpl._svlan_).valueAsString();
             }
             String operation = enp.leaf(evpl._operation_).valueAsString();
             String ovlan = enp.leaf(evpl._operation_vlan_).valueAsString();
                      
             System.out.println("Operation:" + operation + " vlan:" + ovlan);

             // Create the template variables
             vars.putQuoted("sDescribe", serviceDescription);
             vars.putQuoted("BW", bandwidth);
             vars.putQuoted("OPER", operation);
             vars.putQuoted("OLAN", ovlan);
             vars.putQuoted("CONN", conn);
             vars.putQuoted("EBG", bridgeGroup);
             vars.putQuoted("CBB", enp.leaf(evpl._bvid_).valueAsString());
             vars.putQuoted("NAME", serviceName);
             vars.putQuoted("ISID", isid);
             vars.putQuoted("PTYPE", portType);
             vars.putQuoted("VLAN", enp.leaf(evpl._ingress_vlan_).valueAsString());
             vars.putQuoted("CVLAN", cvlan);
             vars.putQuoted("SVLAN", svlan);
             vars.putQuoted("DEVICE", enp.leaf(evpl._device_).valueAsString());
             vars.putQuoted("MAC", enp.leaf(evpl._mac_address_).valueAsString());
             vars.putQuoted("INTF", enp.leaf(evpl._interface_)
                                       .valueAsString()
                                       .replaceAll("\\s", ""));

             // Apply the template
             template.apply(service, vars);

           } catch (ConfException e) {
             throw new DpCallbackException(e.getMessage(), e);
           }
         }  
      }
      catch (Exception e) {
          log.error("RFS Exception : ", e);
      }
      return opaque;
    }

    @ActionCallback(callPoint="evpl-epl-service", callType=ActionCBType.INIT)
    public void init(DpActionTrans trans) throws DpCallbackException {
    }

    @ServiceCallback(servicePoint = "evpl-epl-service",
        callType = ServiceCBType.PRE_MODIFICATION)
     public Properties preModification(ServiceContext context,
                        ServiceOperationType operation,
                        ConfPath path,
                        Properties opaque) 
              throws DpCallbackException {

        return opaque;
     }
}

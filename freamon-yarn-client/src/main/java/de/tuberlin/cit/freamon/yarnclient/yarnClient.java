package de.tuberlin.cit.freamon.yarnclient;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.api.records.*;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class yarnClient {
    public YarnClient yarnClient;
    private ArrayList<ApplicationId> runningApplications = new ArrayList<ApplicationId>();

    public yarnClient() {
        if (System.getenv("HADOOP_PREFIX") != null) {
            initYarnClient(System.getenv("HADOOP_PREFIX") + "/etc/hadoop/yarn-site.xml");
        } else if (System.getenv("YARN_CONF_DIR") != null) {
            initYarnClient(System.getenv("YARN_CONF_DIR") + "/yarn-site.xml");
        } else {
            throw new IllegalArgumentException("Neither HADOOP_PREFIX nor YARN_CONF_DIR are set in the environment");
        }
    }

    private void initYarnClient(String yarnSitePath) {
        Configuration conf = new YarnConfiguration();
        try {
            conf.addResource(new FileInputStream(yarnSitePath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("Starting Yarn Client for " + conf.get(YarnConfiguration.RM_HOSTNAME));
        yarnClient = YarnClient.createYarnClient();
        yarnClient.init(conf);
        yarnClient.start();
    }

    public ApplicationId evalYarnForNewApp() {
        try {
            for (ApplicationReport applicationReport : yarnClient.getApplications()) {
                final YarnApplicationState applicationState = applicationReport.getYarnApplicationState();
                final ApplicationId applicationId = applicationReport.getApplicationId();
                int runningContainers = applicationReport.getApplicationResourceUsageReport().getNumUsedContainers();
//                int reservedContainers = applicationReport.getApplicationResourceUsageReport().getNumReservedContainers();
//                long virtualCores = applicationReport.getApplicationResourceUsageReport().getReservedResources().getVirtualCores();
                //TODO runningContainers>1 is not a good solution
                if (applicationState == YarnApplicationState.RUNNING && !runningApplications.contains(applicationId) && runningContainers > 1) {
                    runningApplications.add(applicationId);
                    System.out.println("Yarn Client: New Application with ID: " + applicationId);
                    return applicationId;
                }
            }
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());

        } catch (YarnException e) {
            System.err.println("YarnException: " + e.getMessage());
        }
        return null;
    }

    public ApplicationId evalYarnForReleasedApp() {
        try {
            for (ApplicationReport applicationReport : yarnClient.getApplications()) {
                final YarnApplicationState applicationState = applicationReport.getYarnApplicationState();
                final ApplicationId applicationId = applicationReport.getApplicationId();
                if (applicationState != YarnApplicationState.RUNNING && runningApplications.contains(applicationId)) {
                    runningApplications.remove(applicationId);
                    System.out.println("Yarn Client: Finished Application with ID " + applicationId);
                    return applicationId;
                }
            }
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());

        } catch (YarnException e) {
            System.err.println("YarnException: " + e.getMessage());
        }
        return null;
    }

    public long[] getApplicationContainerIds(ApplicationId applicationId) {
        try {
            //TODO assume first application attempt always works
            List<ApplicationAttemptReport> applicationAttemptReports = yarnClient.getApplicationAttempts(applicationId);
            if (applicationAttemptReports.size() == 1) {
                ApplicationAttemptId applicationAttemptid = applicationAttemptReports.get(0).getApplicationAttemptId();
                final List<ContainerReport> containerReportList = yarnClient.getContainers(applicationAttemptid);
                long containerIds[] = new long[containerReportList.size()];
                System.out.print("Yarn Client: Found " + containerIds.length + " with IDs: (");
                for (int i = 0; i < containerIds.length; i++) {
                    //TODO Covers also the AM
                    //containerIds[i] = "container_" + applicationId.getClusterTimestamp() + "_" + String.format("%04d", applicationId.getId()) + "_01_" + String.format("%06d", containerReportList.get(i).getContainerId().getContainerId());
                    containerIds[i] = containerReportList.get(i).getContainerId().getContainerId();
                    System.out.print(containerIds[i] + ", ");
                }
                System.out.println(")");
                return containerIds;
            } else {
                System.err.println("Yarn Client: ERROR - could not fetch containerIds, because of too many application attempts ");
            }
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
        } catch (YarnException e) {
            System.err.println("YarnException: " + e.getMessage());
        }
        return null;
    }
}

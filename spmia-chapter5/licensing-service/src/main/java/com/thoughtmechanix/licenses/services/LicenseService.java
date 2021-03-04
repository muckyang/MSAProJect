package com.thoughtmechanix.licenses.services;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import com.thoughtmechanix.licenses.clients.OrganizationRestTemplateClient;
import com.thoughtmechanix.licenses.config.ServiceConfig;
import com.thoughtmechanix.licenses.model.License;
import com.thoughtmechanix.licenses.model.Organization;
import com.thoughtmechanix.licenses.repository.LicenseRepository;
import com.thoughtmechanix.licenses.utils.UserContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
public class LicenseService {
    private static final Logger logger = LoggerFactory.getLogger(LicenseService.class);
    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    ServiceConfig config;

    @Autowired
    OrganizationRestTemplateClient organizationRestClient;

    public License getLicense(String organizationId, String licenseId) {
        License license = licenseRepository.findByOrganizationIdAndLicenseId(organizationId, licenseId);

        Organization org = getOrganization(organizationId);

        return license.withOrganizationName(org.getName()).withContactName(org.getContactName())
                .withContactEmail(org.getContactEmail()).withContactPhone(org.getContactPhone())
                .withComment(config.getExampleProperty());
    }

    @HystrixCommand
    private Organization getOrganization(String organizationId) {
        return organizationRestClient.getOrganization(organizationId);
    }

    private void randomlyRunLong() {
        Random rand = new Random();

        int randomNum = rand.nextInt((100 - 1) + 1) + 1;

        if (randomNum < 30) {// 값 변경에 따라 timeout 비율보다 크게 설정하면 모든 요청이 막힘...
            logger.debug("Sleep!!!!");
            sleep();
        }
    }

    private void sleep() {
        try {
            Thread.sleep(11000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @HystrixCommand(

            // 폴백 메소드 설정
            fallbackMethod = "buildFallbackLicenseList", 
            //스레드 풀 관련 설정 
            threadPoolKey = "licenseByOrgThreadPool", threadPoolProperties = {
                    @HystrixProperty(name = "coreSize", value = "30"),
                    @HystrixProperty(name = "maxQueueSize", value = "10") },

            // 타임아웃 제한을 12초로 설정, 아래 randomlyRunLong 에서 11초 지연했으므로
            // 12초로 설정하면 500에러 발생하지않고 실행됨!
            // @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds",
                    // value = "10000"),

            commandProperties = {
                    
                    @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value = "10"),
                    @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage", value = "50"),
                    @HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds", value = "7000"),
                    @HystrixProperty(name = "metrics.rollingStats.timeInMilliseconds", value = "15000"),
                    @HystrixProperty(name = "metrics.rollingStats.numBuckets", value = "5") })

    public List<License> getLicensesByOrg(String organizationId) {
        logger.debug("LicenseService.getLicensesByOrg  Correlation id: {}",
                UserContextHolder.getContext().getCorrelationId());
        randomlyRunLong();

        return licenseRepository.findByOrganizationId(organizationId);
    }

    // 폴백 메소드
    private List<License> buildFallbackLicenseList(String organizationId) {
        List<License> fallbackList = new ArrayList<>();
        License license = new License().withId("0000000-00-00000").withOrganizationId(organizationId)
                .withProductName("Sorry no licensing information currently available");

        fallbackList.add(license);
        return fallbackList;
    }

    public void saveLicense(License license) {
        license.withId(UUID.randomUUID().toString());

        licenseRepository.save(license);
    }

    public void updateLicense(License license) {
        licenseRepository.save(license);
    }

    public void deleteLicense(License license) {
        licenseRepository.delete(license);
    }

}

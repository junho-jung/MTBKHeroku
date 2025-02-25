package com.heroku.java.Interface;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heroku.java.Config.HeaderTypeList;
import com.heroku.java.Config.SFDCTokenManager;
import com.heroku.java.DTO.FetchTemplateRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api")
public class SFDCInOutInterface {
    private static final Logger logger = LogManager.getLogger(SFDCInOutInterface.class);

    private static final List<String> KAKAO_WHITE_LIST = Arrays.asList(
        Optional.ofNullable(System.getenv("KAKAO_WHITE_LIST")).orElse("0").split(",")
    );

    @Autowired
    private SFDCTokenManager tokenManager;

    private static final String URL_APEXREST = "apexrest";
    private static final String URL_API = "api";
    private static final String URL_SAP = "sap";
    private static final String URL_ASYNC = "async";

    private static final String PATH_ES010 = "sms010";

    @Autowired
    @Qualifier("defaultRestTemplate")
    private RestTemplate restTemplate;

    @PostMapping("/kakao/alim")
    public Map<String, Object> kakaoAlim(@RequestHeader(value="X-API-KEY", required = true) String apiKey, @RequestBody String jsonString) throws Exception {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put("code", true);
        resultMap.put("message", "Great. you\'ve got " + ((int) (Math.random() * 100)) + " points");

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonString);
        JsonNode destinations = jsonNode.path("messages").get(0).path("destinations");
        /* 
        // WHITE LIST ON_OFF
        for (JsonNode destinationNode : destinations) {
            String destination = destinationNode.path("to").asText();
            if (!KAKAO_WHITE_LIST.contains(destination)) {
                resultMap.put("code", false);
                resultMap.put("message", "The destination number " + destination + " is not in the white list.");
                return resultMap;
            }
        }
        */

        String infobipURL = System.getenv("INFOBIP_URL");
        String infobipAPIKey = System.getenv("INFOBIP_KEY");

        // Header
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", HeaderTypeList.APPLICATION_JSON);
        headers.set("Content-Type", HeaderTypeList.APPLICATION_JSON);
        headers.set("Authorization", infobipAPIKey);
        
        resultMap.put("requestBody", jsonString);
        HttpEntity<String> requestEntity = new HttpEntity<>(jsonString, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                infobipURL, 
                HttpMethod.POST, 
                requestEntity, 
                String.class
            );
            
            resultMap.put("status_code", response.getStatusCode().value());
            resultMap.put("message", response.getBody());
        
            logger.info("#############################################");
            logger.info("SUCCESS. KAKAO API Call, {}", response.getBody());
            logger.info("#############################################");
        } catch (HttpClientErrorException e) {
            // 일반적인 HTTP 에러 처리
            resultMap.put("code", false);
            resultMap.put("status_code", e.getStatusCode().value());
            resultMap.put("message", e.getResponseBodyAsString());
        
            logger.error("#############################################");
            logger.error("Error. Request {}. SFDC: {}", jsonString, e.getResponseBodyAsString());
            logger.error("#############################################");
        } catch (Exception e) {
            // 예외 처리
            resultMap.put("code", false);
            resultMap.put("message", e.getMessage());
        
            logger.error("#############################################");
            logger.error("Fail. KAKAO API Call, {}", e.getMessage());
            logger.error("#############################################");
        }

        return resultMap;
    }

    @GetMapping("/wsmoka/template")
    public Map<String, Object> fetchTemplate(@RequestHeader(value="X-API-KEY", required = true) String apiKey, @ModelAttribute FetchTemplateRequest request) throws Exception {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put("code", true);
        resultMap.put("message", "Great. you\'ve got " + ((int) (Math.random() * 100)) + " points");
        logger.info(request);

        // URL
        String WSMokaURL = System.getenv("WS_MOKA_URL");
        UriComponentsBuilder URIBuilder = UriComponentsBuilder.fromHttpUrl(WSMokaURL)
            .queryParam("senderKey", request.getSenderKey())
            .queryParam("since", request.getSince());

        // Header
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", HeaderTypeList.FORM_URLENCODE);

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        logger.info(URIBuilder.toUriString());
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                URIBuilder.toUriString(), 
                HttpMethod.GET, 
                requestEntity, 
                String.class
            );
            
            resultMap.put("status_code", response.getStatusCode().value());
            resultMap.put("message", response.getBody());
        
            logger.info("#############################################");
            logger.info("SUCCESS. WSMOKA API Call, {}", response.getBody());
            logger.info("#############################################");
        } catch (HttpClientErrorException e) {
            // 일반적인 HTTP 에러 처리
            resultMap.put("code", false);
            resultMap.put("status_code", e.getStatusCode().value());
            resultMap.put("message", e.getResponseBodyAsString());
        
            logger.error("#############################################");
            logger.error("Error. Request {}. SFDC: {}", request, e.getResponseBodyAsString());
            logger.error("#############################################");
        } catch (Exception e) {
            // 예외 처리
            resultMap.put("code", false);
            resultMap.put("message", e.getMessage());
        
            logger.error("#############################################");
            logger.error("Fail. WSMOKA API Call, {}", e.getMessage());
            logger.error("#############################################");
        }

        return resultMap;
    }

    @PostMapping("/pages")
    public Integer getPageNumber(@RequestHeader(value="X-API-KEY", required = true) String apiKey, @RequestBody String file) throws UnsupportedEncodingException {
        file = file.replaceFirst("file=", "");
        byte[] pdfData = Base64.getDecoder().decode(file);
        
        // PDF 데이터를 메모리로 읽기
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfData))) {

            // 페이지 수 계산
            return document.getNumberOfPages();
        } catch (IOException e) {
            e.printStackTrace();

            return 0;
        }
    }
    
    @PostMapping("/sap/async/sms010")
    @SuppressWarnings("unchecked")
    public Map<String, Object> asyncSMS010(@RequestHeader(value="X-API-KEY", required = true) String apiKey, @RequestBody String jsonString) throws Exception {
        logger.info("\n{}", jsonString);
        
        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put("code", true);
        resultMap.put("message", "Great. you\'ve got " + ((int) (Math.random() * 100)) + " points");

        Map<String, Object> jsonMap = InterfaceCommon.extractJSON(jsonString);
        List<String> idList = (List<String>) jsonMap.get("idList");
        String parseString = (String) jsonMap.get("parseString");
        
        // URL
        String SAP_URL = System.getenv("SAP_URL");
        UriComponentsBuilder URIBuilderSAP = UriComponentsBuilder.fromHttpUrl(SAP_URL)
            .pathSegment(PATH_ES010);
            
        // Header
        HttpHeaders headers = InterfaceCommon.makeHeadersSAP();
        // Request Info
        HttpEntity<String> requestEntity = new HttpEntity<>(parseString, headers);

        // URL
        String SFDCURL = Optional.ofNullable(System.getenv("SFDC_URL"))
            .orElse("https://app-force-1035--partial.sandbox.my.salesforce.com/services");
        UriComponentsBuilder URIBuilderSFDC = UriComponentsBuilder.fromHttpUrl(SFDCURL)
            .pathSegment(URL_APEXREST)
            .pathSegment(URL_API)
            .pathSegment(URL_SAP)
            .pathSegment(URL_ASYNC)
            .pathSegment(PATH_ES010);

        this.AsyncDoCallOutSAP(String.class, URIBuilderSAP, URIBuilderSFDC, requestEntity, idList);
        return resultMap;
    }

    @Async
    private <T> void AsyncDoCallOutSAP(Object responseType, UriComponentsBuilder URIBuilderSAP, UriComponentsBuilder URIBuilderSFDC, HttpEntity<T> requestEntity, Object returnObj) {
        logger.info("#############################################");
        logger.info("Endpoint URL. {}", URIBuilderSAP.toUriString());
        logger.info("#############################################");

        ResponseEntity<T> responseEntity = null;
        T responseBody = null;
        try {
            // 1. SAP API CALL
            responseEntity = restTemplate.exchange(
                URIBuilderSAP.toUriString(),
                HttpMethod.POST,
                requestEntity,
                InterfaceCommon.getResponseType(responseType)
            );
            responseBody = responseEntity.getBody();
            if(returnObj != null) responseBody = InterfaceCommon.parseJSONNode(responseBody, returnObj);

            logger.info("#############################################");
            logger.info("SUCCESS. Request {}. SAP: {}", requestEntity.getBody(), responseBody);
            logger.info("#############################################");

            // 2. SFDC API CALL
            // Header
            String token = tokenManager.getApiToken();
            HttpHeaders headers = InterfaceCommon.makeHeadersSFDC(token);
            // Request Info
            HttpEntity<T> requestEntitySFDC = new HttpEntity<>(responseBody, headers);
            ResponseEntity<T> responseEntitySFDC = restTemplate.exchange(
                URIBuilderSFDC.toUriString(),
                HttpMethod.POST,
                requestEntitySFDC,
                InterfaceCommon.getResponseType(responseType)
            );

            logger.info("#############################################");
            logger.info("SUCCESS. Request {}. SFDC: {}", requestEntitySFDC.getBody(), responseEntitySFDC.getBody());
            logger.info("#############################################");
        } catch (HttpClientErrorException.Unauthorized e) {
            // SAP는 성공. SFDC가 에러날 경우
            // Unauthorized 예외 처리: 토큰 갱신 후 재시도
            tokenManager.fetchNewToken();

            String token = tokenManager.getApiToken();
            HttpHeaders newHeaders = InterfaceCommon.makeHeadersSFDC(token);
            HttpEntity<T> newRequestEntitySFDC = new HttpEntity<>(responseBody, newHeaders);
    
            ResponseEntity<T> newResponseEntitySFDC = restTemplate.exchange(
                URIBuilderSFDC.toUriString(),
                HttpMethod.POST,
                newRequestEntitySFDC,
                InterfaceCommon.getResponseType(responseType)
            );

            logger.info("#############################################");
            logger.info("SUCCESS(Refresh Token). Request {}. SFDC: {}", newRequestEntitySFDC.getBody(), newResponseEntitySFDC.getBody());
            logger.info("#############################################");
        } catch (HttpClientErrorException e) {
            // HTTP 에러 처리
        
            logger.error("#############################################");
            logger.error("Error. Request {}. SAP: {}", requestEntity.getBody(), e.getResponseBodyAsString());
            logger.error("#############################################");
        } catch (Exception e) {

            logger.error("#############################################");
            logger.error("Fail. Request {}. Heroku: {}", requestEntity.getBody(), e.getMessage());
            logger.error("#############################################");
        }
    }
}

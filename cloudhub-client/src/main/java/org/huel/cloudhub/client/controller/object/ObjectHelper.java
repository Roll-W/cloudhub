package org.huel.cloudhub.client.controller.object;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.huel.cloudhub.client.data.dto.object.ObjectInfo;
import org.huel.cloudhub.client.data.dto.object.ObjectInfoDto;
import org.huel.cloudhub.client.data.dto.object.ObjectInfoVo;
import org.huel.cloudhub.client.event.object.ObjectGetRequestEvent;
import org.huel.cloudhub.client.event.object.ObjectPutRequestEvent;
import org.huel.cloudhub.client.service.object.ObjectMetadataHeaders;
import org.huel.cloudhub.client.service.object.ObjectMetadataService;
import org.huel.cloudhub.client.service.object.ObjectRuntimeException;
import org.huel.cloudhub.client.service.object.ObjectService;
import org.huel.cloudhub.common.ErrorCode;
import org.huel.cloudhub.common.HttpResponseEntity;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpRange;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author RollW
 */
public final class ObjectHelper {
    private static final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public static String readPath(HttpServletRequest request) {
        final String path =
                request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE).toString();
        final String bestMatchingPattern =
                request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE).toString();

        String arguments = antPathMatcher.extractPathWithinPattern(bestMatchingPattern, path);

        if (!arguments.isEmpty()) {
            return "/" + arguments;
        }
        return "";
    }

    public static Map<String, String> buildInitialMetadata(MultipartFile file) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(ObjectMetadataHeaders.CONTENT_TYPE, file.getContentType());
        metadata.put(ObjectMetadataHeaders.OBJECT_LENGTH, "" + file.getSize());
        return metadata;
    }


    public static List<HttpRange> tryGetsRange(HttpServletRequest request) {
        String range = request.getHeader("Range");
        if (range == null) {
            return null;
        }
        return HttpRange.parseRanges(range);
    }

    @Nullable
    public static HttpResponseEntity<String> processGetObject(HttpServletRequest request, HttpServletResponse response, @PathVariable("bucketName") String bucketName, String objectName, ObjectInfo objectInfo, ObjectMetadataService objectMetadataService, ApplicationEventPublisher applicationEventPublisher, ObjectService objectService) throws IOException {
        response.setHeader("Content-Type",
                "application/octet-stream");
        Map<String, String> metadata = objectMetadataService
                .getObjectMetadata(bucketName, objectName);
        if (metadata != null) {
            metadata.forEach(response::setHeader);
        }
        List<HttpRange> ranges = ObjectHelper.tryGetsRange(request);
        response.setHeader("Accept-Ranges", "bytes");
        applicationEventPublisher.publishEvent(
                new ObjectGetRequestEvent(objectInfo));
        if (ranges != null && !ranges.isEmpty()) {
            HttpRange range = ranges.get(0);
            ObjectInfoDto objectInfoDto = objectService.getObjectInBucket(bucketName, objectName);
            if (objectInfoDto == null) {
                throw new ObjectRuntimeException(ErrorCode.ERROR_DATA_NOT_EXIST,
                        "Object not exist");
            }
            long len = objectInfoDto.objectSize();
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setHeader("Content-Disposition", "inline;filename=" + objectName);
            response.setHeader("Content-Range",
                    "bytes " + range.getRangeStart(len) + "-" + range.getRangeEnd(len) + "/" +
                            objectInfoDto.objectSize());
            objectService.getObjectData(objectInfo, response.getOutputStream(),
                    range.getRangeStart(len), range.getRangeEnd(len));
            return null;
        }
        objectService.getObjectData(objectInfo, response.getOutputStream());
        return null;
    }

    public static HttpResponseEntity<ObjectInfoVo> processObjectUpload(HttpServletRequest request, @PathVariable("bucketName") String bucketName, @RequestPart(name = "object") MultipartFile objectFile, ObjectService objectService, ObjectMetadataService objectMetadataService, ApplicationEventPublisher applicationEventPublisher) throws IOException {
        Map<String, String> metadata = ObjectHelper.buildInitialMetadata(objectFile);

        String objectName = ObjectHelper.readPath(request);
        ObjectInfo objectInfo = new ObjectInfo(objectName, bucketName);
        var res =
                objectService.saveObject(objectInfo, objectFile.getInputStream());
        if (res.errorCode().getState()) {
            objectMetadataService.addObjectMetadata(
                    bucketName, objectName, metadata);
            applicationEventPublisher.publishEvent(
                    new ObjectPutRequestEvent(objectInfo));
        }

        return HttpResponseEntity.create(
                res.toResponseBody(ObjectInfoVo::from));
    }


    private ObjectHelper() {
    }
}

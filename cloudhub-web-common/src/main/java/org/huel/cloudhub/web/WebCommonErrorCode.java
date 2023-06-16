package org.huel.cloudhub.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import space.lingu.NonNull;

import java.util.List;

/**
 * @author RollW
 */
public enum WebCommonErrorCode implements ErrorCode, ErrorCodeFinder, ErrorCodeMessageProvider {
    /**
     * Request parameter missing.
     */
    ERROR_PARAM_MISSING("A0201", 400),

    /**
     * Request parameter validate failed.
     */
    ERROR_PARAM_FAILED("A0202", 400),

    /**
     * Request body missing.
     */
    ERROR_BODY_MISSING("A0203", 400),

    /**
     * Not support method or parameter.
     */
    ERROR_NOT_SUPPORT("A0204", 400),

    ERROR_HTTP_REQUEST("A0205", HttpStatus.BAD_REQUEST.value()),
    ERROR_METHOD_NOT_ALLOWED("A0206", HttpStatus.METHOD_NOT_ALLOWED.value()),
    ;

    private final String value;
    private final int status;

    WebCommonErrorCode(String value, int status) {
        this.value = value;
        this.status = status;
    }

    @Override
    public String toString() {
        return "WebCommonError: %s, code: %s".formatted(name(), getCode());
    }

    @NonNull
    @Override
    public String getCode() {
        return value;
    }

    @NonNull
    @Override
    public String getName() {
        return name();
    }

    @Override
    public boolean success() {
        return false;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public ErrorCode fromThrowable(Throwable e, ErrorCode defaultErrorCode) {
        if (e instanceof BusinessRuntimeException sys) {
            return sys.getErrorCode();
        }
        if (e instanceof MissingServletRequestParameterException) {
            return ERROR_PARAM_MISSING;
        }

        return null;
    }

    @Override
    public ErrorCode findErrorCode(String codeValue) {
        return ErrorCodeFinder.from(values(), codeValue);
    }

    private static final List<ErrorCode> CODES = List.of(values());

    @Override
    public List<ErrorCode> listErrorCodes() {
        return CODES;
    }

    public static ErrorCodeFinder getFinderInstance() {
        return ERROR_PARAM_MISSING;
    }
}

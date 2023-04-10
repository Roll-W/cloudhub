package org.huel.cloudhub.web;


import org.huel.cloudhub.web.data.page.Page;

import java.util.List;

/**
 * @author RollW
 */
@SuppressWarnings("unchecked")
final class PageableHttpResponseBody<D> extends HttpResponseBody<List<D>> {
    private int page;
    private int size;
    private int total;

    private static final PageableHttpResponseBody<?> SUCCESS = new PageableHttpResponseBody<>(
            CommonErrorCode.SUCCESS,
            200,
            "OK"
    );

    public PageableHttpResponseBody(ErrorCode errorCode,
                                    int status,
                                    String message) {
        super(errorCode, status, message);
    }

    public PageableHttpResponseBody(ErrorCode errorCode,
                                    int status,
                                    String message,
                                    Page<D> page) {
        super(errorCode, status, message, page.getData());
        this.page = page.getPage();
        this.size = page.getSize();
        this.total = page.getTotal();

    }

    public PageableHttpResponseBody(ErrorCode errorCode,
                                    int status,
                                    String message,
                                    String tip,
                                    Page<D> page) {
        super(errorCode, status, message, tip, page.getData());
        this.page = page.getPage();
        this.size = page.getSize();
        this.total = page.getTotal();
    }

    private PageableHttpResponseBody(HttpResponseBody<List<D>> body,
                                     int page, int size, int total) {
        super(body.getErrorCode(), body.getStatus(), body.getMessage(),
                body.getTip(), body.getData());
        this.page = page;
        this.size = size;
        this.total = total;
    }

    private PageableHttpResponseBody(ErrorCode errorCode,
                                     int status,
                                     String message,
                                     String tip,
                                     List<D> data,
                                     int page,
                                     int size,
                                     int total) {
        super(errorCode, status, message, tip, data);
        this.page = page;
        this.size = size;
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public int getTotal() {
        return total;
    }

    private PageableHttpResponseBody<D> setPage(int page) {
        this.page = page;
        return this;
    }

    private PageableHttpResponseBody<D> setSize(int size) {
        this.size = size;
        return this;
    }

    private PageableHttpResponseBody<D> setTotal(int total) {
        this.total = total;
        return this;
    }

    @Override
    public PageableHttpResponseBody<D> fork() {
        return new PageableHttpResponseBody<>(
                errorCode, status,
                message, tip, data,
                page, size,
                total);
    }

    @Override
    public PageableHttpResponseBody<D> fork(String tip) {
        return new PageableHttpResponseBody<>(
                errorCode, status, message,
                tip,
                data, page, size, total);
    }

    @Override
    public PageableHttpResponseBody<D> fork(String tip, int status) {
        return new PageableHttpResponseBody<>(
                errorCode, status, message,
                tip,
                data, page, size, total);
    }

    @Override
    public PageableHttpResponseBody<D> fork(String message, String tip) {
        return new PageableHttpResponseBody<>(
                errorCode, status, message,
                tip,
                data, page, size, total);
    }

    private static <D> PageableHttpResponseBody<D> transSuccess() {
        return (PageableHttpResponseBody<D>) SUCCESS;
    }

    public static <D> PageableHttpResponseBody<D> success(Page<D> page) {
        PageableHttpResponseBody<D> body = (PageableHttpResponseBody<D>)
                transSuccess().fork()
                .setPage(page.getPage())
                .setSize(page.getSize())
                .setTotal(page.getTotal());
        body.setData(page.getData());
        return body;
    }

    public static <D> PageableHttpResponseBody<D> of(ErrorCode errorCode,
                                                     Page<D> page) {
        return new PageableHttpResponseBody<>(errorCode, errorCode.getStatus(),
                null, page);
    }
}

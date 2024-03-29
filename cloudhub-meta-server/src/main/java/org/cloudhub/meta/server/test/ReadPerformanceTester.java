/*
 * Cloudhub - A high available, scalable distributed file system.
 * Copyright (C) 2022 Cloudhub
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.cloudhub.meta.server.test;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudhub.file.rpc.block.DownloadBlockResponse;
import org.cloudhub.meta.server.service.file.FileDownloadService;
import org.cloudhub.meta.server.service.file.FileUploadService;
import org.cloudhub.file.rpc.block.DownloadBlockResponse;
import org.cloudhub.meta.fs.FileObjectUploadStatus;
import org.cloudhub.meta.server.service.file.FileDeleteService;
import org.cloudhub.meta.server.service.file.FileDownloadCallback;
import org.cloudhub.meta.server.service.file.FileDownloadService;
import org.cloudhub.meta.server.service.file.FileDownloadingException;
import org.cloudhub.meta.server.service.file.FileUploadService;
import org.cloudhub.meta.server.service.file.FileUploadStatusDataCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * 读取性能测试
 *
 * @author RollW
 */
@SuppressWarnings("all")
public class ReadPerformanceTester {
    private final PrintWriter writer;
    private final int testCases;
    private final int dataSize;
    private final FileDownloadService fileDownloadService;
    private final FileUploadService fileUploadService;
    private final FileDeleteService fileDeleteService;
    private final File tempFile;
    private String fileId;
    private final LocalDateTime startTime = LocalDateTime.now();
    private final List<Result> results = new ArrayList<>();

    private final Logger logger = LoggerFactory.getLogger(ReadPerformanceTester.class);

    // if file delete service not null, enable auto-cleaning.
    public ReadPerformanceTester(PrintWriter reportWriter, int testCases, int dataSize,
                                 FileDownloadService fileDownloadService,
                                 FileUploadService fileUploadService,
                                 @Nullable FileDeleteService fileDeleteService) {
        this.testCases = testCases;
        this.dataSize = dataSize;
        this.fileDownloadService = fileDownloadService;
        this.fileUploadService = fileUploadService;
        this.fileDeleteService = fileDeleteService;
        this.writer = reportWriter;
        this.tempFile = new File(RandomStringUtils.randomAlphanumeric(30));
    }

    private void initialFile() throws IOException {
        tempFile.createNewFile();
        RandomFileGenerator generator =
                new RandomFileGenerator(tempFile, dataSize, 0);
        generator.generate();
        CountDownLatch latch = new CountDownLatch(1);
        InputStream inputStream = new FileInputStream(tempFile);
        fileUploadService.uploadFile(inputStream, new FileUploadStatusDataCallback() {
            @Override
            public void onNextStatus(FileObjectUploadStatus status) {
                if (!status.isLastStatus()) {
                    return;
                }
                latch.countDown();
            }

            @Override
            public void onCalc(String fileId) {
                ReadPerformanceTester.this.fileId = fileId;
            }
        });
        try {
            latch.await();
            IOUtils.closeQuietly(inputStream);
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    public void startReadTest() throws IOException {
        initialFile();
        for (int i = 1; i <= testCases; i++) {
            logger.info("Now reading file index={}", i);
            recordNext(i);
        }
        printResults();
        clean();
    }

    private void recordNext(int index) {
        CountDownLatch latch = new CountDownLatch(1);
        long start = System.currentTimeMillis();
        fileDownloadService.downloadFile(NullOutputStream.INSTANCE, fileId,
                new Callback(latch, index, start));
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private class Callback implements FileDownloadCallback {
        private final CountDownLatch latch;
        private final int index;
        private final long start;
        private long end, find;

        private Callback(CountDownLatch latch, int index, long start) {
            this.latch = latch;
            this.index = index;
            this.start = start;
        }

        @Override
        public void onDownloadComplete() {
            end = System.currentTimeMillis();
            results.add(new Result(index, start, end, find, false));
            latch.countDown();
        }

        @Override
        public void onDownloadError(FileDownloadingException e) {
            results.add(new Result(index, start, end, find, true));
            latch.countDown();
        }

        @Override
        public void onSaveCheckMessage(DownloadBlockResponse.CheckMessage checkMessage) {
            find = System.currentTimeMillis();
        }
    }

    private void clean() {
        tempFile.delete();
        if (fileDeleteService != null) {
            fileDeleteService.deleteFileCompletely(fileId);
        }
    }

    private void printResults() {
        printHeader();
        long netSum = 0, totalSum = 0, findSum = 0;
        for (Result result : results) {
            long downloadTime = result.end() - result.find();
            long findTime = result.find() - result.start();
            long totalTime = result.end() - result.start();
            netSum += downloadTime;
            totalSum += totalTime;
            findSum += findTime;

            double downloadRate = dataSize / (downloadTime / 1000d);
            double totalRate = dataSize / (totalTime / 1000d);
            printCase(result.index(), result.start(), findTime, downloadTime, totalTime,
                    downloadRate, totalRate, result.error());
        }
        printSummary(totalSum, netSum, findSum);
        printFooter();
    }

    private void printHeader() {
        writer.println("Start generate read performance report at " + startTime + ".");
        writer.println();
        writer.printf("Result table. Cases=%d, DataSize=%d(MB)\n", testCases, dataSize);
        writer.println("=========================================");
        writer.println("case  start(timestamp)  find(ms)  download(ms)  total(ms)  download(MB/s)  total(MB/s)  error");
        writer.flush();
    }

    private void printCase(int index, long start, long find, long download, long total,
                           double downloadRate, double totalRate, boolean error) {
        writer.printf("%4d  %16d  %8d  %12d  %9d  %14.2f  %11.2f  %5b\n",
                index, start, find, download, total, downloadRate, totalRate, error);
        writer.flush();
    }

    private void printSummary(long total, long totalDownload, long totalFind) {
        double avaTotal = (total * 1.d) / testCases;
        double avaNet = (totalDownload * 1.d) / testCases;
        double avaFind = (totalFind * 1.d) / testCases;
        double avaDownloadRate = (testCases * dataSize) / (totalDownload / 1000d);
        double avaTotalRate = (testCases * dataSize) / (total / 1000d);
        writer.println("+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+");
        writer.println("Result Summary:");
        writer.printf("                       Cases: %-7d\n", testCases);
        writer.printf("               Data Size(MB): %-7d\n", dataSize);
        writer.printf("                   total(ms): %-7d\n", total);
        writer.printf("           Average total(ms): %-7.2f\n", avaTotal);
        writer.printf("    Average total rate(MB/s): %-7.2f\n", avaTotalRate);
        writer.printf("                Sum find(ms): %-7d\n", totalFind);
        writer.printf("            Average find(ms): %-7.2f\n", avaFind);
        writer.printf("            Sum download(ms): %-7d\n", totalDownload);
        writer.printf("        Average download(ms): %-7.2f\n", avaNet);
        writer.printf(" Average download rate(MB/s): %-7.2f\n", avaDownloadRate);
    }

    private void printFooter() {
        LocalDateTime endTime = LocalDateTime.now();
        writer.println("=========================================");
        writer.println("Start: " + startTime);
        writer.println("  End: " + endTime);
        writer.println("Read performance test report end. Generated by Cloudhub.");
        writer.flush();
    }
    private record Result(
            int index,
            long start,
            long end,
            long find,
            boolean error
    ) {
    }

}

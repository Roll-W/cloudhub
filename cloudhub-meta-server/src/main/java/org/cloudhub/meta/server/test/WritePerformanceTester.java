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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudhub.meta.server.service.file.FileUploadService;
import org.cloudhub.meta.fs.FileObjectUploadStatus;
import org.cloudhub.meta.server.service.file.FileDeleteService;
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
 * 写入性能测试
 *
 * @author RollW
 */
public class WritePerformanceTester {
    private final Logger logger = LoggerFactory.getLogger(WritePerformanceTester.class);

    private final PrintWriter writer;
    private final int testCases;
    private final int dataSize;
    private final FileUploadService fileUploadService;
    private final FileDeleteService fileDeleteService;
    private final List<Result> results = new ArrayList<>();
    private final List<FileDelete> fileDeletes = new ArrayList<>();
    private FileDelete fileDelete;
    private final LocalDateTime startTime = LocalDateTime.now();

    // if file delete service not null, enable auto-cleaning.
    public WritePerformanceTester(PrintWriter reportWriter, int testCases, int dataSize,
                                  FileUploadService fileUploadService,
                                  @Nullable FileDeleteService fileDeleteService) {
        this.testCases = testCases;
        this.dataSize = dataSize;
        this.fileUploadService = fileUploadService;
        this.fileDeleteService = fileDeleteService;
        this.writer = reportWriter;
    }

    public void startWriteTest() throws IOException {
        logger.info("Start write test with cases={};dataSize={}", testCases, dataSize);
        for (int i = 1; i <= testCases; i++) {
            logger.debug("Now writing file index={}", i);
            recordNext(i);
        }
        printResult();
    }

    private void printResult() {
        printHeader();
        long netSum = 0, ioSum = 0, totalSum = 0;
        for (Result result : results) {
            totalSum += result.total();
            netSum += result.tempToStore();
            ioSum += result.storeToLast();
            double totalRate = dataSize / (result.total() / 1000d);
            double exceptCalcRate = dataSize / ((result.total() - result.calc()) / 1000d);
            double netRate = dataSize / (result.tempToStore / 1000d);
            double ioRate = dataSize / (result.storeToLast / 1000d);
            printCase(result.caseIndex(), result.start(), result.calc(),
                    result.total(), result.tempToStore(), result.storeToLast(),
                    netRate, ioRate, totalRate, exceptCalcRate);

        }
        printResultSummary(totalSum, netSum, ioSum);
        printFooter();
        for (FileDelete delete : fileDeletes) {
            clean(delete.file, delete.fileId());
        }
        logger.info("Write test end.");
    }

    private void clean(File file, String fileId) {
        file.delete();
        if (fileDeleteService != null) {
            fileDeleteService.deleteFileCompletely(fileId);
        }
    }

    private File generateRandomFile() throws IOException {
        File file = new File(RandomStringUtils.randomAlphanumeric(30));
        RandomFileGenerator fileGenerator =
                new RandomFileGenerator(file, dataSize, 0);
        fileGenerator.generate();
        return file;
    }

    private void recordNext(int index) throws IOException {
        File randomFile = generateRandomFile();
        CountDownLatch latch = new CountDownLatch(1);
        InputStream stream = new FileInputStream(randomFile);
        long start = System.currentTimeMillis();
        fileUploadService.uploadFile(stream,
                new CallbackRecorder(index, start, randomFile, latch));
        try {
            latch.await();
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
        try {
            stream.close();
        } catch (IOException ignored) {

        }
        if (fileDelete != null) {
            clean(fileDelete.file(), fileDelete.fileId());
        }
    }

    private class CallbackRecorder implements FileUploadStatusDataCallback {
        private final int caseIndex;
        private final long start;
        private final File randomFile;
        private String fileId;
        private final CountDownLatch latch;

        long calc, total, temp, store, tempToStore, storeToLast;

        private CallbackRecorder(int caseIndex, long start,
                                 File randomFile, CountDownLatch latch) {
            this.caseIndex = caseIndex;
            this.start = start;
            this.randomFile = randomFile;
            this.latch = latch;
        }

        @Override
        public void onNextStatus(FileObjectUploadStatus status) {
            if (status == FileObjectUploadStatus.TEMPORARY) {
                temp = System.currentTimeMillis();
                calc = temp - start;
                return;
            }
            if (status == FileObjectUploadStatus.STORING) {
                store = System.currentTimeMillis();
                return;
            }

            if (status.isLastStatus()) {
                long end = System.currentTimeMillis();
                total = end - start;
                tempToStore = store - temp;
                storeToLast = end - store;
                collectResult(caseIndex, start, end,
                        calc, total, tempToStore,
                        storeToLast, randomFile, fileId);
                latch.countDown();
            }
        }

        @Override
        public void onCalc(String fileId) {
            this.fileId = fileId;
        }
    }

    private void collectResult(int caseIndex, long start, long end,
                               long calc, long total,
                               long tempToStore, long storeToLast,
                               File randomFile, String fileId) {
        Result result = new Result(
                caseIndex, start, end,
                calc, total, tempToStore, storeToLast);
        results.add(result);
        fileDelete = new FileDelete(randomFile, fileId);
        fileDeletes.add(fileDelete);
    }


    private record Result(
            int caseIndex,
            long start, long end,
            long calc, long total,
            long tempToStore, long storeToLast) {
    }

    private record FileDelete(
            File file,
            String fileId) {

    }

    private void printHeader() {
        writer.println("Start generate write performance report at " + startTime + ".");
        writer.println("Note: - total time not contains the random file generate time.");
        writer.println("      - temp->store: file-server initialization time.");
        writer.println("      - store->last: file-server storage time.");
        writer.println();
        writer.printf("Result table. Cases=%d, DataSize=%d(MB)\n", testCases, dataSize);
        writer.println("=========================================");
        writer.println("case  start(timestamp)  calc(ms)  total(ms)  temp->store(ms)  store->last(ms)  total(MB/s)  exceptCalc(MB/s)  net(MB/s)  storage(MB/s)");
    }

    private void printCase(int caseIndex, long start, long calc, long total,
                           long tempToStore, long storeToLast,
                           double netRate, double ioRate,
                           double totalRate, double exceptCalcTotalRate) {
        writer.printf("%4d  %16d  %8d  %9d  %15d  %15d  %11.2f  %16.2f  %9.2f  %13.2f\n",
                caseIndex, start, calc, total, tempToStore, storeToLast, totalRate,
                exceptCalcTotalRate, netRate, ioRate);
    }

    private void printResultSummary(long total, long netSum,
                                    long ioSum) {
        double avaTotal = total * 1.0 / testCases;
        double avaNet = netSum * 1.0 / testCases;
        double avaIo = ioSum * 1.0 / testCases;
        double avaTotalRate = (testCases * dataSize) / (total / 1000d);
        double avaNetRate = (testCases * dataSize) / (netSum / 1000d);
        double avaIoRate = (testCases * dataSize) / (ioSum / 1000d);
        writer.println("+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+");
        writer.println("Result Summary:");
        writer.printf("                    Cases: %-7d\n", testCases);
        writer.printf("            Data Size(MB): %-7d\n", dataSize);
        writer.printf("                total(ms): %-7d\n", total);
        writer.printf("        Average total(ms): %-7.2f\n", avaTotal);
        writer.printf(" Average total rate(MB/s): %-7.2f\n", avaTotalRate);
        writer.printf("              Sum net(ms): %-7d\n", netSum);
        writer.printf("          Average net(ms): %-7.2f\n", avaNet);
        writer.printf("   Average net rate(MB/s): %-7.2f\n", avaNetRate);
        writer.printf("               Sum io(ms): %-7d\n", ioSum);
        writer.printf("           Average io(ms): %-7.2f\n", avaIo);
        writer.printf("    Average io rate(MB/s): %-7.2f\n", avaIoRate);
    }

    private void printFooter() {
        LocalDateTime endTime = LocalDateTime.now();
        writer.println("=========================================");
        writer.println("Start: " + startTime);
        writer.println("  End: " + endTime);
        writer.println("Write performance test report end. Generated by Cloudhub.");
    }
}

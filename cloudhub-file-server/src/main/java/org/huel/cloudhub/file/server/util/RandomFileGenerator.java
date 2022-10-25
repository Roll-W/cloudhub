package org.huel.cloudhub.file.server.util;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 生成一个指定大小的随机字符串文件，测试用。
 *
 * @author RollW
 */
public final class RandomFileGenerator {
    // 用法：
    // --size: 指定生成文件的大小（单位mb）
    // --add-bytes: 附加的字节大小
    // --path: 生成文件路径

    // --size=15 --add-bytes=2000 --path=test1
    // 生成15mb + 2000b的随机字符串到test1文件

    private static final String DEFAULT_OUTPUT = "RANDOM_FILE";
    private static final int DEFAULT_SIZE = 20; // mb

    private static final String ARG_SIZE = "size";
    private static final String ARG_PATH = "path";
    private static final String ARG_ADD_BYTES = "add-bytes";

    public static void main(String[] args) throws IOException {
        ApplicationArguments applicationArguments =
                new DefaultApplicationArguments(args);
        final int size = firstInt(
                applicationArguments.getOptionValues(ARG_SIZE), DEFAULT_SIZE);
        final int addBytes = firstInt(
                applicationArguments.getOptionValues(ARG_ADD_BYTES), 0);
        final String path = firstString(
                applicationArguments.getOptionValues(ARG_PATH), DEFAULT_OUTPUT);

        File file = new File(path);
        System.out.printf("start generate random file on '%s', size: %dmb, add-bytes: %db.\n",
                path, size, addBytes);
        file.createNewFile();

        BufferedOutputStream outputStream = new BufferedOutputStream(
                new FileOutputStream(file, false));
        for (int i = 0; i < size; i++) {
            outputStream.write(RandomStringUtils.randomAlphanumeric(1024 * 1024)
                    .getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        }

        outputStream.write(RandomStringUtils.randomAlphanumeric(addBytes)
                .getBytes(StandardCharsets.UTF_8));
        outputStream.flush();

        outputStream.close();
        System.out.println("generate complete.");
    }

    private static int firstInt(List<String> strings, int defaultValue) {
        if (strings == null || strings.isEmpty()) {
            return defaultValue;
        }
        return Integer.parseInt(
                strings.stream().findFirst().orElseThrow()
        );
    }

    private static String firstString(List<String> strings, String defaultValue) {
        if (strings == null || strings.isEmpty()) {
            return defaultValue;
        }
        return strings.stream().findFirst().orElseThrow();
    }
}
